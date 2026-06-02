package com.automation.mcp;

import com.automation.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java client for @angiejones/mcp-selenium (v0.2.3+).
 * https://github.com/angiejones/mcp-selenium
 *
 * Fixed bugs in this version:
 *   1. Windows ProcessBuilder — npx must be invoked via "cmd.exe /c npx" on Windows
 *      because npx is a .cmd batch script, not a native executable.
 *   2. Stderr consumer — a daemon thread drains the subprocess stderr pipe so it
 *      never deadlocks (OS pipe buffer is typically only 64 KB).
 *   3. Startup delay — Node.js needs ~1-2 s to load the module before it can
 *      respond to JSON-RPC messages; the handshake now waits for readiness.
 *   4. Removed MCP_TRANSPORT env var — not used by mcp-selenium, was noise.
 *
 * Lifecycle:
 *   1. start()          — launches `npx -y @angiejones/mcp-selenium@latest`, completes handshake
 *   2. startBrowser()   — opens the browser  ← must call before any element tools
 *   3. ... use tools ...
 *   4. closeBrowser()   — closes the browser session
 *   5. stop()           — kills the Node.js process
 */
public class MCPSeleniumClient {

    private static final Logger log = LogManager.getLogger(MCPSeleniumClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /** Maximum ms to wait for Node.js to become ready before sending the handshake. */
    private static final long STARTUP_WAIT_MS = 2_500;

    /** Maximum ms to wait for any individual JSON-RPC response. */
    private static final long REQUEST_TIMEOUT_MS = 30_000;

    private final ConfigManager config = ConfigManager.getInstance();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<MCPMessage>> pending =
        new ConcurrentHashMap<>();

    private Process   mcpProcess;
    private PrintWriter stdin;
    private Thread    readerThread;
    private Thread    stderrThread;
    private volatile boolean running = false;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    /**
     * Launch the Node.js MCP server subprocess and complete the MCP handshake.
     * Safe to call on Windows, macOS, and Linux.
     */
    public void start() throws IOException {
        log.info("Starting mcp-selenium server...");

        ProcessBuilder pb = new ProcessBuilder(buildNpxCommand());
        pb.redirectErrorStream(false);   // keep stderr separate so we can drain it

        mcpProcess = pb.start();
        running    = true;
        log.info("mcp-selenium process started (PID: {})", mcpProcess.pid());

        // ── Drain stderr (BUG FIX #2) ─────────────────────────────────────────
        // If stderr is never read, the OS pipe buffer fills (~64 KB) and the
        // subprocess blocks indefinitely. We consume it in a daemon thread and
        // log every line at DEBUG so diagnostics remain accessible.
        stderrThread = new Thread(() -> {
            try (BufferedReader err = new BufferedReader(
                    new InputStreamReader(mcpProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = err.readLine()) != null) {
                    log.debug("[mcp-stderr] {}", line);
                }
            } catch (IOException ignored) {}
        }, "mcp-stderr");
        stderrThread.setDaemon(true);
        stderrThread.start();

        // ── stdin writer ──────────────────────────────────────────────────────
        stdin = new PrintWriter(
            new OutputStreamWriter(mcpProcess.getOutputStream(), StandardCharsets.UTF_8), true);

        // ── stdout reader ─────────────────────────────────────────────────────
        readerThread = new Thread(this::readLoop, "mcp-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        // ── Wait for Node.js to finish loading (BUG FIX #3) ─────────────────
        // Node.js needs ~1-2 s to resolve and load @angiejones/mcp-selenium before
        // it can handle JSON-RPC messages. Sending the handshake too early causes
        // a 30-second timeout.  We poll the process-alive state and pause briefly.
        waitForProcessReady();

        // ── MCP handshake ─────────────────────────────────────────────────────
        handshake();
        log.info("mcp-selenium ready");
    }

    /**
     * Build the platform-correct npx command.
     *
     * On Windows, npx is npx.cmd — a batch script. ProcessBuilder does NOT
     * invoke the shell, so running "npx" directly fails with:
     *   IOException: Cannot run program "npx": CreateProcess error=2
     * The fix is to invoke cmd.exe /c which does resolve .cmd extensions.
     */
    private List<String> buildNpxCommand() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String pkg = "@angiejones/mcp-selenium@latest";

        if (os.contains("windows")) {
            // cmd /c ensures Windows resolves npx.cmd from PATH
            log.debug("Detected Windows — using cmd.exe /c npx");
            return List.of("cmd.exe", "/c", "npx", "-y", pkg);
        }

        // macOS / Linux: npx is a plain shell script, directly executable
        log.debug("Detected Unix — using npx directly");
        return List.of("npx", "-y", pkg);
    }

    /** Poll until the Node.js process is alive and has had time to load its module. */
    private void waitForProcessReady() {
        long deadline = System.currentTimeMillis() + STARTUP_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!mcpProcess.isAlive()) {
                throw new RuntimeException(
                    "mcp-selenium process exited immediately with code "
                    + mcpProcess.exitValue()
                    + ". Check that Node.js >= 18 and npx are installed.");
            }
            try { Thread.sleep(200); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.debug("Startup wait complete — sending MCP handshake");
    }

    /**
     * Open a browser window using the system-installed binary.
     * MUST be called after start() and before any interact/navigate calls.
     *
     * @param browser  "edge" | "chrome" | "firefox" | "safari"
     * @param headless true for headless/CI execution
     */
    public MCPMessage startBrowser(String browser, boolean headless) {
        Map<String, Object> options = new HashMap<>();
        options.put("headless", headless);

        // Edge-specific: suppress first-run wizard and default-browser nag
        if ("edge".equalsIgnoreCase(browser)) {
            options.put("arguments", List.of(
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-features=msEdgeEnableNurturingFramework"
            ));
        }

        log.info("Opening {} browser (headless={})", browser, headless);
        return callTool("start_browser", mapOf(
            "browser", browser.toLowerCase(Locale.ROOT),
            "options", options
        ));
    }

    /** Convenience overload — reads browser and headless from ConfigManager. */
    public MCPMessage startBrowser() {
        return startBrowser(config.getBrowser(), config.isHeadless());
    }

    /** Close the active browser session (does NOT kill the Node.js process). */
    public MCPMessage closeBrowser() {
        return callTool("close_session", Map.of());
    }

    /** Kill the Node.js subprocess and clean up threads. */
    public void stop() {
        running = false;
        if (mcpProcess != null) {
            mcpProcess.destroyForcibly();
        }
        log.info("mcp-selenium stopped");
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    public MCPMessage navigate(String url) {
        return callTool("navigate", mapOf("url", url));
    }

    // ── Element Interaction ────────────────────────────────────────────────────

    /**
     * @param by    "id" | "css" | "xpath" | "name" | "tag" | "class"
     * @param value the locator string (e.g. "#submit-btn", "//button[@type='submit']")
     */
    public MCPMessage click(String by, String value) {
        return interact("click", by, value, 10_000);
    }

    public MCPMessage doubleClick(String by, String value) {
        return interact("doubleclick", by, value, 10_000);
    }

    public MCPMessage rightClick(String by, String value) {
        return interact("rightclick", by, value, 10_000);
    }

    public MCPMessage hover(String by, String value) {
        return interact("hover", by, value, 10_000);
    }

    public MCPMessage interact(String action, String by, String value, int timeoutMs) {
        return callTool("interact", mapOf(
            "action",  action,
            "by",      by,
            "value",   value,
            "timeout", timeoutMs
        ));
    }

    /** Type text into an element (clears the field first). */
    public MCPMessage sendKeys(String by, String value, String text) {
        return sendKeys(by, value, text, 10_000);
    }

    public MCPMessage sendKeys(String by, String value, String text, int timeoutMs) {
        return callTool("send_keys", mapOf(
            "by",      by,
            "value",   value,
            "text",    text,
            "timeout", timeoutMs
        ));
    }

    /** Press a keyboard key (Enter, Tab, Escape, Space, Backspace, ArrowDown, …). */
    public MCPMessage pressKey(String key) {
        return callTool("press_key", mapOf("key", key));
    }

    /** Upload a file via a file-input element. filePath must be absolute. */
    public MCPMessage uploadFile(String by, String value, String filePath) {
        return callTool("upload_file", mapOf(
            "by",       by,
            "value",    value,
            "filePath", filePath
        ));
    }

    // ── Element Inspection ─────────────────────────────────────────────────────

    public MCPMessage getElementText(String by, String value) {
        return getElementText(by, value, 10_000);
    }

    public MCPMessage getElementText(String by, String value, int timeoutMs) {
        return callTool("get_element_text", mapOf(
            "by",      by,
            "value",   value,
            "timeout", timeoutMs
        ));
    }

    public MCPMessage getElementAttribute(String by, String value, String attribute) {
        return callTool("get_element_attribute", mapOf(
            "by",        by,
            "value",     value,
            "attribute", attribute
        ));
    }

    // ── Screenshot & Script ────────────────────────────────────────────────────

    /** Take a screenshot — result contains base64-encoded PNG. */
    public MCPMessage takeScreenshot() {
        return callTool("take_screenshot", Map.of());
    }

    /** Take a screenshot and save to an absolute file path. */
    public MCPMessage takeScreenshot(String outputPath) {
        return callTool("take_screenshot", mapOf("outputPath", outputPath));
    }

    public MCPMessage executeScript(String script, Object... args) {
        Map<String, Object> p = new HashMap<>();
        p.put("script", script);
        if (args.length > 0) p.put("args", Arrays.asList(args));
        return callTool("execute_script", p);
    }

    // ── Convenience wrappers (use execute_script) ──────────────────────────────

    public String getPageTitle()  { return extractText(executeScript("return document.title")); }
    public String getCurrentUrl() { return extractText(executeScript("return window.location.href")); }
    public String getPageSource() { return extractText(executeScript("return document.documentElement.outerHTML")); }

    public int countElements(String cssSelector) {
        try {
            return Integer.parseInt(extractText(
                executeScript("return document.querySelectorAll(arguments[0]).length", cssSelector)).trim());
        } catch (NumberFormatException e) { return 0; }
    }

    public boolean isElementPresent(String by, String value) {
        try { return !getElementText(by, value, 2_000).isError(); }
        catch (Exception e) { return false; }
    }

    // ── Window / Tab ───────────────────────────────────────────────────────────

    public MCPMessage listWindows()              { return callTool("window", mapOf("action", "list")); }
    public MCPMessage switchWindow(String h)     { return callTool("window", mapOf("action", "switch", "handle", h)); }
    public MCPMessage switchToLatestWindow()     { return callTool("window", mapOf("action", "switch_latest")); }
    public MCPMessage closeWindow()              { return callTool("window", mapOf("action", "close")); }

    // ── Frame ──────────────────────────────────────────────────────────────────

    public MCPMessage switchToFrame(String by, String value) {
        return callTool("frame", mapOf("action", "switch", "by", by, "value", value));
    }

    public MCPMessage switchToFrameByIndex(int index) {
        return callTool("frame", mapOf("action", "switch", "index", index));
    }

    public MCPMessage switchToDefaultContent() {
        return callTool("frame", mapOf("action", "default"));
    }

    // ── Alert ──────────────────────────────────────────────────────────────────

    public MCPMessage acceptAlert()            { return callTool("alert", mapOf("action", "accept")); }
    public MCPMessage dismissAlert()           { return callTool("alert", mapOf("action", "dismiss")); }
    public MCPMessage getAlertText()           { return callTool("alert", mapOf("action", "get_text")); }
    public MCPMessage sendAlertText(String t)  { return callTool("alert", mapOf("action", "send_text", "text", t)); }

    // ── Cookies ────────────────────────────────────────────────────────────────

    public MCPMessage addCookie(String name, String value) {
        return callTool("add_cookie", mapOf("name", name, "value", value));
    }

    public MCPMessage addCookie(String name, String value, String domain, String path,
                                boolean secure, boolean httpOnly) {
        Map<String, Object> p = new HashMap<>();
        p.put("name", name);   p.put("value", value);
        if (domain != null) p.put("domain", domain);
        if (path   != null) p.put("path",   path);
        p.put("secure", secure);  p.put("httpOnly", httpOnly);
        return callTool("add_cookie", p);
    }

    public MCPMessage getCookies()              { return callTool("get_cookies",    Map.of()); }
    public MCPMessage getCookie(String name)    { return callTool("get_cookies",    mapOf("name", name)); }
    public MCPMessage deleteCookie(String name) { return callTool("delete_cookie",  mapOf("name", name)); }
    public MCPMessage deleteAllCookies()        { return callTool("delete_cookie",  Map.of()); }

    // ── Diagnostics ────────────────────────────────────────────────────────────

    public MCPMessage getConsoleLogs(boolean clear)  { return callTool("diagnostics", mapOf("type", "console", "clear", clear)); }
    public MCPMessage getBrowserErrors(boolean clear) { return callTool("diagnostics", mapOf("type", "errors",  "clear", clear)); }
    public MCPMessage getNetworkLogs(boolean clear)  { return callTool("diagnostics", mapOf("type", "network", "clear", clear)); }

    // ── Resources (read-only) ──────────────────────────────────────────────────

    /** "Active session: <id>" or "No active browser session". */
    public MCPMessage getBrowserStatus()    { return readResource("browser-status://current"); }

    /** Page accessibility tree as JSON — useful for AI agents to understand layout. */
    public MCPMessage getAccessibilityTree() { return readResource("accessibility://current"); }

    // ── JSON-RPC core ──────────────────────────────────────────────────────────

    private MCPMessage callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> params = new HashMap<>();
        params.put("name",      toolName);
        params.put("arguments", arguments);
        log.debug("MCP tool → {}", toolName);
        return sendRequest("tools/call", params);
    }

    private MCPMessage readResource(String uri) {
        return sendRequest("resources/read", mapOf("uri", uri));
    }

    private MCPMessage sendRequest(String method, Object params) {
        int id = idCounter.getAndIncrement();
        CompletableFuture<MCPMessage> future = new CompletableFuture<>();
        pending.put(id, future);
        writeMessage(MCPMessage.request(id, method, params));
        try {
            MCPMessage response = future.get(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (response.isError()) {
                log.error("MCP error [{}] {}: {}",
                    response.getError().getCode(), method, response.getError().getMessage());
            }
            return response;
        } catch (Exception e) {
            pending.remove(id);
            throw new RuntimeException("MCP request failed [" + method + "]: " + e.getMessage(), e);
        }
    }

    private void sendNotification(String method, Object params) {
        writeMessage(MCPMessage.notification(method, params));
    }

    private synchronized void writeMessage(MCPMessage msg) {
        try {
            String json = mapper.writeValueAsString(msg);
            log.trace("MCP >> {}", json);
            stdin.println(json);
            stdin.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize MCP message: " + e.getMessage(), e);
        }
    }

    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(mcpProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                log.trace("MCP << {}", trimmed);
                handleIncoming(trimmed);
            }
        } catch (IOException e) {
            if (running) log.error("MCP reader stopped: {}", e.getMessage());
        } finally {
            // Complete any pending futures that are still waiting — prevents 30-s hangs
            // if the process dies unexpectedly.
            pending.forEach((id, future) ->
                future.completeExceptionally(new IOException("mcp-selenium process terminated")));
            pending.clear();
        }
    }

