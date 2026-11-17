// Reglas del negocio desde GET /api/config (hora límite, ventana de entrega, cantidades, zonas).
// Se piden una sola vez por visita; nunca se escriben a mano en el frontend.
import { useAsync } from "./useAsync";
import { publicApi } from "../api/endpoints";
import type { DeliveryZone, PublicConfig } from "../api/types";
import { ZONE_FALLBACK } from "../config/labels";

let cached: Promise<PublicConfig> | null = null;

function loadConfig(): Promise<PublicConfig> {
  if (!cached) {
    cached = publicApi.config().catch((err) => {
      cached = null; // permite reintentar
      throw err;
    });
  }
  return cached;
}

export function useConfig() {
  return useAsync(() => loadConfig(), []);
}

export function zoneLabel(zone: DeliveryZone, config?: PublicConfig): string {
  return config?.zones.find((z) => z.code === zone)?.label ?? ZONE_FALLBACK[zone] ?? zone;
}
