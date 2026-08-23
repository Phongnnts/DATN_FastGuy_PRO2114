import assert from 'node:assert/strict';
import { access, readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../src/', import.meta.url);
const read = (path) => readFile(new URL(path, root), 'utf8');

async function missing(path) {
  await assert.rejects(access(new URL(path, root)));
}

test('removed frontend domains stay absent while modifiers and ordering remain', async () => {
  await Promise.all([
    missing('components/admin/product-editor/ProductComboSection.vue'),
    missing('components/guest/HomepageOccasions.vue'),
    missing('components/common/NotificationBell.vue'),
    missing('views/user/NotificationsPage.vue'),
    missing('views/user/SupportPage.vue'),
    missing('views/staff/SupportPage.vue'),
    missing('stores/notification.js'),
    missing('api/notification.js'),
    missing('api/support.js'),
  ]);

  const [router, api, homepage, editor, header, accountTabs, modifiers, cart, order] = await Promise.all([
    read('router/index.js'),
    read('api/index.js'),
    read('stores/homepage.js'),
    read('views/admin/ProductEditorPage.vue'),
    read('components/common/PublicHeader.vue'),
    read('components/common/AccountTabs.vue'),
    read('components/admin/product-editor/ProductModifiersSection.vue'),
    read('stores/cart.js'),
    read('stores/order.js'),
  ]);

  const removed = [router, api, homepage, editor, header, accountTabs].join('\n');
  assert.doesNotMatch(removed, /ProductCombo|HomepageOccasions|occasionCombos|SupportPage|NotificationsPage|NotificationBell|useNotificationStore|notificationApi|supportApi|\/notifications|\/support/);
  assert.match(router, /path:\s*['"]\/:pathMatch\(\.\*\)\*['"]/);
  assert.match(modifiers, /modifier/i);
  assert.match(cart, /cart/i);
  assert.match(order, /order/i);
});
