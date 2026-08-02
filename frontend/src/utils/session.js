export function decodeTokenPayload(token) {
  if (!token || typeof token !== 'string') return null;
  try {
    const encoded = token.split('.')[1];
    if (!encoded) return null;
    const normalized = encoded.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    return JSON.parse(decodeURIComponent(Array.from(atob(padded), (char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`).join('')));
  } catch {
    return null;
  }
}

export function isTokenValid(token, now = Date.now()) {
  const payload = decodeTokenPayload(token);
  return !!payload && Number.isFinite(Number(payload.exp)) && Number(payload.exp) * 1000 > now;
}

export function parseStoredUser(value) {
  if (!value) return null;
  try {
    const user = JSON.parse(value);
    return user && typeof user === 'object' && !Array.isArray(user) ? user : null;
  } catch {
    return null;
  }
}

export function clearStoredSession() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  if (typeof window !== 'undefined') window.dispatchEvent(new Event('fastguy-session-cleared'));
}
