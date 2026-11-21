// Pedidos del cliente guardados en este navegador.
// El cliente no tiene cuenta: el backend entrega un `accessCode` al crear el pedido y esa es la
// única forma de consultarlo después (GET /api/orders/{id}?code=...). Aquí se guarda cada pedido
// con su código y el último estado conocido. Si algún día existe una cuenta de cliente con su
// propio listado, basta con cambiar este módulo.
import { useSyncExternalStore } from "react";
import { publicApi } from "../api/endpoints";
import { ApiError } from "../api/client";
import type { CreatedOrderResponse, DeliveryZone, OrderStatus, OrderTrackingResponse } from "../api/types";

export interface StoredOrder {
  id: number;
  accessCode: string;
  dishName: string;
  quantity: number;
  customerName: string;
  orderDate: string;
  deliveryTime: string;
  deliveryZone: DeliveryZone;
  deliveryDetail: string;
  total: number;
  status: OrderStatus;
  /** El cliente ya avisó su llegada. */
  arrived?: boolean;
  /** Referencia del pago (para la pasarela simulada). */
  paymentReference: string | null;
  paymentUrl: string | null;
  /** El backend ya no reconoce el pedido (por ejemplo, la base de datos se reinició). */
  notFound?: boolean;
  lastCheckedAt: number;
}

const KEY = "mealup.orders.v1";
const listeners = new Set<() => void>();
let cache: StoredOrder[] = load();

function load(): StoredOrder[] {
  try {
    const raw = localStorage.getItem(KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function save(next: StoredOrder[]) {
  cache = next;
  try {
    localStorage.setItem(KEY, JSON.stringify(next));
  } catch {
    /* sin almacenamiento: queda solo en memoria */
  }
  listeners.forEach((l) => l());
}

function referenceFrom(url: string | null): string | null {
  if (!url) return null;
  try {
    return new URL(url, window.location.origin).searchParams.get("reference");
  } catch {
    return null;
  }
}

export const ordersStore = {
  list(): StoredOrder[] {
    return cache;
  },
  get(id: number): StoredOrder | undefined {
    return cache.find((o) => o.id === id);
  },
  findByReference(reference: string): StoredOrder | undefined {
    return cache.find((o) => o.paymentReference === reference);
  },
  add(created: CreatedOrderResponse) {
    const order: StoredOrder = {
      id: created.id,
      accessCode: created.accessCode,
      dishName: created.dishName,
      quantity: created.quantity,
      customerName: created.customerName,
      orderDate: created.orderDate,
      deliveryTime: created.deliveryTime,
      deliveryZone: created.deliveryZone,
      deliveryDetail: created.deliveryDetail,
      total: created.total,
      status: created.status,
      paymentReference: referenceFrom(created.paymentUrl),
      paymentUrl: created.paymentUrl,
      lastCheckedAt: Date.now(),
    };
    save([order, ...cache.filter((o) => o.id !== order.id)]);
  },
  setPaymentUrl(id: number, paymentUrl: string) {
    save(cache.map((o) => (o.id === id ? { ...o, paymentUrl, paymentReference: referenceFrom(paymentUrl) } : o)));
  },
  applyTracking(t: OrderTrackingResponse) {
    save(
      cache.map((o) =>
        o.id === t.id
          ? {
              ...o,
              status: t.status,
              arrived: t.arrived,
              dishName: t.dishName,
              quantity: t.quantity,
              deliveryTime: t.deliveryTime,
              deliveryZone: t.deliveryZone,
              deliveryDetail: t.deliveryDetail,
              total: t.total,
              notFound: false,
              lastCheckedAt: Date.now(),
            }
          : o,
      ),
    );
  },
  markNotFound(id: number) {
    save(cache.map((o) => (o.id === id ? { ...o, notFound: true, lastCheckedAt: Date.now() } : o)));
  },
  remove(id: number) {
    save(cache.filter((o) => o.id !== id));
  },
  /** Consulta el estado real de un pedido y lo guarda. */
  async refresh(id: number, signal?: AbortSignal): Promise<StoredOrder | undefined> {
    const order = ordersStore.get(id);
    if (!order) return undefined;
    try {
      ordersStore.applyTracking(await publicApi.trackOrder(order.id, order.accessCode, signal));
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) ordersStore.markNotFound(order.id);
      else throw err;
    }
    return ordersStore.get(id);
  },
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};

export function useStoredOrders(): StoredOrder[] {
  return useSyncExternalStore(ordersStore.subscribe, ordersStore.list, ordersStore.list);
}
