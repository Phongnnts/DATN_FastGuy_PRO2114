import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { cartApi } from '@/api';
import { useAuthStore } from '@/stores/auth';
import { useProductStore } from '@/stores/product';

const GUEST_KEY = 'cart_guest';

function userKey(userId) {
  return 'cart_user_' + userId;
}

function getStorage() {
  const auth = useAuthStore();
  return auth.isLoggedIn ? localStorage : sessionStorage;
}

function getKey() {
  const auth = useAuthStore();
  if (auth.isLoggedIn && auth.user) return userKey(auth.user.id);
  return GUEST_KEY;
}

export const useCartStore = defineStore('cart', () => {
  const items = ref([]);
  const isLoaded = ref(false);

  const itemCount = computed(() =>
    items.value.reduce((sum, i) => sum + i.quantity, 0),
  );
  const subtotal = computed(() =>
    items.value.reduce((sum, i) => sum + i.price * i.quantity, 0),
  );

  function loadFromStorage() {
    try {
      const stored = getStorage().getItem(getKey());
      items.value = stored ? JSON.parse(stored) : [];
    } catch {
      items.value = [];
    }
    isLoaded.value = true;
  }

  function save() {
    getStorage().setItem(getKey(), JSON.stringify(items.value));
  }

  function findProduct(productId) {
    const productStore = useProductStore();
    return productStore.allProducts.find((p) => p.productId === Number(productId));
  }

  function itemKey(productId, variantId, modifierOptionIds = []) {
    return productId + '_' + variantId + '_' + [...modifierOptionIds].sort((a, b) => a - b).join(',');
  }

  function managedStock(value) {
    return value === null || value === undefined ? null : Number(value);
  }

  function stockLabel(stock) {
    return stock === null ? 'không giới hạn' : stock;
  }

  async function addItem(productId, variantId, quantity = 1, modifiers = []) {
    const modifierOptionIds = modifiers.map((m) => Number(m.modifierOptionId));
    const auth = useAuthStore();
    if (auth.isLoggedIn) {
      try {
        await cartApi.addItem({ productId, variantId, quantity, modifierOptionIds });
        await fetchCart();
      } catch (err) {
        throw err;
      }
    } else {
      addLocalItem(productId, variantId, quantity, modifiers);
    }
  }

  function addLocalItem(productId, variantId, quantity, modifiers = []) {
    const key = itemKey(productId, variantId, modifiers.map((m) => m.modifierOptionId));
    const existing = items.value.find((i) => i.key === key);
    if (existing) {
      const stock = managedStock(existing.quantityAvailable);
      if (stock !== null && existing.quantity + quantity > stock) throw new Error(`Chỉ còn ${stockLabel(stock)} phần`);
      existing.quantity += quantity;
    } else {
      const productStore = useProductStore();
      const product = productStore.allProducts.find((p) => p.productId === Number(productId))
        || productStore.currentProduct;
      if (!product) return;
      const variant = (product.variants || []).find((v) => v.variantId === Number(variantId))
        || product.defaultVariant;
      if (!variant) return;
      const stock = managedStock(variant.quantityAvailable);
      if (variant.status !== 'AVAILABLE' || (stock !== null && quantity > stock)) throw new Error(`Chỉ còn ${stockLabel(stock)} phần`);
      items.value.push({
        cartItemId: null,
        productId: Number(productId),
        variantId: Number(variantId),
        key,
        name: product.name,
        variantName: variant.variantName,
          price: Number(variant.price) + modifiers.reduce((sum, m) => sum + Number(m.price || 0), 0),
          modifiers,
          image: product.image,
          quantity,
          quantityAvailable: managedStock(variant.quantityAvailable),
          variantStatus: variant.status || 'UNAVAILABLE',
          productStatus: product.inStock ? 'AVAILABLE' : 'UNAVAILABLE',
        });
    }
    save();
  }

  async function updateQuantity(productId, variantId, quantity, modifierOptionIds = []) {
    const key = itemKey(productId, variantId, modifierOptionIds);
    const item = items.value.find((i) => i.key === key);
    if (!item) return;
    if (quantity <= 0) {
      removeItem(productId, variantId);
      return;
    }
    const stock = managedStock(item.quantityAvailable);
    if (stock !== null && quantity > stock) throw new Error(`Chỉ còn ${stock} phần`);
    const oldQuantity = item.quantity;
    item.quantity = quantity;
    save();
    const auth = useAuthStore();
    if (auth.isLoggedIn && item.cartItemId) {
      try { await cartApi.updateItem(item.cartItemId, { quantity }); }
      catch (e) {
        item.quantity = oldQuantity;
        save();
        throw e;
      }
    }
  }

  async function removeItem(productId, variantId, modifierOptionIds = []) {
    const key = itemKey(productId, variantId, modifierOptionIds);
    const index = items.value.findIndex((i) => i.key === key);
    if (index < 0) return;
    const item = items.value[index];
    items.value.splice(index, 1);
    save();
    const auth = useAuthStore();
    if (auth.isLoggedIn && item.cartItemId) {
      try { await cartApi.removeItem(item.cartItemId); }
      catch (e) {
        items.value.splice(index, 0, item);
        save();
        throw e;
      }
    }
  }

  async function fetchCart() {
    try {
      const data = await cartApi.get();
      if (data && data.items) {
        items.value = data.items.map((ci) => ({
          cartItemId: ci.cartItemId,
          productId: ci.productId,
          variantId: ci.variantId,
           key: itemKey(ci.productId, ci.variantId, (ci.modifiers || []).map((m) => m.modifierOptionId)),
           name: ci.productName || '',
          variantName: ci.variantName || '',
           price: ci.unitPrice ? parseFloat(ci.unitPrice) : 0,
           modifiers: ci.modifiers || [],
           image: ci.imageUrl || '',
          quantity: ci.quantity,
          quantityAvailable: managedStock(ci.quantityAvailable),
          variantStatus: ci.variantStatus || 'UNAVAILABLE',
          productStatus: ci.productStatus || 'UNAVAILABLE',
        }));
        save();
      }
    } catch (err) {
      console.error('Cart fetchCart failed:', err);
      return null;
    }
  }

  function clear() {
    items.value = [];
    localStorage.removeItem(GUEST_KEY);
    const auth = useAuthStore();
    if (auth.isLoggedIn && auth.user) {
      localStorage.removeItem(userKey(auth.user.id));
    }
    sessionStorage.removeItem(GUEST_KEY);
  }

  async function migrateToUser() {
    const auth = useAuthStore();
    if (!auth.isLoggedIn) return;
    const localItems = [...items.value];
    const failedItems = [];
    if (localItems.length > 0) {
      for (const item of localItems) {
        try {
          await cartApi.addItem({
            productId: item.productId,
            variantId: item.variantId,
            quantity: item.quantity,
            modifierOptionIds: (item.modifiers || []).map((modifier) => modifier.modifierOptionId),
          });
        } catch {
          failedItems.push(item);
        }
      }
    }
    if (failedItems.length) {
      items.value = failedItems;
      save();
      throw new Error(`Không thể đồng bộ ${failedItems.length} món trong giỏ hàng`);
    }
    sessionStorage.removeItem(GUEST_KEY);
    await fetchCart();
  }

  loadFromStorage();

  return {
    items,
    isLoaded,
    itemCount,
    subtotal,
    addItem,
    updateQuantity,
    removeItem,
    clear,
    migrateToUser,
    fetchCart,
  };
});
