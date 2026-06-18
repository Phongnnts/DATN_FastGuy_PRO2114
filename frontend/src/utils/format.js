export function formatPrice(price) {
  if (price == null) return '0₫'
  return price.toLocaleString('vi-VN') + '₫'
}

export function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('vi-VN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

export function formatDateShort(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('vi-VN', {
    year: 'numeric', month: '2-digit', day: '2-digit'
  })
}

export function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

export function formatPhone(phone) {
  if (!phone) return ''
  const s = phone.replace(/\D/g, '')
  if (s.length === 10) return s.replace(/(\d{4})(\d{3})(\d{3})/, '$1 $2 $3')
  if (s.length === 11) return s.replace(/(\d{4})(\d{3})(\d{4})/, '$1 $2 $3')
  return phone
}

export function daysBetween(a, b) {
  const da = new Date(a)
  const db = new Date(b)
  return Math.floor((db - da) / (1000 * 60 * 60 * 24))
}

export function truncate(str, len = 50) {
  if (!str) return ''
  return str.length > len ? str.substring(0, len) + '...' : str
}
