import { defineStore } from 'pinia';
import { ref } from 'vue';
import { shipperApi } from '@/api';

export const useShipperStore = defineStore('shipper', () => {
  const myOrders = ref([]);
  const currentOrder = ref(null);
  const loading = ref(false);

  function mapOrder(o) {
    return {
      id: o.orderId,
      orderCode: o.orderCode,
      status: o.status,
      customerName: o.customerName || '',
      customerPhone: o.customerPhone || '',
      customerAddress: o.customerAddress || '',
      total: o.finalAmount ? parseFloat(o.finalAmount) : 0,
      shippingFee: o.shippingFee ? parseFloat(o.shippingFee) : 0,
      paymentMethod: o.paymentMethod,
      paymentStatus: o.paymentStatus,
      codCollectedAmount: o.codCollectedAmount != null ? parseFloat(o.codCollectedAmount) : null,
      codCollectedAt: o.codCollectedAt || null,
      deliveryNote: o.deliveryNote || '',
      createdAt: o.createdAt,
      pickedUpAt: o.pickedUpAt || null,
      deliveredAt: o.deliveredAt || null,
      items: o.items || [],
    };
  }

  const dashboard = ref(null);

  async function fetchDashboard() {
    try {
      const data = await shipperApi.getDashboard();
      dashboard.value = data;
      return data;
    } catch { return null; }
  }

  async function fetchMyOrders() {
    loading.value = true;
    try {
      const data = await shipperApi.getMyOrders();
      myOrders.value = Array.isArray(data) ? data.map(mapOrder) : [];
    } catch {
      myOrders.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function fetchHistory() {
    loading.value = true;
    try {
      const data = await shipperApi.getHistory();
      myOrders.value = Array.isArray(data) ? data.map(mapOrder) : [];
    } catch {
      myOrders.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function fetchOrderById(id) {
    loading.value = true;
    try {
      const data = await shipperApi.getOrderById(id);
      currentOrder.value = data ? mapOrder(data) : null;
      return currentOrder.value;
    } catch {
      currentOrder.value = null;
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function pickUpOrder(id) {
    await shipperApi.pickUpOrder(id);
    const idx = myOrders.value.findIndex(o => o.id === id);
    if (idx >= 0) {
      myOrders.value[idx].status = 'PICKED_UP';
    }
  }

  async function deliverOrder(id, collectedAmount) {
    await shipperApi.deliverOrder(id, collectedAmount);
    const order = myOrders.value.find(o => o.id === id);
    if (order) order.status = 'DELIVERED';
    if (dashboard.value) {
      dashboard.value.todayDelivered = (dashboard.value.todayDelivered || 0) + 1;
    }
  }

  return {
    dashboard,
    myOrders,
    currentOrder,
    loading,
    fetchDashboard,
    fetchMyOrders,
    fetchHistory,
    fetchOrderById,
    pickUpOrder,
    deliverOrder,
  };
});
