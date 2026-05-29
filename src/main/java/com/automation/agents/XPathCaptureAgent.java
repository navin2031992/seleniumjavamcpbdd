package com.automation.agents;

import com.automation.models.XPathElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * XPathCaptureAgent — crawls a page with Selenium and automatically
 * discovers, validates, and stores element locators in the XPath registry.
 *
 * The registry is a JSON file at:
 *   src/test/resources/xpath-registry/elements.json
 *
 * This eliminates the need to manually hunt for XPaths and keeps
 * locators in one central, maintainable location.
 */
public class XPathCaptureAgent extends BaseAgent {

    private static final Logger log = LogManager.getLogger(XPathCaptureAgent.class);
    private static final String REGISTRY_PATH = "src/test/resources/xpath-registry/elements.json";

    private final ObjectMapper jsonMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT);

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Capture all interactive elements on the current page and add them
     * to the registry under the given page name.
     */
    public List<XPathElement> capturePageElements(WebDriver driver, String pageName) {
        log.info("Capturing elements on page: {} (URL: {})", pageName, driver.getCurrentUrl());
        List<XPathElement> captured = new ArrayList<>();

        captured.addAll(captureInputs(driver, pageName));
        captured.addAll(captureButtons(driver, pageName));
        captured.addAll(captureLinks(driver, pageName));
        captured.addAll(captureDropdowns(driver, pageName));
        captured.addAll(captureCheckboxes(driver, pageName));

        log.info("Captured {} elements on {}", captured.size(), pageName);
        saveToRegistry(captured, pageName);
        return captured;
    }

    /**
     * Validate all XPath elements in the registry against the live page.
     * Returns a report map: elementKey -> valid/broken.
     */
    public Map<String, Boolean> validateRegistry(WebDriver driver) {
        Map<String, Boolean> report = new LinkedHashMap<>();
        List<XPathElement> elements = loadRegistry();

        elements.forEach(el -> {
            boolean valid = isLocatorValid(driver, el);
            report.put(el.getKey(), valid);
            if (!valid) {
                log.warn("BROKEN locator: {} ({})", el.getKey(), el.getXpath());
            }
        });

        long brokenCount = report.values().stream().filter(v -> !v).count();
        log.info("Registry validation: {}/{} elements valid", elements.size() - brokenCount, elements.size());
        return report;
    }

    /**
     * AI-enhanced element description: ask Claude to give a meaningful
     * name and description for a raw XPath.
     */
    public XPathElement enrichWithAI(XPathElement element) {
        String system = """
            You are a QA engineer. Given an HTML element's XPath and attributes,
            generate a meaningful key name (UPPER_SNAKE_CASE) and a one-line description.
            Return JSON: {"key": "...", "description": "...", "elementType": "INPUT|BUTTON|LINK|..."}
            """;

        String prompt = "XPath: " + element.getXpath()
            + "\nPage: " + element.getPage()
            + "\nURL: " + element.getCapturedFrom();

        String response = askClaude(system, prompt, 512);
        JsonNode node = parseJsonFromResponse(response);

        if (!node.path("key").isMissingNode()) {
            element.setKey(node.path("key").asText(element.getKey()));
            element.setDescription(node.path("description").asText(element.getDescription()));
            element.setElementType(node.path("elementType").asText(element.getElementType()));
        }
        return element;
    }

    // ── Element Capture ────────────────────────────────────────────────────────

    private List<XPathElement> captureInputs(WebDriver driver, String pageName) {
        List<XPathElement> elements = new ArrayList<>();
        List<WebElement> inputs = safeFind(driver, By.cssSelector("input:not([type=hidden])"));

        for (int i = 0; i < inputs.size(); i++) {
            WebElement el = inputs.get(i);
            try {
                String id = el.getAttribute("id");
                String name = el.getAttribute("name");
                String type = el.getAttribute("type");
                String placeholder = el.getAttribute("placeholder");
                String ariaLabel = el.getAttribute("aria-label");

                String label = firstNonEmpty(ariaLabel, placeholder, name, id, type + "_" + i);
                String key = (pageName + "_" + label).toUpperCase()
                    .replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_") + "_INPUT";

                XPathElement xpe = XPathElement.builder()
                    .key(key)
                    .page(pageName)
                    .description(label + " input field")
                    .elementType("INPUT")
                    .id(id)
                    .name(name)
                    .xpath(buildXpath(el, driver))
                    .cssSelector(buildCss(id, name, type))
                    .locatorStrategy(id != null && !id.isEmpty() ? "ID" : "XPATH")
                    .capturedFrom(driver.getCurrentUrl())
                    .capturedBy("XPathCaptureAgent")
                    .capturedAt(LocalDateTime.now())
                    .verified(true)
                    .build();

                elements.add(xpe);
                log.debug("Captured input: {}", key);
            } catch (StaleElementReferenceException e) {
                log.debug("Stale element skipped during input capture");
            }
        }
        return elements;
    }

    private List<XPathElement> captureButtons(WebDriver driver, String pageName) {
        List<XPathElement> elements = new ArrayList<>();
        List<WebElement> buttons = safeFind(driver,
            By.cssSelector("button, input[type=submit], input[type=button], [role=button]"));

        for (WebElement el : buttons) {
            try {
                String text = el.getText().trim();
                String id = el.getAttribute("id");
                String ariaLabel = el.getAttribute("aria-label");
                String label = firstNonEmpty(ariaLabel, text, id, "button");
                if (label.length() > 40) label = label.substring(0, 40);

                String key = (pageName + "_" + label).toUpperCase()
                    .replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_") + "_BUTTON";

                XPathElement xpe = XPathElement.builder()
                    .key(key)
                    .page(pageName)
                    .description(label + " button")
                    .elementType("BUTTON")
                    .id(id)
                    .xpath(buildXpath(el, driver))
                    .cssSelector(id != null && !id.isEmpty() ? "#" + id : "button")
                    .locatorStrategy(id != null && !id.isEmpty() ? "ID" : "XPATH")
                    .capturedFrom(driver.getCurrentUrl())
                    .capturedBy("XPathCaptureAgent")
                    .capturedAt(LocalDateTime.now())
                    .verified(true)
                    .build();

                elements.add(xpe);
            } catch (StaleElementReferenceException ignored) {}
        }
        return elements;
    }

    private List<XPathElement> captureLinks(WebDriver driver, String pageName) {
        List<XPathElement> elements = new ArrayList<>();
        List<WebElement> links = safeFind(driver, By.cssSelector("nav a, header a, [role=navigation] a"));

        for (WebElement el : links) {
            try {
                String text = el.getText().trim();
                String href = el.getAttribute("href");
                if (text.isEmpty() || href == null) continue;

                String key = (pageName + "_" + text).toUpperCase()
                    .replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_") + "_LINK";

                XPathElement xpe = XPathElement.builder()
                    .key(key)
                    .page(pageName)
                    .description(text + " navigation link")
                    .elementType("LINK")
                    .xpath("//a[normalize-space(text())='" + text + "']")
                    .cssSelector("a[href='" + href + "']")
                    .locatorStrategy("XPATH")
                    .capturedFrom(driver.getCurrentUrl())
                    .capturedBy("XPathCaptureAgent")
                    .capturedAt(LocalDateTime.now())
                    .verified(true)
                    .build();

                elements.add(xpe);
            } catch (StaleElementReferenceException ignored) {}
        }
        return elements;
    }

    private List<XPathElement> captureDropdowns(WebDriver driver, String pageName) {
        List<XPathElement> elements = new ArrayList<>();
        safeFind(driver, By.cssSelector("select")).forEach(el -> {
            try {
                String id = el.getAttribute("id");
                String name = el.getAttribute("name");
                String label = firstNonEmpty(name, id, "dropdown");
                String key = (pageName + "_" + label).toUpperCase()
                    .replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_") + "_DROPDOWN";

                elements.add(XPathElement.builder()
                    .key(key).page(pageName)
                    .description(label + " dropdown")
                    .elementType("DROPDOWN").id(id).name(name)
                    .xpath(id != null ? "//select[@id='" + id + "']" : "//select[@name='" + name + "']")
                    .cssSelector(id != null ? "#" + id : "select[name='" + name + "']")
                    .locatorStrategy(id != null ? "ID" : "XPATH")
                    .capturedFrom(driver.getCurrentUrl())
                    .capturedBy("XPathCaptureAgent").capturedAt(LocalDateTime.now()).verified(true)
                    .build());
            } catch (StaleElementReferenceException ignored) {}
        });
        return elements;
    }

    private List<XPathElement> captureCheckboxes(WebDriver driver, String pageName) {
        List<XPathElement> elements = new ArrayList<>();
        safeFind(driver, By.cssSelector("input[type=checkbox], input[type=radio]")).forEach(el -> {
            try {
                String id = el.getAttribute("id");
                String name = el.getAttribute("name");
                String type = el.getAttribute("type");
                String label = firstNonEmpty(id, name, type);
                String key = (pageName + "_" + label).toUpperCase()
                    .replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_")
                    + (type.equals("radio") ? "_RADIO" : "_CHECKBOX");

                elements.add(XPathElement.builder()
                    .key(key).page(pageName)
                    .description(label + " " + type)
                    .elementType(type.toUpperCase()).id(id).name(name)
                    .xpath(id != null ? "//*[@id='" + id + "']" : "//*[@type='" + type + "'][@name='" + name + "']")
                    .cssSelector(id != null ? "#" + id : "[type=" + type + "][name=" + name + "]")
                    .locatorStrategy(id != null ? "ID" : "XPATH")
                    .capturedFrom(driver.getCurrentUrl())
                    .capturedBy("XPathCaptureAgent").capturedAt(LocalDateTime.now()).verified(true)
                    .build());
            } catch (StaleElementReferenceException ignored) {}
        });
        return elements;
    }

    // ── Registry I/O ───────────────────────────────────────────────────────────

    public void saveToRegistry(List<XPathElement> newElements, String pageName) {
        try {
            List<XPathElement> existing = loadRegistry();
            Set<String> existingKeys = new HashSet<>();
            existing.forEach(e -> existingKeys.add(e.getKey()));

            // Add only new elements (don't overwrite verified locators)
            newElements.stream()
                .filter(e -> !existingKeys.contains(e.getKey()))
                .forEach(existing::add);

            File registryFile = new File(REGISTRY_PATH);
            registryFile.getParentFile().mkdirs();
            jsonMapper.writeValue(registryFile, existing);
            log.info("Registry saved: {} total elements", existing.size());
        } catch (IOException e) {
            log.error("Failed to save XPath registry: {}", e.getMessage());
        }
    }

    public List<XPathElement> loadRegistry() {
        try {
            File file = new File(REGISTRY_PATH);
            if (!file.exists()) return new ArrayList<>();
            return Arrays.asList(jsonMapper.readValue(file, XPathElement[].class));
        } catch (IOException e) {
            log.warn("Could not load XPath registry: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<XPathElement> findByKey(String key) {
        return loadRegistry().stream().filter(e -> e.getKey().equals(key)).findFirst();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String buildXpath(WebElement el, WebDriver driver) {
        try {
            return (String) ((JavascriptExecutor) driver).executeScript(
                """
                function getXPath(el) {
                  if (el.id) return '//*[@id="' + el.id + '"]';
                  if (el === document.body) return '/html/body';
                  var ix = 0;
                  var siblings = el.parentNode.childNodes;
                  for (var i = 0; i < siblings.length; i++) {
                    var sibling = siblings[i];
                    if (sibling === el) return getXPath(el.parentNode) + '/' + el.tagName.toLowerCase() + '[' + (ix + 1) + ']';
                    if (sibling.nodeType === 1 && sibling.tagName === el.tagName) ix++;
                  }
                }
                return getXPath(arguments[0]);
                """, el);
        } catch (Exception e) {
            return "//" + el.getTagName();
        }
    }

    private String buildCss(String id, String name, String type) {
        if (id != null && !id.isEmpty()) return "#" + id;
        if (name != null && !name.isEmpty()) return "[name='" + name + "']";
        if (type != null && !type.isEmpty()) return "input[type=" + type + "]";
        return "input";
    }

    private boolean isLocatorValid(WebDriver driver, XPathElement el) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            String loc = el.getBestLocator();
            By by = loc.startsWith("id=") ? By.id(loc.substring(3)) :
                    loc.startsWith("css=") ? By.cssSelector(loc.substring(4)) :
                    By.xpath(loc.substring(6));
            wait.until(ExpectedConditions.presenceOfElementLocated(by));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<WebElement> safeFind(WebDriver driver, By by) {
        try {
            return driver.findElements(by);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return "element";
    }

    @Override
    protected com.fasterxml.jackson.databind.JsonNode parseJsonFromResponse(String r) {
        try {
            String cleaned = r.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return jsonMapper.readTree(cleaned);
        } catch (Exception e) {
            return jsonMapper.createObjectNode();
        }
    }
}
