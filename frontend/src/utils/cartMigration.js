export class CartMigrationError extends Error {
  constructor(failedCount, canonicalConfirmed, message) {
    super(message || (canonicalConfirmed
      ? `Không thể đồng bộ ${failedCount} món trong giỏ hàng`
      : 'Không thể xác nhận giỏ hàng sau khi đăng nhập. Vui lòng thử lại.'));
    this.name = 'CartMigrationError';
    this.failedCount = failedCount;
    this.canonicalConfirmed = canonicalConfirmed;
  }
}

export function createCartMigrationController({ guestStorage, guestKey, addItem, fetchCanonical }) {
  return {
    async migrate() {
      const stored = guestStorage.getItem(guestKey);
      let original = [];
      try {
        original = stored ? JSON.parse(stored) : [];
      } catch {
        original = [];
      }
      const snapshot = Array.isArray(original) ? [...original] : [];
      const failed = [];
      for (const item of snapshot) {
        try {
          await addItem({
            productId: item.productId,
            variantId: item.variantId,
            quantity: item.quantity,
            modifierOptionIds: (item.modifiers || []).map((modifier) => modifier.modifierOptionId),
          });
        } catch {
          failed.push(item);
        }
      }
      let canonical;
      try {
        canonical = await fetchCanonical();
      } catch {
        if (stored === null) guestStorage.removeItem(guestKey);
        else guestStorage.setItem(guestKey, stored);
        throw new CartMigrationError(failed.length, false);
      }
      if (failed.length) guestStorage.setItem(guestKey, JSON.stringify(failed));
      else guestStorage.removeItem(guestKey);
      if (failed.length) throw new CartMigrationError(failed.length, true);
      return { failedCount: 0, canonical };
    },
  };
}

export function createLoginMigrationController({ login, migrate, warn, navigate }) {
  let submitting = false;
  return {
    async submit(email, password, redirect) {
      if (submitting) return null;
      submitting = true;
      try {
        const user = await login(email, password);
        let warning = '';
        let canonicalConfirmed = true;
        try {
          await migrate();
        } catch (error) {
          canonicalConfirmed = error?.canonicalConfirmed !== false;
          warning = error?.message || 'Một số món chưa được đồng bộ.';
          warning = `${warning} Các món chưa đồng bộ vẫn được giữ trong giỏ để bạn kiểm tra lại.`;
          warn(warning);
        }
        if (canonicalConfirmed) {
          const role = user?.role || '';
          await navigate(redirect || (role === 'USER' ? '/home' : `/${role.toLowerCase()}`));
        }
        return { authenticated: true, canonicalConfirmed, warning, user };
      } finally {
        submitting = false;
      }
    },
  };
}
