import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const contractUrl = new URL('../../openapi/fastguy.yaml', import.meta.url);

test('OpenAPI contract covers the categories response consumed by Vue', async () => {
  const contract = await readFile(contractUrl, 'utf8');

  assert.match(contract, /^openapi: 3\.1\.0$/m);
  assert.match(contract, /^  \/categories:$/m);
  assert.match(contract, /^      operationId: listCategories$/m);
  for (const field of ['categoryId', 'name', 'description', 'sortOrder', 'productCount']) {
    assert.match(contract, new RegExp(`^        ${field}:$`, 'm'));
  }
  assert.doesNotMatch(contract, /\$ref:\s*['"]?https?:\/\//);
});

test('frontend categories client uses the contractized endpoint', async () => {
  const source = await readFile(new URL('../src/api/product.js', import.meta.url), 'utf8');

  assert.match(source, /getCategories\(\)\s*{\s*return client\.get\('\/categories'\);/s);
});
