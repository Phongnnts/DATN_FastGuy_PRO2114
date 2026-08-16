export function staffDashboardAttention(data = {}) {
  const overdueOrders = Number(data.overdueOrders || 0);
  const awaitingShipperOrders = Number(data.awaitingShipperOrders || 0);
  const outOfStockSkuCount = Number(data.outOfStockSkuCount || 0);
  const lowStockSkuCount = Number(data.lowStockSkuCount || 0);
  return {
    alertCount: overdueOrders + awaitingShipperOrders + outOfStockSkuCount + lowStockSkuCount,
    overdueOrders,
    awaitingShipperOrders,
    outOfStockSkuCount,
    lowStockSkuCount,
    lowStockThreshold: data.lowStockThreshold,
    routeTab: 'PENDING',
  };
}
