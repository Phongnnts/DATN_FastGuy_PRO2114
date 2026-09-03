<script setup>
import { reactive, ref, computed, nextTick, onBeforeUnmount, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import CheckoutStepper from '@/components/common/CheckoutStepper.vue';
import FormField from '@/components/common/FormField.vue';
import { validPhone } from '@/utils/formValidation';
import { addressModeState, createShippingValidationState, runValidatedShippingSubmit } from '@/utils/shippingFormValidation';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/order';
import { useProductStore } from '@/stores/product';
import { formatPrice } from '@/utils/format';
import { PAYMENT_METHOD_LABEL } from '@/utils/constants';
import { createCouponController } from '@/utils/checkoutCoupon';
import { userApi, shippingApi, orderApi, storeApi } from '@/api';
import couponApi from '@/api/coupon';
import { useToast } from '@/stores/toast';
import { loadAddressHierarchy } from '@/utils/checkoutAddress';
import { isPastOrderCutoff } from '@/utils/orderCutoff';

const toast = useToast();

const router = useRouter();
const auth = useAuthStore();
const cart = useCartStore();
const orderStore = useOrderStore();
const productStore = useProductStore();

const CONFLICT_MESSAGE = 'Một số món trong giỏ vừa hết hàng hoặc không đủ số lượng. Giỏ hàng đã được cập nhật, vui lòng kiểm tra lại trước khi đặt hàng.';
const HCM_GHN_PROVINCE_ID = 202;
const DELIVERY_AREA_MESSAGE = 'FastGuy chỉ giao hàng trong TP. Hồ Chí Minh. Vui lòng chọn lại tỉnh/thành.';

const isGuest = computed(() => !auth.isLoggedIn);

const savedAddresses = ref([]);
const selectedAddressId = ref(null);
const useNewAddress = ref(false);
const phone = ref('');
const recipientName = ref('');
const street = ref('');
const shippingValidation = createShippingValidationState();
const shippingTouched = reactive(shippingValidation.touched);
const shippingErrors = reactive(shippingValidation.errors);
const paymentMethod = ref(isGuest.value ? 'BANK_TRANSFER' : 'COD');
const availablePaymentMethods = ref(['COD', 'BANK_TRANSFER']);
const paymentAvailability = ref({
  COD: { enabled: true },
  BANK_TRANSFER: { enabled: false, reason: 'PayOS tạm không khả dụng' },
});
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
const cutoffNow = ref(new Date());
let cutoffTimer;
const currentStep = ref(1);
const shippingError = ref('');
const provinceError = ref('');

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
const codAccountDialog = ref(false);
const codDialogTrigger = ref(null);
let addressSelectionGeneration = 0;
let applyingSavedAddress = false;

const serviceFee = computed(() => Number(storeConfig.value?.serviceFee) || 0);
const total = computed(() => Math.max(0, cart.subtotal + (shippingFee.value || 0) + serviceFee.value - couponDiscount.value));
const cutoffClosed = computed(() => isPastOrderCutoff(storeConfig.value?.orderCutoffTime, cutoffNow.value));
const isStoreClosed = computed(() => storeConfig.value?.isOpen === false || cutoffClosed.value);
const hasInvalidItems = computed(() => cart.items.some(i => (i.variantStatus && i.variantStatus !== 'AVAILABLE') || (i.quantityAvailable != null && (Number(i.quantityAvailable) <= 0 || i.quantity > Number(i.quantityAvailable)))));

onMounted(async () => {
  cutoffTimer = setInterval(() => { cutoffNow.value = new Date(); }, 30000);
  try {
    const capabilities = await orderApi.getPaymentCapabilities();
    paymentAvailability.value = {
      COD: { enabled: true },
      BANK_TRANSFER: capabilities?.availability?.BANK_TRANSFER || { enabled: false, reason: 'PayOS tạm không khả dụng' },
    };
  } catch {
    paymentAvailability.value.BANK_TRANSFER = { enabled: false, reason: 'PayOS tạm không khả dụng' };
  }
  if (!isPaymentEnabled(paymentMethod.value) && !isGuest.value) paymentMethod.value = 'COD';
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
      const hcm = provinces.value.find(p => Number(p.id) === HCM_GHN_PROVINCE_ID);
      if (hcm) selectedProvince.value = hcm.id;
    }

    if (!isGuest.value) {
      const addrData = await userApi.getAddresses();
      savedAddresses.value = addrData || [];
      const defaultAddr = savedAddresses.value.find(a => a.isDefault);
      if (defaultAddr) await selectAddress(defaultAddr);
      await loadClaimedCoupons();
    }
  } catch {
    provinces.value = [];
    savedAddresses.value = [];
  } finally {
    loadingProvinces.value = false;
  }
});

