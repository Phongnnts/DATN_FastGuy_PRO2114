export function search(list, query, fields = ['name']) {
  if (!query) return list
  const q = query.toLowerCase()
  return list.filter(item =>
    fields.some(f => String(item[f] || '').toLowerCase().includes(q))
  )
}

export function debounce(fn, delay = 300) {
  let timer
  return (...args) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

export function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).substring(2, 6)
}

export function groupBy(arr, key) {
  return arr.reduce((acc, item) => {
    const k = item[key]
    if (!acc[k]) acc[k] = []
    acc[k].push(item)
    return acc
  }, {})
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
