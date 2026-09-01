import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { compile } from 'tailwindcss';

const root = new URL('../', import.meta.url);
const read = path => readFileSync(new URL(path, root), 'utf8');

test('Tailwind v4 setup stays prefixed incremental and reproducibly locked', () => {
  const packageJson = JSON.parse(read('package.json'));
  const packageLock = JSON.parse(read('package-lock.json'));
  const viteConfig = read('vite.config.js');
  const main = read('src/main.js');
  const tailwindPath = new URL('src/assets/styles/tailwind.css', root);

  assert.equal(packageJson.devDependencies.tailwindcss, '^4.3.3');
  assert.equal(packageJson.devDependencies['@tailwindcss/vite'], '^4.3.3');
  assert.equal(packageLock.packages[''].devDependencies.tailwindcss, '^4.3.3');
  assert.equal(packageLock.packages[''].devDependencies['@tailwindcss/vite'], '^4.3.3');
  assert.equal(packageLock.packages['node_modules/tailwindcss'].version, '4.3.3');
  assert.equal(packageLock.packages['node_modules/@tailwindcss/vite'].version, '4.3.3');
  assert.match(viteConfig, /import tailwindcss from '@tailwindcss\/vite'/);
  assert.match(viteConfig, /plugins:\s*\[vue\(\),\s*tailwindcss\(\)\]/);
  assert.ok(existsSync(tailwindPath));
  assert.match(main, /import '\.\/assets\/styles\/tailwind\.css';\r?\nimport '\.\/assets\/styles\/global\.css';/);

  assert.equal(
    read('src/assets/styles/tailwind.css').replaceAll('\r\n', '\n'),
    '@import "tailwindcss/theme.css" layer(theme) prefix(tw);\n@import "tailwindcss/utilities.css" layer(utilities) prefix(tw);\n',
  );
  assert.doesNotMatch(read('src/assets/styles/tailwind.css'), /preflight/i);
});

test('Tailwind compiles only prefixed candidates without Preflight', async () => {
  const entry = new URL('src/assets/styles/tailwind.css', root);
  const compiler = await compile(await readFile(entry, 'utf8'), {
    base: dirname(fileURLToPath(entry)),
    loadStylesheet: async id => {
      const path = fileURLToPath(import.meta.resolve(id));
      return { content: await readFile(path, 'utf8'), base: dirname(path) };
    },
  });
  const css = compiler.build(['tw:flex', 'flex']);

  assert.match(css, /\.tw\\:flex\s*\{\s*display:\s*flex;/);
  assert.doesNotMatch(css, /(?:^|\})\s*\.flex\s*\{/);
  assert.doesNotMatch(css, /box-sizing|::before|::after/);
});
