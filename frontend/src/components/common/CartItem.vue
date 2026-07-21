<script setup>
import { formatPrice } from '@/utils/format';

const props = defineProps({
  item: { type: Object, required: true },
  pending: { type: Boolean, default: false },
});

const emit = defineEmits(['update:quantity', 'remove']);

function changeQty(delta) {
  const stock = props.item.quantityAvailable === null || props.item.quantityAvailable === undefined ? 99 : Number(props.item.quantityAvailable);
  const nextQty = props.item.quantity + delta;
  const newQty = Math.max(1, Math.min(99, Math.min(stock, nextQty)));
  emit('update:quantity', props.item.productId, props.item.variantId, newQty, (props.item.modifiers || []).map((m) => m.modifierOptionId));
}

function remove() {
  emit('remove', props.item.productId, props.item.variantId, (props.item.modifiers || []).map((m) => m.modifierOptionId));
}
</script>

<template>
  <div class="cart-item">
    <div class="cart-img">
      <img :src="item.image || '/images/placeholder-product.svg'" :alt="item.name" @error="$event.currentTarget.src = '/images/placeholder-product.svg'" />
    </div>
    <div class="cart-info">
      <h4 class="cart-name">{{ item.name }}</h4>
      <span v-if="item.variantName" class="cart-variant">{{ item.variantName }}</span>
      <span v-for="modifier in item.modifiers" :key="modifier.modifierOptionId" class="cart-variant">{{ modifier.groupName }}: {{ modifier.name }}</span>
      <div class="cart-stock-warning" v-if="item.variantStatus && item.variantStatus !== 'AVAILABLE' || (item.quantityAvailable != null && Number(item.quantityAvailable) <= 0)">
        Hết hàng
      </div>
      <div class="cart-stock-warning" v-else-if="item.quantityAvailable != null && item.quantity > Number(item.quantityAvailable)">
        Chỉ còn {{ item.quantityAvailable }} phần
      </div>
      <div class="cart-price">{{ formatPrice(item.price) }}</div>
    </div>
    <div class="cart-qty">
       <button class="qty-btn" :disabled="pending || item.quantity <= 1" :aria-label="`Giảm số lượng ${item.name}`" @click="changeQty(-1)">
        <i class="bi bi-dash"></i>
      </button>
       <span class="qty-val" aria-live="polite">{{ item.quantity }}</span>
       <button class="qty-btn" :disabled="pending || (item.quantityAvailable != null && item.quantity >= Number(item.quantityAvailable))" :aria-label="`Tăng số lượng ${item.name}`" @click="changeQty(1)">
        <i class="bi bi-plus"></i>
      </button>
    </div>
    <div class="cart-total">{{ formatPrice(item.price * item.quantity) }}</div>
     <button class="cart-remove" :disabled="pending" :aria-label="`Xóa ${item.name} khỏi giỏ hàng`" @click="remove">
      <i class="bi bi-x-lg"></i>
    </button>
  </div>
</template>

<style scoped>
.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-light);
}
.cart-item:last-child { border-bottom: none; }
.cart-img {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--surface);
}
.cart-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cart-info {
  flex: 1;
  min-width: 0;
}
.cart-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 2px;
}
.cart-variant {
  font-size: 12px;
  color: var(--text-mid);
  display: block;
  margin-bottom: 2px;
}
.cart-stock-warning {
  font-size: 12px;
  color: var(--red-active);
  font-weight: 500;
}
.cart-price {
  font-size: 14px;
  color: var(--primary);
  font-weight: 600;
  margin-top: 2px;
}
.cart-qty {
  display: flex;
  align-items: center;
  gap: 4px;
}
.qty-btn {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--text-mid);
  transition: all var(--transition-fast);
}
.qty-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-50);
}
.qty-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.qty-val {
  font-size: 14px;
  font-weight: 700;
  min-width: 28px;
  text-align: center;
}
.cart-total {
  font-size: 15px;
  font-weight: 700;
  min-width: 80px;
  text-align: right;
  color: var(--text-dark);
}
.cart-remove {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  font-size: 13px;
  color: var(--text-light);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}
.cart-remove:hover {
  background: #fee2e2;
  color: var(--red-active);
}
@media (max-width: 600px) {
  .cart-item { flex-wrap: wrap; gap: 10px; }
  .cart-img { width: 52px; height: 52px; }
  .cart-total { min-width: auto; }
}
</style>
