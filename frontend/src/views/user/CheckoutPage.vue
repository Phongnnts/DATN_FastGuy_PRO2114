<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/order';
import { formatPrice } from '@/utils/format';
import { PAYMENT_METHOD_LABEL } from '@/utils/constants';
import { userApi, shippingApi, orderApi, storeApi } from '@/api';
import couponApi from '@/api/coupon';
import { useToast } from '@/stores/toast';

const toast = useToast();

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
const claimedCouponsLoading = ref(false);
const claimedCouponsError = ref('');
const showMyCoupons = ref(true);
const note = ref('');
const submitting = ref(false);
const IDEMPOTENCY_STORAGE_KEY = 'checkout_idempotency';
let memoryIdempotency = null;

function idempotencyKeyFor(payload) {
  const serialized = JSON.stringify(payload);
  try {
    const saved = JSON.parse(sessionStorage.getItem(IDEMPOTENCY_STORAGE_KEY) || 'null');
    if (saved?.payload === serialized && saved?.key) return saved.key;
  } catch {}
  if (memoryIdempotency?.payload === serialized) return memoryIdempotency.key;
  const key = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  memoryIdempotency = { payload: serialized, key };
  try { sessionStorage.setItem(IDEMPOTENCY_STORAGE_KEY, JSON.stringify(memoryIdempotency)); } catch {}
  return key;
}

function clearIdempotencyKey() {
  memoryIdempotency = null;
  try { sessionStorage.removeItem(IDEMPOTENCY_STORAGE_KEY); } catch {}
}
const storeConfig = ref(null);
const currentStep = ref(1);
const shippingError = ref('');

const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const selectedProvince = ref(null);
const selectedDistrict = ref(null);
const selectedWard = ref(null);
const loadingProvinces = ref(false);
const shippingFee = ref(null);
const feeLoading = ref(false);
const expectedDelivery = ref('');
const createdOrderId = ref(null);
let pendingDistrictId = null;
let pendingWardCode = null;

const serviceFee = computed(() => Number(storeConfig.value?.serviceFee) || 0);
const total = computed(() => Math.max(0, cart.subtotal + (shippingFee.value || 0) + serviceFee.value - couponDiscount.value));
const isStoreClosed = computed(() => storeConfig.value?.isOpen === false);
const hasInvalidItems = computed(() => cart.items.some(i => (i.variantStatus && i.variantStatus !== 'AVAILABLE') || (i.quantityAvailable != null && (Number(i.quantityAvailable) <= 0 || i.quantity > Number(i.quantityAvailable)))));

onMounted(async () => {
  try {
    storeConfig.value = await storeApi.getConfig();
  } catch {
    storeConfig.value = null;
  }
  loadingProvinces.value = true;
  try {
    const provData = await shippingApi.getProvinces();
    provinces.value = (provData || []).map(p => ({
      id: p.ProvinceID || p.province_id || p.provinceId,
      name: p.ProvinceName || p.province_name || p.provinceName,
    }));
    if (isGuest.value) {
      const hcm = provinces.value.find(p => p.name?.includes('Hồ Chí Minh'));
      if (hcm) selectedProvince.value = hcm.id;
    }

    if (!isGuest.value) {
      const addrData = await userApi.getAddresses();
      savedAddresses.value = addrData || [];
      const defaultAddr = savedAddresses.value.find(a => a.isDefault);
      if (defaultAddr) selectAddress(defaultAddr);
      await loadClaimedCoupons();
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
    if (pendingDistrictId && districts.value.some(d => d.id === pendingDistrictId)) {
      selectedDistrict.value = pendingDistrictId;
    }
    pendingDistrictId = null;
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
    if (pendingWardCode && wards.value.some(w => w.code === pendingWardCode)) {
      selectedWard.value = pendingWardCode;
    }
    pendingWardCode = null;
  } catch {
    wards.value = [];
    pendingWardCode = null;
  }
});

async function calculateShipping(code = selectedWard.value) {
  shippingFee.value = null;
  shippingError.value = '';
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
    const feeResp = Number(result.fee);
    if (!Number.isFinite(feeResp) || feeResp < 0) throw new Error('GHN không trả về phí giao hàng hợp lệ');
    shippingFee.value = feeResp;
    if (result.expectedDeliveryTime) expectedDelivery.value = result.expectedDeliveryTime;
  } catch {
    shippingFee.value = null;
    shippingError.value = 'Không thể tính phí giao hàng.';
  } finally {
    feeLoading.value = false;
  }
}

