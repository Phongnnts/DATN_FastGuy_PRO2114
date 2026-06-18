<script setup>
import { formatDate } from '@/utils/format';

defineProps({
  order: { type: Object, required: true },
});
</script>

<template>
  <div class="packing-slip no-print" style="display: none">
    <div class="slip">
      <h2 style="text-align: center; margin-bottom: 16px">
        <span style="color: var(--primary)">FastGuy</span> - PHIẾU ĐÓNG GÓI
      </h2>
      <div class="slip-info">
        <p><strong>Mã đơn:</strong> {{ order.orderCode }}</p>
        <p><strong>Ngày:</strong> {{ formatDate(order.createdAt) }}</p>
        <p><strong>Địa chỉ giao:</strong> {{ order.shippingAddress }}</p>
        <p v-if="order.note"><strong>Ghi chú:</strong> {{ order.note }}</p>
      </div>
      <table style="width: 100%; border-collapse: collapse; margin-top: 16px">
        <thead>
          <tr style="background: #f5f5f5">
            <th style="border: 1px solid #ddd; padding: 8px; text-align: left">
              STT
            </th>
            <th style="border: 1px solid #ddd; padding: 8px; text-align: left">
              Sản phẩm
            </th>
            <th
              style="border: 1px solid #ddd; padding: 8px; text-align: center"
            >
              SL
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, i) in order.items" :key="i">
            <td style="border: 1px solid #ddd; padding: 8px">{{ i + 1 }}</td>
            <td style="border: 1px solid #ddd; padding: 8px">
              {{ item.productName }}
            </td>
            <td
              style="border: 1px solid #ddd; padding: 8px; text-align: center"
            >
              {{ item.quantity }}
            </td>
          </tr>
        </tbody>
      </table>
      <div
        style="
          margin-top: 24px;
          padding-top: 12px;
          border-top: 2px dashed #ccc;
          text-align: center;
          color: #888;
          font-size: 12px;
        "
      >
        <p>Kiểm tra kỹ trước khi giao. Cảm ơn!</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.slip {
  max-width: 400px;
  margin: 0 auto;
  padding: 24px;
  font-size: 14px;
}
.slip-info p {
  margin-bottom: 4px;
}
@media print {
  .packing-slip {
    display: block !important;
  }
}
</style>
