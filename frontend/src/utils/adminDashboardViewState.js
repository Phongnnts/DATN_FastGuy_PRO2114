export function dashboardViewState(data, loadState, loadError) {
  const showContent = Boolean(data);
  let banner = null;
  if (showContent && loadState === 'loading') {
    banner = { role: 'status', message: 'Đang cập nhật tổng quan...' };
  } else if (showContent && loadState === 'error') {
    banner = { role: 'alert', message: loadError || 'Không thể tải tổng quan' };
  }
  return {
    showContent,
    showInitialLoading: !showContent && loadState === 'loading',
    banner,
  };
}
