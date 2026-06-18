export const required = (v) => !!v || 'Vui lòng nhập trường này'

export const email = (v) => /.+@.+\..+/.test(v) || 'Email không hợp lệ'

export const minLength = (min) => (v) =>
  !v || v.length >= min || `Tối thiểu ${min} ký tự`

export const maxLength = (max) => (v) =>
  !v || v.length <= max || `Tối đa ${max} ký tự`

export const phone = (v) =>
  !v || /(84|0[3|5|7|8|9])+([0-9]{8})\b/.test(v) || 'Số điện thoại không hợp lệ'

export const password = (v) =>
  !v || v.length >= 6 || 'Mật khẩu tối thiểu 6 ký tự'

export const confirmPassword = (passwordField) => (v, form) =>
  !v || v === form[passwordField] || 'Mật khẩu không khớp'

export const positiveNumber = (v) =>
  v == null || v === '' || (Number(v) > 0) || 'Phải lớn hơn 0'

export const notNegative = (v) =>
  v == null || v === '' || (Number(v) >= 0) || 'Không được âm'
