export function resolveCanonical(value, to) {
  if (typeof value === 'function') return value(to);
  return value.replace(/:([A-Za-z0-9_]+)/g, (match, key) => to.params[key] ?? match);
}

export function isIndexable(robots) {
  return robots.split(',')[0].trim() === 'index';
}
