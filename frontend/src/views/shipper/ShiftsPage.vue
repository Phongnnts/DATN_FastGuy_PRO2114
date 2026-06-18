<script setup>
import { computed } from 'vue'
import { useShipperStore } from '@/stores/shipper'
import { formatTime } from '@/utils/format'

const shipperStore = useShipperStore()
const current = computed(() => shipperStore.shiftStatus.current)

function checkIn() { shipperStore.checkIn() }
function checkOut() { shipperStore.checkOut() }
</script>

<template>
  <div>
    <div class="card mb-2" style="text-align:center">
      <div style="font-size:40px;margin-bottom:8px">
        <i v-if="current?.status === 'CHECKED_IN'" class="bi bi-check-circle-fill" style="color:#4CAF50"></i>
        <i v-else class="bi bi-clock" style="color:var(--text-light)"></i>
      </div>
      <h4 v-if="current?.status === 'CHECKED_IN'" style="color:#4CAF50;font-weight:700">Đang trực ca</h4>
      <h4 v-else style="color:var(--text-mid);font-weight:700">Chưa check-in</h4>
      <p v-if="current?.checkedInAt" style="font-size:13px;color:var(--text-mid)">Từ {{ formatTime(current.checkedInAt) }}</p>
      <div style="margin-top:12px">
        <button v-if="current?.status !== 'CHECKED_IN'" class="btn btn-primary" @click="checkIn" style="width:100%"><i class="bi bi-box-arrow-in-right"></i> Check-in</button>
        <button v-if="current?.status === 'CHECKED_IN'" class="btn btn-outline" @click="checkOut" style="width:100%"><i class="bi bi-box-arrow-right"></i> Check-out</button>
      </div>
    </div>
    <div class="card">
      <h4 style="font-weight:700;margin-bottom:12px">Lịch sử ca</h4>
      <div v-for="s in shipperStore.shiftStatus.history" :key="s.date" style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--border);font-size:13px">
        <div>
          <div style="font-weight:600">{{ s.date }}</div>
          <div style="color:var(--text-mid)">{{ s.shift }}</div>
        </div>
        <div style="text-align:right">
          <div style="color:var(--text-mid)">{{ formatTime(s.checkedInAt) }} - {{ formatTime(s.checkedOutAt) }}</div>
          <span class="badge badge-success">Hoàn thành</span>
        </div>
      </div>
    </div>
  </div>
</template>
