package com.automation.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe WebDriver factory and lifecycle manager.
 * Each thread gets its own driver instance via ThreadLocal —
 * safe for parallel Cucumber scenarios.
 */
public class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();
    private static final ConfigManager config = ConfigManager.getInstance();

    private DriverManager() {}

    public static WebDriver getDriver() {
        if (driverHolder.get() == null) {
            driverHolder.set(createDriver());
        }
        return driverHolder.get();
    }

    public static void quitDriver() {
        WebDriver driver = driverHolder.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("WebDriver closed for thread: {}", Thread.currentThread().getId());
            } catch (Exception e) {
                log.warn("Error closing WebDriver: {}", e.getMessage());
            } finally {
                driverHolder.remove();
            }
        }
    }

    private static WebDriver createDriver() {
        String browser = config.getBrowser().toLowerCase().trim();
        boolean headless = config.isHeadless();
        boolean useGrid = config.useGrid();

        log.info("Creating {} driver | headless={} | grid={}", browser, headless, useGrid);

        MutableCapabilities caps = switch (browser) {
            case "firefox" -> buildFirefoxOptions(headless);
            case "edge"    -> buildEdgeOptions(headless);
            default        -> buildChromeOptions(headless);
        };

        WebDriver driver;
        if (useGrid) {
            driver = createRemoteDriver(caps);
        } else {
            driver = createLocalDriver(browser, caps);
        }

        applyTimeouts(driver);
        driver.manage().window().maximize();
        return driver;
    }

    private static ChromeOptions buildChromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }
        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--disable-extensions",
            "--disable-infobars",
            "--remote-allow-origins=*",
            "--lang=en-US"
        );

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", config.getDownloadDir());
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.default_content_settings.popups", 0);
        options.setExperimentalOption("prefs", prefs);
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        WebDriverManager.chromedriver().setup();
        return options;
    }

    private static FirefoxOptions buildFirefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) options.addArguments("--headless");
        options.addArguments("--width=1920", "--height=1080");
        WebDriverManager.firefoxdriver().setup();
        return options;
    }

    private static EdgeOptions buildEdgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        WebDriverManager.edgedriver().setup();
        return options;
    }

    private static WebDriver createLocalDriver(String browser, MutableCapabilities caps) {
        return switch (browser) {
            case "firefox" -> new FirefoxDriver((FirefoxOptions) caps);
            case "edge"    -> new EdgeDriver((EdgeOptions) caps);
            default        -> new ChromeDriver((ChromeOptions) caps);
        };
    }

    private static WebDriver createRemoteDriver(MutableCapabilities caps) {
        try {
            return new RemoteWebDriver(new URL(config.getGridUrl()), caps);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Selenium Grid URL: " + config.getGridUrl(), e);
        }
    }

    private static void applyTimeouts(WebDriver driver) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(config.getImplicitWait()));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getPageLoadTimeout()));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
    }
}
