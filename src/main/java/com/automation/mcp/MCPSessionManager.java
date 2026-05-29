package com.automation.mcp;

import com.automation.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread-safe singleton that manages one MCPSeleniumClient per JVM session.
 *
 * Startup sequence enforced here:
 *   1. start()        — launches `npx -y @angiejones/mcp-selenium@latest`
 *   2. handshake      — MCP initialize / initialized
 *   3. startBrowser() — opens the configured browser (Chrome by default)
 *
 * The browser must be started before any tool calls that target elements.
 * Call shutdown() (or let the JVM shutdown hook do it) to close both the
 * browser session and the Node.js subprocess cleanly.
 */
public class MCPSessionManager {

    private static final Logger log = LogManager.getLogger(MCPSessionManager.class);
    private static volatile MCPSessionManager instance;

    private MCPSeleniumClient client;
    private boolean initialized = false;

    private MCPSessionManager() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "mcp-shutdown"));
    }

    public static MCPSessionManager getInstance() {
        if (instance == null) {
            synchronized (MCPSessionManager.class) {
                if (instance == null) instance = new MCPSessionManager();
            }
        }
        return instance;
    }

    /**
     * Returns the active client, starting and initialising it on first call.
     * Throws if mcp.enabled=false in configuration.
     */
    public synchronized MCPSeleniumClient getClient() {
        if (!initialized) {
            ConfigManager config = ConfigManager.getInstance();
            if (!config.isMcpEnabled()) {
                throw new IllegalStateException(
                    "MCP is disabled. Set mcp.enabled=true in application.properties.");
            }
            try {
                client = new MCPSeleniumClient();
                client.start();                             // Node.js process + handshake

                String browser   = config.getBrowser();    // chrome / firefox / edge / safari
                boolean headless = config.isHeadless();
                client.startBrowser(browser, headless);    // open the browser window

                initialized = true;
                log.info("MCPSeleniumClient ready — {} (headless={})", browser, headless);
            } catch (Exception e) {
                throw new RuntimeException("Failed to start MCPSeleniumClient", e);
            }
        }
        return client;
    }

    /** Close the browser session and kill the Node.js subprocess. */
    public synchronized void shutdown() {
        if (initialized && client != null) {
            try { client.closeBrowser(); } catch (Exception ignored) {}
            client.stop();
            initialized = false;
            log.info("MCPSeleniumClient shut down");
        }
    }

    public boolean isActive() { return initialized; }
}
