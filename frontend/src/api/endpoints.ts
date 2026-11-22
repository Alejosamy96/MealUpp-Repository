// Una función por ruta del backend. Las pantallas no llaman a fetch directamente.
import { request } from "./client";
import type {
  CashierOrder,
  CreatedOrderResponse,
  CreateOrderRequest,
  DailyOrdersResponse,
  DeliveryOrder,
  DeliveryZone,
  KitchenOrder,
  KitchenTicket,
  LoginResponse,
  MenuResponse,
  OrderStatus,
  OrderTrackingResponse,
  PublicConfig,
  StaffDish,
  StaffMember,
  StaffOrder,
  StaffRole,
} from "./types";

const q = (v: string | number) => encodeURIComponent(String(v));

// ---------- Público ----------
export const publicApi = {
  config: (signal?: AbortSignal) => request<PublicConfig>("GET", "/api/config", { signal }),
  menu: (signal?: AbortSignal) => request<MenuResponse>("GET", "/api/menu", { signal }),
  createOrder: (body: CreateOrderRequest) => request<CreatedOrderResponse>("POST", "/api/orders", { body }),
  trackOrder: (id: number, code: string, signal?: AbortSignal) =>
    request<OrderTrackingResponse>("GET", `/api/orders/${q(id)}?code=${q(code)}`, { signal }),
  retryPayment: (id: number, code: string) =>
    request<{ paymentUrl: string }>("POST", `/api/orders/${q(id)}/pay?code=${q(code)}`),
  notifyArrival: (id: number, code: string) =>
    request<OrderTrackingResponse>("POST", `/api/orders/${q(id)}/arrived?code=${q(code)}`),
  sandboxApprove: (reference: string) =>
    request<{ received: boolean }>("POST", `/api/payments/sandbox/${q(reference)}/approve`),
  sandboxDecline: (reference: string) =>
    request<{ received: boolean }>("POST", `/api/payments/sandbox/${q(reference)}/decline`),
};

// ---------- Personal ----------
const auth = true;
const zoneParam = (zone?: DeliveryZone | "") => (zone ? `?zone=${q(zone)}` : "");

export const staffApi = {
  login: (username: string, password: string) =>
    request<LoginResponse>("POST", "/api/auth/login", { body: { username, password } }),
  logout: () => request<void>("POST", "/api/auth/logout", { auth }),

  dailyOrders: (zone?: DeliveryZone | "", signal?: AbortSignal) =>
    request<DailyOrdersResponse>("GET", `/api/staff/orders${zoneParam(zone)}`, { auth, signal }),
  updateStatus: (id: number, status: OrderStatus) =>
    request<StaffOrder>("PATCH", `/api/staff/orders/${q(id)}/status`, { auth, body: { status } }),

  kitchenDone: (signal?: AbortSignal) => request<KitchenOrder[]>("GET", "/api/staff/kitchen/done", { auth, signal }),
  kitchenTickets: (signal?: AbortSignal) =>
    request<KitchenTicket[]>("GET", "/api/staff/kitchen/tickets", { auth, signal }),
  cashierOrders: (signal?: AbortSignal) =>
    request<CashierOrder[]>("GET", "/api/staff/cashier/orders", { auth, signal }),
  deliveredOrders: (zone?: DeliveryZone | "", signal?: AbortSignal) =>
    request<DeliveryOrder[]>("GET", `/api/staff/delivery/delivered${zoneParam(zone)}`, { auth, signal }),
  deliveryOrders: (zone?: DeliveryZone | "", signal?: AbortSignal) =>
    request<DeliveryOrder[]>("GET", `/api/staff/delivery/orders${zoneParam(zone)}`, { auth, signal }),

  dishes: (signal?: AbortSignal) => request<StaffDish[]>("GET", "/api/staff/dishes", { auth, signal }),
  createDish: (body: { name: string; description: string; price: number; available: boolean }) =>
    request<StaffDish>("POST", "/api/staff/dishes", { auth, body }),
  updateDish: (id: number, body: { name: string; description: string; price: number }) =>
    request<StaffDish>("PUT", `/api/staff/dishes/${q(id)}`, { auth, body }),
  setDishAvailable: (id: number, available: boolean) =>
    request<StaffDish>("PATCH", `/api/staff/dishes/${q(id)}/availability`, { auth, body: { available } }),
  deleteDish: (id: number) => request<void>("DELETE", `/api/staff/dishes/${q(id)}`, { auth }),
  uploadDishPhoto: (id: number, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return request<StaffDish>("POST", `/api/staff/dishes/${q(id)}/photo`, { auth, form });
  },
  deleteDishPhoto: (id: number) => request<StaffDish>("DELETE", `/api/staff/dishes/${q(id)}/photo`, { auth }),

  members: (signal?: AbortSignal) => request<StaffMember[]>("GET", "/api/staff/members", { auth, signal }),
  createMember: (body: { username: string; fullName: string; password: string; role: StaffRole }) =>
    request<StaffMember>("POST", "/api/staff/members", { auth, body }),
  changeRole: (id: number, role: StaffRole) =>
    request<StaffMember>("PATCH", `/api/staff/members/${q(id)}/role`, { auth, body: { role } }),
  setMemberActive: (id: number, active: boolean) =>
    request<StaffMember>("PATCH", `/api/staff/members/${q(id)}/active`, { auth, body: { active } }),
};