function isSupportedProvince(id) {
  return Number(id) === HCM_GHN_PROVINCE_ID;
}

watch(selectedProvince, async (id) => {
  provinceError.value = id && !isSupportedProvince(id) ? DELIVERY_AREA_MESSAGE : '';
  if (applyingSavedAddress) return;
  const generation = ++addressSelectionGeneration;
  districts.value = [];
  wards.value = [];
  selectedDistrict.value = null;
  selectedWard.value = null;
  shippingValidation.resetDependents(['district', 'ward'], shippingValues());
  shippingFee.value = null;
  if (!id) return;
  try {
    const data = await shippingApi.getDistricts(id);
    districts.value = (data || []).map(d => ({
      id: d.DistrictID || d.district_id || d.districtId,
      name: d.DistrictName || d.district_name || d.districtName,
    }));
    if (generation !== addressSelectionGeneration) return;
  } catch {
    if (generation === addressSelectionGeneration) districts.value = [];
  }
});

watch(selectedDistrict, async (id) => {
  if (applyingSavedAddress) return;
  const generation = ++addressSelectionGeneration;
  wards.value = [];
  selectedWard.value = null;
  shippingValidation.resetDependents(['ward'], shippingValues());
  inputShippingField('district');
  shippingFee.value = null;
  if (!id) return;
  try {
    const data = await shippingApi.getWards(id);
    wards.value = (data || []).map(w => ({
      code: w.WardCode || w.ward_code || w.wardCode,
      name: w.WardName || w.ward_name || w.wardName,
    }));
    if (generation !== addressSelectionGeneration) return;
  } catch {
    if (generation === addressSelectionGeneration) wards.value = [];
  }
});

async function calculateShipping(code = selectedWard.value, generation = addressSelectionGeneration) {
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
    if (generation !== addressSelectionGeneration) return;
    shippingFee.value = feeResp;
    if (result.expectedDeliveryTime) expectedDelivery.value = result.expectedDeliveryTime;
  } catch {
    if (generation !== addressSelectionGeneration) return;
    shippingFee.value = null;
    shippingError.value = 'Dịch vụ giao hàng chưa được cấu hình hoặc tạm không khả dụng. Vui lòng thử lại sau.';
  } finally {
    if (generation === addressSelectionGeneration) feeLoading.value = false;
  }
}

watch(selectedWard, code => { if (!applyingSavedAddress) calculateShipping(code); });

async function selectAddress(addr) {
  selectedAddressId.value = addr.addressId;
  useNewAddress.value = !addr.ghnDistrictId || !addr.ghnWardCode;
  street.value = addr.street || '';
  phone.value = addr.phone || '';
  recipientName.value = addr.recipientName || '';
  if (!useNewAddress.value) await applySavedAddress(addr);
}

async function applySavedAddress(addr) {
  const generation = ++addressSelectionGeneration;
  applyingSavedAddress = true;
  shippingFee.value = null;
  shippingError.value = '';
  try {
    const hierarchy = await loadAddressHierarchy(addr, shippingApi);
    if (generation !== addressSelectionGeneration) return;
    selectedProvince.value = hierarchy.provinceId;
    districts.value = hierarchy.districts;
    wards.value = hierarchy.wards;
    selectedDistrict.value = hierarchy.selectedDistrict;
    selectedWard.value = hierarchy.selectedWard;
    await nextTick();
    applyingSavedAddress = false;
    await calculateShipping(hierarchy.selectedWard, generation);
  } catch {
    if (generation !== addressSelectionGeneration) return;
    useNewAddress.value = true;
    shippingError.value = 'Địa chỉ đã lưu không còn khớp dữ liệu GHN. Vui lòng chọn lại địa chỉ giao hàng.';
  } finally {
    if (generation === addressSelectionGeneration) applyingSavedAddress = false;
  }
}

