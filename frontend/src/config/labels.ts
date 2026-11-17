// Textos en español y reglas de presentación para los valores que llegan del backend.
import type { DeliveryZone, OrderStatus, StaffRole, StaffSection } from "../api/types";

export type ChipTone = "green" | "blue" | "tan" | "amber" | "red" | "gray";

export const STATUS_INFO: Record<OrderStatus, { label: string; tone: ChipTone }> = {
  PENDING_PAYMENT: { label: "Pendiente de pago", tone: "amber" },
  PENDING: { label: "Reservada", tone: "green" },
  IN_PREPARATION: { label: "En preparación", tone: "blue" },
  DELIVERED: { label: "Entregada", tone: "tan" },
  CANCELLED: { label: "Cancelada", tone: "red" },
  EXPIRED: { label: "Vencida", tone: "gray" },
};

export const FINAL_STATUSES: OrderStatus[] = ["DELIVERED", "CANCELLED", "EXPIRED"];

/** Respaldo si /api/config no trae la zona (el texto oficial viene de config.zones). */
export const ZONE_FALLBACK: Record<DeliveryZone, string> = {
  SHOPPING_CENTER: "Centro comercial",
  FURNITURE_STREET: "Calle de los Muebles",
};

export const ROLE_LABELS: Record<StaffRole, string> = {
  ADMIN: "Administradora",
  KITCHEN: "Cocina",
  CASHIER: "Caja",
  DELIVERY: "Entrega",
};

export const SECTION_INFO: Record<StaffSection, { label: string; path: string }> = {
  pedidos: { label: "Pedidos del día", path: "/staff/pedidos" },
  cocina: { label: "Cocina", path: "/staff/cocina" },
  caja: { label: "Caja", path: "/staff/caja" },
  entrega: { label: "Entrega", path: "/staff/entrega" },
  menu: { label: "Menú", path: "/staff/menu" },
  personal: { label: "Personal", path: "/staff/personal" },
};

/** Texto del botón para pasar un pedido al estado indicado. */
export const ACTION_LABELS: Partial<Record<OrderStatus, string>> = {
  IN_PREPARATION: "Empezar preparación",
  DELIVERED: "Marcar entregado",
  CANCELLED: "Cancelar pedido",
};

/**
 * Cambios que cada rol puede pedir desde la pantalla. Refleja OrderStatus.canTransitionTo y
 * StaffRole.canMove del backend, sin los cambios que hace el sistema solo (pago aprobado → PENDING,
 * vencimiento → EXPIRED). El backend vuelve a validar todo.
 */
export function allowedActions(role: StaffRole, status: OrderStatus): OrderStatus[] {
  const valid: Partial<Record<OrderStatus, OrderStatus[]>> = {
    PENDING_PAYMENT: ["CANCELLED"],
    PENDING: ["IN_PREPARATION", "CANCELLED"],
    IN_PREPARATION: ["DELIVERED"],
  };
  const byRole = (to: OrderStatus): boolean => {
    switch (role) {
      case "ADMIN":
        return true;
      case "KITCHEN":
        return status === "PENDING" && to === "IN_PREPARATION";
      case "CASHIER":
        return (status === "PENDING" || status === "PENDING_PAYMENT") && to === "CANCELLED";
      case "DELIVERY":
        return status === "IN_PREPARATION" && to === "DELIVERED";
    }
  };
  return (valid[status] ?? []).filter(byRole);
}
