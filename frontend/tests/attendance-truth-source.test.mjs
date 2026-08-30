import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const adminPage = readFileSync(new URL('../src/views/admin/ShiftsPage.vue', import.meta.url), 'utf8');
const staffPage = readFileSync(new URL('../src/views/staff/StaffShiftsPage.vue', import.meta.url), 'utf8');
const attendanceSpec = readFileSync(new URL('e2e/attendance-report-real-backend.spec.js', import.meta.url), 'utf8');
const operationsSpec = readFileSync(new URL('e2e/operations-real-backend.spec.js', import.meta.url), 'utf8');
const realBackendConfig = readFileSync(new URL('../playwright.real-backend.config.js', import.meta.url), 'utf8');
const operationsFixture = readFileSync(new URL('../../Backend/FastGuy-FastFoodSite/src/test/java/integration/OperationsBrowserFixtureIT.java', import.meta.url), 'utf8');
const realHarness = readFileSync(new URL('../../scripts/run-staff-dispatch-real-e2e.ps1', import.meta.url), 'utf8');
const verifyTargetSource = operationsFixture.slice(operationsFixture.indexOf('private void verifyTarget'), operationsFixture.indexOf('private void seed('));
const weeklySeedSource = operationsFixture.slice(operationsFixture.indexOf('private void seed('), operationsFixture.indexOf('private void seedAttendance'));
const attendanceSeedSource = operationsFixture.slice(operationsFixture.indexOf('private void seedAttendance'), operationsFixture.indexOf('private User user('));
const adminE2eSource = attendanceSpec.slice(attendanceSpec.indexOf("test('Admin"), attendanceSpec.indexOf("test('Staff"));
const staffE2eSource = attendanceSpec.slice(attendanceSpec.indexOf("test('Staff"));

function assertBefore(source, declaration, trigger) {
  assert.ok(source.indexOf(declaration) >= 0 && source.indexOf(declaration) < source.indexOf(trigger), `${declaration} must precede ${trigger}`);
}

test('attendance sources distinguish new manual actions from legacy automatic records', () => {
  for (const source of [adminPage, staffPage]) {
    assert.match(source, /source(?:Label)?\([^)]*\)[^}]*=== 'AUTO' \? 'Tự động trước đây'/);
    assert.match(source, /=== 'MANUAL' \? 'Thủ công'/);
  }
  assert.match(adminPage, /CHECK_IN_WINDOW: 'Có thể check-in thủ công'/);
  assert.match(adminPage, /CHECK_OUT_WINDOW: 'Có thể check-out thủ công'/);
  assert.match(adminPage, /ACTIVE_AUTO: 'Đang làm · tự động trước đây'/);
  assert.match(adminPage, /COMPLETED_AUTO: 'Hoàn tất · tự động trước đây'/);
  assert.match(adminPage, /LATE: 'Chưa check-in'/);
  assert.match(staffPage, /<p>Chọn ngày màu xanh để xem ca\. Check-in và check-out đều do bạn thực hiện\.<\/p>/);
  assert.match(staffPage, /:aria-label="`Check-in thủ công ca \$\{shiftCodeLabel\(shift\.shiftCode\)\} ngày \$\{shift\.shiftDate\}`"/);
  assert.match(staffPage, /:aria-label="`Check-out thủ công ca \$\{shiftCodeLabel\(shift\.shiftCode\)\} ngày \$\{shift\.shiftDate\}`"/);
  assert.match(staffPage, /'Check-in thủ công'/);
  assert.match(staffPage, /'Check-out thủ công'/);
  assert.doesNotMatch(staffPage, /<span class="sr-only">MANUAL AUTO<\/span>/);
});

