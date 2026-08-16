export function canSubmitSettlement(current) {
  return current?.state === 'READY_TO_SUBMIT' && !current?.settlement;
}

export async function submitSettlement(state, { submit, refresh }) {
  state.formError = '';
  state.announcement = '';
  const amount = Number(state.submittedAmount);
  if (state.submittedAmount === '' || !Number.isFinite(amount) || amount < 0) {
    state.formError = 'Nhập số tiền thực nộp từ 0 trở lên.';
    return false;
  }
  try {
    await submit({ shiftId: state.current.shift.shiftId, submittedAmount: amount });
    return true;
  } catch (error) {
    if (error?.status === 409) {
      state.announcement = 'Ca này đã được gửi bàn giao. Trạng thái mới nhất đã được tải.';
      await refresh();
    } else {
      state.formError = error.message || 'Không thể gửi bàn giao COD.';
    }
    return false;
  }
}
