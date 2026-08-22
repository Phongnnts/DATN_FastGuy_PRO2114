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

export function mapProduct(product, complete = true) {
  const variants = Array.isArray(product.variants) ? product.variants.map(mapVariant) : [];
  const defaultVariant = product.defaultVariant ? mapVariant(product.defaultVariant) : null;
  const averageRating = Number(product.averageRating);
  const reviewCount = Number(product.reviewCount);
  const discountPercent = Number(product.discountPercent);
  const originalPrice = parsePrice(product.originalPrice);
  return {
    cardDataComplete: complete !== false,
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
    averageRating: Number.isFinite(averageRating) ? Math.min(5, Math.max(0, averageRating)) : 0,
    reviewCount: Number.isFinite(reviewCount) ? Math.max(0, Math.floor(reviewCount)) : 0,
    soldCount: Math.max(0, Math.floor(Number(product.soldCount ?? product.totalSold) || 0)),
    bestSeller: Boolean(product.bestSeller ?? product.isBestSeller),
    isNew: Boolean(product.isNew),
    discountPercent: Number.isFinite(discountPercent) && discountPercent > 0 ? Math.min(100, Math.round(discountPercent)) : null,
    originalPrice: Number(originalPrice) > 0 ? Number(originalPrice) : null,
    productType: product.productType || (product.combo ? 'COMBO' : null),
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
