// Tipos que reflejan el contrato del backend (docs/API.md y los DTO de src/main/java/.../dto).
// Las horas llegan como "HH:mm:ss" y las fechas como "YYYY-MM-DD".

export type OrderStatus =
  | "PENDING_PAYMENT"
  | "PENDING"
  | "IN_PREPARATION"
  | "DELIVERED"
  | "CANCELLED"
  | "EXPIRED";

export type DeliveryZone = "SHOPPING_CENTER" | "FURNITURE_STREET";

export type StaffRole = "ADMIN" | "KITCHEN" | "CASHIER" | "DELIVERY";

export type StaffSection = "pedidos" | "cocina" | "caja" | "entrega" | "menu" | "personal";

export type TicketStatus = "SENT" | "ACKNOWLEDGED";

export interface PublicConfig {
  timeZone: string;
  cutoffTime: string;
  deliveryStart: string;
  deliveryEnd: string;
  minQuantity: number;
  maxQuantity: number;
  maxOrdersPerDay: number;
  paymentsRequired: boolean;
  currency: string;
  paymentExpirationMinutes: number;
  zones: { code: DeliveryZone; label: string }[];
}

export interface Dish {
  id: number;
  name: string;
  description: string | null;
  price: number;
  thumbnailUrl: string | null;
  imageUrl: string | null;
}

export interface MenuResponse {
  dishes: Dish[];
  message: string | null;
}

export interface CreateOrderRequest {
  dishId: number;
  quantity: number;
  customerName: string;
  deliveryTime: string;
  deliveryZone: DeliveryZone;
  deliveryDetail: string;
}

export interface CreatedOrderResponse {
  id: number;
  dishName: string;
  quantity: number;
  customerName: string;
  orderDate: string;
  deliveryTime: string;
  deliveryZone: DeliveryZone;
  deliveryDetail: string;
  status: OrderStatus;
  total: number;
  accessCode: string;
  paymentUrl: string | null;
}

export interface OrderTrackingResponse {
  id: number;
  status: OrderStatus;
  dishName: string;
  quantity: number;
  deliveryTime: string;
  deliveryZone: DeliveryZone;
  deliveryDetail: string;
  total: number;
  paymentPending: boolean;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  fullName: string;
  role: StaffRole;
  sections: StaffSection[];
}

export interface StaffOrder {
  id: number;
  dishName: string;
  quantity: number;
  customerName: string;
  orderDate: string;
  deliveryTime: string;
  deliveryZone: DeliveryZone;
  deliveryDetail: string;
  status: OrderStatus;
  total: number;
}

export interface DailyOrdersResponse {
  date: string;
  totalLunches: number;
  orders: StaffOrder[];
}

export interface KitchenTicket {
  ticketId: number;
  orderId: number;
  dishName: string;
  quantity: number;
  deliveryTime: string;
  orderStatus: OrderStatus;
  ticketStatus: TicketStatus;
}

export interface CashierOrder {
  id: number;
  customerName: string;
  dishName: string;
  quantity: number;
  unitPrice: number;
  total: number;
  status: OrderStatus;
}

export interface DeliveryOrder {
  id: number;
  customerName: string;
  dishName: string;
  quantity: number;
  deliveryTime: string;
  deliveryZone: DeliveryZone;
  deliveryDetail: string;
  status: OrderStatus;
}

export interface StaffDish {
  id: number;
  name: string;
  description: string | null;
  price: number;
  available: boolean;
  thumbnailUrl: string | null;
  imageUrl: string | null;
}

export interface StaffMember {
  id: number;
  username: string;
  fullName: string;
  role: StaffRole;
  active: boolean;
}
