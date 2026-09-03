export function required(value) {
  return String(value ?? '').trim().length > 0;
}

export function validEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value ?? '').trim());
}

export function validPhone(value) {
  return /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/.test(String(value ?? '').trim());
}

export function validPassword(value) {
  const password = String(value ?? '');
  return password.length >= 8 && password.length <= 72 && /[a-zA-Z]/.test(password) && /[0-9]/.test(password);
}

export function matchesPassword(value, password) {
  return value === password;
}
