export const ROLES = {
  GUEST: 'GUEST',
  USER: 'USER',
  STAFF: 'STAFF',

  ADMIN: 'ADMIN',
};

export const ORDER_STATUS = {
  PENDING: 'PENDING',
  CONFIRMED: 'CONFIRMED',
  PREPARING: 'PREPARING',
  READY: 'READY',
  DELIVERING: 'DELIVERING',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED',
};

export const ORDER_STATUS_LABEL = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PREPARING: 'Đang chuẩn bị',
  READY: 'Đã sẵn sàng',
  DELIVERING: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
};

export const ORDER_STATUS_BADGE = {
  PENDING: 'warning',
  CONFIRMED: 'info',
  PREPARING: 'primary',
  READY: 'success',
  DELIVERING: 'info',
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
  DELIVERING: 'DELIVERING',
  DELIVERED: 'DELIVERED',
  FAILED: 'FAILED',
};

export const DELIVERY_STATUS_LABEL = {
  READY: 'Sẵn sàng',
  DELIVERING: 'Đang giao',
  DELIVERED: 'Đã giao',
  FAILED: 'Thất bại',
};

export const PAYMENT_METHOD = {
  COD: 'COD',
  MOMO: 'MOMO',
  BANK_TRANSFER: 'BANK_TRANSFER',
  VNPAY: 'VNPAY',
};

export const PAYMENT_METHOD_LABEL = {
  COD: 'Tiền mặt (COD)',
  MOMO: 'Ví MoMo',
  BANK_TRANSFER: 'Chuyển khoản',
  VNPAY: 'VNPay',
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

export const INGREDIENT_UNITS = [
  'kg',
  'g',
  'l',
  'ml',
  'cái',
  'hộp',
  'chai',
  'bịch',
];

export const SHIFT_TYPES = ['Sáng (6h-14h)', 'Chiều (14h-22h)', 'Tối (22h-6h)'];

export const DELIVERY_ZONE_TYPES = ['Quận', 'Huyện', 'Phường', 'Xã'];

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
