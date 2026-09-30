// Rendert icon.html in allen Varianten (432×432) nach ./out/*.png
// und die Web-App-Icons (iPhone, Browser) nach ../../icons/
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
// Web-App-Icons (GitHub Pages) direkt in Zielgröße → ../../icons/
mkdirSync(dir + '../../icons', { recursive: true });
for (const [mode, size, name] of [['full', 180, 'apple-touch-icon'], ['full', 192, 'icon-192'],
                                   ['full', 512, 'icon-512'], ['maskable', 512, 'icon-maskable-512']]) {
  const p = await browser.newPage({ viewport: { width: 432, height: 432 }, deviceScaleFactor: size / 432 });
  await p.goto('file://' + dir + 'icon.html#' + mode);
  await p.evaluate(() => document.fonts.ready);
  await p.waitForTimeout(200);
  await p.locator('#c').screenshot({ path: dir + `../../icons/${name}.png` });
  await p.close();
}
await browser.close();
