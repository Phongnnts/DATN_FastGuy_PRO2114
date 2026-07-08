<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/order';
import { formatPrice } from '@/utils/format';
import { PAYMENT_METHOD_LABEL } from '@/utils/constants';
import { userApi, shippingApi, deliveryZoneApi, orderApi } from '@/api';
import couponApi from '@/api/coupon';

const router = useRouter();
const auth = useAuthStore();
const cart = useCartStore();
const orderStore = useOrderStore();

const isGuest = computed(() => !auth.isLoggedIn);

const savedAddresses = ref([]);
const selectedAddressId = ref(null);
const useNewAddress = ref(false);
const phone = ref('');
const recipientName = ref('');
const street = ref('');
const paymentMethod = ref('COD');
const couponCode = ref('');
const appliedCoupon = ref(null);
const couponDiscount = ref(0);
const verifyingCoupon = ref(false);
const couponError = ref('');
const claimedCoupons = ref([]);
const showMyCoupons = ref(false);
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
const expectedDelivery = ref('');
const services = ref([]);
const selectedServiceId = ref(2);
const serviceLoading = ref(false);
const sepayQrUrl = ref('');
const paymentConfirmed = ref(false);
const createdOrderCode = ref('');
const createdOrderId = ref(null);
let paymentPolling = null;

onUnmounted(() => {
  if (paymentPolling) clearInterval(paymentPolling);
});

const total = computed(() => cart.subtotal + (shippingFee.value || 0));

