<script setup>
import { formatPrice, formatDate } from '@/utils/format';
import { ORDER_STATUS_LABEL } from '@/utils/constants';

defineProps({
  order: { type: Object, required: true },
});
</script>

<template>
  <div class="invoice-print no-print" style="display: none">
    <div class="invoice">
      <div class="invoice-header">
        <h1>FastGuy</h1>
        <p>Hotline: 1900 1234</p>
        <p>Địa chỉ: 123 Nguyễn Huệ, Quận 1, TP.HCM</p>
      </div>
      <h2>HÓA ĐƠN BÁN HÀNG</h2>
      <div class="invoice-info">
        <p><strong>Mã đơn:</strong> {{ order.orderCode }}</p>
        <p><strong>Ngày:</strong> {{ formatDate(order.createdAt) }}</p>
        <p><strong>Khách hàng:</strong> Người dùng #{{ order.userId }}</p>
        <p><strong>Địa chỉ:</strong> {{ order.shippingAddress }}</p>
        <p><strong>Ghi chú:</strong> {{ order.note || 'Không có' }}</p>
      </div>
      <table class="invoice-table">
        <thead>
          <tr>
            <th>STT</th>
            <th>Sản phẩm</th>
            <th>Đơn giá</th>
            <th>Số lượng</th>
            <th>Thành tiền</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, i) in order.items" :key="i">
            <td>{{ i + 1 }}</td>
            <td>{{ item.productName }}</td>
            <td>{{ formatPrice(item.price) }}</td>
            <td>{{ item.quantity }}</td>
            <td>{{ formatPrice(item.price * item.quantity) }}</td>
          </tr>
        </tbody>
      </table>
      <div class="invoice-total">
        <div class="total-row">
          <span>Tạm tính:</span><span>{{ formatPrice(order.subtotal) }}</span>
        </div>
        <div class="total-row">
          <span>Phí ship:</span
          ><span>{{
            order.shippingFee > 0 ? formatPrice(order.shippingFee) : 'Miễn phí'
          }}</span>
        </div>
        <div v-if="order.discount > 0" class="total-row">
          <span>Giảm giá:</span><span>-{{ formatPrice(order.discount) }}</span>
        </div>
        <div class="total-row grand-total">
          <span>Tổng cộng:</span><span>{{ formatPrice(order.total) }}</span>
        </div>
      </div>
      <div class="invoice-footer">
        <p>Cảm ơn quý khách! Hẹn gặp lại!</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.invoice {
  max-width: 600px;
  margin: 0 auto;
  padding: 32px;
  font-size: 14px;
}
.invoice-header {
  text-align: center;
  margin-bottom: 24px;
}
.invoice-header h1 {
  font-size: 28px;
  font-weight: 800;
  color: var(--primary);
}
.invoice-header p {
  font-size: 13px;
  color: #555;
}
.invoice h2 {
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 20px;
}
.invoice-info {
  margin-bottom: 20px;
}
.invoice-info p {
  margin-bottom: 4px;
  font-size: 13px;
}
.invoice-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}
.invoice-table th,
.invoice-table td {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
  font-size: 13px;
}
.invoice-table th {
  background: #f5f5f5;
  font-weight: 600;
}
.invoice-total {
  text-align: right;
}
.total-row {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  padding: 4px 0;
  font-size: 14px;
}
.grand-total {
  font-size: 18px;
  font-weight: 800;
  border-top: 2px solid #333;
  padding-top: 8px;
  margin-top: 8px;
}
.invoice-footer {
  text-align: center;
  margin-top: 32px;
  color: #888;
}
@media print {
  .invoice-print {
    display: block !important;
  }
}
</style>
