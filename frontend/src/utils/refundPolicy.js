export const REFUND_STATUSES = ['PENDING', 'REFUNDED', 'REJECTED'];

export function validateRefund({ status, amount, finalAmount, note }) {
  if (status === 'REFUNDED') {
    if (!Number.isFinite(amount) || amount <= 0) return 'Số tiền hoàn phải lớn hơn 0';
    if (Number.isFinite(finalAmount) && amount > finalAmount) return 'Số tiền hoàn không được vượt quá giá trị đơn';
    return '';
  }
  if (status === 'REJECTED') {
    if (!note || !note.trim()) return 'Vui lòng nhập lý do từ chối';
    return '';
  }
  return 'Hành động hoàn tiền không hợp lệ';
}
