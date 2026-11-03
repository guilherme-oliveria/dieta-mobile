import { defineConfig, devices } from "@playwright/test";

const url = process.env.E2E_WEB_URL ?? "http://localhost:8081";

export default defineConfig({
  testDir: ".",
  testMatch: "*.spec.ts",
  fullyParallel: false,
  workers: 1,
  timeout: 180_000,
  expect: { timeout: 15_000 },
  retries: process.env.CI ? 1 : 0,
  reporter: [["list"], ["html", { open: "never", outputFolder: "relatorio" }]],
  use: {
    baseURL: url,
    // Sem locale o Chromium headless do Linux cai em "en-US@posix" e o Compose quebra ("Incorrect locale information provided").
    locale: "pt-BR",
    timezoneId: "America/Sao_Paulo",
    viewport: { width: 1280, height: 900 },
    permissions: ["clipboard-read", "clipboard-write"],
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"], permissions: ["clipboard-read", "clipboard-write"], locale: "pt-BR", viewport: { width: 1280, height: 900 } } }],
});
