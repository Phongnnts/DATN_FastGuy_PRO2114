import { defineStore } from 'pinia';
import { ref } from 'vue';
import { shipperApi } from '@/api';
import { acceptsShipperRequest } from '@/utils/shipperOperations';

const number = value => Number(value ?? 0);
const message = error => error?.response?.data?.message || error?.message || 'Không thể tải dữ liệu';
const mapItem = item => ({ ...item, modifiers: Array.isArray(item.modifiers) ? item.modifiers : [] });
const mapOrder = o => ({ id: o.orderId ?? o.id, orderCode: o.orderCode || '', status: o.status || '', customerName: o.customerName || '', customerPhone: o.customerPhone || '', customerAddress: o.customerAddress || '', total: number(o.finalAmount ?? o.total), shippingFee: number(o.shippingFee), serviceFee: number(o.serviceFee), discount: number(o.discountAmount), paymentMethod: o.paymentMethod || '', paymentStatus: o.paymentStatus || '', itemCount: number(o.itemCount ?? (o.items || []).reduce((sum, item) => sum + number(item.quantity), 0)), assignedAt: o.assignedAt || null, pickedUpAt: o.pickedUpAt || null, deliveredAt: o.deliveredAt || null, createdAt: o.createdAt || null, codCollectedAmount: o.codCollectedAmount == null ? null : number(o.codCollectedAmount), codCollectedAt: o.codCollectedAt || null, deliveryNote: o.deliveryNote || '', items: Array.isArray(o.items) ? o.items.map(mapItem) : [], statusHistory: Array.isArray(o.statusHistory) ? o.statusHistory : [], allowedActions: Array.isArray(o.allowedActions) ? o.allowedActions : [] });

export const useShipperStore = defineStore('shipper', () => {
  const dashboard = ref(null);
  const activeOrders = ref([]);
  const historyOrders = ref([]);
  const historyTotal = ref(0);
  const historyPage = ref(1);
  const historySize = ref(20);
  const currentOrder = ref(null);
  const dashboardLoading = ref(false);
  const dashboardError = ref('');
  const listLoading = ref(false);
  const listError = ref('');
  const historyLoading = ref(false);
  const historyError = ref('');
  const detailLoading = ref(false);
  const detailError = ref('');
  let dashboardGeneration = 0;
  let listGeneration = 0;
  let detailGeneration = 0;

  async function fetchDashboard(silent = false) {
    const requestGeneration = ++dashboardGeneration;
    if (!silent) dashboardLoading.value = true;
    dashboardError.value = '';
    try {
      const data = await shipperApi.getDashboard();
      if (requestGeneration === dashboardGeneration) dashboard.value = data;
      return data;
    } catch (error) {
      if (requestGeneration === dashboardGeneration) dashboardError.value = message(error);
      throw error;
    } finally {
      if (!silent && requestGeneration === dashboardGeneration) dashboardLoading.value = false;
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

  async function fetchHistory({ page = 1, size = 20, fromDate, toDate } = {}) {
    const requestGeneration = ++listGeneration;
    historyLoading.value = true;
    historyError.value = '';
    try {
      const data = await shipperApi.getHistory({ page, size, fromDate, toDate });
      const mapped = (Array.isArray(data) ? data : data?.items || []).map(mapOrder);
      if (acceptsShipperRequest({ requestGeneration, latestGeneration: listGeneration })) {
        historyOrders.value = mapped;
        historyTotal.value = Number(data?.total ?? mapped.length);
        historyPage.value = page;
        historySize.value = size;
      }
      return mapped;
    } catch (error) {
      if (requestGeneration === listGeneration) historyError.value = message(error);
      throw error;
    } finally {
      if (requestGeneration === listGeneration) historyLoading.value = false;
    }
  }
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

  return { dashboard, activeOrders, historyOrders, historyTotal, historyPage, historySize, currentOrder, dashboardLoading, dashboardError, listLoading, listError, historyLoading, historyError, detailLoading, detailError, fetchDashboard, fetchActiveOrders, fetchMyOrders, fetchHistory, invalidateListRequests, fetchOrderById, invalidateDetailRequests, pickUpOrder, deliverOrder };
});
