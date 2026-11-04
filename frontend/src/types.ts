export type Role = "CUSTOMER" | "KITCHEN_STAFF" | "RESTAURANT_ADMIN";

export type OrderStatus = "PLACED" | "ACCEPTED" | "PREPARING" | "READY" | "COMPLETED" | "REJECTED" | "CANCELLED";

export type OrderType = "PICKUP" | "DELIVERY";

export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED" | "CANCELLED";

export type User = {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  role: Role;
  branchId: number | null;
  branchName: string | null;
};

export type Restaurant = {
  id: number;
  name: string;
  description: string;
  logoUrl: string | null;
  active: boolean;
};

export type Branch = {
  id: number;
  restaurantId: number;
  restaurantName: string;
  name: string;
  addressLine: string;
  city: string;
  state: string;
  postalCode: string;
  phone: string;
  opensAt: string;
  closesAt: string;
  active: boolean;
  acceptingOrders: boolean;
};

export type Category = {
  id: number;
  name: string;
  description: string | null;
  displayOrder: number;
  active: boolean;
};

export type MenuItem = {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  description: string;
  price: number;
  vegetarian: boolean;
  imageUrl: string | null;
  available: boolean;
  popular: boolean;
  active: boolean;
};

export type CartLine = {
  id: number;
  menuItemId: number;
  name: string;
  imageUrl: string | null;
  vegetarian: boolean;
  available: boolean;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
};

export type Cart = {
  branch: Branch | null;
  items: CartLine[];
  subtotal: number;
  taxAmount: number;
  deliveryFeeIfDelivery: number;
  pickupTotal: number;
  deliveryTotal: number;
  currency: string;
  itemCount: number;
};

export type OrderItem = {
  id: number;
  menuItemId: number;
  itemName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  vegetarian: boolean;
};

export type Payment = {
  id: number;
  status: PaymentStatus;
  amount: number;
  currency: string;
  provider: string;
  providerReference: string;
  failureReason: string | null;
};

export type HistoryEntry = {
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  note: string | null;
  createdAt: string;
};

export type OrderSummary = {
  id: number;
  orderNumber: string;
  branchId: number;
  branchName: string;
  orderType: OrderType;
  status: OrderStatus;
  totalAmount: number;
  currency: string;
  paymentStatus: PaymentStatus | null;
  createdAt: string;
};

export type OrderDetail = {
  id: number;
  orderNumber: string;
  branchId: number;
  branchName: string;
  orderType: OrderType;
  status: OrderStatus;
  customerName: string;
  customerPhone: string;
  deliveryAddress: string | null;
  specialInstructions: string | null;
  subtotal: number;
  taxAmount: number;
  deliveryFee: number;
  totalAmount: number;
  currency: string;
  estimatedPrepMinutes: number;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
  payment: Payment | null;
  history: HistoryEntry[];
};

export type KitchenOrder = {
  id: number;
  orderNumber: string;
  orderType: OrderType;
  status: OrderStatus;
  specialInstructions: string | null;
  totalAmount: number;
  currency: string;
  estimatedPrepMinutes: number;
  createdAt: string;
  items: OrderItem[];
};

export type PageResult<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type StatusCount = { status: OrderStatus; count: number };

export type Dashboard = {
  totalOrders: number;
  todaysOrders: number;
  pendingOrders: number;
  completedOrders: number;
  revenue: number;
  activeBranches: number;
  availableMenuItems: number;
  ordersByStatus: StatusCount[];
};

export type BranchAvailability = { branchId: number; branchName: string; available: boolean };

export type AdminMenuItem = {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  description: string;
  price: number;
  vegetarian: boolean;
  popular: boolean;
  imageUrl: string | null;
  active: boolean;
  availability: BranchAvailability[];
};

export type AdminMenu = {
  categories: Category[];
  items: AdminMenuItem[];
  branches: Branch[];
};

export type ApiEnvelope<T> = {
  success: boolean;
  data: T;
  message: string | null;
};
