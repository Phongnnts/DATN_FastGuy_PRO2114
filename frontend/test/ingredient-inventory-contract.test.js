import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const contractUrl = new URL('../../openapi/fastguy.yaml', import.meta.url);

function schemaSection(contract, name, nextName) {
  return contract.slice(contract.indexOf(`    ${name}:`), contract.indexOf(`    ${nextName}:`));
}

test('OpenAPI freezes ingredient inventory paths, schemas, enums, and decimal quantities', async () => {
  const contract = await readFile(contractUrl, 'utf8');

  for (const path of [
    '/admin/inventory/items',
    '/admin/inventory/items/{itemId}',
    '/admin/inventory/transactions/adjustments',
    '/admin/inventory/transactions',
    '/admin/product-variants/{variantId}/recipe',
    '/admin/product-variants/{variantId}/availability',
  ]) assert.match(contract, new RegExp(`^  ${path.replace(/[{}]/g, '\\$&')}:$`, 'm'));
  assert.doesNotMatch(contract, /^  \/api\/admin\/inventory\//m);
  assert.doesNotMatch(contract, /^  \/api\/admin\/product-variants\//m);

  for (const schema of ['InventoryItem', 'Recipe', 'RecipeItem', 'InventoryAvailability', 'InventoryTransactionPage', 'InventoryConflict']) {
    assert.match(contract, new RegExp(`^    ${schema}:$`, 'm'));
  }

  assert.match(schemaSection(contract, 'InventoryMode', 'BaseUnit'), /enum: \[INGREDIENT, FINISHED_GOOD, UNTRACKED, SUSPENDED\]/);
  assert.match(schemaSection(contract, 'BaseUnit', 'InventoryItemQuantity'), /enum: \[G, ML, PIECE\]/);
  assert.match(schemaSection(contract, 'InventoryItemQuantity', 'InventoryItem'), /type: number\s+format: decimal\s+minimum: 0\s+maximum: 999999999999999\.9999\s+multipleOf: 0\.0001/s);
  assert.match(schemaSection(contract, 'InventoryAdjustmentRequest', 'InventoryTransaction'), /quantity:\s+anyOf:\s+- \{ type: number, format: decimal, minimum: -999999999999999\.9999, maximum: -0\.0001, multipleOf: 0\.0001 \}\s+- \{ type: number, format: decimal, minimum: 0\.0001, maximum: 999999999999999\.9999, multipleOf: 0\.0001 \}/);
  assert.match(schemaSection(contract, 'InventoryTransaction', 'InventoryTransactionPage'), /quantity: \{ type: number, format: decimal, minimum: -999999999999999\.9999, maximum: 999999999999999\.9999, multipleOf: 0\.0001 \}/);

  for (const request of ['InventoryAdjustmentRequest']) {
    const section = schemaSection(contract, request, 'InventoryTransaction');
    for (const field of ['inventoryItemId', 'quantity', 'expectedOnHandQuantity', 'reason', 'note']) assert.match(section, new RegExp(`^        ${field}:`, 'm'));
    assert.match(section, /quantity:\s+anyOf:/);
    assert.match(section, /expectedOnHandQuantity: \{ \$ref: '#\/components\/schemas\/InventoryItemQuantity' \}/);
  }

  const availability = schemaSection(contract, 'InventoryAvailability', 'InventoryConflict');
  assert.deepEqual([...availability.matchAll(/^        (\w+):/gm)].map((match) => match[1]), ['availabilityStatus', 'remainingServings']);
  assert.match(availability, /enum: \[IN_STOCK, LOW_STOCK, OUT_OF_STOCK, UNTRACKED, SUSPENDED\]/);
});
