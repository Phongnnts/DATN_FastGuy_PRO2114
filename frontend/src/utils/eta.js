export const BUSINESS_ZONE = 'Asia/Ho_Chi_Minh';

const LOCAL_ISO = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,3}))?$/;
const OFFSET_ISO = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,3}))?(?:Z|([+-])(\d{2}):(\d{2}))$/;
const formatter = new Intl.DateTimeFormat('vi-VN', {
  timeZone: BUSINESS_ZONE,
  hour: '2-digit',
  minute: '2-digit',
  day: '2-digit',
  month: '2-digit',
  hourCycle: 'h23',
});

function validComponents(year, month, day, hour, minute, second) {
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  return month >= 1 && month <= 12 && day >= 1 && day <= days[month - 1] && hour <= 23 && minute <= 59 && second <= 59;
}

function parseBackendEta(value) {
  if (typeof value !== 'string' || value.trim() !== value || !value) return null;
  const local = LOCAL_ISO.exec(value);
  if (local) {
    const [, year, month, day, hour, minute, second, fraction = '0'] = local;
    if (!validComponents(+year, +month, +day, +hour, +minute, +second)) return null;
    const timestamp = Date.UTC(+year, +month - 1, +day, +hour - 7, +minute, +second, +fraction.padEnd(3, '0'));
    const storeTime = new Date(timestamp + 7 * 60 * 60 * 1000);
    if (
      storeTime.getUTCFullYear() !== +year ||
      storeTime.getUTCMonth() !== +month - 1 ||
      storeTime.getUTCDate() !== +day ||
      storeTime.getUTCHours() !== +hour ||
      storeTime.getUTCMinutes() !== +minute ||
      storeTime.getUTCSeconds() !== +second
    ) return null;
    return new Date(timestamp);
  }
  const offset = OFFSET_ISO.exec(value);
  if (!offset) return null;
  const [, year, month, day, hour, minute, second, , , offsetHour, offsetMinute] = offset;
  if (!validComponents(+year, +month, +day, +hour, +minute, +second)) return null;
  if (offsetHour !== undefined && (+offsetHour > 23 || +offsetMinute > 59)) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function createEtaModel(value) {
  const date = parseBackendEta(value);
  if (!date) return null;
  const parts = Object.fromEntries(formatter.formatToParts(date).map(({ type, value: part }) => [type, part]));
  return {
    datetime: date.toISOString(),
    display: `${parts.hour}:${parts.minute} ${parts.day}/${parts.month}`,
  };
}
