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
 * Real tools (18 total):
 *   Browser :  start_browser, navigate, close_session
 *   Interact:  interact, send_keys, press_key, upload_file
 *   Inspect :  get_element_text, get_element_attribute
 *   Capture :  take_screenshot, execute_script
 *   Windows :  window, frame, alert
 *   Cookies :  add_cookie, get_cookies, delete_cookie
 *   Debug   :  diagnostics
 *
 * Resources (read-only):
 *   browser-status://current  — active session ID or "No active browser session"
 *   accessibility://current   — page accessibility tree as JSON
 *
 * Locator strategies accepted by element-targeting tools:
 *   "id" | "css" | "xpath" | "name" | "tag" | "class"
 *
 * Lifecycle:
 *   1. new MCPSeleniumClient()
 *   2. start()          — launches `npx -y @angiejones/mcp-selenium@latest`
 *   3. startBrowser()   — opens the browser (must call BEFORE any interaction)
 *   4. ... use tools ...
 *   5. closeBrowser()   — closes the browser session
 *   6. stop()           — kills the Node.js process
 */
public class MCPSeleniumClient {

    private static final Logger log = LogManager.getLogger(MCPSeleniumClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ConfigManager config = ConfigManager.getInstance();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<MCPMessage>> pending =
        new ConcurrentHashMap<>();

    private Process mcpProcess;
    private PrintWriter stdin;
    private Thread readerThread;
    private volatile boolean running = false;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    /** Launch the Node.js MCP server subprocess and complete the MCP handshake. */
    public void start() throws IOException {
        log.info("Starting mcp-selenium server...");

        String nodePath = config.getMcpNodePath();
        String npx = nodePath.equals("node") ? "npx"
            : nodePath.replace("node", "npx").replace("node.exe", "npx.cmd");

        ProcessBuilder pb = new ProcessBuilder(npx, "-y", "@angiejones/mcp-selenium@latest");
        pb.environment().put("MCP_TRANSPORT", "stdio");
        pb.redirectErrorStream(false);

        mcpProcess = pb.start();
        running = true;

        stdin = new PrintWriter(
            new OutputStreamWriter(mcpProcess.getOutputStream(), StandardCharsets.UTF_8), true);

        readerThread = new Thread(this::readLoop, "mcp-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        handshake();
        log.info("mcp-selenium ready (PID: {})", mcpProcess.pid());
    }

    /** Open a browser window. Must be called before any interact/navigate calls. */
    public MCPMessage startBrowser(String browser, boolean headless) {
        Map<String, Object> options = new HashMap<>();
        options.put("headless", headless);

        return callTool("start_browser", Map.of(
            "browser", browser,
            "options", options
        ));
    }

    /** Close the active browser session (does NOT kill the Node.js process). */
    public MCPMessage closeBrowser() {
        return callTool("close_session", Map.of());
    }

    /** Kill the Node.js subprocess. */
    public void stop() {
        running = false;
        if (mcpProcess != null) {
            mcpProcess.destroyForcibly();
            log.info("mcp-selenium stopped");
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    /** Navigate the browser to a URL. */
    public MCPMessage navigate(String url) {
        return callTool("navigate", Map.of("url", url));
    }

    // ── Element Interaction ────────────────────────────────────────────────────

    /**
     * Click an element.
     * @param by    locator strategy: "id" | "css" | "xpath" | "name" | "tag" | "class"
     * @param value locator value (e.g. "#login-btn", "//button[@type='submit']")
     */
    public MCPMessage click(String by, String value) {
        return interact("click", by, value, 10000);
    }

    /** Double-click an element. */
    public MCPMessage doubleClick(String by, String value) {
        return interact("doubleclick", by, value, 10000);
    }

    /** Right-click an element (context menu). */
    public MCPMessage rightClick(String by, String value) {
        return interact("rightclick", by, value, 10000);
    }

    /** Hover over an element. */
    public MCPMessage hover(String by, String value) {
        return interact("hover", by, value, 10000);
    }

    /**
     * Perform a mouse action on an element.
     * @param action "click" | "doubleclick" | "rightclick" | "hover"
     */
    public MCPMessage interact(String action, String by, String value, int timeoutMs) {
        return callTool("interact", Map.of(
            "action", action,
            "by",     by,
            "value",  value,
            "timeout", timeoutMs
        ));
    }

    /**
     * Type text into an element (clears the field first).
     * @param by    locator strategy
     * @param value locator value
     * @param text  text to type
     */
    public MCPMessage sendKeys(String by, String value, String text) {
        return sendKeys(by, value, text, 10000);
    }

    public MCPMessage sendKeys(String by, String value, String text, int timeoutMs) {
        return callTool("send_keys", Map.of(
            "by",      by,
            "value",   value,
            "text",    text,
            "timeout", timeoutMs
        ));
    }

    /**
     * Press a keyboard key in the focused element.
     * Common keys: Enter, Tab, Escape, Space, Backspace, ArrowDown, ArrowUp, F5
     */
    public MCPMessage pressKey(String key) {
        return callTool("press_key", Map.of("key", key));
    }

    /**
     * Upload a file through a file input element.
     * @param filePath absolute path to the file on the local machine
     */
    public MCPMessage uploadFile(String by, String value, String filePath) {
        return callTool("upload_file", Map.of(
            "by",       by,
            "value",    value,
            "filePath", filePath
        ));
    }

    // ── Element Inspection ─────────────────────────────────────────────────────

    /** Get the visible text content of an element. */
    public MCPMessage getElementText(String by, String value) {
        return getElementText(by, value, 10000);
    }

    public MCPMessage getElementText(String by, String value, int timeoutMs) {
        return callTool("get_element_text", Map.of(
            "by",      by,
            "value",   value,
            "timeout", timeoutMs
        ));
    }

    /** Get the value of an HTML attribute on an element. */
    public MCPMessage getElementAttribute(String by, String value, String attribute) {
        return callTool("get_element_attribute", Map.of(
            "by",        by,
            "value",     value,
            "attribute", attribute
        ));
    }

    // ── Screenshot & Script ───────────────────────────────────────────────────

    /**
     * Take a screenshot.
     * @return MCPMessage whose result contains base64-encoded PNG data
     */
    public MCPMessage takeScreenshot() {
        return callTool("take_screenshot", Map.of());
    }

    /**
     * Take a screenshot and save it to a file.
     * @param outputPath absolute path to save the PNG file
     */
    public MCPMessage takeScreenshot(String outputPath) {
        return callTool("take_screenshot", Map.of("outputPath", outputPath));
    }

    /**
     * Execute arbitrary JavaScript in the browser context.
     * @param script JavaScript code string
     * @param args   optional arguments passed to the script as `arguments[0]`, `arguments[1]`, ...
     */
    public MCPMessage executeScript(String script, Object... args) {
        Map<String, Object> params = new HashMap<>();
        params.put("script", script);
        if (args.length > 0) params.put("args", Arrays.asList(args));
        return callTool("execute_script", params);
    }

    // ── Convenience wrappers using execute_script ─────────────────────────────

    /** Get the current page title. */
    public String getPageTitle() {
        MCPMessage result = executeScript("return document.title");
        return extractText(result);
    }

    /** Get the current page URL. */
    public String getCurrentUrl() {
        MCPMessage result = executeScript("return window.location.href");
        return extractText(result);
    }

    /** Get the full outer HTML of the page. */
    public String getPageSource() {
        MCPMessage result = executeScript("return document.documentElement.outerHTML");
        return extractText(result);
    }

    /** Count how many elements match a CSS selector. */
    public int countElements(String cssSelector) {
        MCPMessage result = executeScript(
            "return document.querySelectorAll(arguments[0]).length", cssSelector);
        try {
            return Integer.parseInt(extractText(result).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Check whether an element is present on the page. */
    public boolean isElementPresent(String by, String value) {
        try {
            MCPMessage result = getElementText(by, value, 2000);
            return !result.isError();
        } catch (Exception e) {
            return false;
        }
    }

    // ── Window / Tab management ────────────────────────────────────────────────

    /** List all open window handles. */
    public MCPMessage listWindows() {
        return callTool("window", Map.of("action", "list"));
    }

    /** Switch focus to a specific window handle. */
    public MCPMessage switchWindow(String handle) {
        return callTool("window", Map.of("action", "switch", "handle", handle));
    }

    /** Switch focus to the most recently opened window or tab. */
    public MCPMessage switchToLatestWindow() {
        return callTool("window", Map.of("action", "switch_latest"));
    }

    /** Close the current window or tab. */
    public MCPMessage closeWindow() {
        return callTool("window", Map.of("action", "close"));
    }

    // ── Frame management ───────────────────────────────────────────────────────

    /** Switch into an iframe identified by a locator. */
    public MCPMessage switchToFrame(String by, String value) {
        return callTool("frame", Map.of(
            "action", "switch",
            "by",     by,
            "value",  value
        ));
    }

    /** Switch into an iframe by its zero-based index. */
    public MCPMessage switchToFrameByIndex(int index) {
        return callTool("frame", Map.of("action", "switch", "index", index));
    }

    /** Return focus to the top-level document (exit all frames). */
    public MCPMessage switchToDefaultContent() {
        return callTool("frame", Map.of("action", "default"));
    }

    // ── Alert / Dialog handling ────────────────────────────────────────────────

    /** Accept the currently open alert/confirm/prompt dialog. */
    public MCPMessage acceptAlert() {
        return callTool("alert", Map.of("action", "accept"));
    }

    /** Dismiss (cancel) the currently open alert/confirm dialog. */
    public MCPMessage dismissAlert() {
        return callTool("alert", Map.of("action", "dismiss"));
    }

    /** Get the text of the currently open alert dialog. */
    public MCPMessage getAlertText() {
        return callTool("alert", Map.of("action", "get_text"));
    }

    /** Type text into a prompt dialog, then accept it. */
    public MCPMessage sendAlertText(String text) {
        return callTool("alert", Map.of("action", "send_text", "text", text));
    }

    // ── Cookie management ──────────────────────────────────────────────────────

    /** Add a cookie to the current session. */
    public MCPMessage addCookie(String name, String value) {
        return callTool("add_cookie", Map.of("name", name, "value", value));
    }

    /** Add a cookie with full options (domain, path, secure, httpOnly, expiry). */
    public MCPMessage addCookie(String name, String value, String domain, String path,
                                boolean secure, boolean httpOnly) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("value", value);
        if (domain != null) params.put("domain", domain);
        if (path != null)   params.put("path", path);
        params.put("secure", secure);
        params.put("httpOnly", httpOnly);
        return callTool("add_cookie", params);
    }

    /** Get all cookies for the current session. */
    public MCPMessage getCookies() {
        return callTool("get_cookies", Map.of());
    }

    /** Get a specific cookie by name. */
    public MCPMessage getCookie(String name) {
        return callTool("get_cookies", Map.of("name", name));
    }

    /** Delete a specific cookie by name. */
    public MCPMessage deleteCookie(String name) {
        return callTool("delete_cookie", Map.of("name", name));
    }

    /** Delete ALL cookies for the current session. */
    public MCPMessage deleteAllCookies() {
        return callTool("delete_cookie", Map.of());
    }

    // ── Diagnostics (requires WebDriver BiDi) ─────────────────────────────────

    /** Get captured browser console logs. @param clear whether to clear the buffer after reading */
    public MCPMessage getConsoleLogs(boolean clear) {
        return callTool("diagnostics", Map.of("type", "console", "clear", clear));
    }

    /** Get captured JavaScript errors. */
    public MCPMessage getBrowserErrors(boolean clear) {
        return callTool("diagnostics", Map.of("type", "errors", "clear", clear));
    }

    /** Get captured network requests/responses. */
    public MCPMessage getNetworkLogs(boolean clear) {
        return callTool("diagnostics", Map.of("type", "network", "clear", clear));
    }

    // ── Resources ─────────────────────────────────────────────────────────────

    /** Read the current browser session status resource. */
    public MCPMessage getBrowserStatus() {
        return readResource("browser-status://current");
    }

    /** Read the page accessibility tree as JSON (useful for AI agents). */
    public MCPMessage getAccessibilityTree() {
        return readResource("accessibility://current");
    }

    // ── JSON-RPC core ──────────────────────────────────────────────────────────

    private MCPMessage callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);
        log.debug("Calling MCP tool: {} {}", toolName, arguments);
        return sendRequest("tools/call", params);
    }

    private MCPMessage readResource(String uri) {
        return sendRequest("resources/read", Map.of("uri", uri));
    }

    private MCPMessage sendRequest(String method, Object params) {
        int id = idCounter.getAndIncrement();
        CompletableFuture<MCPMessage> future = new CompletableFuture<>();
        pending.put(id, future);
        writeMessage(MCPMessage.request(id, method, params));
        try {
            MCPMessage response = future.get(30, TimeUnit.SECONDS);
            if (response.isError()) {
                log.error("MCP error [{}]: {}", response.getError().getCode(),
                    response.getError().getMessage());
            }
            return response;
        } catch (Exception e) {
            pending.remove(id);
            throw new RuntimeException("MCP request failed: " + method, e);
        }
    }

    private void sendNotification(String method, Object params) {
        writeMessage(MCPMessage.notification(method, params));
    }

    private void writeMessage(MCPMessage msg) {
        try {
            String json = mapper.writeValueAsString(msg);
            log.trace("MCP >> {}", json);
            stdin.println(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write MCP message", e);
        }
    }

    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(mcpProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                log.trace("MCP << {}", line);
                handleIncoming(line);
            }
        } catch (IOException e) {
            if (running) log.error("MCP read error: {}", e.getMessage());
        }
    }

    private void handleIncoming(String json) {
        try {
            MCPMessage msg = mapper.readValue(json, MCPMessage.class);
            if (msg.getId() != null) {
                CompletableFuture<MCPMessage> future = pending.remove(msg.getId());
                if (future != null) future.complete(msg);
            }
        } catch (Exception e) {
            log.warn("Could not parse MCP message: {}", e.getMessage());
        }
    }

    // ── MCP Handshake ──────────────────────────────────────────────────────────

    private void handshake() {
        Map<String, Object> params = Map.of(
            "protocolVersion", "2024-11-05",
            "clientInfo", Map.of("name", "java-selenium-bdd", "version", "1.0.0"),
            "capabilities", Map.of()
        );
        MCPMessage response = sendRequest("initialize", params);
        if (response.isError()) {
            throw new RuntimeException("MCP handshake failed: " + response.getError().getMessage());
        }
        sendNotification("notifications/initialized", null);
        log.info("MCP handshake complete");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String extractText(MCPMessage msg) {
        if (msg == null || msg.getResult() == null) return "";
        try {
            JsonNode node = mapper.valueToTree(msg.getResult());
            // tools/call result: { content: [{ type: "text", text: "..." }] }
            if (node.has("content")) {
                JsonNode first = node.path("content").get(0);
                if (first != null && first.has("text")) return first.path("text").asText();
            }
            return node.asText();
        } catch (Exception e) {
            return msg.getResult().toString();
        }
    }
}
