package com.automation.utils;

import com.automation.config.ConfigManager;
import io.qameta.allure.Allure;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Screenshot utilities — captures on demand and attaches to Allure reports.
 */
public class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);
    private static final String SCREENSHOT_DIR = ConfigManager.getInstance().getScreenshotDir();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtils() {}

    /**
     * Capture a screenshot, save to disk, and attach to the current Allure report step.
     * Returns the file path or null on failure.
     */
    public static String captureAndAttach(WebDriver driver, String label) {
        if (driver == null) return null;
        try {
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            // Attach to Allure
            Allure.addAttachment(label, "image/png", new ByteArrayInputStream(bytes), ".png");

            // Save to disk
            String filename = sanitise(label) + "_" + LocalDateTime.now().format(TS) + ".png";
            Path dir = Paths.get(SCREENSHOT_DIR);
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            FileUtils.writeByteArrayToFile(target.toFile(), bytes);

            log.info("Screenshot saved: {}", target);
            return target.toString();
        } catch (Exception e) {
            log.error("Failed to capture screenshot '{}': {}", label, e.getMessage());
            return null;
        }
    }

    /**
     * Capture only to disk (no Allure attachment) — useful for debug snapshots.
     */
    public static String captureToDisk(WebDriver driver, String label) {
        if (driver == null) return null;
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String filename = sanitise(label) + "_" + LocalDateTime.now().format(TS) + ".png";
            Path target = Paths.get(SCREENSHOT_DIR, filename);
            Files.createDirectories(target.getParent());
            FileUtils.copyFile(src, target.toFile());
            return target.toString();
        } catch (IOException e) {
            log.error("Failed to save screenshot: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns raw bytes of the current screenshot.
     */
    public static byte[] captureAsBytes(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    private static String sanitise(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").substring(0, Math.min(name.length(), 80));
    }
}
