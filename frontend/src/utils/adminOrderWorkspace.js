export const PRIMARY_ORDER_STATUSES = Object.freeze([
  { key: '', label: 'Tất cả' },
  { key: 'ATTENTION', label: 'Cần xử lý' },
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PREPARING', label: 'Đang chế biến' },
  { key: 'READY', label: 'Sẵn sàng' },
  { key: 'PICKED_UP', label: 'Đang giao' },
  { key: 'DELIVERED', label: 'Đã giao' },
]);

export const OTHER_ORDER_STATUSES = Object.freeze([
  { key: 'ASSIGNED', label: 'Đã gán shipper' },
  { key: 'DELIVERY_FAILED', label: 'Giao chưa thành công' },
  { key: 'RETURNED_TO_STORE', label: 'Đã trả về cửa hàng' },
  { key: 'CANCELLED', label: 'Đã hủy' },
  { key: 'REFUND_PENDING', label: 'Cần hoàn tiền' },
]);

const VALID_STATUS_KEYS = new Set(['ATTENTION', ...PRIMARY_ORDER_STATUSES.map(item => item.key), ...OTHER_ORDER_STATUSES.map(item => item.key)]);
const PAYMENT_STATUS_LABELS = { PAID: 'Đã thanh toán', UNPAID: 'Chờ thanh toán', FAILED: 'Thất bại', REFUNDED: 'Đã hoàn' };
const REFUND_STATUS_LABELS = { PENDING: 'Chờ hoàn', REFUNDED: 'Đã hoàn', REJECTED: 'Từ chối' };
const SORT_LABELS = { CREATED_DESC: 'Mới nhất' };
const INLINE_ACTION_META = {
  CONFIRMED: { label: 'Xác nhận đơn', tone: 'primary', requiresNote: false },
  PREPARING: { label: 'Bắt đầu chế biến', tone: 'primary', requiresNote: false },
  READY: { label: 'Đánh dấu sẵn sàng', tone: 'primary', requiresNote: false },
  RETURNED_TO_STORE: { label: 'Xác nhận đã về cửa hàng', tone: 'warning', requiresNote: false },
  CANCELLED: { label: 'Hủy đơn', tone: 'danger', requiresNote: true },
};

export function normalizeOrderStatus(raw) {
  return typeof raw === 'string' && VALID_STATUS_KEYS.has(raw) ? raw : '';
}

export function isOtherOrderStatus(status) {
  return OTHER_ORDER_STATUSES.some(item => item.key === status);
}

export function paymentMethodLabel(method) {
  return method === 'BANK_TRANSFER' ? 'PayOS' : method === 'COD' ? 'COD' : 'Chưa xác định';
}

export function paymentStatusLabel(status) {
  return PAYMENT_STATUS_LABELS[status] || 'Chưa có trạng thái';
}

export function activeOrderFilterChips(query = {}) {
  const chips = [];
  if (typeof query.search === 'string' && query.search.trim()) chips.push({ key: 'search', label: `Tìm: ${query.search.trim()}` });
  if (PAYMENT_STATUS_LABELS[query.paymentStatus]) chips.push({ key: 'paymentStatus', label: `Thanh toán: ${PAYMENT_STATUS_LABELS[query.paymentStatus]}` });
  if (REFUND_STATUS_LABELS[query.refundStatus]) chips.push({ key: 'refundStatus', label: `Hoàn tiền: ${REFUND_STATUS_LABELS[query.refundStatus]}` });
  if (SORT_LABELS[query.sort]) chips.push({ key: 'sort', label: `Sắp xếp: ${SORT_LABELS[query.sort]}` });
  if (typeof query.fromDate === 'string' && query.fromDate) chips.push({ key: 'fromDate', label: `Từ: ${query.fromDate}` });
  if (typeof query.toDate === 'string' && query.toDate) chips.push({ key: 'toDate', label: `Đến: ${query.toDate}` });
  return chips;
}

export function inlineOrderActionMeta(action) {
  return INLINE_ACTION_META[action] ? { key: action, ...INLINE_ACTION_META[action] } : null;
}

export function inlineOrderActions(allowedActions) {
  if (!Array.isArray(allowedActions)) return [];
  return allowedActions.map(inlineOrderActionMeta).filter(Boolean);
}