watch(selectedWard, calculateShipping);

function selectAddress(addr) {
  selectedAddressId.value = addr.addressId;
  useNewAddress.value = !addr.ghnDistrictId || !addr.ghnWardCode;
  street.value = addr.street || '';
  phone.value = addr.phone || '';
  recipientName.value = addr.recipientName || '';
  pendingDistrictId = addr.ghnDistrictId || null;
  pendingWardCode = addr.ghnWardCode || null;
  selectedProvince.value = addr.ghnProvinceId || null;
}

function useManualEntry() {
  selectedAddressId.value = null;
  useNewAddress.value = true;
  street.value = '';
  phone.value = '';
  recipientName.value = '';
  pendingWardCode = null;
}

function selectedAddress() {
  return savedAddresses.value.find(a => a.addressId === selectedAddressId.value);
}

function getFullAddress() {
  const saved = selectedAddress();
  const prov = provinces.value.find(p => p.id === selectedProvince.value);
  const dist = districts.value.find(d => d.id === selectedDistrict.value);
  const ward = wards.value.find(w => w.code === selectedWard.value);
  return [
    street.value.trim(),
    ward?.name || saved?.wardName || '',
    dist?.name || saved?.districtName || '',
    prov?.name || saved?.provinceName || '',
  ].filter(Boolean).join(', ');
}

function isValidPhone(value) {
  return /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/.test(value.trim());
}

function canPlaceOrder() {
  return selectedWard.value && selectedDistrict.value && shippingFee.value !== null
    && recipientName.value.trim().length >= 2 && isValidPhone(phone.value) && street.value.trim().length >= 5;
}

async function loadClaimedCoupons() {
  claimedCouponsLoading.value = true;
  claimedCouponsError.value = '';
  try {
    const data = await couponApi.getClaimed();
    claimedCoupons.value = Array.isArray(data) ? data : [];
  } catch {
    claimedCoupons.value = [];
    claimedCouponsError.value = 'Không thể tải ví mã. Vui lòng thử lại.';
  } finally {
    claimedCouponsLoading.value = false;
  }
}

function getProvinceName() {
  return provinces.value.find(p => p.id === selectedProvince.value)?.name || selectedAddress()?.provinceName || '';
}

function getDistrictName() {
  return districts.value.find(d => d.id === selectedDistrict.value)?.name || selectedAddress()?.districtName || '';
}

function getWardName() {
  return wards.value.find(w => w.code === selectedWard.value)?.name || selectedAddress()?.wardName || '';
}

let couponVerification = 0;

async function verifyCoupon() {
  if (!couponCode.value.trim()) return;
  const verification = ++couponVerification;
  verifyingCoupon.value = true;
  couponError.value = '';
  appliedCoupon.value = null;
  couponDiscount.value = 0;
  try {
    const res = await couponApi.verify(couponCode.value, cart.subtotal, shippingFee.value || 0);
    if (verification !== couponVerification) return;
    if (res.valid) {
      appliedCoupon.value = res;
      couponDiscount.value = res.discount;
    } else {
      couponError.value = res.message || 'Mã không hợp lệ';
    }
  } catch {
    if (verification === couponVerification) couponError.value = 'Lỗi kiểm tra mã';
  } finally {
    if (verification === couponVerification) verifyingCoupon.value = false;
  }
}

