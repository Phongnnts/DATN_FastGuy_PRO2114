export function matchesKitchenSearch(order, searchTerm) {
  const query = String(searchTerm ?? '').trim().toLocaleLowerCase('vi');
  return !query || [order.orderCode, order.customerName, order.customerPhone]
    .some((value) => String(value ?? '').toLocaleLowerCase('vi').includes(query));
}

export function kitchenItemCount(order) {
  const canonical = Number(order.itemCount);
  if (order.itemCount != null && Number.isFinite(canonical)) return canonical;
  return (order.items || []).reduce((sum, item) => {
    const quantity = Number(item.quantity);
    return sum + (Number.isFinite(quantity) ? quantity : 0);
  }, 0);
}

export function waitingDuration(createdAt, now = Date.now()) {
  if (createdAt == null || createdAt === '') return 'Chưa rõ';
  const created = new Date(createdAt).getTime();
  if (!Number.isFinite(created)) return 'Chưa rõ';
  const minutes = Math.max(0, Math.floor((now - created) / 60000));
  return minutes < 60 ? `${minutes} phút` : `${Math.floor(minutes / 60)} giờ ${minutes % 60} phút`;
}

export function acceptsKitchenRequest({ requestGeneration, latestGeneration, requestTab, activeTab }) {
  return requestGeneration === latestGeneration && requestTab === activeTab;
}

export function staffOrderDiscount(order) {
  const discount = Number(order.discountAmount ?? 0);
  return Number.isFinite(discount) ? discount : 0;
}

export function staffOrderItemTotal(item) {
  const total = Number(item.totalPrice);
  if (item.totalPrice != null && Number.isFinite(total)) return total;
  return Number(item.unitPrice ?? item.price ?? 0) * Number(item.quantity ?? 0);
}

export function sortAvailableShippers(shippers) {
  return [...shippers].sort((a, b) => Number(a.activeOrderCount ?? 0) - Number(b.activeOrderCount ?? 0) || String(a.fullName ?? '').localeCompare(String(b.fullName ?? ''), 'vi'));
}

export function validDispatchSelections(selections, shippers) {
  const availableIds = new Set(shippers.map((shipper) => String(shipper.id)));
  return Object.fromEntries(Object.entries(selections).filter(([, shipperId]) => availableIds.has(String(shipperId))));
}

export function acceptsDispatchRequest({ requestGeneration, latestGeneration, stopped }) {
  return !stopped && requestGeneration === latestGeneration;
}
