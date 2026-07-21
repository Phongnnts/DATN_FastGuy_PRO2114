import { defineStore } from 'pinia';
import { ref } from 'vue';
import { adminApi } from '@/api';

export const useAdminStore = defineStore('admin', () => {
  const dashboard = ref(null);
  const allUsers = ref([]);
  const allProducts = ref([]);
  const allCategories = ref([]);
  const allOrders = ref([]);

  const loading = ref(false);
  const error = ref('');

  function mapProduct(p) {
    return {
      id: p.productId,
      name: p.name,
      categoryId: p.categoryId,
      categoryName: p.categoryName || '',
      basePrice: typeof p.basePrice === 'string' ? parseFloat(p.basePrice) : p.basePrice || 0,
      price: typeof p.price === 'string' ? parseFloat(p.price) : p.price || 0,
      image: p.imageUrl || '',
      description: p.description || '',
       status: p.status || 'AVAILABLE',
       availableFrom: p.availableFrom || '',
       availableTo: p.availableTo || '',
       inStock: p.status === 'AVAILABLE',
      galleryImages: Array.isArray(p.galleryImages) ? p.galleryImages : [],
       variants: Array.isArray(p.variants) ? p.variants.map(v => ({ ...v })) : [],
       modifierGroups: Array.isArray(p.modifierGroups) ? p.modifierGroups.map(group => ({ ...group, options: [...(group.options || [])] })) : [],
       combo: p.combo ? { ...p.combo, items: [...(p.combo.items || [])] } : null,
     };
  }

  function mapCategory(c) {
    return {
      id: c.categoryId,
      name: c.name,
      slug: c.name
        ? c.name.toLowerCase().replace(/ \/ /g, '-').replace(/\s+/g, '-')
        : '',
      description: c.description || '',
      productCount: c.productCount || 0,
    };
  }

  async function fetchDashboard() {
    try {
      const data = await adminApi.getDashboard();
      dashboard.value = data;
      return data;
    } catch (e) {
      error.value = e.message;
      return null;
    }
  }

  async function fetchUsers() {
    const data = await adminApi.getUsers();
    allUsers.value = Array.isArray(data) ? data : [];
    return allUsers.value;
  }

  async function fetchProducts() {
    const data = await adminApi.getProducts();
    allProducts.value = Array.isArray(data) ? data.map(mapProduct) : [];
    return allProducts.value;
  }

  async function fetchCategories() {
    const data = await adminApi.getCategories();
    allCategories.value = Array.isArray(data) ? data.map(mapCategory) : [];
    return allCategories.value;
  }

  async function fetchOrders(params) {
    try {
      const data = await adminApi.getOrders(params);
      allOrders.value = Array.isArray(data) ? data : [];
      error.value = '';
      return allOrders.value;
    } catch (e) {
      error.value = e.message;
      throw e;
    }
  }


  async function createUser(data) {
    const res = await adminApi.createUser(data);
    await fetchUsers();
    return res;
  }

  async function updateUser(id, data) {
    await adminApi.updateUser(id, data);
    await fetchUsers();
  }

  async function deleteUser(id) {
    await adminApi.deleteUser(id);
    await fetchUsers();
  }

  async function createProduct(data) {
    const res = await adminApi.createProduct(data);
    await fetchProducts();
    return res;
  }

  async function updateProduct(id, data) {
    await adminApi.updateProduct(id, data);
    await fetchProducts();
  }

  async function deleteProduct(id) {
    await adminApi.deleteProduct(id);
    await fetchProducts();
  }

  async function fetchVariants(productId) {
    const data = await adminApi.getVariants(productId);
    return Array.isArray(data) ? data : [];
  }

  async function createVariant(productId, data) {
    const res = await adminApi.createVariant(productId, data);
    await fetchProducts();
    return res;
  }

  async function updateVariant(id, data) {
    await adminApi.updateVariant(id, data);
    await fetchProducts();
  }

  async function deleteVariant(id) {
    await adminApi.deleteVariant(id);
    await fetchProducts();
  }

   async function createModifierGroup(productId, data) {
      const result = await adminApi.createModifierGroup(productId, data);
      await fetchProducts();
      return result;
    }

    async function createModifierOption(groupId, data) {
      const result = await adminApi.createModifierOption(groupId, data);
      await fetchProducts();
      return result;
    }

    async function updateModifierGroup(groupId, data) {
      await adminApi.updateModifierGroup(groupId, data);
      await fetchProducts();
    }

    async function deleteModifierGroup(groupId) {
      await adminApi.deleteModifierGroup(groupId);
      await fetchProducts();
    }

    async function updateModifierOption(groupId, optionId, data) {
      await adminApi.updateModifierOption(groupId, optionId, data);
      await fetchProducts();
    }

    async function deleteModifierOption(groupId, optionId) {
      await adminApi.deleteModifierOption(groupId, optionId);
      await fetchProducts();
    }

    async function saveCombo(productId, data) {
      const result = await adminApi.saveCombo(productId, data);
      await fetchProducts();
      return result;
    }

    async function updateCombo(productId, data) {
      await adminApi.updateCombo(productId, data);
      await fetchProducts();
    }

    async function createComboItem(productId, data) {
      const result = await adminApi.createComboItem(productId, data);
      await fetchProducts();
      return result;
    }

    async function deleteComboItem(productId, itemId) {
      await adminApi.deleteComboItem(productId, itemId);
      await fetchProducts();
    }

  async function createCategory(data) {
    const res = await adminApi.createCategory(data);
    await fetchCategories();
    return res;
  }

  async function updateCategory(id, data) {
    await adminApi.updateCategory(id, data);
    await fetchCategories();
  }

  async function deleteCategory(id) {
    await adminApi.deleteCategory(id);
    await fetchCategories();
  }

  return {
    dashboard,
    allUsers,
    allProducts,
    allCategories,
    allOrders,
    loading,
    error,
    fetchDashboard,
    fetchUsers,
    fetchProducts,
    fetchCategories,
    fetchOrders,
    createUser,
    updateUser,
    deleteUser,
    createProduct,
    updateProduct,
    deleteProduct,
    fetchVariants,
    createVariant,
    updateVariant,
     deleteVariant,
      createModifierGroup,
      createModifierOption,
      updateModifierGroup,
      deleteModifierGroup,
      updateModifierOption,
      deleteModifierOption,
      saveCombo,
      updateCombo,
      createComboItem,
      deleteComboItem,
     createCategory,
     updateCategory,
     deleteCategory,

  };
});