watch([() => cart.subtotal, shippingFee], () => {
  if (appliedCoupon.value) verifyCoupon();
});

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
  if (submitting.value) return;
  if (isStoreClosed.value) return toast.error('Cua hang hien da dong cua. Vui long quay lai trong gio hoat dong');
  if (hasInvalidItems.value) return toast.error('Co mon da het hang hoac vuot ton kho, vui long cap nhat gio hang');
  if (!canPlaceOrder()) return toast.error('Vui long dien day du thong tin giao hang');
  const fullAddress = getFullAddress();
  if (!fullAddress) return toast.error('Vui long nhap dia chi');
  submitting.value = true;
  try {
    if (isGuest.value) {
      const items = cart.items.map(i => ({
        productId: i.productId,
        variantId: i.variantId,
        quantity: i.quantity,
        modifierOptionIds: (i.modifiers || []).map((modifier) => modifier.modifierOptionId),
      }));
      const payload = {
        customerName: recipientName.value.trim(),
        phone: phone.value.trim(),
        address: fullAddress,
        deliveryNote: note.value,
        paymentMethod: paymentMethod.value,
        items,
        ghnProvinceId: selectedProvince.value,
        ghnDistrictId: selectedDistrict.value,
        ghnWardCode: selectedWard.value,
        toProvinceName: getProvinceName(),
        toDistrictName: getDistrictName(),
        toWardName: getWardName(),
        couponCode: appliedCoupon.value?.code || '',
      };
      const result = await orderApi.guestCheckout(payload, idempotencyKeyFor(payload));
      clearIdempotencyKey();
      cart.clear();
      if (result.checkoutUrl) {
        window.location.assign(result.checkoutUrl);
        return;
      }
      router.push(`/track-order?code=${result.orderCode}&created=1`);
      return;
    }

    const payload = {
      customerName: recipientName.value.trim(),
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
      couponCode: appliedCoupon.value?.code || '',
      cartSignature: cart.items.map(item => ({
        variantId: item.variantId,
        quantity: item.quantity,
        modifierOptionIds: (item.modifiers || []).map(modifier => modifier.modifierOptionId).sort((a, b) => a - b),
      })).sort((a, b) => a.variantId - b.variantId),
    };
    const result = await orderStore.createOrder(payload, idempotencyKeyFor(payload));
    clearIdempotencyKey();
    createdOrderId.value = result.id;
    cart.clear();
    if (result.checkoutUrl) {
      window.location.assign(result.checkoutUrl);
      return;
    }
    router.push(`/account/orders/${createdOrderId.value}?created=1`);
  } catch (e) {
    toast.error(e.message);
  } finally {
    submitting.value = false;
  }
}

</script>

