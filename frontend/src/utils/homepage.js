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
