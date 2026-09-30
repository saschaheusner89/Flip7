// Rendert icon.html in allen Varianten (432×432) nach ./out/*.png
import pw from '/opt/node22/lib/node_modules/playwright/index.js'; const { chromium } = pw;
import { mkdirSync } from 'fs';
const dir = new URL('.', import.meta.url).pathname;
mkdirSync(dir + 'out', { recursive: true });
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 432, height: 432 }, deviceScaleFactor: 1 });
for (const mode of ['fg', 'bg', 'mono', 'legacy']) {
  await page.goto('file://' + dir + 'icon.html#' + mode);
  await page.reload();
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(200);
  await page.locator('#c').screenshot({ path: dir + `out/${mode}.png`, omitBackground: true });
}
await browser.close();
