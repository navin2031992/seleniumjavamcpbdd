# ── Stage 1: Build ────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
# Pre-download dependencies (cached layer)
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn compile -q

# ── Stage 2: Test execution ────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

# Install Chrome + dependencies
RUN apt-get update && apt-get install -y \
    wget curl unzip gnupg2 ca-certificates \
    fonts-liberation libappindicator3-1 libasound2 \
    libatk-bridge2.0-0 libatk1.0-0 libcups2 libdbus-1-3 \
    libdrm2 libgbm1 libgtk-3-0 libnspr4 libnss3 \
    libxcomposite1 libxdamage1 libxfixes3 libxrandr2 \
    libxss1 libxtst6 xdg-utils \
    --no-install-recommends && \
    wget -q -O /tmp/chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt install -y /tmp/chrome.deb && \
    rm /tmp/chrome.deb && \
    rm -rf /var/lib/apt/lists/*

# Install Node.js (for MCP Selenium support)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g @angiejones/mcp-selenium 2>/dev/null || true

WORKDIR /app

# Copy Maven repo and project from builder
COPY --from=builder /root/.m2 /root/.m2
COPY --from=builder /app /app

# Allure CLI for report generation
RUN wget -q https://github.com/allure-framework/allure2/releases/download/2.25.0/allure-2.25.0.tgz && \
    tar -zxf allure-2.25.0.tgz -C /opt && \
    ln -s /opt/allure-2.25.0/bin/allure /usr/local/bin/allure && \
    rm allure-2.25.0.tgz

# Volumes for test results (mount from host)
VOLUME ["/app/target/allure-results", "/app/target/screenshots"]

ENV DISPLAY=:99
ENV TEST_ENV=staging
ENV BROWSER=chrome
ENV HEADLESS=true

CMD ["mvn", "test", "-Pci", "-Dheadless=true"]
