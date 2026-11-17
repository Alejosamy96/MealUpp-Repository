// Sesión del personal. El token se guarda en sessionStorage (no en localStorage, como pide docs/API.md):
// sobrevive a recargar la página, pero se borra al cerrar la pestaña.
import { useSyncExternalStore } from "react";
import type { LoginResponse, StaffRole, StaffSection } from "../api/types";

export interface StaffSession {
  token: string;
  username: string;
  expiresAtMs: number;
  fullName: string;
  role: StaffRole;
  sections: StaffSection[];
}

export type EndReason = "expired" | "logout" | null;

const KEY = "mealup.staff.session";
let current: StaffSession | null = read();
let endReason: EndReason = null;
const listeners = new Set<() => void>();

function read(): StaffSession | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as StaffSession;
    if (!parsed.token || parsed.expiresAtMs <= Date.now()) {
      sessionStorage.removeItem(KEY);
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

function emit() {
  listeners.forEach((l) => l());
}

/** Vencimiento: se toma del propio token (campo exp) y, si no se puede, de expiresAt. */
function expiryOf(login: LoginResponse): number {
  try {
    const payload = JSON.parse(atob(login.token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    if (typeof payload.exp === "number") return payload.exp * 1000;
  } catch {
    /* el token no es un JWT legible: se usa expiresAt */
  }
  const parsed = Date.parse(login.expiresAt);
  return Number.isNaN(parsed) ? Date.now() + 8 * 3600_000 : parsed;
}

export const session = {
  get(): StaffSession | null {
    if (current && current.expiresAtMs <= Date.now()) session.clear("expired");
    return current;
  },
  token(): string | null {
    return session.get()?.token ?? null;
  },
  start(login: LoginResponse, username: string) {
    current = {
      token: login.token,
      username,
      expiresAtMs: expiryOf(login),
      fullName: login.fullName,
      role: login.role,
      sections: login.sections,
    };
    endReason = null;
    try {
      sessionStorage.setItem(KEY, JSON.stringify(current));
    } catch {
      /* almacenamiento no disponible: la sesión vive solo en memoria */
    }
    emit();
  },
  clear(reason: EndReason) {
    if (!current) return;
    current = null;
    endReason = reason;
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      /* nada que limpiar */
    }
    emit();
  },
  /** Motivo del último cierre (para mostrar "tu sesión venció" en el login). Se consume una vez. */
  takeEndReason(): EndReason {
    const r = endReason;
    endReason = null;
    return r;
  },
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};

export function useStaffSession(): StaffSession | null {
  return useSyncExternalStore(session.subscribe, () => session.get(), () => null);
}
