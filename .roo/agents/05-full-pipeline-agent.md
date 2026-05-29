# Agent: Full AI Pipeline Agent

**Roo Code mode slug:** `full-pipeline`
**Trigger:** Use this agent when you want to go from a Jira ticket (or user story) to passing
automated tests in one command — with zero manual steps.
**Requires:** MCP Selenium + terminal access + file write access.

---

## What this agent does

Runs the complete 4-phase pipeline automatically:

```
PHASE 1 — DISCOVER        PHASE 2 — GENERATE       PHASE 3 — CODE          PHASE 4 — VALIDATE
─────────────────         ──────────────────        ──────────────          ──────────────────
Open browser         →    Read Jira ticket     →    Write Page Object  →    mvn compile -q
Navigate to app           Extract AC                Write Step Defs         mvn test @smoke
Read accessibility        Write feature file        Update Factory          Fix failures
  tree                    Save .feature                                      Rerun until green
Capture locators
Save registry
Close browser
```

---

## How to use

### Full pipeline from a Jira ticket
```
@full-pipeline

Run the full pipeline for PROJ-101
App URL: https://yourapp.com
Login credentials: admin@example.com / Admin@123!
```

### Full pipeline from a user story (no Jira)
```
@full-pipeline

Run the full pipeline for this user story:

As a customer I want to add items to my shopping cart so that I can
review them before checkout.

Acceptance Criteria:
- Clicking "Add to Cart" increases the cart count in the header
- Adding the same item twice increases quantity, not count
- Removing an item decreases the cart count
- An empty cart shows "Your cart is empty"

App: https://demo-shop.example.com
Login needed: No (items can be added without login)
Target page path: /products
```

---

## Phase details

### Phase 1 — Browser Discovery (MCP Selenium)

The agent uses these MCP tools in order:

```
start_browser  → browser: "chrome", options: {headless: false}
navigate       → url: "https://yourapp.com/login"
[read resource] → accessibility://current  (DOM map)
execute_script → scan all inputs, buttons, links, dropdowns
take_screenshot → confirm what was captured
[if login needed]:
  send_keys    → by: "id", value: "email", text: "admin@..."
  send_keys    → by: "id", value: "password", text: "..."
  interact     → action: "click", by: "css", value: "button[type=submit]"
  navigate     → url: "https://yourapp.com/target-page"
  [read resource] → accessibility://current again
close_session
```

All discovered elements are saved to `xpath-registry/elements.json`.

### Phase 2 — Test Generation

For each acceptance criterion, the agent creates:
- 1 positive scenario
- 1 negative/boundary variant
- A Scenario Outline if data sets repeat the same flow

Output: `src/test/resources/features/<page>/<TICKET>.feature`

### Phase 3 — Code Generation

- **Page Object**: uses locators from the registry
- **Step Definitions**: maps every Gherkin line to a Java method
- **PageObjectFactory**: adds the new page if it's shared

### Phase 4 — Validation

```bash
mvn compile -q                                      # catch Java errors
mvn test '-Dcucumber.filter.tags=@smoke'            # run critical path
# if failures → read errors.log → fix locators/assertions → rerun
```

---

## Example complete session

```
User:  @full-pipeline Run the full pipeline for the cart page at https://shop.example.com/cart

Agent: Starting Phase 1 — Browser Discovery...
       ✓ Browser opened (Chrome)
       ✓ Navigated to https://shop.example.com/cart
       ✓ Read accessibility tree: found 12 interactive elements
       ✓ Captured: CART_REMOVE_BUTTON, CART_QUANTITY_INPUT, CART_TOTAL_TEXT...
       ✓ Screenshot saved: target/screenshots/cart-discovery.png
       ✓ Registry updated: 5 new entries added to elements.json
       ✓ Browser closed

       Starting Phase 2 — Feature File Generation...
       ✓ Wrote src/test/resources/features/cart/PROJ-205.feature
         → 4 scenarios (2 positive, 1 negative, 1 boundary)

       Starting Phase 3 — Code Generation...
       ✓ Created src/test/java/com/automation/pages/CartPage.java
       ✓ Created src/test/java/com/automation/steps/CartSteps.java
       ✓ Updated PageObjectFactory.java

       Starting Phase 4 — Validation...
       ✓ mvn compile -q → 0 errors
       ✓ Running @smoke scenarios...
         PASSED: Cart count updates when item added
         FAILED: Cart total displays correctly (NoSuchElementException)
       
       Investigating failure...
       → Screenshot shows total is in a <span class="order-total"> not ".cart-total"
       → Fixing CartPage @FindBy(css = ".order-total")...
       ✓ Fix applied
       ✓ Rerunning...
         PASSED: Cart total displays correctly
       
       ✅ All scenarios passing!
       Run 'mvn allure:serve' to view the full report.
```

---

## Files this agent reads
- `src/test/resources/config/application.properties`
- `src/test/resources/xpath-registry/elements.json`
- `src/test/java/com/automation/steps/*.java`
- `target/logs/errors.log` (after test runs)

## Files this agent writes
- `src/test/resources/xpath-registry/elements.json`
- `src/test/resources/features/<page>/<TICKET>.feature`
- `src/test/java/com/automation/pages/<Name>Page.java`
- `src/test/java/com/automation/steps/<Name>Steps.java`
- `src/test/java/com/automation/hooks/PageObjectFactory.java`

## Confirmation prompts

The agent **always asks before**:
- Modifying a page object or step file that has passing tests
- Overwriting an element in the registry with `failureCount = 0`
- Running tests on the `prod` environment