function useManualEntry() {
  const state = addressModeState('manual');
  Object.assign(shippingValidation.errors, { recipientName: '', phone: '', district: '', ward: '', street: '' });
  shippingValidation.reset();
  selectedAddressId.value = state.selectedAddressId;
  useNewAddress.value = state.useNewAddress;
  recipientName.value = state.recipientName;
  phone.value = state.phone;
  street.value = state.street;
  selectedDistrict.value = state.district;
  selectedWard.value = state.ward;
  shippingFee.value = null;
  shippingError.value = '';
  addressSelectionGeneration += 1;
}

async function returnToSavedAddresses() {
  shippingValidation.reset();
  shippingError.value = '';
  const saved = savedAddresses.value.find(address => address.isDefault) || savedAddresses.value[0];
  if (saved) await selectAddress(saved);
  else { useNewAddress.value = false; selectedAddressId.value = null; }
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

function shippingValues() { return { recipientName: recipientName.value, phone: phone.value, district: selectedDistrict.value, ward: selectedWard.value, street: street.value }; }
function blurShippingField(field) { shippingValidation.touch(field, shippingValues()); }
function inputShippingField(field) { shippingValidation.update(field, shippingValues()); }
function validateShippingForm() { return shippingValidation.validateAll(shippingValues()); }

function canPlaceOrder() {
  return selectedWard.value && selectedDistrict.value && shippingFee.value !== null
    && recipientName.value.trim().length >= 2 && validPhone(phone.value) && street.value.trim().length >= 5;
}

function isPaymentEnabled(key) {
  if (isGuest.value && key === 'COD') return false;
  return paymentAvailability.value[key]?.enabled === true;
}

async function selectPaymentMethod(key, event) {
  if (isGuest.value && key === 'COD') {
    codDialogTrigger.value = event?.currentTarget || document.activeElement;
    codAccountDialog.value = true;
    document.body.style.overflow = 'hidden';
    await nextTick();
    document.querySelector('.cod-account-dialog .btn-primary')?.focus();
    return;
  }
  if (!isPaymentEnabled(key)) return;
  paymentMethod.value = key;
}

function closeCodAccountDialog() {
  codAccountDialog.value = false;
  document.body.style.overflow = '';
  nextTick(() => codDialogTrigger.value?.focus());
}

function goToAccount(name) {
  closeCodAccountDialog();
  router.push({ name, query: { redirect: '/checkout' } });
}

function handleCodDialogKeydown(event) {
  if (event.key === 'Escape') { event.preventDefault(); closeCodAccountDialog(); return; }
  if (event.key !== 'Tab') return;
  const buttons = [...event.currentTarget.querySelectorAll('button:not([disabled])')];
  if (!buttons.length) return;
  if (event.shiftKey && document.activeElement === buttons[0]) { event.preventDefault(); buttons.at(-1).focus(); }
  else if (!event.shiftKey && document.activeElement === buttons.at(-1)) { event.preventDefault(); buttons[0].focus(); }
}

onBeforeUnmount(() => { addressSelectionGeneration += 1; clearInterval(cutoffTimer); document.body.style.overflow = ''; });

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

const couponState = {
  get code() { return couponCode.value; },
  set code(value) { couponCode.value = value; },
  get applied() { return appliedCoupon.value; },
  set applied(value) { appliedCoupon.value = value; },
  get discount() { return couponDiscount.value; },
  set discount(value) { couponDiscount.value = value; },
  get verifying() { return verifyingCoupon.value; },
  set verifying(value) { verifyingCoupon.value = value; },
  get error() { return couponError.value; },
  set error(value) { couponError.value = value; },
};
const couponController = createCouponController(couponState, couponApi.verify);

function verifyCoupon() {
  return couponController.verify(cart.subtotal, shippingFee.value || 0);
}

function invalidateCoupon() {
  couponController.invalidate();
}

watch([() => cart.subtotal, shippingFee], invalidateCoupon);

function cancelCoupon() {
  couponController.remove();
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
  return runValidatedShippingSubmit(shippingValidation, shippingValues(), executePlaceOrder);
}

async function executePlaceOrder() {
  try { storeConfig.value = await storeApi.getConfig(); cutoffNow.value = new Date(); }
  catch {}
  if (storeConfig.value?.isOpen === false || isPastOrderCutoff(storeConfig.value?.orderCutoffTime, new Date())) return toast.error('Cửa hàng hiện đã đóng cửa. Vui lòng quay lại trong giờ hoạt động');
  if (hasInvalidItems.value) return toast.error('Co mon da het hang hoac vuot ton kho, vui long cap nhat gio hang');
  if (!isSupportedProvince(selectedProvince.value)) return toast.error(DELIVERY_AREA_MESSAGE);
  if (shippingFee.value === null) return toast.error('Dịch vụ giao hàng tạm không khả dụng. Vui lòng thử lại sau.');
  if (!isPaymentEnabled(paymentMethod.value)) return toast.error('Phương thức thanh toán đang chọn không khả dụng.');
  if (!canPlaceOrder()) return toast.error('Vui lòng điền đầy đủ thông tin giao hàng');
  const fullAddress = getFullAddress();
  if (!fullAddress) return toast.error('Vui lòng nhập địa chỉ');
  let paymentWindow = paymentMethod.value === 'BANK_TRANSFER' ? window.open('', '_blank') : null;
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
      if (result.paymentRetryable) {
        paymentWindow?.close();
        toast.error(`Đơn ${result.orderCode} đã được tạo. Bấm đặt hàng lại để thử tạo liên kết thanh toán.`);
        return;
      }
      clearIdempotencyKey();
      cart.clear();
      if (result.checkoutUrl) {
        if (result.returnProof) sessionStorage.setItem(`guest-payment-proof:${result.orderCode}`, result.returnProof);
        if (!paymentWindow) { window.location.assign(result.checkoutUrl); return; }
        paymentWindow.location.href = result.checkoutUrl;
        window.__fastGuyPaymentWindow = paymentWindow;
        router.push({ name: 'PaymentReturn', query: { orderId: result.orderId, orderCode: result.orderCode, returnProof: result.returnProof } });
        return;
      }
      sessionStorage.setItem(`order-success:${result.orderCode}`, '1');
      router.push({ name: 'OrderSuccess', query: { orderCode: result.orderCode } });
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
    createdOrderId.value = result.id;
    if (result.paymentRetryable) {
      paymentWindow?.close();
      toast.error(`Đơn ${result.orderCode} đã được tạo. Bấm đặt hàng lại để thử tạo liên kết thanh toán.`);
      return;
    }
    clearIdempotencyKey();
    cart.clear();
    if (result.checkoutUrl) {
      if (!paymentWindow) { window.location.assign(result.checkoutUrl); return; }
      paymentWindow.location.href = result.checkoutUrl;
      window.__fastGuyPaymentWindow = paymentWindow;
      router.push({ name: 'PaymentReturn', query: { orderId: createdOrderId.value, orderCode: result.orderCode } });
      return;
    }
    router.push({ name: 'OrderSuccess', query: { orderId: createdOrderId.value, orderCode: result.orderCode } });
  } catch (e) {
    paymentWindow?.close();
    if (e?.status === 409) {
      clearIdempotencyKey();
      if (!isGuest.value) await cart.fetchCart();
      await productStore.refreshAvailability();
      toast.error(CONFLICT_MESSAGE);
    } else if (e?.code === 'STORE_CLOSED' || e?.response?.data?.code === 'STORE_CLOSED') {
      toast.error(e?.response?.data?.message || e.message || 'Cửa hàng đã ngừng nhận đơn.');
    } else {
      toast.error(e.message);
    }
  } finally {
    submitting.value = false;
  }
}

