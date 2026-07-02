import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { orderApi } from '@/api';
import { useAuthStore } from '@/stores/auth';

export const useOrderStore = defineStore('order', () => {
  const allOrders = ref([]);
  const currentOrder = ref(null);
  const loading = ref(false);

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
      discount: 0,
      total: o.finalAmount ? parseFloat(o.finalAmount) : 0,
      paymentMethod: o.paymentMethod,
      paymentStatus: o.paymentStatus,
      shippingAddress: o.customerAddress || '',
      note: o.deliveryNote || '',
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
      createdAt: o.createdAt,
      items: o.items || [],
    };
  }

  async function fetchOrders() {
    loading.value = true;
    try {
      const data = await orderApi.getAll();
      allOrders.value = (data || []).map(mapOrderListItem);
    } catch {
      allOrders.value = [];
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
    try {
      const data = await orderApi.getById(id);
      currentOrder.value = data ? mapOrder(data) : null;
    } catch {
      currentOrder.value = null;
    } finally {
      loading.value = false;
    }
  }

  async function trackOrder(code) {
    try {
      const data = await orderApi.trackOrder(code);
      return data ? mapOrder(data) : null;
    } catch {
      return null;
    }
  }

  async function createOrder(orderData) {
    loading.value = true;
    try {
      const data = await orderApi.create({
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
        shippingFee: orderData.shippingFee || 0,
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
        sepayQrUrl: data.sepayQrUrl,
        transferContent: data.transferContent,
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
    userOrders,
    fetchOrders,
    fetchById,
    trackOrder,
    createOrder,
    cancelOrder,
  };
});