<template>
  <div class="checkout-page">
    <div class="container">
      <div class="checkout-breadcrumb"><router-link to="/">Trang chủ</router-link><i class="bi bi-chevron-right"></i><router-link to="/cart">Giỏ hàng</router-link><i class="bi bi-chevron-right"></i><strong>Thanh toán</strong></div>
      <div class="checkout-stepper" aria-label="Các bước thanh toán">
        <div class="step" :class="{ active: currentStep === 1, complete: currentStep > 1 }" :aria-current="currentStep === 1 ? 'step' : undefined"><span><i v-if="currentStep > 1" class="bi bi-check-lg"></i><template v-else>1</template></span><strong>Địa chỉ</strong></div><div class="step-line"></div>
        <div class="step" :class="{ active: currentStep === 2, complete: currentStep > 2 }" :aria-current="currentStep === 2 ? 'step' : undefined"><span><i v-if="currentStep > 2" class="bi bi-check-lg"></i><template v-else>2</template></span><strong>Ưu đãi</strong></div><div class="step-line"></div>
        <div class="step" :class="{ active: currentStep === 3 }" :aria-current="currentStep === 3 ? 'step' : undefined"><span>3</span><strong>Thanh toán</strong></div>
      </div>
    <div class="checkout-layout" v-if="cart.items.length > 0">
        <div class="checkout-main">
          <div v-if="storeConfig" class="store-status" :class="{ closed: isStoreClosed }">
            <i :class="isStoreClosed ? 'bi bi-shop-window' : 'bi bi-check-circle-fill'"></i>
            <span>{{ isStoreClosed ? 'Cửa hàng hiện đã đóng cửa' : 'Cửa hàng đang mở cửa' }} · {{ storeConfig.openTime }} - {{ storeConfig.closeTime }}</span>
          </div>
          <div v-show="currentStep === 1" class="card mb-3 checkout-block">
           <h3><i class="bi bi-geo-alt"></i> Thông tin nhận hàng</h3>

          <div v-if="!isGuest && savedAddresses.length > 0 && !useNewAddress" class="saved-addresses">
            <div
              v-for="addr in savedAddresses"
              :key="addr.addressId"
              class="saved-address-item"
              :class="{ selected: selectedAddressId === addr.addressId }"
              role="radio"
              tabindex="0"
              :aria-checked="selectedAddressId === addr.addressId"
              @click="selectAddress(addr)"
              @keydown.space.prevent="selectAddress(addr)"
              @keydown.enter="selectAddress(addr)"
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
            <div class="delivery-area">
              <i class="bi bi-geo-alt-fill"></i>
              Giao hàng nội thành TP. Hồ Chí Minh
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
           <div v-if="shippingError" class="shipping-error" role="alert">{{ shippingError }} <button type="button" @click="calculateShipping()">Thử lại</button></div>
           <div v-if="feeLoading" class="fee-loading" style="margin-top:8px;font-size:13px;color:var(--text-mid)">
            <i class="bi bi-arrow-repeat spin"></i> Đang tính phí giao hàng...
          </div>
          <div v-else-if="shippingFee !== null" class="fee-result" style="margin-top:8px">
            <i class="bi bi-truck"></i>
            Phí giao hàng: <strong>{{ formatPrice(shippingFee) }}</strong>
            <span v-if="expectedDelivery" style="display:block;font-size:12px;color:var(--text-mid);margin-top:4px">
              <i class="bi bi-clock"></i> Dự kiến: {{ expectedDelivery }}
            </span>
          </div>
          <div v-if="storeConfig" class="fee-result" style="margin-top:8px">
            <i class="bi bi-receipt"></i> Phí phục vụ: <strong>{{ formatPrice(serviceFee) }}</strong>
          </div>
        </div>
         <div v-show="currentStep === 3" class="card mb-3 checkout-block">
           <h3><i class="bi bi-credit-card"></i> Phương thức thanh toán</h3>
          <div class="payment-selector">
            <div
              v-for="(label, key) in PAYMENT_METHOD_LABEL"
              :key="key"
               class="payment-option"
               :class="{ selected: paymentMethod === key }"
               role="radio"
                :tabindex="paymentMethod === key ? 0 : -1"
                :aria-checked="paymentMethod === key"
                @click="paymentMethod = key"
                @keydown.space.prevent="paymentMethod = key"
                @keydown.enter="paymentMethod = key"
                @keydown.right.prevent="paymentMethod = key === 'COD' ? 'BANK_TRANSFER' : 'COD'"
                @keydown.left.prevent="paymentMethod = key === 'COD' ? 'BANK_TRANSFER' : 'COD'"
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
            <p><strong>Thanh toán PayOS</strong></p>
            <p style="color:var(--text-mid);font-size:13px;margin-top:4px">Cửa hàng xác nhận tồn kho trước, sau đó bạn sẽ nhận được link PayOS để thanh toán an toàn.</p>
          </div>
        </div>
         <div v-show="currentStep === 3" class="card mb-3 checkout-block">
           <h3><i class="bi bi-chat-left-text"></i> Ghi chú cho cửa hàng</h3>
          <textarea
            v-model="note"
            class="form-textarea"
            placeholder="Ghi chú cho cửa hàng..."
            rows="2"
          ></textarea>
        </div>
      </div>
       <div v-show="currentStep >= 2" class="checkout-sidebar">
        <div class="card order-summary-card">
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
                <div v-if="item.variantStatus && item.variantStatus !== 'AVAILABLE' || (item.quantityAvailable != null && Number(item.quantityAvailable) <= 0)" class="item-stock-warning">Hết hàng</div>
                <div v-else-if="item.quantityAvailable != null && item.quantity > Number(item.quantityAvailable)" class="item-stock-warning">Chỉ còn {{ item.quantityAvailable }} phần</div>
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
          <div v-if="!isGuest" class="checkout-coupon">
            <div class="coupon-header">
              <i class="bi bi-tag"></i>
              <span>Mã giảm giá</span>
            </div>

            <div v-if="!isGuest && !appliedCoupon" class="my-coupons">
              <div class="my-coupons-heading">
                <div>
                  <span>Mã đã nhận</span>
                  <small v-if="claimedCouponsLoading">Đang tải ví mã...</small>
                  <small v-else-if="claimedCoupons.length">Chọn một mã để áp dụng</small>
                  <small v-else-if="claimedCouponsError">{{ claimedCouponsError }}</small>
                  <small v-else>Chưa có mã nào trong ví của bạn</small>
                </div>
                <button class="my-coupons-toggle" @click="loadClaimedCoupons" :disabled="claimedCouponsLoading" title="Làm mới ví mã">
                  <i :class="claimedCouponsLoading ? 'bi bi-arrow-repeat spin' : 'bi bi-arrow-clockwise'"></i>
                </button>
                <button v-if="claimedCoupons.length" class="my-coupons-toggle" @click="showMyCoupons = !showMyCoupons">
                  <span class="my-count">{{ claimedCoupons.length }}</span>
                  <i :class="showMyCoupons ? 'bi bi-chevron-up' : 'bi bi-chevron-down'"></i>
                </button>
              </div>
              <router-link v-if="!claimedCouponsLoading && !claimedCoupons.length && !claimedCouponsError" to="/promotions" class="coupon-empty-link">
                Xem và nhận mã khuyến mãi <i class="bi bi-arrow-right"></i>
              </router-link>
              <transition name="slide">
                <div v-if="showMyCoupons && claimedCoupons.length" class="claimed-list">
                  <button v-for="c in claimedCoupons" :key="c.claimedId" type="button" class="claimed-item" @click="selectClaimedCoupon(c)">
                    <span class="claimed-ticket-icon"><i class="bi bi-ticket-perforated"></i></span>
                    <span class="claimed-left">
                      <strong class="claimed-code">{{ c.code }}</strong>
                      <span class="claimed-desc">{{ c.description || 'Áp dụng cho đơn hàng đủ điều kiện' }}</span>
                    </span>
                    <i class="bi bi-arrow-up-right"></i>
                  </button>
                </div>
              </transition>
            </div>

            <div v-if="!appliedCoupon && couponError" class="coupon-body">
              <div class="coupon-msg error">
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
            <div class="summary-row" v-if="storeConfig">
              <span>Phí phục vụ</span><span>{{ formatPrice(serviceFee) }}</span>
            </div>
            <div class="summary-row" v-if="couponDiscount > 0">
              <span>Giảm giá</span><span style="color: var(--success)">-{{ formatPrice(couponDiscount) }}</span>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-row" style="font-size: 18px; font-weight: 800">
              <span>Tổng cộng</span><span>{{ formatPrice(total) }}</span>
            </div>
          </div>
          <p v-if="isStoreClosed" class="stock-warning">Cửa hàng đang đóng cửa ({{ storeConfig.openTime }} - {{ storeConfig.closeTime }}), chưa thể đặt hàng.</p>
          <p v-else-if="hasInvalidItems" class="stock-warning">Có món đã hết hàng hoặc vượt tồn kho</p>
          <button
            v-if="currentStep === 3"
            class="btn btn-lg btn-primary checkout-btn"
            @click="placeOrder"
            :disabled="submitting || isStoreClosed || !canPlaceOrder() || feeLoading || hasInvalidItems"
          >
            <i v-if="submitting" class="bi bi-arrow-repeat spin"></i>
            {{ submitting ? 'Đang xử lý...' : 'Đặt hàng' }}
          </button>
        </div>
      </div>
      <div class="checkout-actions">
        <button v-if="currentStep > 1" type="button" class="btn btn-outline" @click="currentStep--">Quay lại</button>
        <button v-if="currentStep < 3" type="button" class="btn btn-primary" :disabled="currentStep === 1 && !canPlaceOrder()" @click="currentStep++">Tiếp tục</button>
      </div>
    </div>
    <div v-else class="empty-state" style="padding: 60px 0">
      <i class="bi bi-cart3"></i>
      <h3>Giỏ hàng trống</h3>
      <router-link to="/menu" class="btn btn-primary">Mua sắm ngay</router-link>
    </div>
    </div>
  </div>
