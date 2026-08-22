import { mapProduct } from './productMapper.js';

function listFrom(data) {
  if (Array.isArray(data)) return data;
  return data?.content || data?.items || data?.data || [];
}

export function createFavoriteLoadController({ getFavorites, getCatalog, apply, fail, warn, setLoading }) {
  let generation = 0;

  async function load() {
    const request = ++generation;
    setLoading(true);
    try {
      const favorites = listFrom(await getFavorites());
      if (request !== generation) return;
      const fallback = favorites.map(product => mapProduct(product, false));
      try {
        const catalog = listFrom(await getCatalog()).map(mapProduct);
        if (request !== generation) return;
        const byId = new Map(catalog.map(product => [Number(product.productId), product]));
        apply(fallback.map(product => byId.get(Number(product.productId)) || product));
      } catch (error) {
        if (request !== generation) return;
        apply(fallback);
        warn(error);
      }
    } catch (error) {
      if (request === generation) fail(error);
    } finally {
      if (request === generation) setLoading(false);
    }
  }

  function invalidate() {
    generation += 1;
    setLoading(false);
  }

  return { load, invalidate };
}
