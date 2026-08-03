import { defineStore } from 'pinia';
import { ref } from 'vue';
import { shipperApi } from '@/api';
import { acceptsShipperRequest } from '@/utils/shipperOperations';

const number = value => Number(value ?? 0);
const message = error => error?.response?.data?.message || error?.message || 'Không thể tải dữ liệu';
const mapItem = item => ({ ...item, modifiers: Array.isArray(item.modifiers) ? item.modifiers : [] });
const mapOrder = o => ({ id: o.orderId ?? o.id, orderCode: o.orderCode || '', status: o.status || '', customerName: o.customerName || '', customerPhone: o.customerPhone || '', customerAddress: o.customerAddress || '', total: number(o.finalAmount ?? o.total), shippingFee: number(o.shippingFee), paymentMethod: o.paymentMethod || '', paymentStatus: o.paymentStatus || '', itemCount: number(o.itemCount ?? (o.items || []).reduce((sum, item) => sum + number(item.quantity), 0)), assignedAt: o.assignedAt || null, pickedUpAt: o.pickedUpAt || null, deliveredAt: o.deliveredAt || null, createdAt: o.createdAt || null, codCollectedAmount: o.codCollectedAmount == null ? null : number(o.codCollectedAmount), codCollectedAt: o.codCollectedAt || null, deliveryNote: o.deliveryNote || '', items: Array.isArray(o.items) ? o.items.map(mapItem) : [], statusHistory: Array.isArray(o.statusHistory) ? o.statusHistory : [], allowedActions: Array.isArray(o.allowedActions) ? o.allowedActions : [] });

export const useShipperStore = defineStore('shipper', () => {
  const dashboard = ref(null);
  const activeOrders = ref([]);
  const historyOrders = ref([]);
  const currentOrder = ref(null);
  const dashboardLoading = ref(false);
  const dashboardError = ref('');
  const listLoading = ref(false);
  const listError = ref('');
  const detailLoading = ref(false);
  const detailError = ref('');
  let dashboardGeneration = 0;
  let listGeneration = 0;
  let detailGeneration = 0;

  async function fetchDashboard() {
    const requestGeneration = ++dashboardGeneration;
    dashboardLoading.value = true;
    dashboardError.value = '';
    try {
      const data = await shipperApi.getDashboard();
      if (requestGeneration === dashboardGeneration) dashboard.value = data;
      return data;
    } catch (error) {
      if (requestGeneration === dashboardGeneration) dashboardError.value = message(error);
      throw error;
    } finally {
      if (requestGeneration === dashboardGeneration) dashboardLoading.value = false;
    }
  }

  async function loadList(mode, request, silent = false) {
    const requestGeneration = ++listGeneration;
    if (!silent) listLoading.value = true;
    listError.value = '';
    try {
      const data = await request();
      const mapped = Array.isArray(data) ? data.map(mapOrder) : [];
      if (acceptsShipperRequest({ requestGeneration, latestGeneration: listGeneration, requestMode: mode, currentMode: mode })) {
        if (mode === 'active') activeOrders.value = mapped;
        else historyOrders.value = mapped;
      }
      return mapped;
    } catch (error) {
      if (requestGeneration === listGeneration) listError.value = message(error);
      throw error;
    } finally {
      if (!silent && requestGeneration === listGeneration) listLoading.value = false;
    }
  }

  const fetchActiveOrders = (silent = false) => loadList('active', () => shipperApi.getActiveOrders(), silent);
  const fetchMyOrders = fetchActiveOrders;
  const fetchHistory = () => loadList('history', () => shipperApi.getHistory());
  const invalidateListRequests = () => { listGeneration += 1; };

  async function fetchOrderById(id) {
    const requestGeneration = ++detailGeneration;
    detailLoading.value = true;
    detailError.value = '';
    currentOrder.value = null;
    try {
      const data = await shipperApi.getOrderById(id);
      const mapped = data ? mapOrder(data) : null;
      if (requestGeneration === detailGeneration) currentOrder.value = mapped;
      return requestGeneration === detailGeneration ? mapped : null;
    } catch (error) {
      if (requestGeneration === detailGeneration) detailError.value = message(error);
      throw error;
    } finally {
      if (requestGeneration === detailGeneration) detailLoading.value = false;
    }
  }

  const invalidateDetailRequests = () => { detailGeneration += 1; };
  const pickUpOrder = id => shipperApi.pickUpOrder(id);
  const deliverOrder = (id, collectedAmount) => shipperApi.deliverOrder(id, collectedAmount);

  return { dashboard, activeOrders, historyOrders, currentOrder, dashboardLoading, dashboardError, listLoading, listError, detailLoading, detailError, fetchDashboard, fetchActiveOrders, fetchMyOrders, fetchHistory, invalidateListRequests, fetchOrderById, invalidateDetailRequests, pickUpOrder, deliverOrder };
});
