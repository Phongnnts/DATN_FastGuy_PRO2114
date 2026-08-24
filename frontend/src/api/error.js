export function normalizeApiError(error) {
  const body = error.response?.data;
  const normalized = new Error(body?.message || error.message || 'Lỗi không xác định');
  normalized.status = error.response?.status;
  normalized.data = body?.data;
  if (body && body.currentOnHandQuantity !== undefined) normalized.currentOnHandQuantity = body.currentOnHandQuantity;
  return normalized;
}
