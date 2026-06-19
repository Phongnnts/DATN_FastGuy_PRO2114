import { defineStore } from 'pinia';
import { ref } from 'vue';
import { adminApi } from '@/api';

export const useAdminStore = defineStore('admin', () => {
  const dashboard = ref(null);
  const allUsers = ref([]);
  const allProducts = ref([]);
  const allCategories = ref([]);
  const allOrders = ref([]);
  const allZones = ref([]);
  const allShifts = ref([]);
  const allSchedules = ref([]);
  const allIngredients = ref([]);
  const loading = ref(false);
  const error = ref('');

  function mapProduct(p) {
    return {
      id: p.productId,
      name: p.name,
      categoryId: p.categoryId,
      categoryName: p.categoryName || '',
      price: typeof p.price === 'string' ? parseFloat(p.price) : p.price || 0,
      discountPrice: null,
      image: p.imageUrl || '',
      description: p.description || '',
      inStock: p.status === 'AVAILABLE',
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
    try {
      const data = await adminApi.getUsers();
      allUsers.value = Array.isArray(data) ? data : [];
      return allUsers.value;
    } catch {
      return [];
    }
  }

  async function fetchProducts() {
    try {
      const data = await adminApi.getProducts();
      allProducts.value = Array.isArray(data) ? data.map(mapProduct) : [];
      return allProducts.value;
    } catch {
      return [];
    }
  }

  async function fetchCategories() {
    try {
      const data = await adminApi.getCategories();
      allCategories.value = Array.isArray(data) ? data.map(mapCategory) : [];
      return allCategories.value;
    } catch {
      return [];
    }
  }

  async function fetchOrders() {
    try {
      const data = await adminApi.getOrders();
      allOrders.value = Array.isArray(data) ? data : [];
      return allOrders.value;
    } catch {
      return [];
    }
  }

  async function fetchZones() {
    try {
      const data = await adminApi.getDeliveryZones();
      allZones.value = Array.isArray(data) ? data : [];
      return allZones.value;
    } catch {
      return [];
    }
  }

  async function fetchIngredients() {
    try {
      const data = await adminApi.getIngredients();
      allIngredients.value = Array.isArray(data) ? data : [];
      return allIngredients.value;
    } catch {
      return [];
    }
  }

  async function fetchShifts() {
    try {
      const data = await adminApi.getShifts();
      allShifts.value = Array.isArray(data) ? data : [];
      return allShifts.value;
    } catch {
      return [];
    }
  }

  async function fetchSchedules(params) {
    try {
      const data = await adminApi.getSchedules(params);
      allSchedules.value = Array.isArray(data) ? data : [];
      return allSchedules.value;
    } catch {
      return [];
    }
  }

  async function createUser(data) {
    try {
      const res = await adminApi.createUser(data);
      await fetchUsers();
      return res;
    } catch {
      return null;
    }
  }

  async function updateUser(id, data) {
    try {
      await adminApi.updateUser(id, data);
      await fetchUsers();
    } catch {}
  }

  async function deleteUser(id) {
    try {
      await adminApi.deleteUser(id);
      await fetchUsers();
    } catch {}
  }

  async function createProduct(data) {
    try {
      const res = await adminApi.createProduct(data);
      await fetchProducts();
      return res;
    } catch {
      return null;
    }
  }

  async function updateProduct(id, data) {
    try {
      await adminApi.updateProduct(id, data);
      await fetchProducts();
    } catch {}
  }

  async function deleteProduct(id) {
    try {
      await adminApi.deleteProduct(id);
      await fetchProducts();
    } catch {}
  }

  async function createCategory(data) {
    try {
      const res = await adminApi.createCategory(data);
      await fetchCategories();
      return res;
    } catch {
      return null;
    }
  }

  async function updateCategory(id, data) {
    try {
      await adminApi.updateCategory(id, data);
      await fetchCategories();
    } catch {}
  }

  async function deleteCategory(id) {
    try {
      await adminApi.deleteCategory(id);
      await fetchCategories();
    } catch {}
  }

  async function createZone(data) {
    try {
      const res = await adminApi.createZone(data);
      await fetchZones();
      return res;
    } catch {
      return null;
    }
  }

  async function updateZone(id, data) {
    try {
      await adminApi.updateZone(id, data);
      await fetchZones();
    } catch {}
  }

  async function deleteZone(id) {
    try {
      await adminApi.deleteZone(id);
      await fetchZones();
    } catch {}
  }

  async function createIngredient(data) {
    try {
      const res = await adminApi.createIngredient(data);
      await fetchIngredients();
      return res;
    } catch {
      return null;
    }
  }

  async function updateIngredient(id, data) {
    try {
      await adminApi.updateIngredient(id, data);
      await fetchIngredients();
    } catch {}
  }

  async function deleteIngredient(id) {
    try {
      await adminApi.deleteIngredient(id);
      await fetchIngredients();
    } catch {}
  }

  async function createShift(data) {
    try {
      const res = await adminApi.createShift(data);
      await fetchShifts();
      return res;
    } catch {
      return null;
    }
  }

  async function updateShift(id, data) {
    try {
      await adminApi.updateShift(id, data);
      await fetchShifts();
    } catch {}
  }

  async function deleteShift(id) {
    try {
      await adminApi.deleteShift(id);
      await fetchShifts();
    } catch {}
  }

  async function createSchedule(data) {
    try {
      const res = await adminApi.createSchedule(data);
      await fetchSchedules();
      return res;
    } catch {
      return null;
    }
  }

  async function getRevenueReport(params) {
    try {
      return await adminApi.getRevenueReport(params);
    } catch {
      return [];
    }
  }

  async function getTopProducts(params) {
    try {
      return await adminApi.getTopProducts(params);
    } catch {
      return [];
    }
  }

  return {
    dashboard,
    allUsers,
    allProducts,
    allCategories,
    allOrders,
    allZones,
    allShifts,
    allSchedules,
    allIngredients,
    loading,
    error,
    fetchDashboard,
    fetchUsers,
    fetchProducts,
    fetchCategories,
    fetchOrders,
    fetchZones,
    fetchIngredients,
    fetchShifts,
    fetchSchedules,
    createUser,
    updateUser,
    deleteUser,
    createProduct,
    updateProduct,
    deleteProduct,
    createCategory,
    updateCategory,
    deleteCategory,
    createZone,
    updateZone,
    deleteZone,
    createIngredient,
    updateIngredient,
    deleteIngredient,
    createShift,
    updateShift,
    deleteShift,
    createSchedule,
    getRevenueReport,
    getTopProducts,
  };
});
