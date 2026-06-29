import { defineStore } from 'pinia';
import { ref } from 'vue';
import { shipperApi } from '@/api';

export const useShipperStore = defineStore('shipper', () => {
  const availableOrders = ref([]);
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

  async function fetchAvailableOrders() {
    loading.value = true;
    try {
      const data = await shipperApi.getAvailableOrders();
      availableOrders.value = Array.isArray(data) ? data.map(mapOrder) : [];
    } catch {
      availableOrders.value = [];
    } finally {
      loading.value = false;
    }
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
    const order = myOrders.value.find(o => o.id === id);
    if (order) order.status = 'PICKED_UP';
    availableOrders.value = availableOrders.value.filter(o => o.id !== id);
  }

  async function deliverOrder(id) {
    await shipperApi.deliverOrder(id);
    const order = myOrders.value.find(o => o.id === id);
    if (order) order.status = 'DELIVERED';
    if (dashboard.value) {
      dashboard.value.todayDelivered = (dashboard.value.todayDelivered || 0) + 1;
    }
  }

  async function cancelOrder(id, reason) {
    await shipperApi.cancelOrder(id, reason);
    const order = myOrders.value.find(o => o.id === id);
    if (order) order.status = 'CANCELLED';
  }

  return {
    dashboard,
    availableOrders,
    myOrders,
    currentOrder,
    loading,
    fetchDashboard,
    fetchAvailableOrders,
    fetchMyOrders,
    fetchOrderById,
    pickUpOrder,
    deliverOrder,
    cancelOrder,
  };
});
