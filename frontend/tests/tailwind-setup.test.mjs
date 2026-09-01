import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import test from 'node:test';

const root = new URL('../', import.meta.url);
const read = path => readFileSync(new URL(path, root), 'utf8');

test('Tailwind v4 is available incrementally without replacing existing CSS', () => {
  const packageJson = JSON.parse(read('package.json'));
  const viteConfig = read('vite.config.js');
  const main = read('src/main.js');
  const tailwindPath = new URL('src/assets/styles/tailwind.css', root);

  assert.ok(packageJson.devDependencies.tailwindcss);
  assert.ok(packageJson.devDependencies['@tailwindcss/vite']);
  assert.match(viteConfig, /import tailwindcss from '@tailwindcss\/vite'/);
  assert.match(viteConfig, /plugins:\s*\[vue\(\),\s*tailwindcss\(\)\]/);
  assert.ok(existsSync(tailwindPath));
  assert.ok(main.indexOf("import './assets/styles/tailwind.css'") < main.indexOf("import './assets/styles/global.css'"));

  const tailwind = read('src/assets/styles/tailwind.css');
  assert.match(tailwind, /tailwindcss\/theme\.css.*prefix\(tw\)/);
  assert.match(tailwind, /tailwindcss\/utilities\.css.*prefix\(tw\)/);
  assert.doesNotMatch(tailwind, /preflight/);
});