    private void handleIncoming(String json) {
        try {
            MCPMessage msg = mapper.readValue(json, MCPMessage.class);
            if (msg.getId() != null) {
                CompletableFuture<MCPMessage> future = pending.remove(msg.getId());
                if (future != null) {
                    future.complete(msg);
                } else {
                    log.warn("No pending request for response id={}", msg.getId());
                }
            }
            // Server-initiated notifications (no id) are intentionally ignored
        } catch (Exception e) {
            log.warn("Failed to parse MCP message '{}': {}", json, e.getMessage());
        }
    }

    // ── MCP Handshake ──────────────────────────────────────────────────────────

    private void handshake() {
        log.debug("Sending MCP initialize...");
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("clientInfo",  mapOf("name", "java-selenium-bdd", "version", "1.0.0"));
        params.put("capabilities", Map.of());

        MCPMessage response = sendRequest("initialize", params);
        if (response.isError()) {
            throw new RuntimeException(
                "MCP handshake failed: " + response.getError().getMessage());
        }
        log.debug("MCP initialize OK — sending initialized notification");
        sendNotification("notifications/initialized", null);
        log.info("MCP handshake complete");
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    /** Extract text from a tools/call result: {content:[{type:"text",text:"..."}]}. */
    private String extractText(MCPMessage msg) {
        if (msg == null || msg.getResult() == null) return "";
        try {
            JsonNode node = mapper.valueToTree(msg.getResult());
            if (node.has("content")) {
                JsonNode first = node.path("content").get(0);
                if (first != null && first.has("text")) return first.path("text").asText();
            }
            return node.asText();
        } catch (Exception e) {
            return msg.getResult().toString();
        }
    }

    /**
     * Mutable Map.of replacement that accepts an arbitrary number of key-value pairs.
     * Map.of() is immutable; some methods need to add further entries after creation.
     */
    @SuppressWarnings("unchecked")
    private static <V> Map<String, V> mapOf(Object... pairs) {
        Map<String, V> m = new HashMap<>(pairs.length / 2);
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((String) pairs[i], (V) pairs[i + 1]);
        }
        return m;
    }
}
