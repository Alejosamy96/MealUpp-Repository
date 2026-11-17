// Perfil del cliente guardado solo en este navegador (el backend no tiene cuentas de cliente).
// El nombre se reutiliza como customerName al hacer una reserva.
import { useSyncExternalStore } from "react";

export interface Profile {
  name: string;
  email: string;
}

const KEY = "mealup.profile.v1";
const EMPTY: Profile = { name: "", email: "" };
const listeners = new Set<() => void>();
let cache: Profile = load();

function load(): Profile {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? { ...EMPTY, ...JSON.parse(raw) } : EMPTY;
  } catch {
    return EMPTY;
  }
}

export const profileStore = {
  get: (): Profile => cache,
  save(next: Profile) {
    cache = { name: next.name.trim(), email: next.email.trim() };
    try {
      localStorage.setItem(KEY, JSON.stringify(cache));
    } catch {
      /* solo en memoria */
    }
    listeners.forEach((l) => l());
  },
  clear() {
    cache = EMPTY;
    try {
      localStorage.removeItem(KEY);
    } catch {
      /* nada que limpiar */
    }
    listeners.forEach((l) => l());
  },
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};

export function useProfile(): Profile {
  return useSyncExternalStore(profileStore.subscribe, profileStore.get, profileStore.get);
}
