# Roo Code Integration Guide
## AI Agents for the Universal BDD Selenium Framework

> **What is Roo Code?**
> Roo Code is a VS Code extension that gives you an AI coding assistant
> (powered by Claude) directly inside your editor. It can read files,
> write code, run terminal commands, and call MCP servers — all from a
> chat panel in VS Code.
>
> Download: https://marketplace.visualstudio.com/items?itemName=RooVeterinaryInc.roo-cline

---

## Table of Contents

1. [How It All Fits Together](#1-how-it-all-fits-together)
2. [Prerequisites](#2-prerequisites)
3. [Setup — Step by Step](#3-setup--step-by-step)
4. [The 5 Agents — What They Do](#4-the-5-agents--what-they-do)
5. [MCP Selenium — Real Tool Reference](#5-mcp-selenium--real-tool-reference)
6. [Agent Workflows with Examples](#6-agent-workflows-with-examples)
7. [The Full Pipeline in One Command](#7-the-full-pipeline-in-one-command)
8. [Configuring the MCP Server](#8-configuring-the-mcp-server)
9. [Troubleshooting](#9-troubleshooting)
10. [Quick-Reference Cheat Sheet](#10-quick-reference-cheat-sheet)

---

## 1. How It All Fits Together

```
  VS Code + Roo Code Extension
  ┌─────────────────────────────────────────────────────────┐
  │                                                         │
  │  You type a message in the chat panel                   │
  │  "@full-pipeline Run tests for PROJ-101"                │
  │                  │                                      │
  │                  ▼                                      │
  │  Roo Code picks the right custom agent (mode)           │
  │  from .roomodes based on the @ prefix                   │
  │                  │                                      │
  │          ┌───────┴────────┐                             │
  │          │                │                             │
  │    Reads/writes      Calls MCP server                   │
  │    project files     (@angiejones/mcp-selenium)         │
  │    (Page Objects,    │                                  │
  │    Feature files,    ▼                                  │
  │    Step defs)   Node.js process                         │
  │                 launches Chrome/Firefox                  │
  │                 navigates to your app                    │
  │                 captures DOM / screenshots               │
  │                      │                                  │
  │                      ▼                                  │
  │               Real browser running                      │
  │               your application                          │
  └─────────────────────────────────────────────────────────┘
```

**Agent → What it generates:**

```
Jira Ticket ──────► [jira-to-bdd]        ──► .feature file
                                                   │
                                                   ▼
Running App ──────► [xpath-discovery]    ──► elements.json
                                                   │
                                    ┌──────────────┘
                                    ▼
                    [test-script-writer] ──► Page Object + Step Defs
                                                   │
                                                   ▼
                    [test-runner]        ──► mvn test → pass/fail
                                                   │
                                                   ▼
                    [full-pipeline]      ──► all of the above, automated
```

---

## 2. Prerequisites

### Required
| Tool | Purpose | Check |
|---|---|---|
| VS Code | Editor | `code --version` |
| Roo Code extension | AI agent | VS Code Extensions panel |
| Java 17+ | Run the framework | `java -version` |
| Maven 3.8+ | Build tool | `mvn -version` |
| Chrome | Browser for tests | — |
| Node.js 18+ | Run MCP server | `node --version` |
| npm | Install MCP server | `npm --version` |

### Optional
| Tool | Purpose |
|---|---|
| Claude API key | Power the AI agents (Roo Code uses this) |
| Anthropic API key | Java agent's `TestGeneratorAgent` |
| Jira API token | Pull stories from Jira |

### Install the MCP Selenium server

```bash
# Install globally (recommended)
npm install -g @angiejones/mcp-selenium

# Verify
npx @angiejones/mcp-selenium --version
```

---

## 3. Setup — Step by Step

### Step 1 — Install Roo Code

1. Open VS Code
2. Press `Ctrl+Shift+X` (Extensions panel)
3. Search "Roo Code"
4. Click Install on **Roo Code** by RooVeterinaryInc

### Step 2 — Open the project

```
File → Open Folder → select c:\NewInitiatives\javaseleniumbdd
```

### Step 3 — Configure the MCP server

The file `.roo/mcp.json` is already in the project. Roo Code reads it automatically.

To verify, open the Roo Code panel (`Ctrl+Shift+P` → "Roo Code: Open") and check
the MCP Servers section — you should see **selenium** listed.

If it's not there, open VS Code Settings and add:
```json
{
  "roo-cline.mcpServers": {
    "selenium": {
      "command": "npx",
      "args": ["-y", "@angiejones/mcp-selenium@latest"]
    }
  }
}
```

### Step 4 — Add your API key (Roo Code)

In VS Code Settings → search "Roo Code API" → set your Anthropic or OpenAI key.

### Step 5 — Configure the test application

Edit `src/test/resources/config/application.properties`:
```properties
app.base.url=https://your-application.com
login.path=/login
test.user.email=youruser@company.com
test.user.password=YourPassword123!
```

### Step 6 — Verify the custom modes are loaded

In the Roo Code chat panel, click the mode dropdown at the top.
You should see these modes:
- 🎫 Jira → BDD Test Writer
- 🔍 XPath Discovery Agent
- ✍️ Test Script Writer
- ▶️ Test Runner & Analyst
- 🚀 Full AI Pipeline Agent

If they are missing, check that `.roomodes` exists in the project root.

---

## 4. The 5 Agents — What They Do

| Mode | Slug | Does | Needs |
|---|---|---|---|
| 🎫 Jira → BDD Test Writer | `jira-to-bdd` | Reads Jira / story → writes `.feature` files | Nothing extra |
| 🔍 XPath Discovery Agent | `xpath-discovery` | Opens browser → captures element locators | MCP Selenium, Node.js |
| ✍️ Test Script Writer | `test-script-writer` | Writes Page Objects + Step Definitions | Feature file + elements.json |
| ▶️ Test Runner & Analyst | `test-runner` | Runs Maven tests → diagnoses failures → fixes | Maven, Chrome |
| 🚀 Full AI Pipeline Agent | `full-pipeline` | All of the above, end-to-end | MCP Selenium, Maven |

---

## 5. MCP Selenium — Real Tool Reference

> Source: github.com/angiejones/mcp-selenium v0.2.3

These are the **actual tools** the MCP server exposes. The XPath Discovery Agent and Full
Pipeline Agent call them directly through Roo Code.

### Browser Tools

```
start_browser(browser, options)
  browser : "chrome" | "firefox" | "edge" | "safari"
  options : { headless: boolean, arguments: string[] }
  ← MUST be called first before any element interaction

navigate(url)
  url     : full URL string

close_session()
  ← call this when done to free the browser
```

### Element Interaction Tools

```
interact(action, by, value, timeout?)
  action  : "click" | "doubleclick" | "rightclick" | "hover"
  by      : "id" | "css" | "xpath" | "name" | "tag" | "class"
  value   : the locator string (e.g. "#login-btn", "//button[@type='submit']")
  timeout : milliseconds, default 10000

send_keys(by, value, text, timeout?)
  ← clears the field first, then types text

press_key(key)
  key     : "Enter" | "Tab" | "Escape" | "Space" | "Backspace" | "ArrowDown" | ...

upload_file(by, value, filePath, timeout?)
  filePath: absolute path to file on local machine
```

### Element Inspection Tools

```
get_element_text(by, value, timeout?)
  ← returns the visible text of the element

get_element_attribute(by, value, attribute, timeout?)
  ← returns the value of an HTML attribute (e.g. "href", "value", "class")
```

### Capture Tools

```
take_screenshot(outputPath?)
  outputPath: optional; if omitted returns base64-encoded PNG
  ← always call this after a critical step to confirm state

execute_script(script, args?)
  script: JavaScript code string
  args  : passed as arguments[0], arguments[1], ...
  ← returns whatever the script returns
```

### Window / Tab Tools

```
window(action, handle?)
  action: "list"         ← get all window handles
        | "switch"       ← switch to specific handle (requires handle param)
        | "switch_latest"← switch to most recently opened tab
        | "close"        ← close current tab

frame(action, by?, value?, index?, timeout?)
  action: "switch"   ← enter an iframe (by locator or index)
        | "default"  ← return to main document
```

### Alert / Dialog Tools

```
alert(action, text?, timeout?)
  action: "accept"    ← click OK
        | "dismiss"   ← click Cancel
        | "get_text"  ← read the alert message
        | "send_text" ← type into a prompt dialog (requires text param)
```

### Cookie Tools

```
add_cookie(name, value, domain?, path?, secure?, httpOnly?, expiry?)
get_cookies(name?)     ← omit name to get all cookies
delete_cookie(name?)   ← omit name to delete all cookies
```

### Diagnostics (requires Chrome with BiDi)

```
diagnostics(type, clear?)
  type  : "console" | "errors" | "network"
  clear : true to empty the buffer after reading
```

### Resources (read-only)

```
browser-status://current  ← "Active session: abc123" or "No active browser session"
accessibility://current   ← full DOM accessibility tree as JSON
                            (use this to understand page structure without writing XPath)
```

### Locator strategy guide

| Strategy | Example value | When to use |
|---|---|---|
| `id` | `"email"` | Best — unique, stable |
| `css` | `"#email"`, `".btn-primary"`, `"button[type=submit]"` | Very good — flexible |
| `xpath` | `"//button[text()='Login']"` | Use when no id/css available |
| `name` | `"username"` | Good for form `<input name="...">` |
| `tag` | `"button"` | Broad — only when element is unique by tag |
| `class` | `"submit-btn"` | Fragile if multiple classes — prefer css |

---

## 6. Agent Workflows with Examples

### 6.1 Write tests from a Jira story

Switch to mode **🎫 Jira → BDD Test Writer** and type:

```
Write BDD tests for this story:

As a logged-in user I want to update my profile picture
so that other users can recognise me.

Acceptance Criteria:
- I can upload a JPG or PNG up to 5MB
- Images over 5MB show an error "File too large"
- Non-image files show "Invalid file type"
- After upload, the new photo appears in the header within 3 seconds
```

**Output:**
```
src/test/resources/features/profile/profile-picture.feature
```

---

### 6.2 Discover element locators from a live page

Switch to mode **🔍 XPath Discovery Agent** and type:

```
Capture all elements on the profile settings page.
URL: https://yourapp.com/settings/profile
Page name: ProfileSettingsPage
The user is already logged in — you can use auth token: Bearer abc123
```

**The agent will:**
1. Call `start_browser` → open Chrome
2. Call `add_cookie` → inject the auth token
3. Call `navigate` → go to the profile page
4. Read `accessibility://current` → map the DOM
5. Call `get_element_attribute` on each found element
6. Save entries to `elements.json`
7. Call `take_screenshot` → confirm
8. Call `close_session`

---

### 6.3 Write Java code from a feature file

Switch to mode **✍️ Test Script Writer** and type:

```
Create the Page Object and Step Definitions for:
- Feature: src/test/resources/features/profile/profile-picture.feature
- Page name: ProfileSettingsPage
- Use the elements in xpath-registry that have page = "ProfileSettingsPage"
```

---

### 6.4 Run tests and fix failures

Switch to mode **▶️ Test Runner & Analyst** and type:

```
Run the @profile tests and fix any failures
```

**The agent will:**
1. Run `mvn compile -q` → catch compilation errors
2. Run `mvn test '-Dcucumber.filter.tags=@profile'`
3. If failure: read `target/logs/errors.log`
4. Identify: broken locator / wrong assertion / timing
5. Propose fix → apply with your approval
6. Rerun

---

### 6.5 Full pipeline — zero manual steps

Switch to mode **🚀 Full AI Pipeline Agent** and type:

```
Run the full pipeline for the profile picture feature.
App: https://yourapp.com
Login: admin@example.com / Admin@123!
Start on: /settings/profile
Story: Users can upload a profile picture (JPG/PNG, max 5MB)
```

The agent runs all 4 phases automatically and reports back when tests are green.

---

## 7. The Full Pipeline in One Command

For teams using CI, you can trigger the full pipeline without Roo Code by running
the `AgentOrchestrator` directly from Maven:

```bash
# Requires ANTHROPIC_API_KEY and JIRA_* environment variables
mvn exec:java -Dexec.mainClass="com.automation.agents.AgentOrchestrator"

# For a specific ticket
mvn exec:java \
  -Dexec.mainClass="com.automation.agents.AgentOrchestrator" \
  -Dexec.args="--ticket=PROJ-101"
```

This runs the Java agents (not the Roo Code agents), which use the same Claude API
but operate programmatically.

---

## 8. Configuring the MCP Server

### Project-level config (already in this project)

File: `.roo/mcp.json`
```json
{
  "mcpServers": {
    "selenium": {
      "command": "npx",
      "args": ["-y", "@angiejones/mcp-selenium@latest"],
      "disabled": false,
      "alwaysAllow": [
        "start_browser", "navigate", "interact", "send_keys",
        "get_element_text", "get_element_attribute", "press_key",
        "take_screenshot", "execute_script", "window", "frame",
        "alert", "add_cookie", "get_cookies", "delete_cookie",
        "diagnostics", "close_session", "upload_file"
      ]
    }
  }
}
```

### VS Code user settings (global — for all projects)

`File → Preferences → Settings → search "roo-cline mcpServers"`:
```json
{
  "roo-cline.mcpServers": {
    "selenium": {
      "command": "npx",
      "args": ["-y", "@angiejones/mcp-selenium@latest"]
    }
  }
}
```

### Direct add via Claude Code CLI

```bash
claude mcp add selenium -- npx -y @angiejones/mcp-selenium@latest
```

### Goose Desktop (one-click)

Paste this URL into your browser address bar:
```
goose://extension?cmd=npx&arg=-y&arg=%40angiejones%2Fmcp-selenium%40latest&id=selenium-mcp&name=Selenium%20MCP&description=automates%20browser%20interactions
```

---

## 9. Troubleshooting

### MCP server not appearing in Roo Code

1. Check `.roo/mcp.json` exists in the project root
2. Reload VS Code window: `Ctrl+Shift+P` → "Developer: Reload Window"
3. Check Node.js is on PATH: open a VS Code terminal → `node --version`

### `start_browser` fails with permission error

```bash
# On Windows, run VS Code as administrator once to install npm packages
# Or install globally first:
npm install -g @angiejones/mcp-selenium
```

### Browser opens but navigate fails

The MCP server uses `npx` to launch on first call which may take a few seconds.
Tell the agent: "Try navigate again after a 3-second pause".

### Elements not found after navigation

Some apps load content asynchronously. Tell the agent:
```
After navigating to /dashboard, wait for the element with id "main-content"
to appear before capturing elements
```

The agent should call:
```
get_element_text(by:"id", value:"main-content", timeout:15000)
```
to wait for the page to be ready.

### Sessions not cleaned up (browser windows stay open)

If Roo Code is interrupted mid-agent, stray browser windows may remain.
Kill them manually or run:
```bash
# Windows
taskkill /F /IM chrome.exe /T

# Mac/Linux
pkill -f "Google Chrome"
```

### `AmbiguousStepDefinitionsException` after generating code

The agent created a step definition that already exists.
Run `mvn compile -q` to see the exact duplicate, then remove it from the
newly generated file.

### Allure report not generating

```bash
# Install Allure CLI
npm install -g allure-commandline

# Or use Maven plugin
mvn allure:serve
```

---

## 10. Quick-Reference Cheat Sheet

### Switch modes in Roo Code

Click the mode name at the top of the chat panel → select from list.

### Agent commands

```
# Generate tests from Jira
@jira-to-bdd   Write BDD tests for PROJ-101

# Capture locators from live app
@xpath-discovery   Capture elements on https://yourapp.com/login   Page: LoginPage

# Write Java code
@test-script-writer   Create Page Object for RegistrationPage using xpath-registry

# Run and fix tests
@test-runner   Run @regression tests on staging and fix failures

# Full automation
@full-pipeline   Run the full pipeline for PROJ-101
```

### MCP Selenium quick calls

```javascript
// Open browser (always first)
start_browser  browser:"chrome"  options:{headless:false}

// Navigate
navigate  url:"https://yourapp.com/login"

// Type into input by id
send_keys  by:"id"  value:"email"  text:"user@example.com"

// Click button by CSS
interact  action:"click"  by:"css"  value:"button[type=submit]"

// Read text
get_element_text  by:"css"  value:".welcome-msg"

// Read attribute
get_element_attribute  by:"id"  value:"email"  attribute:"placeholder"

// Screenshot
take_screenshot  outputPath:"target/screenshots/login-state.png"

// Run JS
execute_script  script:"return document.title"

// Read DOM tree
[resource]  accessibility://current

// Close browser
close_session
```

### File locations

```
.roomodes                           ← custom agent definitions (edit to change behaviour)
.roo/mcp.json                       ← MCP server configuration
.roo/agents/01-jira-bdd-agent.md    ← detailed guide for Jira agent
.roo/agents/02-xpath-discovery-agent.md
.roo/agents/03-test-script-writer.md
.roo/agents/04-test-runner-agent.md
.roo/agents/05-full-pipeline-agent.md
src/test/resources/xpath-registry/elements.json  ← locator store
src/test/resources/config/application.properties ← app URL + credentials
```

---

*For MCP Selenium source: https://github.com/angiejones/mcp-selenium*
*For Roo Code docs: https://docs.roocode.com*
