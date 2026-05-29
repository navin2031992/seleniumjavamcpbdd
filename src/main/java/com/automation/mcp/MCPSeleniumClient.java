package com.automation.mcp;

import com.automation.config.ConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java client for the mcp-selenium Node.js MCP server.
 * (https://github.com/angiejones/mcp-selenium)
 *
 * Lifecycle:
 *   1. start()  — launches `npx -y @angiejones/mcp-selenium` as a subprocess
 *   2. initialize() — MCP handshake
 *   3. call tool methods (navigate, click, type, etc.)
 *   4. stop()   — kills the subprocess
 *
 * Communication: JSON-RPC 2.0 over the subprocess's stdin/stdout.
 */
public class MCPSeleniumClient {

    private static final Logger log = LogManager.getLogger(MCPSeleniumClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ConfigManager config = ConfigManager.getInstance();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<MCPMessage>> pending = new ConcurrentHashMap<>();

    private Process mcpProcess;
    private PrintWriter stdinWriter;
    private Thread readerThread;
    private volatile boolean running = false;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    public void start() throws IOException {
        log.info("Starting mcp-selenium server process...");

        // Resolve npx from the configured node path directory so it works on any OS.
        // On Windows npx lives next to node.exe; on Linux/macOS it is on PATH.
        String nodePath = config.getMcpNodePath();  // e.g. "node" or "/usr/local/bin/node"
        String npx = nodePath.equals("node") ? "npx"
            : nodePath.replace("node", "npx").replace("node.exe", "npx.cmd");

        ProcessBuilder pb = new ProcessBuilder(npx, "-y", "@angiejones/mcp-selenium");
        pb.environment().put("MCP_TRANSPORT", "stdio");
        pb.redirectErrorStream(false);
        mcpProcess = pb.start();
        running = true;

        stdinWriter = new PrintWriter(
            new OutputStreamWriter(mcpProcess.getOutputStream(), StandardCharsets.UTF_8), true);

        // Background thread reads responses from stdout
        readerThread = new Thread(this::readLoop, "mcp-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        log.info("mcp-selenium server started (PID: {})", mcpProcess.pid());
        initialize();
    }

    public void stop() {
        running = false;
        if (mcpProcess != null) {
            mcpProcess.destroyForcibly();
            log.info("mcp-selenium server stopped");
        }
    }

    // ── MCP Handshake ──────────────────────────────────────────────────────────

    private void initialize() {
        try {
            Map<String, Object> clientInfo = new HashMap<>();
            clientInfo.put("name", "java-selenium-bdd");
            clientInfo.put("version", "1.0.0");

            Map<String, Object> params = new HashMap<>();
            params.put("protocolVersion", "2024-11-05");
            params.put("clientInfo", clientInfo);
            params.put("capabilities", Map.of());

            MCPMessage response = sendRequest("initialize", params);
            if (response.isError()) {
                throw new RuntimeException("MCP initialize failed: " + response.getError().getMessage());
            }

            // Send initialized notification (no response expected)
            sendNotification("notifications/initialized", null);
            log.info("MCP handshake complete");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MCP connection", e);
        }
    }

    // ── Selenium Tool Methods ──────────────────────────────────────────────────

    /**
     * Navigate the browser to a URL.
     */
    public MCPMessage navigate(String url) {
        return callTool("navigate", Map.of("url", url));
    }

    /**
     * Click an element identified by a CSS or XPath selector.
     */
    public MCPMessage click(String selector) {
        return callTool("click", Map.of("selector", selector));
    }

    /**
     * Type text into an input element.
     */
    public MCPMessage type(String selector, String text) {
        return callTool("fill", Map.of("selector", selector, "value", text));
    }

    /**
     * Take a screenshot and return base64-encoded PNG.
     */
    public MCPMessage screenshot() {
        return callTool("screenshot", Map.of());
    }

    /**
     * Get full page HTML source.
     */
    public MCPMessage getPageSource() {
        return callTool("getPageSource", Map.of());
    }

    /**
     * Get the current page title.
     */
    public MCPMessage getTitle() {
        return callTool("getTitle", Map.of());
    }

    /**
     * Get the current URL.
     */
    public MCPMessage getCurrentUrl() {
        return callTool("getCurrentUrl", Map.of());
    }

    /**
     * Find elements matching a CSS selector; returns count and attributes.
     */
    public MCPMessage findElements(String selector) {
        return callTool("querySelector", Map.of("selector", selector));
    }

    /**
     * Execute arbitrary JavaScript in the browser context.
     */
    public MCPMessage executeScript(String script, Object... args) {
        return callTool("executeScript", Map.of("script", script, "args", args));
    }

    /**
     * Hover over an element.
     */
    public MCPMessage hover(String selector) {
        return callTool("hover", Map.of("selector", selector));
    }

    /**
     * Select an option from a <select> element.
     */
    public MCPMessage selectOption(String selector, String value) {
        return callTool("selectOption", Map.of("selector", selector, "value", value));
    }

    /**
     * Wait for an element to appear (up to timeoutMs).
     */
    public MCPMessage waitForElement(String selector, int timeoutMs) {
        return callTool("waitForElement", Map.of("selector", selector, "timeout", timeoutMs));
    }

    /**
     * Get attribute value of an element.
     */
    public MCPMessage getAttribute(String selector, String attribute) {
        return callTool("getAttribute", Map.of("selector", selector, "attribute", attribute));
    }

    /**
     * Get inner text of an element.
     */
    public MCPMessage getText(String selector) {
        return callTool("getText", Map.of("selector", selector));
    }

    /**
     * Submit a form.
     */
    public MCPMessage submitForm(String selector) {
        return callTool("submit", Map.of("selector", selector));
    }

    /**
     * Press a key (Enter, Tab, Escape, etc.).
     */
    public MCPMessage pressKey(String selector, String key) {
        return callTool("pressKey", Map.of("selector", selector, "key", key));
    }

    /**
     * Clear an input field.
     */
    public MCPMessage clearField(String selector) {
        return callTool("clearField", Map.of("selector", selector));
    }

    /**
     * Check or uncheck a checkbox.
     */
    public MCPMessage setCheckbox(String selector, boolean checked) {
        return callTool("setCheckbox", Map.of("selector", selector, "checked", checked));
    }

    /**
     * Switch to an iframe by selector or index.
     */
    public MCPMessage switchToFrame(String selector) {
        return callTool("switchToFrame", Map.of("selector", selector));
    }

    /**
     * Switch back to the default content from an iframe.
     */
    public MCPMessage switchToDefaultContent() {
        return callTool("switchToDefaultContent", Map.of());
    }

    /**
     * Scroll element into view.
     */
    public MCPMessage scrollToElement(String selector) {
        return callTool("scrollToElement", Map.of("selector", selector));
    }

    // ── JSON-RPC core ──────────────────────────────────────────────────────────

    private MCPMessage callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);
        return sendRequest("tools/call", params);
    }

    private MCPMessage sendRequest(String method, Object params) {
        int id = idCounter.getAndIncrement();
        CompletableFuture<MCPMessage> future = new CompletableFuture<>();
        pending.put(id, future);

        MCPMessage msg = MCPMessage.request(id, method, params);
        writeMessage(msg);

        try {
            MCPMessage response = future.get(30, TimeUnit.SECONDS);
            if (response.isError()) {
                log.error("MCP error [{}]: {}", response.getError().getCode(), response.getError().getMessage());
            }
            return response;
        } catch (Exception e) {
            pending.remove(id);
            throw new RuntimeException("MCP request timed out or failed: " + method, e);
        }
    }

    private void sendNotification(String method, Object params) {
        writeMessage(MCPMessage.notification(method, params));
    }

    private void writeMessage(MCPMessage msg) {
        try {
            String json = mapper.writeValueAsString(msg);
            log.debug("MCP >> {}", json);
            stdinWriter.println(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize MCP message", e);
        }
    }

    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(mcpProcess.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while (running && (line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                log.debug("MCP << {}", line);
                handleMessage(line);
            }
        } catch (IOException e) {
            if (running) log.error("MCP read error: {}", e.getMessage());
        }
    }

    private void handleMessage(String json) {
        try {
            MCPMessage msg = mapper.readValue(json, MCPMessage.class);
            if (msg.getId() != null) {
                CompletableFuture<MCPMessage> future = pending.remove(msg.getId());
                if (future != null) future.complete(msg);
            }
            // Notifications (no id) are logged only
        } catch (Exception e) {
            log.warn("Failed to parse MCP message: {}", e.getMessage());
        }
    }
}
