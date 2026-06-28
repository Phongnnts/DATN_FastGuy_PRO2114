<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/order';
import { formatPrice } from '@/utils/format';
import { PAYMENT_METHOD_LABEL } from '@/utils/constants';
import { userApi, shippingApi, deliveryZoneApi } from '@/api';

const router = useRouter();
const auth = useAuthStore();
const cart = useCartStore();
const orderStore = useOrderStore();

const savedAddresses = ref([]);
const selectedAddressId = ref(null);
const useNewAddress = ref(false);
const phone = ref('');
const recipientName = ref('');
const street = ref('');
const paymentMethod = ref('COD');
const note = ref('');
const submitting = ref(false);

const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const selectedProvince = ref(null);
const selectedDistrict = ref(null);
const selectedWard = ref(null);
const loadingProvinces = ref(false);
const shippingFee = ref(null);
const shippingProvider = ref('');
const feeLoading = ref(false);

const total = computed(() => cart.subtotal + (shippingFee.value || 0));

onMounted(async () => {
  try {
    const [provData, addrData] = await Promise.all([
      shippingApi.getProvinces(),
      userApi.getAddresses(),
    ]);
    provinces.value = (provData || []).map(p => ({
      id: p.ProvinceID || p.province_id || p.provinceId,
      name: p.ProvinceName || p.province_name || p.provinceName,
    }));
    savedAddresses.value = addrData || [];
    const defaultAddr = savedAddresses.value.find(a => a.isDefault);
    if (defaultAddr) selectAddress(defaultAddr);
  } catch {
    provinces.value = [];
    savedAddresses.value = [];
  }
});

watch(selectedProvince, async (id) => {
  districts.value = [];
  wards.value = [];
  selectedDistrict.value = null;
  selectedWard.value = null;
  shippingFee.value = null;
  if (!id) return;
  try {
    const data = await shippingApi.getDistricts(id);
    districts.value = (data || []).map(d => ({
      id: d.DistrictID || d.district_id || d.districtId,
      name: d.DistrictName || d.district_name || d.districtName,
    }));
  } catch {
    districts.value = [];
  }
});

watch(selectedDistrict, async (id) => {
  wards.value = [];
  selectedWard.value = null;
  shippingFee.value = null;
  if (!id) return;
  try {
    const data = await shippingApi.getWards(id);
    wards.value = (data || []).map(w => ({
      code: w.WardCode || w.ward_code || w.wardCode,
      name: w.WardName || w.ward_name || w.wardName,
    }));
  } catch {
    wards.value = [];
  }
});

watch(selectedWard, async (code) => {
  shippingFee.value = null;
  if (!code || !selectedDistrict.value) return;
  feeLoading.value = true;
  try {
    const result = await shippingApi.calculateFee({
      toDistrictId: selectedDistrict.value,
      toWardCode: code,
      weight: 1000,
      length: 20,
      width: 20,
      height: 10,
    });
    const fee = result.fee || result.shippingFee || 0;
    shippingFee.value = typeof fee === 'number' ? fee : parseFloat(fee) || 0;
    shippingProvider.value = result.shippingProvider || 'GHN';
  } catch {
    try {
      const zones = await deliveryZoneApi.getAll();
      const zone = (zones || []).find(z => z.districtName?.toLowerCase().includes(
        (districts.value.find(d => d.id === selectedDistrict.value)?.name || '').toLowerCase()
      ));
      shippingFee.value = zone ? (zone.shippingFee || 15000) : 15000;
      shippingProvider.value = 'FALLBACK_ZONE';
    } catch {
      shippingFee.value = 15000;
      shippingProvider.value = 'FALLBACK_ZONE';
    }
  } finally {
    feeLoading.value = false;
  }
});

function selectAddress(addr) {
  selectedAddressId.value = addr.addressId;
  useNewAddress.value = false;
  street.value = addr.street || '';
  phone.value = addr.phone || '';
  recipientName.value = addr.recipientName || '';
  if (addr.ghnDistrictId) selectedDistrict.value = addr.ghnDistrictId;
  if (addr.ghnWardCode) selectedWard.value = addr.ghnWardCode;
}

function useManualEntry() {
  selectedAddressId.value = null;
  useNewAddress.value = true;
  street.value = '';
  phone.value = '';
  recipientName.value = '';
}

function getFullAddress() {
  const prov = provinces.value.find(p => p.id === selectedProvince.value);
  const dist = districts.value.find(d => d.id === selectedDistrict.value);
  const ward = wards.value.find(w => w.code === selectedWard.value);
  return [
    recipientName.value.trim(),
    street.value.trim(),
    ward?.name || '',
    dist?.name || '',
    prov?.name || '',
  ].filter(Boolean).join(', ');
}

function canPlaceOrder() {
  return selectedWard.value && selectedDistrict.value && shippingFee.value !== null
    && recipientName.value.trim() && phone.value.trim() && street.value.trim();
}

function getProvinceName() {
  const p = provinces.value.find(p => p.id === selectedProvince.value);
  return p?.name || '';
}

function getDistrictName() {
  const d = districts.value.find(d => d.id === selectedDistrict.value);
  return d?.name || '';
}

function getWardName() {
  const w = wards.value.find(w => w.code === selectedWard.value);
  return w?.name || '';
}

