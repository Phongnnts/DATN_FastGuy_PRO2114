import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = path => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

test('COD settlement API exposes shipper and admin operations', () => {
  const api = read('../src/api/codSettlement.js');
  const index = read('../src/api/index.js');
  assert.match(api, /client\.get\('\/cod-settlements\/current'\)/);
  assert.match(api, /client\.get\('\/cod-settlements\/mine'\)/);
  assert.match(api, /client\.post\('\/cod-settlements', data\)/);
  assert.match(api, /client\.get\('\/cod-settlements\/admin', \{ params: \{ status \} \}\)/);
  assert.match(api, /client\.put\(`\/cod-settlements\/\$\{id\}\/verify`, data\)/);
  assert.match(index, /codSettlementApi/);
});
