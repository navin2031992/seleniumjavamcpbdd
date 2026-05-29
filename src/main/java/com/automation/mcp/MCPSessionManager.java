package com.automation.mcp;

import com.automation.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread-safe singleton that manages one MCPSeleniumClient per test session.
 * The client is started lazily on first access and stopped on shutdown.
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

    public synchronized MCPSeleniumClient getClient() {
        if (!initialized) {
            if (!ConfigManager.getInstance().isMcpEnabled()) {
                throw new IllegalStateException(
                    "MCP is disabled. Set mcp.enabled=true in application.properties to use MCPSeleniumClient.");
            }
            client = new MCPSeleniumClient();
            try {
                client.start();
                initialized = true;
                log.info("MCPSeleniumClient started successfully");
            } catch (Exception e) {
                throw new RuntimeException("Failed to start MCPSeleniumClient", e);
            }
        }
        return client;
    }

    public synchronized void shutdown() {
        if (initialized && client != null) {
            client.stop();
            initialized = false;
            log.info("MCPSeleniumClient stopped");
        }
    }

    public boolean isActive() { return initialized; }
}
