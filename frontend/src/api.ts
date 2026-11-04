import axios from "axios";
import type {
  AdminMenu,
  AdminMenuItem,
  ApiEnvelope,
  Branch,
  BranchAvailability,
  Cart,
  Category,
  Dashboard,
  KitchenOrder,
  MenuItem,
  OrderDetail,
  OrderStatus,
  OrderSummary,
  PageResult,
  Restaurant,
  User
} from "./types";

export const api = axios.create({ withCredentials: true });

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>) {
  const response = await promise;
  return response.data.data;
}

export const authApi = {
  me: () => unwrap<User>(api.get("/api/auth/me")),
  login: (email: string, password: string) => unwrap<User>(api.post("/api/auth/login", { email, password })),
  register: (body: { fullName: string; email: string; password: string; phone: string }) =>
    unwrap<User>(api.post("/api/auth/register", body)),
  logout: () => api.post("/api/auth/logout")
};

export const catalogApi = {
  restaurants: () => unwrap<Restaurant[]>(api.get("/api/restaurants")),
  branches: () => unwrap<Branch[]>(api.get("/api/branches")),
  branch: (id: number) => unwrap<Branch>(api.get(`/api/branches/${id}`)),
  categories: () => unwrap<Category[]>(api.get("/api/menu/categories")),
  featured: () => unwrap<MenuItem[]>(api.get("/api/menu/featured")),
  menu: (branchId: number, params: Record<string, string | number | boolean | undefined>) =>
    unwrap<MenuItem[]>(api.get(`/api/branches/${branchId}/menu`, { params })),
  item: (id: number, branchId: number) => unwrap<MenuItem>(api.get(`/api/menu/items/${id}`, { params: { branchId } }))
};

export const cartApi = {
  get: () => unwrap<Cart>(api.get("/api/cart")),
  setBranch: (branchId: number) => unwrap<Cart>(api.put("/api/cart/branch", { branchId })),
  add: (menuItemId: number, quantity: number) => unwrap<Cart>(api.post("/api/cart/items", { menuItemId, quantity })),
  update: (cartItemId: number, quantity: number) => unwrap<Cart>(api.put(`/api/cart/items/${cartItemId}`, { quantity })),
  remove: (cartItemId: number) => unwrap<Cart>(api.delete(`/api/cart/items/${cartItemId}`)),
  clear: () => unwrap<Cart>(api.delete("/api/cart"))
};

export const orderApi = {
  place: (body: unknown, idempotencyKey: string) =>
    unwrap<OrderDetail>(api.post("/api/orders", body, { headers: { "Idempotency-Key": idempotencyKey } })),
  mine: () => unwrap<PageResult<OrderSummary>>(api.get("/api/orders/my", { params: { size: 20 } })),
  get: (id: number) => unwrap<OrderDetail>(api.get(`/api/orders/${id}`)),
  tracking: (id: number) => unwrap<OrderDetail>(api.get(`/api/orders/${id}/tracking`)),
  cancel: (id: number) => unwrap<OrderDetail>(api.post(`/api/orders/${id}/cancel`))
};

export const kitchenApi = {
  list: (status?: OrderStatus) => unwrap<KitchenOrder[]>(api.get("/api/kitchen/orders", { params: status ? { status } : {} })),
  update: (id: number, status: OrderStatus) => unwrap<KitchenOrder>(api.patch(`/api/kitchen/orders/${id}/status`, { status }))
};

export const adminApi = {
  dashboard: () => unwrap<Dashboard>(api.get("/api/admin/dashboard")),
  branches: () => unwrap<Branch[]>(api.get("/api/admin/branches")),
  createBranch: (body: unknown) => unwrap<Branch>(api.post("/api/admin/branches", body)),
  updateBranch: (id: number, body: unknown) => unwrap<Branch>(api.put(`/api/admin/branches/${id}`, body)),
  operations: (id: number, active: boolean, acceptingOrders: boolean) =>
    unwrap<Branch>(api.patch(`/api/admin/branches/${id}/operations`, { active, acceptingOrders })),
  menu: () => unwrap<AdminMenu>(api.get("/api/admin/menu")),
  createCategory: (body: unknown) => unwrap<Category>(api.post("/api/admin/categories", body)),
  createItem: (body: unknown) => unwrap<AdminMenuItem>(api.post("/api/admin/menu/items", body)),
  updateItem: (id: number, body: unknown) => unwrap<AdminMenuItem>(api.put(`/api/admin/menu/items/${id}`, body)),
  deactivateItem: (id: number) => api.delete(`/api/admin/menu/items/${id}`),
  availability: (branchId: number, itemId: number, available: boolean) =>
    unwrap<BranchAvailability>(api.put(`/api/admin/branches/${branchId}/menu/${itemId}`, { available })),
  orders: (params: Record<string, string | number | undefined>) =>
    unwrap<PageResult<OrderSummary>>(api.get("/api/admin/orders", { params })),
  order: (id: number) => unwrap<OrderDetail>(api.get(`/api/admin/orders/${id}`)),
  staff: (body: unknown) => unwrap<User>(api.post("/api/admin/staff", body))
};
