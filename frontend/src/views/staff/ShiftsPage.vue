<script setup>
import { computed } from 'vue'
import { useStaffStore } from '@/stores/staff'
import { formatTime } from '@/utils/format'
import { SCHEDULE_STATUS_LABEL } from '@/utils/constants'

const staffStore = useStaffStore()
const current = computed(() => staffStore.shiftStatus.current)
const history = computed(() => staffStore.shiftStatus.history)

function checkIn() { staffStore.checkIn() }
function checkOut() { staffStore.checkOut() }
</script>

<template>
  <div>
    <div class="page-header"><h1>Ca làm việc</h1></div>
    <div class="card mb-3" style="text-align:center">
      <div style="font-size:48px;margin-bottom:12px">
        <i v-if="current?.status === 'CHECKED_IN'" class="bi bi-check-circle-fill" style="color:#4CAF50"></i>
        <i v-else class="bi bi-clock" style="color:var(--text-light)"></i>
      </div>
      <h3 v-if="current?.status === 'CHECKED_IN'" style="color:#4CAF50">Đã check-in</h3>
      <h3 v-else style="color:var(--text-mid)">Chưa check-in</h3>
      <p v-if="current?.checkedInAt" style="color:var(--text-mid);font-size:14px">Check-in lúc {{ formatTime(current.checkedInAt) }}</p>
      <div style="margin-top:16px;display:flex;gap:12px;justify-content:center">
        <button v-if="current?.status !== 'CHECKED_IN'" class="btn btn-primary" @click="checkIn"><i class="bi bi-box-arrow-in-right"></i> Check-in</button>
        <button v-if="current?.status === 'CHECKED_IN'" class="btn btn-outline" @click="checkOut"><i class="bi bi-box-arrow-right"></i> Check-out</button>
      </div>
    </div>
    <div class="card">
      <h3 style="font-weight:700;margin-bottom:16px">Lịch sử</h3>
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>Ngày</th><th>Ca</th><th>Check-in</th><th>Check-out</th><th>Trạng thái</th></tr></thead>
          <tbody>
            <tr v-for="s in history" :key="s.date">
              <td>{{ s.date }}</td>
              <td>{{ s.shift }}</td>
              <td>{{ formatTime(s.checkedInAt) }}</td>
              <td>{{ formatTime(s.checkedOutAt) }}</td>
              <td><span class="badge badge-success">{{ SCHEDULE_STATUS_LABEL.CHECKED_OUT }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
