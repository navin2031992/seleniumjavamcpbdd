# Agent: XPath Discovery Agent

**Roo Code mode slug:** `xpath-discovery`
**Trigger:** Use this agent to automatically find and save all element locators from a live web page.
**Requires:** MCP Selenium server (`npm install -g @angiejones/mcp-selenium`)

---

## What this agent does

Opens your application in a real browser, reads the DOM structure, and builds the
`xpath-registry/elements.json` file — the central store of element locators used by all page objects.

You never have to manually inspect elements again.

---

## MCP Selenium tools this agent uses

| Tool | What it does | Key params |
|---|---|---|
| `start_browser` | Opens system Edge (default) or any browser | `browser` (default: "edge"), `options.headless` |
| `navigate` | Goes to a URL | `url` |
| `execute_script` | Runs JavaScript to inspect DOM | `script` |
| `get_element_text` | Reads element text | `by`, `value` |
| `get_element_attribute` | Reads HTML attribute | `by`, `value`, `attribute` |
| `take_screenshot` | Captures current browser state | `outputPath` (optional) |
| `accessibility://current` | Full accessibility tree (JSON) | resource read |
| `close_session` | Closes the browser | — |

### Locator strategies accepted
```
"id"    → by element's id attribute         (most stable)
"css"   → CSS selector                       (very good)
"xpath" → XPath expression                   (flexible)
"name"  → by name attribute                  (good for forms)
"tag"   → by HTML tag name                   (broad)
"class" → by CSS class name                  (fragile)
```

---

## How to use

### Discover elements on one page
```
@xpath-discovery

Capture all elements on the login page of https://yourapp.com/login
Page name: LoginPage
Browser: edge   ← system-installed Edge is used by default
```

### Discover elements across multiple pages
```
@xpath-discovery

Capture elements on these pages:
- https://yourapp.com/login  → LoginPage
- https://yourapp.com/register → RegistrationPage
- https://yourapp.com/dashboard → DashboardPage
```

### Validate existing registry against live app
```
@xpath-discovery

Validate the existing xpath-registry/elements.json against
https://yourapp.com/login and tell me which locators are broken
```

---

## Output — registry entry format

Each discovered element is saved as:
```json
{
  "key": "LOGIN_EMAIL_INPUT",
  "page": "LoginPage",
  "description": "Email address input field on the login form",
  "xpath": "//input[@id='email']",
  "cssSelector": "#email",
  "id": "email",
  "name": "email",
  "locatorStrategy": "ID",
  "elementType": "INPUT",
  "isDynamic": false,
  "capturedFrom": "/login",
  "capturedBy": "XPathDiscoveryAgent",
  "verified": true,
  "failureCount": 0
}
```

**Key naming convention:** `PAGENAME_ELEMENTLABEL_ELEMENTTYPE`
- All uppercase, underscores between words
- Types: `INPUT`, `BUTTON`, `LINK`, `DROPDOWN`, `CHECKBOX`, `RADIO`, `TEXT`, `IMAGE`

---

## Safe update rules

The agent **never overwrites** an entry where `failureCount = 0`.
To force an update on a specific element, set `"failureCount": 1` in the registry first.

---

## Files this agent reads
- `src/test/resources/config/application.properties` — base URL, browser, headless mode
- `src/test/resources/xpath-registry/elements.json` — existing registry (to merge into)

## Files this agent writes
- `src/test/resources/xpath-registry/elements.json` — adds new entries, never removes

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `start_browser` fails | Check Node.js is installed: `node --version` |
| Elements not found | The page may need login first — ask the agent to log in first |
| Locators are fragile (dynamic IDs) | Set `"isDynamic": true` in the registry |
| Page uses React/Angular | Tell the agent to wait for the `accessibility://current` tree to stabilise |
