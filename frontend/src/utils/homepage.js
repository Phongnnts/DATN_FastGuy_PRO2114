export const OCCASIONS = {
  QUICK_BREAK: { label: 'Bữa nhanh gọn', copy: 'Gọn nhẹ cho một khoảng nghỉ ngắn.', color: 'orange' },
  OFFICE_LUNCH: { label: 'Bữa trưa văn phòng', copy: 'Đủ vị cho giờ trưa bận rộn.', color: 'charcoal' },
  STUDENT: { label: 'Combo sinh viên', copy: 'Dễ chọn cho ngày học dài.', color: 'amber' },
  GROUP: { label: 'Ăn vui theo nhóm', copy: 'Chia sẻ món ngon cùng mọi người.', color: 'green' },
};

export function isTrustedHomepageAvatar(value) {
  if (typeof value !== 'string') return false;
  try {
    const url = new URL(value);
    return url.protocol === 'https:'
      && url.hostname === 'res.cloudinary.com'
      && /^\/ds4dnsj0o\/image\/upload\/(?:[a-z][A-Za-z0-9_.:,%-]*\/)*(?:v\d+\/)?Image_Cloudinery\/Avatar\//.test(url.pathname);
  } catch {
    return false;
  }
}

export function mapHomepageProduct(product) {
  return {
    ...product,
    image: product.imageUrl || '',
    variants: Array.isArray(product.variants) ? product.variants : [],
    modifierGroups: Array.isArray(product.modifierGroups) ? product.modifierGroups : [],
  };
}

export function mapOccasion(item) {
  const content = OCCASIONS[item.occasion];
  return content && item.product ? { ...content, occasion: item.occasion, product: mapHomepageProduct(item.product) } : null;
}
