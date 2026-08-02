export function toLocalDateKey(date) {
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function parseShiftEndDatetime(dateKey, end, start = end) {
  const dayOffset = localTimeLessThan(start, end) ? 1 : 0;
  const [y, m, d] = dateKey.split('-').map(Number);
  const [hh, mm, ss, ms] = splitLocalTime(end);
  return new Date(y, m - 1, d + dayOffset, hh, mm, ss, ms);
}

export function isShiftEndPassed(now, endDatetime, startDatetime = endDatetime) {
  const t = now.getTime();
  if (startDatetime <= endDatetime) return t >= endDatetime.getTime();
  return t >= endDatetime.getTime() && t < startDatetime.getTime();
}

function localTimeLessThan(start, end) {
  const [sh, sm] = start.split(':').map(Number);
  const [eh, em] = end.split(':').map(Number);
  return eh * 60 + em < sh * 60 + sm;
}

function splitLocalTime(time) {
  const [h = 0, m = 0, raw = '0'] = time.split(':');
  const [sec, ms = '0'] = String(raw).split('.');
  return [Number(h), Number(m), Number(sec), Number(ms.padEnd(3, '0').slice(0, 3))];
}