</script>

<template>
  <div class="checkout-page">
    <div class="container">
      <div class="checkout-breadcrumb"><router-link to="/home">Trang chủ</router-link><i class="bi bi-chevron-right"></i><router-link to="/cart">Giỏ hàng</router-link><i class="bi bi-chevron-right"></i><strong>Thanh toán</strong></div>
      <CheckoutStepper :current="currentStep === 3 ? 3 : 2" />
      <div v-if="storeConfig" class="store-status" :class="{ closed: isStoreClosed }">
        <i :class="isStoreClosed ? 'bi bi-shop-window' : 'bi bi-check-circle-fill'"></i>
        <span>{{ isStoreClosed ? 'Cửa hàng hiện đã đóng cửa' : 'Cửa hàng đang mở cửa' }} · {{ storeConfig.openTime }} - {{ storeConfig.closeTime }} · Nhận đơn đến {{ storeConfig.orderCutoffTime }}</span>
      </div>
    <div v-if="cart.items.length > 0" class="checkout-shell">
      <div class="checkout-layout">
        <div class="checkout-main">
          <div v-show="currentStep <= 2" class="card mb-3 checkout-block checkout-section delivery-section">
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

          <div v-if="useNewAddress || savedAddresses.length === 0" class="manual-address-form">
            <button v-if="useNewAddress && savedAddresses.length" type="button" class="saved-address-back" @click="returnToSavedAddresses"><i class="bi bi-arrow-left" aria-hidden="true"></i> Chọn địa chỉ đã lưu</button>
            <FormField id="checkout-recipient" label="Tên người nhận" required :error="shippingErrors.recipientName"><template #default="{ controlAttrs }"><input v-bind="controlAttrs" v-model="recipientName" class="form-input" placeholder="Họ tên người nhận" minlength="2" maxlength="100" @blur="blurShippingField('recipientName')" @input="inputShippingField('recipientName')" /></template></FormField>
            <div class="delivery-area">
              <i class="bi bi-geo-alt-fill"></i>
              Giao hàng nội thành TP. Hồ Chí Minh
            </div>
            <div v-if="provinceError" class="shipping-error" role="alert">{{ provinceError }}</div>
            <FormField id="checkout-district" label="Quận / Huyện" required :error="shippingErrors.district"><template #default="{ controlAttrs }"><select v-bind="controlAttrs" v-model="selectedDistrict" class="form-select" :disabled="!selectedProvince" @blur="blurShippingField('district')" @change="inputShippingField('district')"><option :value="null">Chọn quận/huyện</option><option v-for="d in districts" :key="d.id" :value="d.id">{{ d.name }}</option></select></template></FormField>
            <FormField id="checkout-ward" label="Phường / Xã" required :error="shippingErrors.ward"><template #default="{ controlAttrs }"><select v-bind="controlAttrs" v-model="selectedWard" class="form-select" :disabled="!selectedDistrict" @blur="blurShippingField('ward')" @change="inputShippingField('ward')"><option :value="null">Chọn phường/xã</option><option v-for="w in wards" :key="w.code" :value="w.code">{{ w.name }}</option></select></template></FormField>
            <FormField id="checkout-street" label="Số nhà, tên đường" required :error="shippingErrors.street"><template #default="{ controlAttrs }"><input v-bind="controlAttrs" v-model="street" class="form-input" placeholder="VD: 123 Nguyễn Huệ" minlength="5" maxlength="255" @blur="blurShippingField('street')" @input="inputShippingField('street')" /></template></FormField>
            <FormField id="checkout-phone" label="Số điện thoại" required :error="shippingErrors.phone"><template #default="{ controlAttrs }"><input v-bind="controlAttrs" v-model="phone" type="tel" class="form-input" placeholder="Số điện thoại nhận hàng" @blur="blurShippingField('phone')" @input="inputShippingField('phone')" /></template></FormField>
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
        </div>
