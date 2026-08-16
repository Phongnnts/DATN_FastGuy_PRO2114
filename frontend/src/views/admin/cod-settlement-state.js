export function validateVerification({ settlement, status, verifiedAmount, reason }) {
  const amount = Number(verifiedAmount);
  const submitted = Number(settlement?.submittedAmount);
  if (verifiedAmount === '' || !Number.isFinite(amount) || amount < 0) return 'Số tiền kiểm đếm không hợp lệ.';
  if (status === 'SETTLED' && amount !== submitted) return 'Số tiền khớp phải bằng số đã nộp.';
  if (status === 'SHORT' && amount >= submitted) return 'Thiếu tiền cần số kiểm đếm thấp hơn số đã nộp.';
  if (status === 'OVER' && amount <= submitted) return 'Thừa tiền cần số kiểm đếm cao hơn số đã nộp.';
  if (status !== 'SETTLED' && !reason.trim()) return 'Vui lòng nhập lý do chênh lệch.';
  return '';
}

export function buildVerificationPayload(settlement, status, verifiedAmount, reason) {
  return {
    expectedStatus: settlement.status,
    status,
    verifiedAmount: Number(verifiedAmount),
    reason: status === 'SETTLED' ? null : reason.trim(),
  };
}

export function canVerifySettlement(settlement) {
  return settlement?.status === 'SUBMITTED';
}

export function focusCycleTarget({ controls, active, shiftKey }) {
  if (!controls.length) return null;
  if (!controls.includes(active)) return controls[0];
  if (shiftKey && active === controls[0]) return controls[controls.length - 1];
  if (!shiftKey && active === controls[controls.length - 1]) return controls[0];
  return null;
}

export function createModalLifecycle({ document, getDialog, getFocusable, onEscape, getFallback }) {
  let opener = null;
  let open = false;
  const focusInitial = () => (getFocusable()[0] || getDialog())?.focus();
  const keydown = event => { if (open && event.key === 'Escape') onEscape(); };
  const focusin = event => { if (open && !getDialog()?.contains(event.target)) focusInitial(); };
  return {
    attach() {
      document.addEventListener('keydown', keydown);
      document.addEventListener('focusin', focusin);
    },
    detach() {
      document.removeEventListener('keydown', keydown);
      document.removeEventListener('focusin', focusin);
    },
    open(target) {
      opener = target;
      open = true;
      focusInitial();
    },
    close() {
      open = false;
      const target = opener?.isConnected ? opener : getFallback();
      opener = null;
      if (target?.isConnected !== false) target?.focus();
    },
  };
}

export function openVerification(state, settlement, trigger) {
  state.selected = settlement;
  state.status = 'SETTLED';
  state.verifiedAmount = String(settlement.submittedAmount);
  state.reason = '';
  state.formError = '';
  state.conflictMessage = '';
  state.restoreTarget = trigger;
  state.focusTarget = 'first-control';
  return state;
}

export function closeVerification(state) {
  state.selected = null;
  state.focusTarget = null;
  return state;
}

export async function submitVerification(state, actions) {
  state.formError = validateVerification({ settlement: state.selected, status: state.status, verifiedAmount: state.verifiedAmount, reason: state.reason });
  if (state.formError) return false;
  const settlement = state.selected;
  try {
    await actions.verify(settlement.settlementId, buildVerificationPayload(settlement, state.status, state.verifiedAmount, state.reason));
    state.rows = await actions.refresh();
    state.successMessage = 'Đã xác nhận bàn giao COD.';
    closeVerification(state);
    return true;
  } catch (error) {
    if (error?.status === 409) {
      state.conflictMessage = 'Bàn giao đã được xử lý ở nơi khác. Danh sách mới nhất đã được tải.';
      state.rows = await actions.refresh();
      return false;
    }
    throw error;
  }
}

export function acceptsAdminCodRequest({ requestGeneration, latestGeneration, requestStatus, activeStatus, stopped }) {
  return !stopped && requestGeneration === latestGeneration && requestStatus === activeStatus;
}
