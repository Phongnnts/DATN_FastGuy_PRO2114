import { defineStore } from 'pinia';
import { ref } from 'vue';
import { staffApi } from '@/api';

export const useStaffStore = defineStore('staff', () => {
  const dashboard = ref(null);
  const allOrders = ref([]);
  const loading = ref(false);
  const error = ref('');
  let fetchVersion = 0;

  function mapOrder(o) {
    return {
      id: o.orderId,
      orderCode: o.orderCode,
      userId: o.userId,
      customerName: o.customerName || '',
      status: o.orderStatus || o.status,
      items: (o.items || []).map((i) => ({
        productId: i.productId,
        variantId: i.variantId || null,
        productName: i.productName,
        variantName: i.variantName || '',
        price:
          typeof i.unitPrice === 'string'
            ? parseFloat(i.unitPrice)
            : i.unitPrice || 0,
        quantity: i.quantity,
        totalPrice:
          typeof i.totalPrice === 'string'
            ? parseFloat(i.totalPrice)
            : i.totalPrice || 0,
        image: i.imageUrl || '',
      })),
      subtotal: o.totalAmount ? parseFloat(o.totalAmount) : 0,
      shippingFee: o.shippingFee ? parseFloat(o.shippingFee) : 0,
      discount: 0,
      total: o.finalAmount ? parseFloat(o.finalAmount) : 0,
      paymentMethod: o.paymentMethod,
      paymentStatus: o.paymentStatus,
      codCollectedAmount: o.codCollectedAmount != null ? parseFloat(o.codCollectedAmount) : null,
      codCollectedAt: o.codCollectedAt || null,
      shippingAddress: o.customerAddress || '',
       note: o.deliveryNote || '',
       createdAt: o.createdAt,
       cancelledBy: o.cancelledBy || null,
       failureReason: o.failureReason || '',
       refundStatus: o.refundStatus || null,
       refundNote: o.refundNote || '',
       shipperId: o.shipperId || null,
       shipperName: o.shipperName || '',
       assignedAt: o.assignedAt || null,
       statusHistory: o.statusHistory || [
        { status: o.orderStatus || o.status, time: o.createdAt, note: '' },
      ],
      internalNotes: o.internalNotes || [],
    };
  }

  function mapOrderListItem(o) {
    return {
      id: o.orderId,
      orderCode: o.orderCode,
      userId: o.userId || o.customerName,
      customerName: o.customerName || '',
      status: o.orderStatus || o.status,
      items: o.items || [],
      total: o.finalAmount ? parseFloat(o.finalAmount) : 0,
      createdAt: o.createdAt,
       paymentMethod: o.paymentMethod || '',
       paymentStatus: o.paymentStatus || 'UNPAID',
       shipperId: o.shipperId || null,
       shipperName: o.shipperName || '',
       updatedAt: o.updatedAt || null,
     };
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
      return allOrders.value;
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
      return allOrders.value;
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
    } catch {
      return null;
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
      return allOrders.value;
    } finally {
      loading.value = false;
    }
  }

  async function fetchHistory() {
    loading.value = true;
    try {
      const data = await staffApi.getOrderHistory();
      allOrders.value = Array.isArray(data) ? data.map(mapOrderListItem) : [];
      return allOrders.value;
    } catch { return []; }
    finally { loading.value = false; }
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
      return allOrders.value;
    } finally {
      loading.value = false;
    }
  }

  async function updateOrderStatus(id, status, failureReason) {
    try {
      await staffApi.updateOrderStatus(id, status, failureReason);
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
      throw new Error(e.message || 'Không thể cập nhật trạng thái');
    }
  }

  function getOrderById(id) {
    return allOrders.value.find((o) => o.id === Number(id)) || null;
  }

  async function saveInternalNote(orderId, content) {
    try {
      await staffApi.saveInternalNote(orderId, content);
    } catch {}
  }

  return {
    dashboard,
    allOrders,
    loading,
    error,
    fetchDashboard,
    fetchOrders,
    fetchConfirmedOrders,
    fetchPreparingOrders,
    fetchReadyOrders,
    fetchHistory,
    fetchOrderById,
    updateOrderStatus,
    saveInternalNote,
    getOrderById,
  };
});
