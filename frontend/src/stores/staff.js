import { defineStore } from 'pinia';
import { ref } from 'vue';
import { staffApi } from '@/api';
import { createDispatchRequestGate, kitchenItemCount, staffOrderDiscount, staffOrderItemTotal } from '@/utils/staffKitchen';
import { createPerKeyRequestGate } from '@/utils/staffHandover';

export const useStaffStore = defineStore('staff', () => {
  const dashboard = ref(null);
  const allOrders = ref([]);
  const historyTotal = ref(0);
  const historyPage = ref(1);
  const historySize = ref(20);
  const loading = ref(false);
  const error = ref('');
  const dispatchItems = ref([]);
  const dispatchCounts = ref({ priority: 0, new: 0, review: 0 });
  const dispatchLoading = ref(false);
  const dispatchError = ref('');
  const kitchenQueues = ref({});
  let fetchVersion = 0;
  let dispatchFilter = 'PRIORITY';
  const dispatchRequestGate = createDispatchRequestGate(dispatchFilter);
  const kitchenRequestGate = createPerKeyRequestGate();

  function mapOrder(o) {
    return {
      id: o.orderId,
      orderCode: o.orderCode,
      userId: o.userId ?? null,
      customerName: o.customerName ?? '',
      customerPhone: o.customerPhone ?? '',
      status: o.status ?? o.orderStatus,
      itemCount: kitchenItemCount(o),
      items: (o.items || []).map((item) => ({
        productId: item.productId,
        variantId: item.variantId ?? null,
        productName: item.productName,
        variantName: item.variantName ?? '',
        price:
          typeof item.unitPrice === 'string'
            ? parseFloat(item.unitPrice)
            : item.unitPrice || 0,
        quantity: item.quantity,
        totalPrice: staffOrderItemTotal(item),
        image: item.imageUrl ?? '',
        modifiers: Array.isArray(item.modifiers) ? item.modifiers : [],
      })),
      subtotal: o.totalAmount ? parseFloat(o.totalAmount) : 0,
      shippingFee: Number(o.shippingFee ?? 0),
      serviceFee: Number(o.serviceFee ?? 0),
      discount: staffOrderDiscount(o),
      total: Number(o.finalAmount ?? 0),
      paymentMethod: o.paymentMethod,
      paymentStatus: o.paymentStatus,
      codCollectedAmount: o.codCollectedAmount != null ? parseFloat(o.codCollectedAmount) : null,
      codCollectedAt: o.codCollectedAt || null,
      shippingAddress: o.customerAddress || '',
       note: o.deliveryNote || '',
       createdAt: o.createdAt,
       endedAt: o.endedAt || o.updatedAt || null,
       cancelledBy: o.cancelledBy || null,
       failureReason: o.failureReason || '',
       refundStatus: o.refundStatus || null,
       refundAmount: Number(o.refundAmount ?? 0),
       refundedAt: o.refundedAt || null,
       refundNote: o.refundNote || '',
       shipperId: o.shipperId || null,
       shipperName: o.shipperName || '',
        assignedAt: o.assignedAt || null,
        deliveryFailureCode: o.deliveryFailureCode || '',
        failureNote: o.failureNote || '',
        deliveryFailedAt: o.deliveryFailedAt || null,
        deliveryAttemptCount: Number(o.deliveryAttemptCount || 0),
        deliveryAttemptLimit: Number(o.deliveryAttemptLimit || 0),
        retryScheduledAt: o.retryScheduledAt || null,
        statusHistory: o.statusHistory || [
        { status: o.status || o.orderStatus, time: o.createdAt, note: '' },
      ],
      statusEnteredAt: o.statusEnteredAt ?? null,
      expiresAt: o.expiresAt ?? null,
      remainingSeconds: o.remainingSeconds ?? null,
      receivedAt: Date.now(),
      timeoutPolicy: o.timeoutPolicy ?? null,
      ownerShiftCode: o.ownerShiftCode ?? null,
      allowedActions: Array.isArray(o.allowedActions) ? o.allowedActions : [],
      internalNotes: Array.isArray(o.internalNotes) ? o.internalNotes : [],
    };
  }

  function mapOrderListItem(o) {
    return {
      id: o.orderId,
      orderCode: o.orderCode,
      userId: o.userId ?? null,
      customerName: o.customerName ?? '',
      customerPhone: o.customerPhone ?? '',
      shippingAddress: o.customerAddress ?? '',
      status: o.status ?? o.orderStatus,
      itemCount: kitchenItemCount(o),
      items: (o.items || []).map((item) => ({
        ...item,
        modifiers: Array.isArray(item.modifiers) ? item.modifiers : [],
      })),
      total: o.finalAmount ? parseFloat(o.finalAmount) : 0,
      createdAt: o.createdAt,
      endedAt: o.endedAt || o.updatedAt || null,
       paymentMethod: o.paymentMethod || '',
       paymentStatus: o.paymentStatus || 'UNPAID',
       shipperId: o.shipperId || null,
       shipperName: o.shipperName || '',
        updatedAt: o.updatedAt || null,
        deliveryFailureCode: o.deliveryFailureCode || '',
        failureNote: o.failureNote || '',
        deliveryFailedAt: o.deliveryFailedAt || null,
        deliveryAttemptCount: Number(o.deliveryAttemptCount || 0),
        deliveryAttemptLimit: Number(o.deliveryAttemptLimit || 0),
        retryScheduledAt: o.retryScheduledAt || null,
        readyAt: o.readyAt || null,
         classification: o.classification,
          minutesUntilClose: o.minutesUntilClose ?? null,
          statusEnteredAt: o.statusEnteredAt ?? null,
          expiresAt: o.expiresAt ?? null,
          remainingSeconds: o.remainingSeconds ?? null,
          receivedAt: Date.now(),
          timeoutPolicy: o.timeoutPolicy ?? null,
          ownerShiftCode: o.ownerShiftCode ?? null,
          waitingSince: o.waitingSince ?? null,
       };
   }

   async function fetchDispatchOrders(filter) {

    const request = dispatchRequestGate.begin(filter);
    const filterChanged = filter !== dispatchFilter;
    dispatchFilter = filter;
    if (filterChanged) dispatchItems.value = [];
    dispatchLoading.value = true;
    dispatchError.value = '';
    try {
      const data = await staffApi.getDispatchOrders(filter);
      if (!dispatchRequestGate.accepts(request)) return dispatchItems.value;
      dispatchItems.value = Array.isArray(data?.items) ? data.items.map(mapOrderListItem) : [];
      dispatchCounts.value = {
        priority: Number(data?.counts?.priority ?? 0),
        new: Number(data?.counts?.new ?? 0),
        review: Number(data?.counts?.review ?? 0),
      };
      return dispatchItems.value;
    } catch (e) {
      if (dispatchRequestGate.accepts(request)) {
        dispatchError.value = e.message || 'Không thể tải danh sách điều phối';
      }
      throw e;
    } finally {
      if (dispatchRequestGate.accepts(request)) dispatchLoading.value = false;
    }
  }

  function invalidateDispatch() {
    dispatchRequestGate.invalidate();
    dispatchLoading.value = false;
  }

  async function fetchDashboard() {
    loading.value = true;
    error.value = '';
    try {
      const data = await staffApi.getDashboard();
      dashboard.value = data;
      return data;
    } catch (e) {
      error.value = e.message || 'Không thể tải tổng quan';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchKitchenOrders(tab) {
    const request = kitchenRequestGate.begin(tab);
    const requests = {
      PENDING: staffApi.getOrders,
      CONFIRMED: staffApi.getConfirmedOrders,
      PREPARING: staffApi.getPreparingOrders,
       READY: staffApi.getReadyOrders,
       DELIVERY_FAILED: staffApi.getDeliveryFailedOrders,
    };
    const data = await requests[tab]();
    const mapped = Array.isArray(data) ? data.map(mapOrderListItem) : [];
    if (!kitchenRequestGate.accepts(request)) return kitchenQueues.value[tab] || [];
    kitchenQueues.value = { ...kitchenQueues.value, [tab]: mapped };
    return mapped;
  }

  async function fetchOrders() {
    const version = ++fetchVersion;
    loading.value = true;
    error.value = '';
    try {
      const data = await staffApi.getOrders();
      if (version !== fetchVersion) return allOrders.value;
      allOrders.value = Array.isArray(data) ? data.map(mapOrderListItem) : [];
      return allOrders.value;
    } catch (e) {
      error.value = e.message || 'Không thể tải danh sách đơn hàng';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchConfirmedOrders() {
    const version = ++fetchVersion;
    loading.value = true;
    error.value = '';
    try {
      const data = await staffApi.getConfirmedOrders();
      if (version !== fetchVersion) return allOrders.value;
      allOrders.value = Array.isArray(data) ? data.map(mapOrderListItem) : [];
      return allOrders.value;
    } catch (e) {
      error.value = e.message || 'Không thể tải danh sách đơn hàng';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchOrderById(id) {
    loading.value = true;
    try {
      const data = await staffApi.getOrderById(id);
      const mapped = data ? mapOrder(data) : null;
      if (mapped) {
        const idx = allOrders.value.findIndex((o) => o.id === mapped.id);
        if (idx >= 0) allOrders.value[idx] = mapped;
        else allOrders.value.unshift(mapped);
      }
      return mapped;
    } catch (e) {
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchPreparingOrders() {
    const version = ++fetchVersion;
    loading.value = true;
    error.value = '';
    try {
      const data = await staffApi.getPreparingOrders();
      if (version !== fetchVersion) return allOrders.value;
      allOrders.value = Array.isArray(data) ? data.map(mapOrderListItem) : [];
      return allOrders.value;
    } catch (e) {
      error.value = e.message || 'Không thể tải danh sách đơn hàng';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchHistory(params = {}) {
    const version = ++fetchVersion;
    loading.value = true;
    error.value = '';
    try {
      const data = await staffApi.getOrderHistory(params);
      if (version !== fetchVersion) return allOrders.value;
      allOrders.value = Array.isArray(data?.items) ? data.items.map(mapOrderListItem) : [];
      historyTotal.value = Number(data?.total) || 0;
      historyPage.value = Number(data?.page) || 1;
      historySize.value = Number(data?.size) || 20;
      return allOrders.value;
    } catch (e) {
      error.value = e.message || 'Không thể tải danh sách đơn hàng';
      throw e;
    } finally { loading.value = false; }
  }

  async function fetchReadyOrders() {
    const version = ++fetchVersion;
    loading.value = true;
    error.value = '';
    try {
      const data = await staffApi.getReadyOrders();
      if (version !== fetchVersion) return allOrders.value;
      allOrders.value = Array.isArray(data) ? data.map(mapOrderListItem) : [];
      return allOrders.value;
    } catch (e) {
      error.value = e.message || 'Không thể tải danh sách đơn hàng';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function updateOrderStatus(id, status, expectedStatus, failureReason) {
    try {
      await staffApi.updateOrderStatus(id, status, expectedStatus, failureReason);
      const order = allOrders.value.find((o) => o.id === id);
      if (order) {
        order.status = status;
        if (!order.statusHistory) order.statusHistory = [];
        order.statusHistory.push({
          status,
          time: new Date().toISOString(),
          note: status === 'CANCELLED' ? (failureReason || '') : '',
        });
      }
    } catch (e) {
      if (!e.message) e.message = 'Không thể cập nhật trạng thái';
      throw e;
    }
  }

  function getOrderById(id) {
    return allOrders.value.find((o) => o.id === Number(id)) || null;
  }

  async function saveInternalNote(orderId, content) {
    error.value = '';
    try {
      await staffApi.saveInternalNote(orderId, content);
    } catch (e) {
      error.value = e.message || 'Không thể lưu ghi chú nội bộ';
      throw e;
    }
  }

  return {
    dashboard,
    allOrders,
    historyTotal,
    historyPage,
    historySize,
    loading,
    error,
    dispatchItems,
    dispatchCounts,
     dispatchLoading,
     dispatchError,
     kitchenQueues,
     fetchDashboard,
    fetchKitchenOrders,
    fetchOrders,
    fetchConfirmedOrders,
    fetchPreparingOrders,
    fetchReadyOrders,
    fetchDispatchOrders,
    invalidateDispatch,
    fetchHistory,
    fetchOrderById,
    updateOrderStatus,
    saveInternalNote,
    getOrderById,
  };
});
