import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { shipperApi } from '@/api'

export const useShipperStore = defineStore('shipper', () => {
  const allDeliveries = ref([])
  const shiftStatus = ref({ current: null })
  const loading = ref(false)

  const readyDeliveries = computed(() => allDeliveries.value.filter(d => d.status === 'READY'))
  const deliveringDeliveries = computed(() => allDeliveries.value.filter(d => d.status === 'DELIVERING'))
  const deliveredDeliveries = computed(() => allDeliveries.value.filter(d => d.status === 'DELIVERED'))

  function mapDelivery(d) {
    return {
      id: d.orderId,
      orderCode: d.orderCode,
      status: d.orderStatus,
      customerName: d.customerName || '',
      customerPhone: d.customerPhone || '',
      address: d.customerAddress || '',
      total: d.finalAmount ? parseFloat(d.finalAmount) : 0,
      paymentMethod: d.paymentMethod || '',
      note: d.deliveryNote || '',
      items: (d.items || []).map(i => ({
        name: i.productName,
        quantity: i.quantity,
        price: typeof i.unitPrice === 'string' ? parseFloat(i.unitPrice) : (i.unitPrice || 0)
      })),
      notes: d.notes || []
    }
  }

  async function fetchDeliveries() {
    loading.value = true
    try {
      const data = await shipperApi.getDeliveries()
      allDeliveries.value = Array.isArray(data) ? data.map(mapDelivery) : []
      return allDeliveries.value
    } catch { return [] } finally { loading.value = false }
  }

  async function fetchDeliveryById(id) {
    loading.value = true
    try {
      const data = await shipperApi.getDeliveryById(id)
      const mapped = data ? mapDelivery(data) : null
      if (mapped) {
        const idx = allDeliveries.value.findIndex(d => d.id === mapped.id)
        if (idx >= 0) allDeliveries.value[idx] = mapped
        else allDeliveries.value.unshift(mapped)
      }
      return mapped
    } catch { return null } finally { loading.value = false }
  }

  async function acceptDelivery(id) {
    try {
      await shipperApi.acceptDelivery(id)
      const d = allDeliveries.value.find(del => del.id === id)
      if (d) d.status = 'DELIVERING'
    } catch {}
  }

  async function updateDeliveryStatus(id, status, data = {}) {
    try {
      await shipperApi.updateDeliveryStatus(id, status, data)
      const d = allDeliveries.value.find(del => del.id === id)
      if (d) {
        d.status = status
        if (status === 'DELIVERED') {
          d.deliveredAt = new Date().toISOString()
          d.collectedCOD = data.collectedCOD ?? d.collectedCOD
        }
      }
    } catch {}
  }

  async function removeDelivery(id) {
    try {
      await shipperApi.removeDelivery(id)
      allDeliveries.value = allDeliveries.value.filter(d => d.id !== id)
    } catch {}
  }

  async function saveDeliveryNote(deliveryId, content) {
    try {
      await shipperApi.saveDeliveryNote(deliveryId, content)
      const d = allDeliveries.value.find(del => del.id === deliveryId)
      if (d) {
        if (!d.notes) d.notes = []
        d.notes.push({ content, createdAt: new Date().toISOString() })
      }
    } catch {}
  }

  async function fetchHistory() {
    loading.value = true
    try {
      const data = await shipperApi.getHistory()
      return Array.isArray(data) ? data.map(mapDelivery) : []
    } catch { return [] } finally { loading.value = false }
  }

  return { allDeliveries, shiftStatus, loading, readyDeliveries, deliveringDeliveries, deliveredDeliveries, fetchDeliveries, fetchDeliveryById, acceptDelivery, updateDeliveryStatus, removeDelivery, saveDeliveryNote, fetchHistory }
})
