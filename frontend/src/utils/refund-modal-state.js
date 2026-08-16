export function focusCycleTarget({ controls, active, shiftKey }) {
  if (!controls.length) return null;
  if (!controls.includes(active)) return controls[0];
  if (shiftKey && active === controls[0]) return controls[controls.length - 1];
  if (!shiftKey && active === controls[controls.length - 1]) return controls[0];
  return null;
}

export function createRefundModalLifecycle({ document, getDialog, getFocusable, onEscape, getFallback }) {
  let opener = null;
  let open = false;
  const focusInitial = () => (getFocusable()[0] || getDialog())?.focus();
  const keydown = event => {
    if (!open || event.key !== 'Escape') return;
    event.preventDefault();
    onEscape();
  };
  const focusin = event => {
    if (open && !getDialog()?.contains(event.target)) focusInitial();
  };
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