</template>

<style scoped>
.checkout-actions { display: flex; justify-content: space-between; gap: 12px; grid-column: 1 / -1; }
.shipping-error { margin-top: 8px; color: var(--red-active); font-size: 13px; }
.shipping-error button { color: var(--primary-dark); font-weight: 700; text-decoration: underline; }
.store-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: var(--radius);
  background: #ecfdf5;
  color: #047857;
  font-size: 13px;
  font-weight: 600;
}
.store-status.closed { background: #fef2f2; color: var(--red-active); }
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
.delivery-area {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: var(--primary-light);
  color: var(--primary-dark);
  font-size: 13px;
  font-weight: 600;
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
.item-stock-warning,
.stock-warning {
  margin-top: 4px;
  color: var(--red-active);
  font-size: 12px;
  font-weight: 600;
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
.coupon-input-group { display: flex; min-width: 0; gap: 8px; }
.coupon-input {
  flex: 1 1 auto;
  min-width: 0;
  padding: 10px 12px;
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
  flex: 0 0 auto;
  padding: 10px 14px;
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
.my-coupons {
  margin: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fffaf4;
}
.my-coupons-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
}
.my-coupons-heading > div { display: grid; gap: 2px; }
.my-coupons-heading span { font-size: 13px; font-weight: 800; color: var(--text-dark); }
.my-coupons-heading small { font-size: 11px; color: var(--text-mid); }
.coupon-empty-link { display: flex; align-items: center; justify-content: space-between; margin: 0 14px 10px; padding: 9px 10px; border-radius: 7px; background: #fff; color: var(--primary-dark); font-size: 12px; font-weight: 700; }
.my-coupons-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-mid);
}
.my-coupons-toggle:hover { color: var(--primary-dark); }
.my-count {
  display: grid;
  place-items: center;
  min-width: 22px;
  height: 22px;
  background: var(--primary-dark);
  color: #fff !important;
  font-size: 11px;
  font-weight: 700;
  padding: 0 6px;
  border-radius: 999px;
}
.claimed-list { padding: 0 14px 14px; display: flex; flex-direction: column; gap: 6px; }
.claimed-item {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 9px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s;
}
.claimed-item:hover { border-color: var(--primary-dark); background: var(--primary-light); transform: translateY(-1px); }
.claimed-ticket-icon { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 8px; background: #fff0e8; color: var(--primary-dark); }
.claimed-left { display: grid; flex: 1; min-width: 0; gap: 2px; }
.claimed-code { font-size: 13px; font-weight: 800; text-transform: uppercase; display: block; color: var(--text-dark); }
.claimed-desc { font-size: 12px; color: var(--text-mid); margin-top: 1px; }
.claimed-item i { color: var(--text-light); font-size: 12px; }
.coupon-manual-label { display: block; margin-bottom: 7px; color: var(--text-mid); font-size: 12px; font-weight: 700; }
.slide-enter-active,
.slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from,
.slide-leave-to { opacity: 0; transform: translateY(-8px); }

/* Checkout route layout */
.checkout-page { min-height: 100vh; padding: 22px 0 56px; background: #fff8f0; }
.checkout-breadcrumb { display: flex; align-items: center; gap: 8px; color: var(--text-mid); font-size: 12px; margin-bottom: 18px; }
.checkout-breadcrumb a { color: var(--text-dark); font-weight: 600; }.checkout-breadcrumb i { color: var(--text-light); font-size: 10px; }
.checkout-stepper { display: grid; grid-template-columns: auto 1fr auto 1fr auto; align-items: center; gap: 12px; padding: 18px 24px; margin-bottom: 16px; border: 1px solid var(--border-light); border-radius: var(--radius-lg); background: #fff; }
.checkout-stepper .step { display: grid; justify-items: center; gap: 6px; color: var(--text-mid); font-size: 12px; white-space: nowrap; }.checkout-stepper .step span { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 50%; background: #e9eef8; font-size: 12px; font-weight: 800; }.checkout-stepper .step.complete { color: var(--primary-dark); }.checkout-stepper .step.complete span { background: #ffc89b; color: var(--primary-dark); }.checkout-stepper .step.active { color: var(--primary-dark); }.checkout-stepper .step.active span { background: var(--primary-dark); color: #fff; }.checkout-stepper .step-line { height: 1px; background: linear-gradient(90deg, var(--primary-100), var(--border)); }
.checkout-layout { grid-template-columns: minmax(0, 1fr) 340px; gap: 18px; }.checkout-main { min-width: 0; }.checkout-block { border-radius: var(--radius-lg); }.checkout-main h3, .checkout-sidebar h3 { display: flex; align-items: center; gap: 8px; font-size: 15px; }.checkout-main h3 > i { color: var(--primary-dark); }
.saved-address-item { border: 1px solid var(--border); border-radius: var(--radius); }.saved-address-item.selected { border-color: var(--primary-dark); background: var(--primary-light); box-shadow: 0 0 0 3px var(--primary-50); }.saved-address-item:hover { border-color: var(--primary); }.radio-circle.checked { border-color: var(--primary-dark); background: var(--primary-dark); }
.payment-option { border: 1px solid var(--border); border-radius: var(--radius); }.payment-option.selected { border-color: var(--primary-dark); background: #fff0e8; box-shadow: 0 0 0 3px var(--primary-50); }.payment-option:hover { border-color: var(--primary); }.payment-option i:first-child, .selected-icon { color: var(--primary-dark) !important; }
.order-summary-card { border-radius: var(--radius-lg); overflow: hidden; }.order-summary-card::before { content: ''; display: block; height: 4px; margin: -24px -24px 18px; background: linear-gradient(90deg, var(--primary-dark), var(--route-amber)); }.checkout-sidebar .card { top: 82px; }.checkout-item { border-bottom-color: var(--border-light); }.checkout-item img { border-radius: var(--radius-sm); }.checkout-summary { border-top: 1px dashed var(--border); padding-top: 12px; }.checkout-summary .summary-row:last-child span:last-child { color: var(--primary-dark); font-size: 24px; letter-spacing: -.03em; }.checkout-btn { min-height: 48px; border-radius: var(--radius-full); background: linear-gradient(135deg, var(--primary-dark), var(--route-orange)); box-shadow: 0 12px 22px rgba(212,97,58,.2); }.checkout-coupon { border-color: var(--border-light); border-radius: var(--radius); }.coupon-header { border-bottom-color: var(--border-light); }.coupon-header i, .my-coupons-toggle i:first-child { color: var(--primary-dark); }.coupon-input { border: 1px solid var(--border); border-radius: var(--radius-sm); }.coupon-input:focus { border-color: var(--primary-dark); }.coupon-btn { border-radius: var(--radius-sm); background: var(--primary-dark); }.coupon-btn:hover { background: var(--route-orange); }
@media (max-width: 768px) { .checkout-stepper { grid-template-columns: repeat(3, 1fr); gap: 4px; padding: 14px 8px; }.checkout-stepper .step { white-space: normal; text-align: center; font-size: 10px; }.checkout-stepper .step-line { display: none; }.checkout-layout { grid-template-columns: 1fr; }.checkout-sidebar .card { position: static; }.checkout-page { padding-top: 16px; } }
@media (max-width: 380px) { .coupon-input-group { flex-direction: column; }.coupon-btn { width: 100%; } }
@media (max-width: 768px) { .checkout-actions { position: sticky; bottom: 0; z-index: 5; margin: 0 -12px -12px; padding: 12px; background: #fff; border-top: 1px solid var(--border); }.checkout-actions .btn { flex: 1; } }
</style>
