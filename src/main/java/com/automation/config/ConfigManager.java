package com.automation.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration manager.
 * Loads base application.properties then overlays environment-specific
 * (dev/staging/prod) values. All runtime settings flow through here.
 */
public class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);
    private static volatile ConfigManager instance;
    private final Properties props = new Properties();

    private ConfigManager() {
        loadProperties("config/application.properties");
        String env = System.getProperty("env", System.getenv().getOrDefault("TEST_ENV", "dev"));
        loadProperties("config/" + env + ".properties");
        log.info("ConfigManager initialised for environment: {}", env);
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) instance = new ConfigManager();
            }
        }
        return instance;
    }

    private void loadProperties(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                props.load(is);
                log.debug("Loaded properties from: {}", path);
            } else {
                log.warn("Properties file not found: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to load properties from: {}", path, e);
        }
    }

    public String get(String key) {
        return System.getProperty(key, props.getProperty(key, ""));
    }

    public String get(String key, String defaultValue) {
        String value = System.getProperty(key, props.getProperty(key));
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value.isEmpty() ? defaultValue : Boolean.parseBoolean(value);
    }

    // Alias for Hooks.java compatibility
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        try {
            return value.isEmpty() ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String value = get(key);
        try {
            return value.isEmpty() ? defaultValue : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ── Convenience accessors ──────────────────────────────────────────────────

    public String getBaseUrl()         { return get("app.base.url"); }
    public String getBrowser()         { return get("browser", "chrome"); }
    public boolean isHeadless()        { return getBoolean("headless", false); }
    public String getGridUrl()         { return get("grid.url", ""); }
    public boolean useGrid()           { return !getGridUrl().isEmpty(); }

    public long getImplicitWait()      { return getLong("wait.implicit", 0); }
    public long getExplicitWait()      { return getLong("wait.explicit", 15); }
    public long getPageLoadTimeout()   { return getLong("wait.page.load", 30); }

    public String getJiraBaseUrl()     { return get("jira.base.url"); }
    public String getJiraProject()     { return get("jira.project.key"); }

    // Sensitive credentials: properties file → system property → environment variable
    public String getJiraUsername() {
        return getWithEnvFallback("jira.username", "JIRA_USERNAME");
    }
    public String getJiraApiToken() {
        return getWithEnvFallback("jira.api.token", "JIRA_API_TOKEN");
    }
    public String getAnthropicApiKey() {
        return getWithEnvFallback("anthropic.api.key", "ANTHROPIC_API_KEY");
    }

    public String getAnthropicModel()  { return get("anthropic.model", "claude-sonnet-4-6"); }

    private String getWithEnvFallback(String propKey, String envVar) {
        String value = get(propKey);
        if (value == null || value.isEmpty()) {
            value = System.getenv(envVar);
        }
        return value != null ? value : "";
    }

    public String getScreenshotDir()   { return get("screenshot.dir", "target/screenshots"); }
    public String getDownloadDir()     { return get("download.dir", "target/downloads"); }

    public boolean isMcpEnabled()      { return getBoolean("mcp.enabled", false); }
    public String getMcpNodePath()     { return get("mcp.node.path", "node"); }
}
