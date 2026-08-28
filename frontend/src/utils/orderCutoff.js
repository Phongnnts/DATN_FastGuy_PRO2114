export function isPastOrderCutoff(cutoff, now = new Date()) {
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(cutoff || '')) return false;
  const [hours, minutes] = cutoff.split(':', 2).map(Number);
  return now.getHours() * 60 + now.getMinutes() >= hours * 60 + minutes;
}