test('real attendance E2E covers manual instruction on desktop and mobile with API and browser evidence', () => {
  assert.doesNotMatch(operationsSpec, /getByText\('MANUAL AUTO'\).*toBeAttached/);
  assert.match(operationsSpec, /Check-in và check-out đều do bạn thực hiện\./);
  assert.match(attendanceSpec, /Check-in và check-out đều do bạn thực hiện\./);
  assert.match(attendanceSpec, /page\.goto\('\/admin\/attendance'\)/);
  assert.doesNotMatch(attendanceSpec, /getByRole\('tab', \{ name: 'Duyệt công' \}\)/);
  assert.match(attendanceSpec, /getByRole\('heading', \{ name: 'Chấm công & tiền công' \}\)/);
  assert.doesNotMatch(attendanceSpec, /getByText\(\/Không có chấm công phù hợp\|Phút duyệt\//);
  assert.doesNotMatch(attendanceSpec, /getByText\('Không có chấm công phù hợp\.'/);
  assert.match(attendanceSpec, /getByRole\('spinbutton', \{ name: 'Phút duyệt' \}\)\.first\(\)/);
  assert.match(realHarness, /FASTGUY_E2E_ATTENDANCE_TRUTH_R8/);
  assert.match(operationsFixture, /boolean attendanceTruthR8 = "true"\.equalsIgnoreCase\(System\.getenv\("FASTGUY_E2E_ATTENDANCE_TRUTH_R8"\)\) && "FastGuyDB_Attendance061_Test"\.equals\(requiredEnv\("FASTGUY_E2E_DB_NAME"\)\);/);
  assert.equal((weeklySeedSource.match(/UPDATE WorkShift/g) || []).length, 1);
  assert.match(weeklySeedSource, /if \(current && existing > 0 && !attendanceTruthR8\) \{[^}]*UPDATE WorkShift/s);
  assert.match(weeklySeedSource, /if \(attendanceTruthR8\) seedAttendance\(em, staff, now\);/);
  assert.match(operationsFixture, /FastGuyDB_Attendance061_Test/);
  assert.match(verifyTargetSource, /FastGuyDB_Attendance061_Test[^}]*061_work_shift_attendance_approval/s);
  assert.match(operationsFixture, /seedAttendance\([^)]*staff[^)]*now/);
  assert.match(attendanceSeedSource, /YearMonth\.from\(now\)\.minusMonths\(1\)/);
  assert.match(attendanceSeedSource, /atDay\(1\)/);
  assert.match(attendanceSeedSource, /atEndOfMonth\(\)/);
  assert.match(attendanceSeedSource, /SELECT COUNT_BIG\(\*\) FROM WorkShift WHERE shift_date=:date AND shift_code=:code AND staff_role_snapshot='STAFF'/);
  assert.match(attendanceSeedSource, /INSERT INTO WorkShift/);
  assert.doesNotMatch(attendanceSeedSource, /UPDATE WorkShift/);
  assert.match(attendanceSeedSource, /No free Staff attendance slot in previous month/);
  assert.match(attendanceSeedSource, /'CHECKED_OUT'[^\r\n]*'STAFF'[^\r\n]*'MANUAL'[^\r\n]*'MANUAL'[^\r\n]*'PENDING'/);
  assert.match(operationsFixture, /DELETE WorkShift WHERE user_id IN \(:ids\)/);
  assert.match(operationsFixture, /SELECT COUNT_BIG\(\*\) FROM WorkShift WHERE user_id IN \(:ids\)/);
  assert.match(attendanceSpec, /timeZone: 'Asia\/Ho_Chi_Minh'/);
  assert.match(attendanceSpec, /setDate\(1\)/);
  assert.match(attendanceSpec, /setMonth\(.*getMonth\(\) - 1\)/);
  assert.doesNotMatch(attendanceSpec, /page\.getByLabel\('Tháng'\)/);
  assert.match(adminE2eSource, /const attendancePanel = page\.getByRole\('region', \{ name: 'Chấm công' \}\);/);
  assert.match(staffE2eSource, /const attendancePanel = page\.getByRole\('region', \{ name: 'Chấm công tháng' \}\);/);
  assert.ok((attendanceSpec.match(/const monthInput = attendancePanel\.getByLabel\('Tháng'\);/g) || []).length === 2);
  assert.ok((attendanceSpec.match(/await monthInput\.fill\(attendanceMonth\);[\s\S]*?await monthInput\.press\('Tab'\);/g) || []).length === 2);
  assertBefore(adminE2eSource, "const attendanceResponse = page.waitForResponse", "await page.goto('/admin/attendance')");
  assertBefore(adminE2eSource, "const selectedAttendanceResponse = page.waitForResponse", "await monthInput.fill(attendanceMonth)");
  assertBefore(staffE2eSource, "const attendanceResponse = page.waitForResponse", "await page.goto('/staff/shifts')");
  assertBefore(staffE2eSource, "const selectedAttendanceResponse = page.waitForResponse", "await monthInput.fill(attendanceMonth)");
  assert.match(attendanceSpec, /getByText\('Chờ duyệt', \{ exact: true \}\)\.first\(\)/);
  assert.match(attendanceSpec, /\/api\/shifts\/attendance/);
  assert.match(attendanceSpec, /errors\)\.toEqual\(\[\]\)/);
  for (const project of ['desktop-chrome', 'mobile-chrome']) assert.match(realBackendConfig, new RegExp(`name: '${project}'`));
});

test('attendance truth copy does not remove compatibility and conflict branches', () => {
  assert.match(adminPage, /sourceValue === 'AUTO'/);
  assert.match(staffPage, /source === 'AUTO'/);
  assert.match(staffPage, /shiftApi\.checkIn\(shift\.shiftId\)/);
  assert.match(staffPage, /shiftApi\.checkOut\(shift\.shiftId\)/);
  assert.match(staffPage, /e\.status === 409/);
  assert.match(staffPage, /activeOwnershipCount/);
});
