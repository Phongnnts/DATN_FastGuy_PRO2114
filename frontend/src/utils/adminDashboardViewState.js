export function dashboardViewState(data, loadState, loadError, availability) {
  if (data) {
    if (Object.values(availability || {}).includes('UNAVAILABLE')) return 'partial';
    return loadState === 'loading' ? 'refreshing' : 'ready';
  }
  if (loadState === 'loading') return 'loading';
  if (loadError?.status === 403 || loadError?.response?.status === 403) return 'forbidden';
  return 'error';
}
