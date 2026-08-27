export function nextTabIndex(current, key, count) {
  if (key === 'Home') return 0;
  if (key === 'End') return count - 1;
  if (key === 'ArrowLeft') return (current - 1 + count) % count;
  if (key === 'ArrowRight') return (current + 1) % count;
  return current;
}

export function handoverFocusTarget(removedIndex, previousCount) {
  if (previousCount <= 1) return { type: 'tab' };
  return { type: 'claim', index: Math.min(removedIndex, previousCount - 2) };
}

export function createPerKeyRequestGate() {
  const generations = new Map();
  return {
    begin(key) {
      const generation = (generations.get(key) || 0) + 1;
      generations.set(key, generation);
      return { key, generation };
    },
    accepts(request) {
      return generations.get(request.key) === request.generation;
    },
  };
}

export async function focusAfterUnlock(unlock, render, focus) {
  unlock();
  await render();
  focus();
}
