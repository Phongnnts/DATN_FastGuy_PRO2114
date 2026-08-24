import assert from 'node:assert/strict';
import test from 'node:test';
import * as recipe from '../src/utils/recipeAdmin.js';
import { buildRecipePayload, validateRecipeForm } from '../src/utils/inventoryItem.js';

const items = [
  { inventoryItemId: 7, name: 'Ức gà', baseUnit: 'G', availableQuantity: 1500, averageUnitCost: 85 },
  { inventoryItemId: 8, name: 'Sốt', baseUnit: 'ML', availableQuantity: 750, averageUnitCost: null },
];
const draft = { yieldQuantity: '2', active: true, items: [
  { inventoryItemId: '7', quantity: '300' },
  { inventoryItemId: '8', quantity: '50' },
] };

test('recipe and settings payload builders include their exact baseline versions', () => {
  assert.deepEqual(buildRecipePayload({ ...draft, inventoryMode: 'INGREDIENT' }, '2026-08-24T10:11:12'), {
    yieldQuantity: 2,
    active: true,
    items: [{ inventoryItemId: 7, quantity: 300 }, { inventoryItemId: 8, quantity: 50 }],
    expectedUpdatedAt: '2026-08-24T10:11:12',
  });
  assert.deepEqual(recipe.buildInventorySettingsPayload('FINISHED_GOOD', '2026-08-24T10:11:13'), { inventoryMode: 'FINISHED_GOOD', expectedUpdatedAt: '2026-08-24T10:11:13' });
  assert.deepEqual(buildRecipePayload(draft, null), {
    yieldQuantity: 2,
    active: true,
    items: [{ inventoryItemId: 7, quantity: 300 }, { inventoryItemId: 8, quantity: 50 }],
    expectedUpdatedAt: null,
  });
  assert.deepEqual(validateRecipeForm(draft), {});
});

test('inventory quantity formatting converts only display-friendly large base units', () => {
  assert.equal(recipe.formatInventoryQuantity(1500, 'G'), '1,5 kg');
  assert.equal(recipe.formatInventoryQuantity(750, 'ML'), '750 ml');
  assert.equal(recipe.formatInventoryQuantity(2500, 'ML'), '2,5 L');
  assert.equal(recipe.formatInventoryQuantity(3, 'PIECE'), '3 cái');
});

test('draft presentation derives capacity and reports incomplete costs without zero', () => {
  const presentation = recipe.presentRecipeDraft(draft, items, 50000);
  assert.equal(presentation.availableServings, 10);
  assert.equal(presentation.limitingInventoryItemId, 7);
  assert.equal(presentation.lines[0].requiredPerServing, 150);
  assert.equal(presentation.lines[0].costPerServing, 12750);
  assert.equal(presentation.costStatus, 'INCOMPLETE');
  assert.equal(presentation.recipeCostPerServing, null);
  assert.deepEqual(presentation.missingCostItems.map((item) => item.name), ['Sốt']);
});

test('zero average cost is missing in draft capacity semantics', () => {
  const presentation = recipe.presentRecipeDraft(
    { yieldQuantity: '1', active: true, items: [{ inventoryItemId: '7', quantity: '1' }] },
    [{ ...items[0], averageUnitCost: 0 }],
    50000,
  );
  assert.equal(presentation.lines[0].costAvailable, false);
  assert.equal(presentation.lines[0].costPerServing, null);
  assert.equal(presentation.costStatus, 'INCOMPLETE');
});

test('dirty comparison normalizes numeric strings and payload ordering', () => {
  const baseline = { recipe: buildRecipePayload(draft), inventoryMode: 'INGREDIENT' };
  assert.equal(recipe.isRecipeDraftDirty({ ...draft, yieldQuantity: '2.0000' }, baseline), false);
  assert.equal(recipe.isRecipeDraftDirty({ ...draft, items: [{ inventoryItemId: '7', quantity: '301' }, draft.items[1]] }, baseline), true);
});


test('recipe and settings dirty state are independent', () => {
  const baseline = { recipe: buildRecipePayload(draft), inventoryMode: 'INGREDIENT' };
  assert.equal(recipe.isRecipeDraftDirty(draft, baseline), false);
  assert.equal(recipe.isRecipeDraftDirty({ ...draft, yieldQuantity: '3' }, baseline), true);
  assert.equal(recipe.isInventorySettingsDirty('FINISHED_GOOD', baseline), true);
  assert.equal(recipe.isInventorySettingsDirty('INGREDIENT', baseline), false);
});
