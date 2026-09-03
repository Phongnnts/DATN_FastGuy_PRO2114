const dateKey = value => {
  const date = new Date(value);
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 10);
};

export function inventoryAnalyticsPeriod(days, now = new Date()) {
  const to = new Date(now);
  const from = new Date(now);
  from.setDate(from.getDate() - Math.max(0, days - 1));
  return { fromDate: dateKey(from), toDate: dateKey(to), granularity: 'DAY' };
}

export function inventoryAnalyticsDelta(current, previous) {
  const value = Number(current || 0) - Number(previous || 0);
  const comparable = Number(previous) > 0;
  return { value, percent: comparable ? value / Number(previous) * 100 : null, comparable };
}

export function normalizeInventoryAnalyticsSeries(series) {
  const fields = ['inventoryValue', 'receiptValue', 'consumptionValue', 'wasteValue', 'adjustmentLossValue', 'adjustmentGainValue'];
  return (Array.isArray(series) ? series : []).map(point => Object.fromEntries([['date', point.date], ...fields.map(field => [field, Number(point[field]) || 0])]));
}
