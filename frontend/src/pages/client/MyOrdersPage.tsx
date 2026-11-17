// Mis reservas: las reservas hechas desde este navegador, con su estado real consultado al backend.
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { errorMessage } from "../../api/client";
import type { OrderStatus } from "../../api/types";
import { Button, LinkButton } from "../../components/Button";
import { DishImage } from "../../components/DishImage";
import { Icon } from "../../components/Icon";
import { Tabs } from "../../components/Tabs";
import { ComingSoonBadge, EmptyState, Notice, PageHeader, StatusChip } from "../../components/ui";
import { FINAL_STATUSES } from "../../config/labels";
import { formatDate, formatTime, nowInZone } from "../../lib/format";
import { ordersStore, useStoredOrders, type StoredOrder } from "../../services/ordersStore";
import styles from "./MyOrdersPage.module.css";

type TabKey = "proximas" | "historicas" | "canceladas";

const CANCELLED: OrderStatus[] = ["CANCELLED", "EXPIRED"];

function tabOf(order: StoredOrder, today: string): TabKey {
  if (CANCELLED.includes(order.status)) return "canceladas";
  if (order.status === "DELIVERED" || order.orderDate < today || order.notFound) return "historicas";
  return "proximas";
}

const EMPTY_TEXT: Record<TabKey, string> = {
  proximas: "No tienes reservas próximas",
  historicas: "Aún no tienes reservas anteriores",
  canceladas: "No tienes reservas canceladas",
};

export function MyOrdersPage() {
  const orders = useStoredOrders();
  const [tab, setTab] = useState<TabKey>("proximas");
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const today = nowInZone().date;

  const refreshActive = async () => {
    const active = ordersStore.list().filter((o) => !o.notFound && !FINAL_STATUSES.includes(o.status) && o.orderDate >= nowInZone().date);
    if (active.length === 0) return;
    setRefreshing(true);
    const results = await Promise.allSettled(active.map((o) => ordersStore.refresh(o.id)));
    const failed = results.find((r) => r.status === "rejected") as PromiseRejectedResult | undefined;
    setError(failed ? errorMessage(failed.reason) : null);
    setRefreshing(false);
  };

  useEffect(() => {
    void refreshActive();
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible") void refreshActive();
    }, 20_000);
    return () => window.clearInterval(timer);
  }, []);

  const grouped: Record<TabKey, StoredOrder[]> = { proximas: [], historicas: [], canceladas: [] };
  for (const o of orders) grouped[tabOf(o, today)].push(o);
  grouped.proximas.sort((a, b) => (a.orderDate + a.deliveryTime).localeCompare(b.orderDate + b.deliveryTime));
  const visible = grouped[tab];

  return (
    <>
      <PageHeader
        icon="calendar"
        title="Mis reservas"
        subtitle="Aquí puedes ver el estado de tus reservas y hacer seguimiento en tiempo real."
        actions={
          <Button variant="ghost" size="sm" onClick={() => void refreshActive()} loading={refreshing} icon={<Icon name="refresh" size={16} />} aria-label="Actualizar estados">
            Actualizar
          </Button>
        }
      />

      <Tabs
        idPrefix="reservas"
        label="Filtrar reservas"
        value={tab}
        onChange={setTab}
        tabs={[
          { key: "proximas", label: "Próximas", count: grouped.proximas.length },
          { key: "historicas", label: "Históricas" },
          { key: "canceladas", label: "Canceladas" },
        ]}
      />

      {error && (
        <div className={styles.gap}>
          <Notice tone="error">No pudimos actualizar algunos estados. {error}</Notice>
        </div>
      )}

      <div id="reservas-panel" role="tabpanel" aria-labelledby={`reservas-tab-${tab}`} className={styles.panel}>
        {visible.length === 0 ? (
          <EmptyState icon="calendar" title={EMPTY_TEXT[tab]}>
            {tab === "proximas" && (
              <LinkButton to="/reservar" size="sm">
                Haz tu reserva
              </LinkButton>
            )}
          </EmptyState>
        ) : (
          <ul className={styles.list}>
            {visible.map((o) => (
              <li key={o.id}>
                <Link to={`/pedido/${o.id}`} className={styles.card}>
                  <DishImage name={o.dishName} className={styles.thumb} iconSize={26} />
                  <div className={styles.info}>
                    <p className={styles.dish}>
                      {o.dishName}
                      {o.quantity > 1 && <span className={styles.qty}> × {o.quantity}</span>}
                    </p>
                    <p className={styles.when}>
                      {formatDate(o.orderDate)} · <span className={styles.nowrap}>{formatTime(o.deliveryTime)}</span>
                    </p>
                    <StatusChip status={o.status} />
                  </div>
                  <Icon name="chevronRight" size={22} className={styles.chevron} />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>

      {tab === "proximas" && (
        <section className={styles.arrival} aria-labelledby="arrival-title">
          <div className={styles.arrivalHead}>
            <Icon name="bell" size={26} className={styles.bell} />
            <div>
              <h2 id="arrival-title">¿Ya llegaste?</h2>
              <p>Avísanos en la cafetería para que empiecen a emplatar tu almuerzo.</p>
            </div>
          </div>
          <Button variant="outline" block disabled aria-describedby="arrival-soon">
            Notificar llegada
          </Button>
          <p id="arrival-soon" className={styles.soonRow}>
            <ComingSoonBadge /> Esta función todavía no está disponible.
          </p>
        </section>
      )}

      <p className={styles.footnote}>Las reservas se guardan en este dispositivo. Si cambias de navegador no las verás aquí.</p>
    </>
  );
}
