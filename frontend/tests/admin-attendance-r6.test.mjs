import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const page=readFileSync(new URL('../src/views/admin/AttendancePage.vue',import.meta.url),'utf8');
const api=readFileSync(new URL('../src/api/admin.js',import.meta.url),'utf8');
test('R6 exposes append-only pay rate history and creation',()=>{assert.match(api,/getStaffPayRates/);assert.match(api,/createStaffPayRate/);assert.match(page,/Mức công nhân viên/);assert.match(page,/effectiveFrom/);assert.match(page,/regularHourlyRate/);assert.match(page,/overtimeHourlyRate/);});
test('R6 renders snapshot preview missing and legacy states truthfully',()=>{for(const field of ['previewTotalPayAmount','totalPayAmount','paySnapshotStatus'])assert.match(page,new RegExp(field));assert.match(page,/Chưa cấu hình mức công/);assert.match(page,/Không có snapshot lịch sử/);assert.match(page,/Kết quả tháng/);assert.doesNotMatch(page,/Bảng lương|Thuế|Bảo hiểm/);});
test('R6 blocks approval without an effective rate',()=>{assert.match(page,/:disabled="[^"]*effectiveRegularHourlyRate/);});
