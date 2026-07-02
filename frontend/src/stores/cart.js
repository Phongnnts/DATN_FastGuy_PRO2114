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

  function itemKey(productId, variantId) {
    return productId + '_' + variantId;
  }

  async function addItem(productId, variantId, quantity = 1) {
    const auth = useAuthStore();
    if (auth.isLoggedIn) {
      try {
        await cartApi.addItem({ productId, variantId, quantity });
        await fetchCart();
      } catch (err) {
        console.error('Cart API addItem failed, falling back to local:', err);
        addLocalItem(productId, variantId, quantity);
      }
    } else {
      addLocalItem(productId, variantId, quantity);
    }
  }

  function addLocalItem(productId, variantId, quantity) {
    const key = itemKey(productId, variantId);
    const existing = items.value.find((i) => i.key === key);
    if (existing) {
      existing.quantity += quantity;
    } else {
      const productStore = useProductStore();
      const product = productStore.allProducts.find((p) => p.productId === Number(productId))
        || productStore.currentProduct;
      if (!product) return;
      const variant = (product.variants || []).find((v) => v.variantId === Number(variantId))
        || product.defaultVariant;
      if (!variant) return;
      items.value.push({
        cartItemId: null,
        productId: Number(productId),
        variantId: Number(variantId),
        key,
        name: product.name,
        variantName: variant.variantName,
        price: variant.price,
        image: product.image,
        quantity,
      });
    }
    save();
  }

  async function updateQuantity(productId, variantId, quantity) {
    const key = itemKey(productId, variantId);
    const item = items.value.find((i) => i.key === key);
    if (!item) return;
    if (quantity <= 0) {
      removeItem(productId, variantId);
      return;
    }
    item.quantity = quantity;
    save();
    const auth = useAuthStore();
    if (auth.isLoggedIn && item.cartItemId) {
      try { await cartApi.updateItem(item.cartItemId, { quantity }); }
      catch (e) { console.error('Sync cart failed:', e); }
    }
  }

  async function removeItem(productId, variantId) {
    const key = itemKey(productId, variantId);
    const item = items.value.find((i) => i.key === key);
    if (!item) return;
    items.value = items.value.filter((i) => i.key !== key);
    save();
    const auth = useAuthStore();
    if (auth.isLoggedIn && item.cartItemId) {
      try { await cartApi.removeItem(item.cartItemId); }
      catch (e) { console.error('Sync remove failed:', e); }
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
          key: itemKey(ci.productId, ci.variantId),
          name: ci.productName || '',
          variantName: ci.variantName || '',
          price: ci.unitPrice ? parseFloat(ci.unitPrice) : 0,
          image: ci.imageUrl || '',
          quantity: ci.quantity,
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
    if (localItems.length > 0) {
      for (const item of localItems) {
        try {
          await cartApi.addItem({
            productId: item.productId,
            variantId: item.variantId,
            quantity: item.quantity,
          });
        } catch (err) {
          console.error('Cart migration addItem failed:', err);
        }
      }
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