onMounted(async () => {
  loadingProvinces.value = true;
  try {
    const provData = await shippingApi.getProvinces();
    provinces.value = (provData || []).map(p => ({
      id: p.ProvinceID || p.province_id || p.provinceId,
      name: p.ProvinceName || p.province_name || p.provinceName,
    }));
    // Auto-select TP.HCM
    const hcm = provinces.value.find(p => p.name?.includes('Hồ Chí Minh'));
    if (hcm) selectedProvince.value = hcm.id;

    if (!isGuest.value) {
      const addrData = await userApi.getAddresses();
      savedAddresses.value = addrData || [];
      const defaultAddr = savedAddresses.value.find(a => a.isDefault);
      if (defaultAddr) selectAddress(defaultAddr);
      try {
        claimedCoupons.value = await couponApi.getClaimed() || [];
      } catch {}
    }
  } catch {
    provinces.value = [];
    savedAddresses.value = [];
  } finally {
    loadingProvinces.value = false;
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
    const feeResp = result.fee || result.shippingFee || 0;
    shippingFee.value = typeof feeResp === 'number' ? feeResp : parseFloat(feeResp) || 0;
    shippingProvider.value = result.shippingProvider || 'GHN';
    if (result.expectedDeliveryTime) expectedDelivery.value = result.expectedDeliveryTime;
    if (selectedDistrict.value) {
      try {
        const svc = await shippingApi.getServices({ toDistrictId: selectedDistrict.value });
        services.value = Array.isArray(svc) ? svc : [];
        if (services.value.length) selectedServiceId.value = services.value[0].service_id || 2;
      } catch { services.value = []; }
    }
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
  if (addr.ghnDistrictId) {
    selectedDistrict.value = addr.ghnDistrictId;
  } else if (addr.zone?.shippingFee) {
    shippingFee.value = addr.zone.shippingFee;
    shippingProvider.value = 'FALLBACK_ZONE';
  }
  if (addr.ghnWardCode) selectedWard.value = addr.ghnWardCode;
  if (!selectedProvince.value && addr.provinceName?.includes('Hồ Chí Minh')) {
    selectedProvince.value = provinces.value.find(p => p.name?.includes('Hồ Chí Minh'))?.id;
  }
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

function isValidPhone(value) {
  return /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/.test(value.trim());
}

function canPlaceOrder() {
  if (shippingProvider.value === 'FALLBACK_ZONE') {
    return shippingFee.value !== null
      && recipientName.value.trim().length >= 2
      && isValidPhone(phone.value)
      && street.value.trim().length >= 5;
  }
  return selectedWard.value && selectedDistrict.value && shippingFee.value !== null
    && recipientName.value.trim().length >= 2 && isValidPhone(phone.value) && street.value.trim().length >= 5;
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

async function verifyCoupon() {
  if (!couponCode.value.trim()) return;
  verifyingCoupon.value = true;
  couponError.value = '';
  appliedCoupon.value = null;
  couponDiscount.value = 0;
  try {
    const total = cart.items.reduce((s, i) => s + (i.price * i.quantity), 0);
    const res = await couponApi.verify(couponCode.value, total, shippingFee.value || 0);
    if (res.valid) {
      appliedCoupon.value = res;
      couponDiscount.value = res.discount;
    } else {
      couponError.value = res.message || 'Mã không hợp lệ';
    }
  } catch {
    couponError.value = 'Lỗi kiểm tra mã';
  } finally {
    verifyingCoupon.value = false;
  }
}

function cancelCoupon() {
  couponCode.value = '';
  appliedCoupon.value = null;
  couponDiscount.value = 0;
  couponError.value = '';
  showMyCoupons.value = false;
}

function selectClaimedCoupon(c) {
  couponCode.value = c.code;
  appliedCoupon.value = null;
  couponDiscount.value = 0;
  couponError.value = '';
  showMyCoupons.value = false;
  verifyCoupon();
}

async function placeOrder() {
  if (!canPlaceOrder()) return alert('Vui lòng điền đầy đủ thông tin giao hàng');
  const fullAddress = getFullAddress();
  if (!fullAddress) return alert('Vui lòng nhập địa chỉ');
  submitting.value = true;
  try {
    if (isGuest.value) {
      const items = cart.items.map(i => ({
        productId: i.productId,
        variantId: i.variantId,
        quantity: i.quantity,
      }));
      const result = await orderApi.guestCheckout({
        customerName: recipientName.value.trim(),
        phone: phone.value.trim(),
        address: fullAddress,
        deliveryNote: note.value,
        paymentMethod: paymentMethod.value,
        items,
        shippingFee: shippingFee.value,
        ghnProvinceId: selectedProvince.value,
        ghnDistrictId: selectedDistrict.value,
        ghnWardCode: selectedWard.value,
        toProvinceName: getProvinceName(),
        toDistrictName: getDistrictName(),
        toWardName: getWardName(),
        couponCode: appliedCoupon.value?.code || '',
      });
      cart.clear();
      router.push(`/track-order?code=${result.orderCode}`);
      return;
    }

    const result = await orderStore.createOrder({
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
      couponCode: appliedCoupon.value?.code || '',
    });
    createdOrderId.value = result.id;
    if (paymentMethod.value === 'BANK_TRANSFER' && result.sepayQrUrl) {
      sepayQrUrl.value = result.sepayQrUrl;
      createdOrderCode.value = result.orderCode;
      paymentPolling = setInterval(async () => {
        try {
          const status = await orderApi.getPaymentStatus(createdOrderId.value);
            if (status.paymentStatus === 'PAID') {
                paymentConfirmed.value = true;
                clearInterval(paymentPolling);
                paymentPolling = null;
                cart.clear();
                setTimeout(() => {
                  router.push(`/account/orders/${createdOrderId.value}`);
                }, 2000);
              }
        } catch (e) {}
      }, 5000);
    } else {
      cart.clear();
      router.push(`/account/orders/${createdOrderId.value}`);
    }
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

          <div v-if="!isGuest && savedAddresses.length > 0 && !useNewAddress" class="saved-addresses">
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
                  {{ addr.street }}, {{ addr.wardName }}, {{ addr.districtName || addr.city }}
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
              <input v-model="recipientName" class="form-input" placeholder="Họ tên người nhận" minlength="2" maxlength="100" required />
            </div>
            <div class="form-group">
              <label class="form-label">Tỉnh / Thành phố</label>
              <select v-if="loadingProvinces" class="form-select" disabled><option>Đang tải dữ liệu...</option></select>
              <select v-else v-model="selectedProvince" class="form-select">
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
              <input v-model="street" class="form-input" placeholder="VD: 123 Nguyễn Huệ" minlength="5" maxlength="255" required />
            </div>
            <div class="form-group">
              <label class="form-label">Số điện thoại</label>
              <input v-model="phone" type="tel" class="form-input" placeholder="Số điện thoại nhận hàng" pattern="^(0|\+84)(3|5|7|8|9)[0-9]{8}$" required />
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
            <span v-if="expectedDelivery" style="display:block;font-size:12px;color:var(--text-mid);margin-top:4px">
              <i class="bi bi-clock"></i> Dự kiến: {{ expectedDelivery }}
            </span>
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
                    : 'bi bi-qr-code-scan'
                "
              ></i>
              <span>{{ label }}</span>
              <i
                v-if="paymentMethod === key"
                class="bi bi-check-circle-fill selected-icon"
              ></i>
            </div>
          </div>
          <div v-if="paymentMethod === 'BANK_TRANSFER'" class="card" style="margin-top:12px;padding:16px;background:#f8f9fa;border:1px solid var(--border);border-radius:var(--radius-sm)">
            <p><strong>Ngân hàng:</strong> Techcombank</p>
            <p><strong>Số tài khoản:</strong> 19074734124014</p>
            <p><strong>Chủ tài khoản:</strong> FASTGUY</p>
            <p><strong>Nội dung:</strong> Mã đơn hàng + SĐT</p>
            <p style="color:var(--text-mid);font-size:13px;margin-top:4px">Sau khi chuyển khoản, vui lòng chờ xác nhận</p>
          </div>
        </div>
        <div v-if="paymentMethod === 'BANK_TRANSFER' && sepayQrUrl" class="card mb-3">
          <h3>Quét mã QR để thanh toán</h3>
          <div style="text-align:center;padding:20px">
            <img :src="sepayQrUrl" alt="QR thanh toán" style="width:250px;height:250px" />
            <p style="font-size:24px;font-weight:800;margin:12px 0">{{ formatPrice(cart.subtotal + (shippingFee || 0)) }}</p>
            <p style="color:var(--text-mid)">Nội dung: TT-{{ createdOrderCode }}</p>
            <div v-if="paymentConfirmed" style="text-align:center;padding:12px">
              <i class="bi bi-check-circle-fill" style="color:#10b981;font-size:48px"></i>
              <p style="color:#10b981;font-weight:600">Đã thanh toán thành công!</p>
            </div>
            <div v-else style="text-align:center;padding:12px">
              <i class="bi bi-arrow-repeat spin"></i> Đang chờ cổng thanh toán xác nhận...
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
              :key="item.key || item.productId + '_' + item.variantId"
              class="checkout-item"
            >
              <img :src="item.image" :alt="item.name" />
              <div class="checkout-item-info">
                <div class="checkout-item-name">{{ item.name }}</div>
                <div v-if="item.variantName" class="item-variant">{{ item.variantName }}</div>
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
          <div class="checkout-coupon">
            <div class="coupon-header">
              <i class="bi bi-tag"></i>
              <span>Mã giảm giá</span>
            </div>

            <div v-if="!appliedCoupon" class="coupon-body">
              <div class="coupon-input-group">
                <input v-model="couponCode" class="coupon-input" placeholder="Nhập mã giảm giá" @keyup.enter="verifyCoupon" :disabled="verifyingCoupon">
                <button class="coupon-btn" @click="verifyCoupon" :disabled="verifyingCoupon || !couponCode.trim()">
                  <span v-if="verifyingCoupon" class="spinner-sm"></span>
                  <span v-else>Áp dụng</span>
                </button>
              </div>
              <div v-if="couponError" class="coupon-msg error">
                <i class="bi bi-exclamation-circle"></i> {{ couponError }}
              </div>
            </div>

            <div v-if="appliedCoupon" class="coupon-applied">
              <div class="applied-left">
                <i class="bi bi-check-circle-fill"></i>
                <div>
                  <div class="applied-code">{{ appliedCoupon.code }}</div>
                  <div class="applied-desc">{{ appliedCoupon.description || 'Giảm giá đơn hàng' }}</div>
                </div>
              </div>
              <div class="applied-right">
                <span class="applied-discount">-{{ formatPrice(couponDiscount) }}</span>
                <button class="applied-remove" @click="cancelCoupon" title="Xoá mã"><i class="bi bi-x-lg"></i></button>
              </div>
            </div>

            <div v-if="!isGuest && claimedCoupons.length > 0 && !appliedCoupon" class="my-coupons">
              <button class="my-coupons-toggle" @click="showMyCoupons = !showMyCoupons">
                <i class="bi bi-wallet2"></i> Mã của tôi
                <span class="my-count">{{ claimedCoupons.length }}</span>
                <i :class="showMyCoupons ? 'bi bi-chevron-up' : 'bi bi-chevron-down'"></i>
              </button>
              <transition name="slide">
                <div v-if="showMyCoupons" class="claimed-list">
                  <div v-for="c in claimedCoupons" :key="c.claimedId" class="claimed-item" @click="selectClaimedCoupon(c)">
                    <div class="claimed-left">
                      <span class="claimed-code">{{ c.code }}</span>
                      <span class="claimed-desc">{{ c.description }}</span>
                    </div>
                    <i class="bi bi-chevron-right"></i>
                  </div>
                </div>
              </transition>
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
            <div class="summary-row" v-if="couponDiscount > 0">
              <span>Giảm giá</span><span style="color: var(--success)">-{{ formatPrice(couponDiscount) }}</span>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-row" style="font-size: 18px; font-weight: 800">
              <span>Tổng cộng</span><span>{{ formatPrice(total - couponDiscount) }}</span>
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
.item-variant {
  font-size: 11px;
  color: var(--text-mid);
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
.checkout-sidebar .card {
  position: sticky;
  top: 24px;
}
@media (max-width: 768px) {
  .checkout-layout {
    grid-template-columns: 1fr;
  }
  .checkout-sidebar .card {
    position: static;
  }
}
.checkout-coupon {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #fff;
  margin-bottom: 16px;
}
.coupon-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  color: var(--text-dark);
}
.coupon-header i { color: var(--primary); font-size: 16px; }
.coupon-body { padding: 14px 16px; }
.coupon-input-group { display: flex; gap: 8px; }
.coupon-input {
  flex: 1;
  padding: 10px 14px;
  border: 2px solid var(--border);
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.coupon-input:focus { border-color: var(--primary); }
.coupon-input:disabled { background: var(--bg); }
.coupon-btn {
  padding: 10px 20px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.coupon-btn:hover { background: var(--primary-hover); }
.coupon-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.spinner-sm {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #fff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
.coupon-msg {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: 6px;
}
.coupon-msg.error {
  background: #fef2f2;
  color: #dc2626;
}
.coupon-msg.success {
  background: #f0fdf4;
  color: #16a34a;
}
.coupon-applied {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f0fdf4;
  border-radius: 0 0 var(--radius) var(--radius);
  gap: 12px;
}
.applied-left { display: flex; align-items: center; gap: 10px; flex: 1; }
.applied-left i { color: #16a34a; font-size: 20px; flex-shrink: 0; }
.applied-code { font-size: 14px; font-weight: 700; text-transform: uppercase; }
.applied-desc { font-size: 12px; color: var(--text-mid); margin-top: 1px; }
.applied-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.applied-discount { font-size: 15px; font-weight: 800; color: #16a34a; }
.applied-remove {
  width: 28px;
  height: 28px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-light);
  transition: all 0.15s;
}
.applied-remove:hover { border-color: #dc2626; color: #dc2626; }
.my-coupons { border-top: 1px solid var(--border); }
.my-coupons-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-mid);
  transition: color 0.15s;
}
.my-coupons-toggle:hover { color: var(--primary); }
.my-coupons-toggle i:first-child { color: var(--primary); }
.my-count {
  margin-left: auto;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 11px;
  font-weight: 700;
  padding: 1px 8px;
  border-radius: 999px;
}
.claimed-list { padding: 0 12px 12px; display: flex; flex-direction: column; gap: 6px; }
.claimed-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}
.claimed-item:hover { border-color: var(--primary); background: var(--primary-light); }
.claimed-left { flex: 1; }
.claimed-code { font-size: 13px; font-weight: 700; text-transform: uppercase; display: block; }
.claimed-desc { font-size: 12px; color: var(--text-mid); margin-top: 1px; }
.claimed-item i { color: var(--text-light); font-size: 12px; }
.slide-enter-active,
.slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from,
.slide-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
