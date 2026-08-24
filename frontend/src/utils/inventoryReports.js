export async function loadInventoryReports(loaders) {
  const entries = await Promise.all(Object.entries(loaders).map(async ([key, load]) => {
    try {
      return [key, { data: await load(), error: '' }];
    } catch (error) {
      return [key, { data: null, error: error.message || 'Không thể tải báo cáo.' }];
    }
  }));
  return Object.fromEntries(entries);
}
