package com.automation.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a UI element captured by XPathCaptureAgent.
 * Stored in the XPath registry (xpath-registry/elements.json) for reuse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XPathElement {
    private String key;            // e.g. LOGIN_USERNAME_INPUT
    private String page;           // e.g. LoginPage
    private String description;    // human-readable description
    private String xpath;          // primary locator
    private String cssSelector;    // fallback CSS selector
    private String id;             // id attribute if present
    private String name;           // name attribute if present
    private String locatorStrategy;// XPATH, CSS, ID, NAME
    private String elementType;    // INPUT, BUTTON, LINK, DROPDOWN, etc.
    @JsonProperty("isDynamic")
    private boolean isDynamic;     // true if locator has dynamic parts
    private String capturedFrom;   // URL where element was found
    private String capturedBy;     // agent or person who captured
    private LocalDateTime capturedAt;
    @JsonProperty("verified")
    private boolean verified;      // has been verified to work
    private int failureCount;      // times this locator failed

    public String getBestLocator() {
        if (id != null && !id.isEmpty()) return "id=" + id;
        if (cssSelector != null && !cssSelector.isEmpty()) return "css=" + cssSelector;
        return "xpath=" + xpath;
    }
}
