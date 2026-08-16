export function canMutateRefund(row) {
  return row?.refundStatus === 'PENDING';
}

export const canProcessRefund = canMutateRefund;

export function canViewRefundDetail(row) {
  return row?.refundStatus === 'REFUNDED' || row?.refundStatus === 'REJECTED';
}

export function refundAuditDetail(row) {
  return {
    processor: row.refundProcessedByName || (row.refundProcessedBy ? `Admin #${row.refundProcessedBy}` : '—'),
    reference: row.refundReference || '—',
    note: row.refundNote || '—',
    refundedAt: row.refundedAt || null,
  };
}

export function buildRefundPresentation(row) {
  if (row.refundStatus === 'PENDING') {
    return { paymentLabel: 'Chưa xác nhận hoàn', refundLabel: 'Chờ hoàn thủ công', pendingDetail: 'Tiền chưa được xác nhận đã hoàn', audit: null };
  }
  const audit = refundAuditDetail(row);
  if (row.refundStatus === 'REFUNDED') return { paymentLabel: 'Đã hoàn', refundLabel: 'Đã xác nhận hoàn', pendingDetail: '', audit };
  return { paymentLabel: 'Không hoàn tiền', refundLabel: 'Đã từ chối', pendingDetail: '', audit };
}

export async function submitPendingRefund(state, submit) {
  const current = state.rows.find(row => row.orderId === state.selected?.orderId);
  if (!canMutateRefund(state.selected) || !canMutateRefund(current)) {
    state.errorMessage = 'Yêu cầu hoàn tiền đã được xử lý. Vui lòng tải lại dữ liệu mới nhất.';
    return false;
  }
  state.errorMessage = '';
  await submit(state.selected);
  state.statusMessage = 'Đã lưu kết quả hoàn tiền.';
  return true;
}
