// Cocina: comandas por preparar, ordenadas por hora de entrega. Llegan en tiempo real por
// WebSocket (/topic/comandas); al conectar se piden las pendientes por REST para no perder nada.
// Respaldo: si no hay WebSocket, se consulta cada 15 s; y siempre hay recarga cada 30 s y manual.
import { useCallback, useEffect } from "react";
import { staffApi } from "../../api/endpoints";
import type { KitchenTicket, OrderStatus } from "../../api/types";
import { Icon } from "../../components/Icon";
import { Card, Chip, EmptyState, ErrorState, Loading, PageHeader, StatusChip } from "../../components/ui";
import { useAsync } from "../../hooks/useAsync";
import { useStompTopic } from "../../hooks/useStompTopic";
import { formatTime } from "../../lib/format";
import { REFRESH_MS, RefreshBar, StatusActions, staffStyles as s } from "./shared";
import styles from "./KitchenPage.module.css";

interface TicketEvent {
  event: "CREATED" | "UPDATED";
  ticketId: number;
  orderId: number;
  dishName: string;
  quantity: number;
  deliveryTime: string;
  orderStatus: OrderStatus;
  ticketStatus: KitchenTicket["ticketStatus"];
}

const VISIBLE: OrderStatus[] = ["PENDING", "IN_PREPARATION"];
const sortByTime = (list: KitchenTicket[]) => [...list].sort((a, b) => a.deliveryTime.localeCompare(b.deliveryTime));

export function KitchenPage() {
  const tickets = useAsync((signal) => staffApi.kitchenTickets(signal), [], { intervalMs: REFRESH_MS });
  const { setData, reload } = tickets;

  const onEvent = useCallback(
    (e: TicketEvent) => {
      setData((prev) => {
        const rest = (prev ?? []).filter((t) => t.ticketId !== e.ticketId);
        if (!VISIBLE.includes(e.orderStatus)) return rest;
        const { event: _event, ...ticket } = e;
        return sortByTime([...rest, ticket]);
      });
    },
    [setData],
  );
  const socket = useStompTopic<TicketEvent>("/topic/comandas", onEvent, reload);
  // Respaldo cada 15 s cuando el tiempo real no está disponible.
  useEffect(() => {
    if (socket === "live") return;
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible") reload();
    }, 15_000);
    return () => window.clearInterval(timer);
  }, [socket, reload]);

  const list = sortByTime((tickets.data ?? []).filter((t) => VISIBLE.includes(t.orderStatus)));
  const totalLunches = list.reduce((sum, t) => sum + t.quantity, 0);

  return (
    <>
      <PageHeader icon="chef" title="Cocina" subtitle="Qué preparar y para qué hora." />
      <div className={s.toolbar}>
        <p className={styles.summary}>
          <strong>{totalLunches}</strong> almuerzos en {list.length} comandas
        </p>
        <RefreshBar updatedAt={tickets.updatedAt} loading={tickets.loading} onRefresh={reload} socket={socket} />
      </div>

      {tickets.loading && !tickets.data ? (
        <Loading label="Cargando comandas…" />
      ) : tickets.error && !tickets.data ? (
        <ErrorState message={tickets.error} onRetry={reload} />
      ) : list.length === 0 ? (
        <EmptyState icon="chef" title="No hay comandas pendientes">
          <p>Las nuevas comandas aparecerán aquí apenas se confirme el pago.</p>
        </EmptyState>
      ) : (
        <ul className={s.grid}>
          {list.map((t) => (
            <li key={t.ticketId}>
              <Card className={`${s.item} ${t.orderStatus === "PENDING" ? styles.pending : styles.cooking}`}>
                <div className={s.itemHead}>
                  <p className={styles.time}>
                    <Icon name="clock" size={20} /> {formatTime(t.deliveryTime)}
                  </p>
                  <div className={styles.chips}>
                    {t.ticketStatus === "SENT" && t.orderStatus === "PENDING" && <Chip tone="amber">Nueva</Chip>}
                    <StatusChip status={t.orderStatus} />
                  </div>
                </div>
                <p className={styles.dish}>
                  <span className={styles.qty}>{t.quantity}×</span> {t.dishName}
                </p>
                <p className={s.itemMeta}>Pedido #{t.orderId}</p>
                <StatusActions orderId={t.orderId} status={t.orderStatus} onChanged={reload} />
              </Card>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
