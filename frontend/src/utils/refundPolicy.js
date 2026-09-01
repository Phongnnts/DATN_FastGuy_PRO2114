export const REFUND_STATUSES = ['PENDING', 'REFUNDED', 'REJECTED'];

export function validateRefund({ status, amount, finalAmount, note, reference, proof }) {
  if (status === 'REFUNDED') {
    if (!Number.isFinite(finalAmount) || !Number.isFinite(amount) || amount !== finalAmount) return 'Chỉ hỗ trợ hoàn toàn bộ giá trị đơn';
    if (!reference || !reference.trim()) return 'Vui lòng nhập mã tham chiếu hoàn tiền';
    if (!proof) return 'Vui lòng chọn ảnh bằng chứng hoàn tiền';
    return '';
  }
  if (status === 'REJECTED') {
    if (!note || !note.trim()) return 'Vui lòng nhập lý do từ chối';
    return '';
  }
  return 'Hành động hoàn tiền không hợp lệ';
}
