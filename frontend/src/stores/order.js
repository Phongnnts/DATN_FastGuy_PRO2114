import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { orderApi } from '@/api';
import { useAuthStore } from '@/stores/auth';

export const useOrderStore = defineStore('order', () => {
  const allOrders = ref([]);
  const currentOrder = ref(null);
  const loading = ref(false);
  const error = ref('');

  const userOrders = computed(() => allOrders.value);

  function mapOrder(o) {
    return {
      id: o.orderId,
      orderCode: o.orderCode,
      userId: o.userId,
      status: o.status,
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
        image: i.image || '',
      })),
      subtotal: o.totalAmount ? parseFloat(o.totalAmount) : 0,
      shippingFee: o.shippingFee ? parseFloat(o.shippingFee) : 0,
      discount: o.discountAmount ? parseFloat(o.discountAmount) : 0,
      total: o.finalAmount ? parseFloat(o.finalAmount) : 0,
      paymentMethod: o.paymentMethod,
      paymentStatus: o.paymentStatus,
      codCollectedAmount: o.codCollectedAmount != null ? parseFloat(o.codCollectedAmount) : null,
      codCollectedAt: o.codCollectedAt || null,
      shippingAddress: o.customerAddress || '',
      note: o.deliveryNote || '',
      checkoutUrl: o.checkoutUrl || '',
      createdAt: o.createdAt,
      updatedAt: o.updatedAt,
      statusHistory: o.statusHistory || [
        { status: o.status, time: o.createdAt, note: '' },
      ],
    };
  }

  function mapOrderListItem(o) {
    return {
      id: o.orderId,
      orderCode: o.orderCode,
      status: o.status,
      total: o.finalAmount ? parseFloat(o.finalAmount) : 0,
      paymentMethod: o.paymentMethod || '',
      paymentStatus: o.paymentStatus || 'UNPAID',
      createdAt: o.createdAt,
      items: o.items || [],
    };
  }

  async function fetchOrders() {
    loading.value = true;
    error.value = '';
    try {
      const data = await orderApi.getAll();
      allOrders.value = (data || []).map(mapOrderListItem);
    } catch (e) {
      error.value = e.message || 'Không thể tải đơn hàng';
    } finally {
      loading.value = false;
    }
  }

  async function fetchById(id) {
    if (currentOrder.value?.id === Number(id)) return currentOrder.value;
    await fetchDetail(id);
    return currentOrder.value;
  }

  async function fetchDetail(id) {
    loading.value = true;
    error.value = '';
    try {
      const data = await orderApi.getById(id);
      currentOrder.value = data ? mapOrder(data) : null;
    } catch (e) {
      currentOrder.value = null;
      error.value = e.message || 'Không thể tải chi tiết đơn hàng';
    } finally {
      loading.value = false;
    }
  }

  async function trackOrder(code, phoneSuffix) {
    try {
      const data = await orderApi.trackOrder(code, phoneSuffix);
      if (!data) return null;
      return {
        orderCode: data.orderCode,
        status: data.orderStatus,
        paymentMethod: data.paymentMethod,
        paymentStatus: data.paymentStatus,
        createdAt: data.createdAt,
        items: (data.items || []).map((item) => ({
          productId: item.productId || `${item.name}-${item.quantity}`,
          productName: item.productName || item.name,
          quantity: item.quantity,
          image: item.image || item.imageUrl || '',
        })),
        checkoutUrl: data.checkoutUrl || '',
        statusHistory: (data.statusHistory || []).map((entry) => ({
          status: entry.status,
          time: entry.timestamp,
        })),
      };
    } catch (e) {
      throw new Error(e.message || 'Không thể tra cứu đơn hàng');
    }
  }

  async function createOrder(orderData) {
    loading.value = true;
    try {
      const data = await orderApi.create({
        customerName: orderData.customerName || '',
        address: orderData.address || '',
        phone: orderData.phone || '',
        deliveryNote: orderData.deliveryNote || '',
        paymentMethod: orderData.paymentMethod || 'COD',
        ghnProvinceId: orderData.ghnProvinceId || null,
        ghnDistrictId: orderData.ghnDistrictId || null,
        ghnWardCode: orderData.ghnWardCode || null,
        toProvinceName: orderData.toProvinceName || '',
        toDistrictName: orderData.toDistrictName || '',
        toWardName: orderData.toWardName || '',
        couponCode: orderData.couponCode || '',
      });
      const newOrder = {
        id: data.orderId,
        orderCode: data.orderCode,
        status: data.status,
        paymentStatus: data.paymentStatus,
        total: data.finalAmount ? parseFloat(data.finalAmount) : 0,
        createdAt: new Date().toISOString(),
        items: orderData.items || [],
        checkoutUrl: data.checkoutUrl || '',
      };
      allOrders.value.unshift(newOrder);
      return newOrder;
    } catch (e) {
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function cancelOrder(id) {
    try {
      await orderApi.cancel(id);
      const order = allOrders.value.find((o) => o.id === Number(id));
      if (order) order.status = 'CANCELLED';
    } catch {
      throw new Error('Không thể hủy đơn hàng');
    }
  }

  return {
    allOrders,
    currentOrder,
    loading,
    error,
    userOrders,
    fetchOrders,
    fetchById,
    trackOrder,
    createOrder,
    cancelOrder,
  };
});
