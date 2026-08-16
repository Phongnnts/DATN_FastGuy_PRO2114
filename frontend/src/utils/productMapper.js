const PLACEHOLDER_IMAGE = 'https://placehold.co/400x300/FFF0E8/D4764A?text=FastGuy';

function ensureImage(url) {
  return url && url.trim() ? url : PLACEHOLDER_IMAGE;
}

function parsePrice(value) {
  return typeof value === 'string' ? parseFloat(value) : value;
}

function mapVariant(variant) {
  return {
    ...variant,
    price: parsePrice(variant.price) || 0,
    quantityAvailable: variant.quantityAvailable === null || variant.quantityAvailable === undefined ? null : Number(variant.quantityAvailable),
    status: variant.status || 'UNAVAILABLE',
  };
}

export function mapProduct(product) {
  const variants = Array.isArray(product.variants) ? product.variants.map(mapVariant) : [];
  const defaultVariant = product.defaultVariant ? mapVariant(product.defaultVariant) : null;
  return {
    productId: product.productId,
    name: product.name,
    categoryId: product.categoryId,
    categoryName: product.categoryName || '',
    basePrice: parsePrice(product.basePrice),
    price: parsePrice(product.price),
    discountPrice: parsePrice(product.discountPrice) || null,
    defaultVariant,
    variants,
    image: ensureImage(product.imageUrl),
    description: product.description || '',
    rating: product.rating || 0,
    reviewCount: product.reviewCount || 0,
    soldCount: Number(product.soldCount ?? product.totalSold) || 0,
    bestSeller: Boolean(product.bestSeller ?? product.isBestSeller),
    productType: product.productType || (product.combo ? 'COMBO' : 'SIMPLE'),
    availableFrom: product.availableFrom || '',
    availableTo: product.availableTo || '',
    isAvailable: product.isAvailable !== false,
    isAvailableNow: product.isAvailableNow !== undefined ? product.isAvailableNow : true,
    inStock: product.inStock !== undefined ? product.inStock : variants.some(variant => variant.status === 'AVAILABLE' && (variant.quantityAvailable === null || variant.quantityAvailable > 0)),
    featured: product.featured || false,
    galleryImages: product.galleryImages || [],
    modifierGroups: Array.isArray(product.modifierGroups) ? product.modifierGroups.map(group => ({ ...group, options: (group.options || []).map(option => ({ ...option, price: parsePrice(option.price) || 0 })) })) : [],
    combo: product.combo ? { ...product.combo, items: product.combo.items || [] } : null,
  };
}