<div v-show="currentStep === 3" class="card mb-3 checkout-block checkout-section payment-section">
            <h3><i class="bi bi-credit-card"></i> Phương thức thanh toán</h3>
          <div class="payment-selector">
            <div
               v-for="key in availablePaymentMethods"
               :key="key"
               class="payment-option"
               :class="{ selected: paymentMethod === key, disabled: !isPaymentEnabled(key) }"
               role="radio"
                :tabindex="isGuest && key === 'COD' ? 0 : paymentMethod === key && isPaymentEnabled(key) ? 0 : -1"
                :aria-checked="paymentMethod === key"
                :aria-disabled="!isPaymentEnabled(key) && !(isGuest && key === 'COD')"
                 @click="selectPaymentMethod(key, $event)"
                 @keydown.space.prevent="selectPaymentMethod(key, $event)"
                 @keydown.enter="selectPaymentMethod(key, $event)"
            >
              <i
                :class="
                  key === 'COD'
                    ? 'bi bi-cash'
                    : 'bi bi-qr-code-scan'
                "
              ></i>
               <span>{{ PAYMENT_METHOD_LABEL[key] }}</span>
               <i
                 v-if="paymentMethod === key"
                 class="bi bi-check-circle-fill selected-icon"
               ></i>
               <small v-if="!isPaymentEnabled(key)">{{ paymentAvailability[key]?.reason }}</small>
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
       <div class="checkout-sidebar checkout-summary-panel">
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
          <div class="checkout-coupon checkout-section coupon-section">
            <div class="coupon-header">
              <i class="bi bi-tag"></i>
              <span>Mã giảm giá</span>
            </div>

            <form class="coupon-manual" @submit.prevent="verifyCoupon" v-if="!appliedCoupon">
              <label for="checkout-coupon-code">Nhập mã giảm giá</label>
              <div class="coupon-manual-row">
                <input
                  id="checkout-coupon-code"
                  v-model.trim="couponCode"
                  class="form-input"
                  type="text"
                  autocomplete="off"
                  maxlength="50"
                  aria-describedby="checkout-coupon-status"
                  placeholder="Ví dụ: FASTGUY10"
                />
                <button type="submit" class="btn btn-outline" :disabled="verifyingCoupon || !couponCode.trim()">
                  {{ verifyingCoupon ? 'Đang kiểm tra...' : 'Áp dụng' }}
                </button>
              </div>
              <p v-if="verifyingCoupon" id="checkout-coupon-status" class="coupon-msg" role="status">Đang kiểm tra mã giảm giá.</p>
              <p v-else-if="couponError" id="checkout-coupon-status" class="coupon-msg error" role="alert">{{ couponError }}</p>
              <p v-else id="checkout-coupon-status" class="sr-only">Nhập mã rồi chọn Áp dụng.</p>
            </form>

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

            <div v-if="appliedCoupon" class="coupon-applied" role="status">
              <div class="applied-left">
                <i class="bi bi-check-circle-fill"></i>
                <div>
                  <div class="applied-code">{{ appliedCoupon.code }}</div>
                  <div class="applied-desc">{{ appliedCoupon.description || 'Giảm giá đơn hàng' }}</div>
                </div>
              </div>
              <div class="applied-right">
                <span class="applied-discount">-{{ formatPrice(couponDiscount) }}</span>
                <button type="button" class="applied-remove" @click="cancelCoupon" aria-label="Xoá mã giảm giá" title="Xoá mã"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
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
            <div class="summary-row" v-if="couponDiscount > 0">
              <span>Giảm giá</span><span style="color: var(--success)">-{{ formatPrice(couponDiscount) }}</span>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-row" style="font-size: 18px; font-weight: 800">
              <span>Tổng cộng</span><span>{{ formatPrice(total) }}</span>
            </div>
          </div>
          <p v-if="cutoffClosed" class="stock-warning">Cửa hàng ngừng nhận đơn lúc {{ storeConfig.orderCutoffTime }}, chưa thể đặt hàng.</p>
          <p v-else-if="isStoreClosed" class="stock-warning">Cửa hàng đang đóng cửa ({{ storeConfig.openTime }} - {{ storeConfig.closeTime }}), chưa thể đặt hàng.</p>
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
        <button v-if="currentStep === 3" type="button" class="btn btn-outline" @click="currentStep = 1">Quay lại thông tin</button>
        <button v-if="currentStep === 1" type="button" class="btn btn-primary" :disabled="!canPlaceOrder()" @click="currentStep = 3">Tiếp tục thanh toán</button>
      </div>
    </div>
    </div>
    <div v-else class="empty-state" style="padding: 60px 0">
      <i class="bi bi-cart3"></i>
      <h3>Giỏ hàng trống</h3>
      <router-link to="/menu" class="btn btn-primary">Mua sắm ngay</router-link>
    </div>
    </div>
    <div v-if="codAccountDialog" class="dialog-overlay" @click.self="closeCodAccountDialog">
      <section class="cod-account-dialog" role="dialog" aria-modal="true" aria-labelledby="cod-account-title" aria-describedby="cod-account-message" tabindex="-1" @keydown="handleCodDialogKeydown">
        <h2 id="cod-account-title">COD chỉ dành cho khách có tài khoản</h2>
        <p id="cod-account-message">Vui lòng đăng ký hoặc đăng nhập để thanh toán khi nhận hàng. Khách vãng lai có thể thanh toán bằng QR PayOS.</p>
        <div class="dialog-actions"><button type="button" class="btn btn-primary" @click="goToAccount('Login')">Đăng nhập</button><button type="button" class="btn btn-outline" @click="goToAccount('Register')">Đăng ký</button><button type="button" class="btn btn-outline" @click="closeCodAccountDialog">Đóng</button></div>
      </section>
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
.payment-option.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.payment-option.disabled:hover {
  border-color: var(--border);
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
  min-width: 44px;
  min-height: 44px;
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
.coupon-manual { display: grid; gap: var(--space-2); margin: var(--space-3); }
.coupon-manual > label { font-size: 13px; font-weight: 700; }
.coupon-manual-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: var(--space-2); }
.coupon-manual-row .form-input,
.coupon-manual-row .btn { min-height: var(--control-height); }
.coupon-msg { margin: 0; color: var(--text-mid); font-size: 13px; }
.coupon-msg.error { color: var(--red-active); }
@media (max-width: 480px) { .coupon-manual-row { grid-template-columns: 1fr; } .coupon-manual-row .btn { width: 100%; } }
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

