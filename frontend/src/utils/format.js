export function formatPrice(price) {
  if (price == null) return '0₫';
  return price.toLocaleString('vi-VN') + '₫';
}

export function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDateShort(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

export function formatTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

export function formatPhone(phone) {
  if (!phone) return '';
  const s = phone.replace(/\D/g, '');
  if (s.length === 10) {
    const local = s.startsWith('0') ? s.substring(1) : s;
    return '+84 ' + local.replace(/(\d{3})(\d{3})(\d{3})/, '$1 $2 $3');
  }
  if (s.length === 11 && s.startsWith('0')) {
    return '+84 ' + s.substring(1).replace(/(\d{3})(\d{3})(\d{3})/, '$1 $2 $3');
  }
  if (s.length === 11 && s.startsWith('84')) {
    return '+' + s.substring(0, 2) + ' ' + s.substring(2).replace(/(\d{3})(\d{3})(\d{3})/, '$1 $2 $3');
  }
  return phone;
}

export function normalizePhone(phone) {
  if (!phone) return '';
  let s = phone.replace(/\D/g, '');
  if (s.length === 10 && s.startsWith('0')) s = '84' + s.substring(1);
  if (s.length === 9) s = '84' + s;
  return '+' + s;
}

export function daysBetween(a, b) {
  const da = new Date(a);
  const db = new Date(b);
  return Math.floor((db - da) / (1000 * 60 * 60 * 24));
}

export function truncate(str, len = 50) {
  if (!str) return '';
  return str.length > len ? str.substring(0, len) + '...' : str;
}