async function placeOrder() {
  if (!canPlaceOrder()) return alert('Vui lòng điền đầy đủ thông tin giao hàng');
  const fullAddress = getFullAddress();
  if (!fullAddress) return alert('Vui lòng nhập địa chỉ');
  submitting.value = true;
  try {
    const order = await orderStore.createOrder({
      address: fullAddress,
      phone: phone.value.trim(),
      deliveryNote: note.value,
      paymentMethod: paymentMethod.value,
      ghnProvinceId: selectedProvince.value,
      ghnDistrictId: selectedDistrict.value,
      ghnWardCode: selectedWard.value,
      toProvinceName: getProvinceName(),
      toDistrictName: getDistrictName(),
      toWardName: getWardName(),
      shippingFee: shippingFee.value,
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

          <div v-if="savedAddresses.length > 0 && !useNewAddress" class="saved-addresses">
            <div
              v-for="addr in savedAddresses"
              :key="addr.addressId"
              class="saved-address-item"
              :class="{ selected: selectedAddressId === addr.addressId }"
              @click="selectAddress(addr)"
            >
              <div class="saved-address-radio">
                <div class="radio-circle" :class="{ checked: selectedAddressId === addr.addressId }"></div>
              </div>
              <div class="saved-address-info">
                <div class="saved-address-name">
                  <strong>{{ addr.recipientName }}</strong>
                  <span>{{ addr.phone }}</span>
                  <span v-if="addr.isDefault" class="badge-sm">Mặc định</span>
                </div>
                <div class="saved-address-detail">
                  {{ addr.street }}, {{ addr.ward }}, {{ addr.city }}
                </div>
              </div>
            </div>
            <button class="btn btn-sm btn-outline" style="margin-top: 8px" @click="useManualEntry">
              <i class="bi bi-plus-lg"></i> Nhập địa chỉ mới
            </button>
          </div>

          <div v-if="useNewAddress || savedAddresses.length === 0">
            <div class="form-group">
              <label class="form-label">Tên người nhận</label>
              <input v-model="recipientName" class="form-input" placeholder="Họ tên người nhận" required />
            </div>
            <div class="form-group">
              <label class="form-label">Tỉnh / Thành phố</label>
              <select v-model="selectedProvince" class="form-select">
                <option :value="null">Chọn tỉnh/thành</option>
                <option v-for="p in provinces" :key="p.id" :value="p.id">{{ p.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Quận / Huyện</label>
              <select v-model="selectedDistrict" class="form-select" :disabled="!selectedProvince">
                <option :value="null">Chọn quận/huyện</option>
                <option v-for="d in districts" :key="d.id" :value="d.id">{{ d.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Phường / Xã</label>
              <select v-model="selectedWard" class="form-select" :disabled="!selectedDistrict">
                <option :value="null">Chọn phường/xã</option>
                <option v-for="w in wards" :key="w.code" :value="w.code">{{ w.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Số nhà, tên đường</label>
              <input v-model="street" class="form-input" placeholder="VD: 123 Nguyễn Huệ" required />
            </div>
            <div class="form-group">
              <label class="form-label">Số điện thoại</label>
              <input v-model="phone" type="tel" class="form-input" placeholder="Số điện thoại nhận hàng" required />
            </div>
          </div>

          <div class="preview-address" v-if="getFullAddress()">
            <i class="bi bi-geo-alt"></i>
            <span>{{ getFullAddress() }}</span>
          </div>
          <div v-if="feeLoading" class="fee-loading" style="margin-top:8px;font-size:13px;color:var(--text-mid)">
            <i class="bi bi-arrow-repeat spin"></i> Đang tính phí giao hàng...
          </div>
          <div v-else-if="shippingFee !== null" class="fee-result" style="margin-top:8px">
            <i class="bi bi-truck"></i>
            Phí giao hàng: <strong>{{ formatPrice(shippingFee) }}</strong>
            <span v-if="shippingProvider === 'FALLBACK_ZONE'" style="font-size:12px;color:var(--text-mid)"> (ước tính)</span>
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
              <span>Phí giao hàng</span>
              <span v-if="feeLoading">Đang tính...</span>
              <span v-else>{{ shippingFee !== null ? formatPrice(shippingFee) : '—' }}</span>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-row" style="font-size: 18px; font-weight: 800">
              <span>Tổng cộng</span><span>{{ formatPrice(total) }}</span>
            </div>
          </div>
          <button
            class="btn btn-lg btn-primary checkout-btn"
            @click="placeOrder"
            :disabled="submitting || !canPlaceOrder() || feeLoading"
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
.saved-addresses {
  margin-bottom: 16px;
}
.saved-address-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border: 2px solid var(--border);
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
}
.saved-address-item:hover {
  border-color: var(--primary);
}
.saved-address-item.selected {
  border-color: var(--primary);
  background: var(--primary-light, #eff6ff);
}
.saved-address-radio {
  padding-top: 2px;
}
.radio-circle {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border);
  border-radius: 50%;
  transition: all 0.2s;
}
.radio-circle.checked {
  border-color: var(--primary);
  background: var(--primary);
  box-shadow: inset 0 0 0 3px #fff;
}
.saved-address-info {
  flex: 1;
}
.saved-address-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
  flex-wrap: wrap;
}
.saved-address-name span {
  font-size: 13px;
  color: var(--text-mid);
}
.badge-sm {
  background: var(--primary-light, #e3f2fd);
  color: var(--primary, #2563eb);
  padding: 1px 6px;
  border-radius: 99px;
  font-size: 10px;
  font-weight: 600;
}
.saved-address-detail {
  font-size: 13px;
  color: var(--text-light);
}
.preview-address {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #E8F5E9;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  color: #2E7D32;
  margin-top: 8px;
}
.preview-address i {
  font-size: 18px;
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
.fee-loading i, .spin {
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