<style scoped>
.dialog-overlay{position:fixed;z-index:1000;inset:0;display:grid;place-items:center;padding:16px;background:rgba(25,18,14,.55)}
.cod-account-dialog{width:min(460px,100%);padding:26px;border-radius:20px;background:#fff;box-shadow:0 24px 70px rgba(20,12,8,.24)}
.cod-account-dialog h2{margin:0 0 10px;font-size:21px}.cod-account-dialog p{color:var(--text-mid);line-height:1.6}
.dialog-actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:20px}.dialog-actions .btn{min-height:44px}
</style>

<style scoped>
.checkout-page{background:linear-gradient(180deg,#fff8f0 0%,#faf8f6 100%)}.checkout-shell{display:grid;gap:14px}.checkout-layout{grid-template-columns:minmax(0,1fr) 360px;gap:18px}.checkout-main{display:grid;align-content:start;gap:14px}.checkout-section,.order-summary-card{border:1px solid #ece4de!important;border-radius:18px!important;background:#fff;box-shadow:0 12px 34px rgba(40,27,20,.05)}.delivery-section,.payment-section{padding:22px!important}.delivery-section>h3,.payment-section>h3{margin-bottom:20px!important;font-size:17px!important}.delivery-section>h3 i,.payment-section>h3 i{display:grid;width:36px;height:36px;place-items:center;border-radius:11px;color:#fff!important;background:#df683e}.manual-address-form{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.manual-address-form .saved-address-back,.manual-address-form .delivery-area,.manual-address-form .preview-address{grid-column:1/-1}.saved-address-back{display:inline-flex;width:max-content;min-height:44px;align-items:center;gap:7px;padding:0 14px;border:1px solid #e1d8d2;border-radius:999px;color:#8f3f23;background:#fff;font-size:12px;font-weight:800}.saved-address-back:hover{border-color:#df683e;background:#fff7f2}.checkout-summary-panel{position:sticky;top:82px}.order-summary-card::before{height:4px!important;background:linear-gradient(90deg,#df683e,#f3b05f)!important}.coupon-section{margin-top:14px;background:#fffaf6}.coupon-header{padding-bottom:11px}.coupon-manual-row{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:8px}.coupon-manual-row .btn{min-height:44px}.payment-selector{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.payment-option{min-height:92px;border:1px solid #e7ddd7!important;border-radius:14px!important;background:#fff}.payment-option.selected{border-color:#df683e!important;box-shadow:0 0 0 3px rgba(223,104,62,.11)}.checkout-actions{padding:4px 0}.checkout-actions .btn{min-width:128px;min-height:44px;border-radius:999px}.checkout-btn{width:100%;min-height:50px!important}.form-input,.form-select,.form-textarea{border-color:#e1d8d2;border-radius:11px}.form-input:focus,.form-select:focus,.form-textarea:focus{border-color:#df683e;box-shadow:0 0 0 3px rgba(223,104,62,.11)}@media(max-width:768px){.checkout-layout{grid-template-columns:1fr}.checkout-summary-panel{position:static}.delivery-section,.payment-section{padding:17px!important}.manual-address-form{grid-template-columns:1fr}.manual-address-form .saved-address-back,.manual-address-form .delivery-area{grid-column:auto}.payment-selector{grid-template-columns:1fr}.checkout-actions{position:sticky;z-index:5;bottom:0;padding:10px 0;background:rgba(250,248,246,.94);backdrop-filter:blur(10px)}}
</style>
