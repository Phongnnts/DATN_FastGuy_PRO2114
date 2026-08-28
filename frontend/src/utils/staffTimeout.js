export function createServerCountdown(order, receivedAt = Date.now()) {
  const initial = Math.max(0, Number(order?.remainingSeconds ?? 0));
  return { remaining: (now = Date.now()) => Math.max(0, initial - Math.floor((now - receivedAt) / 1000)) };
}

export function formatRemaining(seconds) {
  const value = Math.max(0, Number(seconds) || 0);
  return `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(value % 60).padStart(2, '0')}`;
}
