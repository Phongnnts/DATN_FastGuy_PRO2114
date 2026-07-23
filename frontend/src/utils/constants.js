export const ROLES = {
  GUEST: 'GUEST',
  USER: 'USER',
  STAFF: 'STAFF',
  SHIPPER: 'SHIPPER',
  ADMIN: 'ADMIN',
};

export const ORDER_STATUS = {
  PENDING: 'PENDING',
  CONFIRMED: 'CONFIRMED',
  PREPARING: 'PREPARING',
  READY: 'READY',
  PICKED_UP: 'PICKED_UP',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED',
};

export const ORDER_STATUS_LABEL = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PREPARING: 'Đang chuẩn bị',
  READY: 'Sẵn sàng giao',
  PICKED_UP: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
};

export const ORDER_STATUS_BADGE = {
  PENDING: 'warning',
  CONFIRMED: 'info',
  PREPARING: 'primary',
  READY: 'success',
  PICKED_UP: 'info',
  DELIVERED: 'success',
  CANCELLED: 'danger',
};

export const SCHEDULE_STATUS = {
  PENDING: 'PENDING',
  CHECKED_IN: 'CHECKED_IN',
  CHECKED_OUT: 'CHECKED_OUT',
  ABSENT: 'ABSENT',
};

export const SCHEDULE_STATUS_LABEL = {
  PENDING: 'Chờ',
  CHECKED_IN: 'Đã check-in',
  CHECKED_OUT: 'Đã check-out',
  ABSENT: 'Vắng',
};

export const DELIVERY_STATUS = {
  READY: 'READY',
  PICKED_UP: 'PICKED_UP',
  DELIVERED: 'DELIVERED',
  FAILED: 'FAILED',
};

export const DELIVERY_STATUS_LABEL = {
  READY: 'Sẵn sàng giao',
  PICKED_UP: 'Đang giao',
  DELIVERED: 'Đã giao',
  FAILED: 'Thất bại',
};

export const PAYMENT_METHOD = {
  COD: 'COD',
  BANK_TRANSFER: 'BANK_TRANSFER',
};

export const PAYMENT_METHOD_LABEL = {
  COD: 'Tiền mặt (COD)',
  BANK_TRANSFER: 'Chuyển khoản (QR Code)',
};

export const PAYMENT_STATUS = {
  UNPAID: 'UNPAID',
  PAID: 'PAID',
  REFUNDED: 'REFUNDED',
};

export const PAYMENT_STATUS_LABEL = {
  UNPAID: 'Chưa thanh toán',
  PAID: 'Đã thanh toán',
  REFUNDED: 'Đã hoàn tiền',
};

// export const SHIFT_TYPES = ['Sáng (6h-14h)', 'Chiều (14h-22h)', 'Tối (22h-6h)'];



export const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

export const CLOUDINARY = {
  cloudName: 'ds4dnsj0o',
  uploadPreset: 'upload-fastguy',
  uploadUrl: 'https://api.cloudinary.com/v1_1/ds4dnsj0o/image/upload',
  folders: [
    'Image_Cloudinery/Burger/',
    'Image_Cloudinery/GaRan/',
    'Image_Cloudinery/Tacos/',
    'Image_Cloudinery/Pizza/',
    'Image_Cloudinery/Com/',
    'Image_Cloudinery/BanhMi/',
    'Image_Cloudinery/GoiTom/',
    'Image_Cloudinery/KhoaiTay/',
    'Image_Cloudinery/Nuoc/',
  ],
};
