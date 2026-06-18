<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/order';
import { formatPrice } from '@/utils/format';
import { PAYMENT_METHOD_LABEL } from '@/utils/constants';
import { deliveryZoneApi } from '@/api';

const router = useRouter();
const auth = useAuthStore();
const cart = useCartStore();
const orderStore = useOrderStore();

const zones = ref([]);
const selectedZone = ref(null);
const addressDetail = ref('');
const phone = ref('');
const paymentMethod = ref('COD');
const note = ref('');
const submitting = ref(false);
const loadingZones = ref(true);

const shippingFee = computed(() => {
  const zone = zones.value.find(z => z.zoneId === selectedZone.value);
  return zone ? (zone.shippingFee || 0) : 0;
});
const total = computed(() => cart.subtotal + shippingFee.value);

onMounted(async () => {
  try {
    const data = await deliveryZoneApi.getAll();
    zones.value = data || [];
    if (zones.value.length > 0) selectedZone.value = zones.value[0].zoneId;
  } catch {
    zones.value = [];
  } finally {
    loadingZones.value = false;
  }
});

async function placeOrder() {
  if (!selectedZone.value) return alert('Vui lòng chọn quận/huyện');
  if (!addressDetail.value.trim()) return alert('Vui lòng nhập địa chỉ');
  submitting.value = true;
  try {
    const order = await orderStore.createOrder({
      zoneId: selectedZone.value,
      address: addressDetail.value.trim(),
      phone: phone.value.trim(),
      paymentMethod: paymentMethod.value,
      note: note.value,
    });
    cart.clear();
    router.push(`/account/orders/${order.id}`);
  } catch (e) {
    alert(e.message);
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="checkout-page">
    <div class="checkout-layout" v-if="cart.items.length > 0">
      <div class="checkout-main">
        <div class="card mb-3">
          <h3>Địa chỉ giao hàng</h3>
          <div class="form-group">
            <label class="form-label">Quận / Huyện</label>
            <select v-model="selectedZone" class="form-select" :disabled="loadingZones">
              <option v-if="loadingZones" value="">Đang tải...</option>
              <option v-for="z in zones" :key="z.zoneId" :value="z.zoneId">
                {{ z.districtName }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Số điện thoại</label>
            <input v-model="phone" type="tel" class="form-input" placeholder="Số điện thoại nhận hàng" />
          </div>
          <div class="form-group">
            <label class="form-label">Địa chỉ</label>
            <textarea
              v-model="addressDetail"
              class="form-textarea"
              placeholder="Số nhà, tên đường, phường/xã..."
              rows="2"
            ></textarea>
          </div>
        </div>
        <div class="card mb-3">
          <h3>Phương thức thanh toán</h3>
          <div class="payment-selector">
            <div
              v-for="(label, key) in PAYMENT_METHOD_LABEL"
              :key="key"
              class="payment-option"
              :class="{ selected: paymentMethod === key }"
              @click="paymentMethod = key"
            >
              <i
                :class="
                  key === 'COD'
                    ? 'bi bi-cash'
                    : key === 'MOMO'
                      ? 'bi bi-phone'
                      : 'bi bi-credit-card'
                "
              ></i>
              <span>{{ label }}</span>
              <i
                v-if="paymentMethod === key"
                class="bi bi-check-circle-fill selected-icon"
              ></i>
            </div>
          </div>
        </div>
        <div class="card mb-3">
          <h3>Ghi chú</h3>
          <textarea
            v-model="note"
            class="form-textarea"
            placeholder="Ghi chú cho cửa hàng..."
            rows="2"
          ></textarea>
        </div>
      </div>
      <div class="checkout-sidebar">
        <div class="card">
          <h3>Đơn hàng</h3>
          <div class="checkout-items">
            <div
              v-for="item in cart.items"
              :key="item.productId"
              class="checkout-item"
            >
              <img :src="item.image" :alt="item.name" />
              <div class="checkout-item-info">
                <div class="checkout-item-name">{{ item.name }}</div>
                <div class="checkout-item-qty">x{{ item.quantity }}</div>
              </div>
              <div class="checkout-item-price">
                {{
                  formatPrice(
                    (item.discountPrice || item.price) * item.quantity,
                  )
                }}
              </div>
            </div>
          </div>
          <div class="checkout-summary">
            <div class="summary-row">
              <span>Tạm tính</span><span>{{ formatPrice(cart.subtotal) }}</span>
            </div>
            <div class="summary-row">
              <span>Phí giao hàng</span><span>{{ formatPrice(shippingFee) }}</span>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-row" style="font-size: 18px; font-weight: 800">
              <span>Tổng cộng</span><span>{{ formatPrice(total) }}</span>
            </div>
          </div>
          <button
            class="btn btn-lg btn-primary checkout-btn"
            @click="placeOrder"
            :disabled="submitting"
          >
            <i v-if="submitting" class="bi bi-arrow-repeat spin"></i>
            {{ submitting ? 'Đang xử lý...' : 'Đặt hàng' }}
          </button>
        </div>
      </div>
    </div>
    <div v-else class="empty-state" style="padding: 60px 0">
      <i class="bi bi-cart3"></i>
      <h3>Giỏ hàng trống</h3>
      <router-link to="/menu" class="btn btn-primary">Mua sắm ngay</router-link>
    </div>
  </div>
</template>

<style scoped>
.checkout-layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
  align-items: start;
}
.checkout-main h3,
.checkout-sidebar h3 {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
}
.payment-selector {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.payment-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px;
  border: 2px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: border-color 0.2s;
}
.payment-option:hover {
  border-color: var(--primary);
}
.payment-option.selected {
  border-color: var(--primary);
  background: var(--primary-light);
}
.payment-option i:first-child {
  font-size: 20px;
  color: var(--primary);
}
.payment-option span {
  flex: 1;
  margin-left: 10px;
  font-size: 14px;
}
.selected-icon {
  color: var(--primary) !important;
  font-size: 18px;
}
.checkout-items {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
}
.checkout-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.checkout-item img {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
}
.checkout-item-info {
  flex: 1;
}
.checkout-item-name {
  font-size: 13px;
  font-weight: 600;
}
.checkout-item-qty {
  font-size: 12px;
  color: var(--text-mid);
}
.checkout-item-price {
  font-size: 14px;
  font-weight: 600;
}
.checkout-summary {
  margin-top: 12px;
}
.checkout-btn {
  width: 100%;
  margin-top: 16px;
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 768px) {
  .checkout-layout {
    grid-template-columns: 1fr;
  }
}
</style>
