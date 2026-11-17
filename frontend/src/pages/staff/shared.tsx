// Piezas comunes del panel del personal: barra de refresco, filtro de zona y botones de estado.
import { useEffect, useState } from "react";
import { errorMessage } from "../../api/client";
import { staffApi } from "../../api/endpoints";
import type { DeliveryZone, OrderStatus } from "../../api/types";
import { Button } from "../../components/Button";
import { Icon } from "../../components/Icon";
import { ACTION_LABELS, allowedActions } from "../../config/labels";
import { useConfig } from "../../hooks/useConfig";
import type { SocketState } from "../../hooks/useStompTopic";
import { relativeSeconds } from "../../lib/format";
import { useStaffSession } from "../../services/session";
import styles from "./shared.module.css";

export const REFRESH_MS = 30_000;

export function RefreshBar({ updatedAt, loading, onRefresh, socket }: { updatedAt: number | null; loading: boolean; onRefresh: () => void; socket?: SocketState }) {
  const [, force] = useState(0);
  useEffect(() => {
    const t = window.setInterval(() => force((n) => n + 1), 5000);
    return () => window.clearInterval(t);
  }, []);
  return (
    <div className={styles.refreshBar}>
      {socket && (
        <span className={`${styles.live} ${socket === "live" ? styles.liveOn : ""}`} title={socket === "live" ? "Recibiendo cambios en tiempo real" : "Sin tiempo real: se actualiza cada 30 s"}>
          <span className={styles.liveDot} aria-hidden="true" />
          {socket === "live" ? "En vivo" : socket === "connecting" ? "Conectando…" : "Cada 30 s"}
        </span>
      )}
      <span className={styles.updated} aria-live="polite">
        {updatedAt ? `Actualizado ${relativeSeconds(updatedAt)}` : ""}
      </span>
      <Button variant="ghost" size="sm" onClick={onRefresh} loading={loading} icon={<Icon name="refresh" size={16} />}>
        Actualizar
      </Button>
    </div>
  );
}

export function ZoneFilter({ value, onChange }: { value: DeliveryZone | ""; onChange: (z: DeliveryZone | "") => void }) {
  const config = useConfig();
  const zones = config.data?.zones ?? [];
  return (
    <div className={styles.zoneFilter} role="group" aria-label="Filtrar por zona">
      {[{ code: "" as const, label: "Todas" }, ...zones].map((z) => (
        <button key={z.code || "all"} type="button" aria-pressed={value === z.code} className={value === z.code ? styles.zoneOn : styles.zone} onClick={() => onChange(z.code)}>
          {z.label}
        </button>
      ))}
    </div>
  );
}

/** Botones de cambio de estado: solo los válidos para el estado actual y el rol de quien entra. */
export function StatusActions({ orderId, status, onChanged }: { orderId: number; status: OrderStatus; onChanged: () => void }) {
  const current = useStaffSession();
  const [busy, setBusy] = useState<OrderStatus | null>(null);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [error, setError] = useState<string | null>(null);
  if (!current) return null;
  const actions = allowedActions(current.role, status);
  if (actions.length === 0) return null;

  const run = async (to: OrderStatus) => {
    if (to === "CANCELLED" && !confirmCancel) {
      setConfirmCancel(true);
      return;
    }
    setBusy(to);
    setError(null);
    try {
      await staffApi.updateStatus(orderId, to);
      setConfirmCancel(false);
      onChanged();
    } catch (err) {
      setError(errorMessage(err));
      onChanged();
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className={styles.actions}>
      <div className={styles.actionRow}>
        {actions.map((to) =>
          to === "CANCELLED" ? (
            <Button key={to} variant="danger" size="sm" loading={busy === to} disabled={busy !== null} onClick={() => run(to)}>
              {confirmCancel ? "¿Confirmas cancelar?" : ACTION_LABELS[to]}
            </Button>
          ) : (
            <Button key={to} variant={to === "DELIVERED" ? "secondary" : "primary"} size="sm" loading={busy === to} disabled={busy !== null} onClick={() => run(to)}>
              {ACTION_LABELS[to]}
            </Button>
          ),
        )}
        {confirmCancel && (
          <Button variant="ghost" size="sm" onClick={() => setConfirmCancel(false)}>
            No
          </Button>
        )}
      </div>
      {error && (
        <p className={styles.actionError} role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
export { styles as staffStyles };
