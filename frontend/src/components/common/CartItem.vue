<script setup>
import { formatPrice } from '@/utils/format';

const props = defineProps({
  item: { type: Object, required: true },
});

const emit = defineEmits(['update:quantity', 'remove']);

function changeQty(delta) {
  const newQty = Math.max(1, props.item.quantity + delta);
  emit('update:quantity', props.item.productId, props.item.variantId, newQty);
}

function remove() {
  emit('remove', props.item.productId, props.item.variantId);
}
</script>

<template>
  <div class="cart-item">
    <div class="cart-item-img-wrap">
      <img :src="item.image" :alt="item.name" class="cart-item-img" />
    </div>
    <div class="cart-item-info">
      <h4>{{ item.name }}</h4>
      <div v-if="item.variantName" class="cart-item-option">
        {{ item.variantName }}
      </div>
      <div class="cart-item-price">
        {{ formatPrice(item.price) }}
      </div>
    </div>
    <div class="cart-item-qty">
      <button class="qty-btn" @click="changeQty(-1)">
        <i class="bi bi-dash"></i>
      </button>
      <span class="qty-value">{{ item.quantity }}</span>
      <button class="qty-btn" @click="changeQty(1)">
        <i class="bi bi-plus"></i>
      </button>
    </div>
    <div class="cart-item-total">
      {{ formatPrice(item.price * item.quantity) }}
    </div>
    <button class="cart-item-remove" @click="remove">
      <i class="bi bi-trash3"></i>
    </button>
  </div>
</template>

<style scoped>
.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
}
.cart-item:last-child {
  border-bottom: none;
}
.cart-item-img-wrap {
  width: 72px;
  height: 72px;
  background: #f9f9f9;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  flex-shrink: 0;
}
.cart-item-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 8px;
}
.cart-item-info {
  flex: 1;
  min-width: 0;
}
.cart-item-info h4 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}
.cart-item-option {
  font-size: 12px;
  color: var(--text-mid);
  margin-bottom: 2px;
}
.cart-item-price {
  font-size: 14px;
  color: var(--red-active);
  font-weight: 600;
}
.cart-item-qty {
  display: flex;
  align-items: center;
  gap: 6px;
}
.qty-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
}
.qty-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.qty-value {
  font-size: 16px;
  font-weight: 700;
  min-width: 24px;
  text-align: center;
}
.cart-item-total {
  font-size: 16px;
  font-weight: 700;
  min-width: 80px;
  text-align: right;
}
.cart-item-remove {
  width: 36px;
  height: 36px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: none;
  font-size: 16px;
  color: var(--text-light);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.15s;
}
.cart-item-remove:hover {
  border-color: var(--red-active);
  color: var(--red-active);
}
@media (max-width: 600px) {
  .cart-item {
    flex-wrap: wrap;
    gap: 10px;
  }
  .cart-item-img-wrap {
    width: 56px;
    height: 56px;
  }
  .cart-item-total {
    min-width: auto;
  }
}
</style>
