export function normalizeEstimatedDeliveryMinutes(value) {
  return Number.isFinite(value) && Number.isInteger(value) && value >= 10 && value <= 180 ? value : null;
}

export function createStoreConfigController({ requestConfig, applyEstimate }) {
  let generation = 0;
  let stopped = false;

  async function load() {
    const requestGeneration = ++generation;
    try {
      const config = await requestConfig();
      if (!stopped && requestGeneration === generation) {
        applyEstimate(normalizeEstimatedDeliveryMinutes(config?.estimatedDeliveryMinutes));
      }
    } catch {
      if (!stopped && requestGeneration === generation) applyEstimate(null);
    }
  }

  function stop() {
    stopped = true;
    generation += 1;
  }

  return { load, stop };
}
