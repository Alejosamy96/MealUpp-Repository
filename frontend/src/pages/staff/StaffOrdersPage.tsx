// Pedidos del día (ADMIN): total de almuerzos, filtro por zona y cambio de estado.
import { useState } from "react";
import { staffApi } from "../../api/endpoints";
import type { DeliveryZone, OrderStatus } from "../../api/types";
import { Icon } from "../../components/Icon";
import { Card, EmptyState, ErrorState, Loading, PageHeader, StatusChip } from "../../components/ui";
import { useAsync } from "../../hooks/useAsync";
import { useConfig, zoneLabel } from "../../hooks/useConfig";
import { useStompTopic } from "../../hooks/useStompTopic";
import { formatCOP, formatDate, formatTime, itemsText } from "../../lib/format";
import { REFRESH_MS, RefreshBar, StatusActions, ZoneFilter, staffStyles as s } from "./shared";

const ORDER: OrderStatus[] = ["PENDING", "IN_PREPARATION", "READY", "PENDING_PAYMENT", "DELIVERED", "CANCELLED", "EXPIRED"];

export function StaffOrdersPage() {
  const [zone, setZone] = useState<DeliveryZone | "">("");
  const config = useConfig();
  const orders = useAsync((signal) => staffApi.dailyOrders(zone, signal), [zone], { intervalMs: REFRESH_MS });
  const socket = useStompTopic("/topic/pedidos", () => orders.reload());

  const list = orders.data?.orders ?? [];
  const counts = ORDER.reduce<Record<string, number>>((acc, st) => ({ ...acc, [st]: list.filter((o) => o.status === st).length }), {});

  return (
    <>
      <PageHeader icon="list" title="Pedidos del día" subtitle={orders.data ? formatDate(orders.data.date) : undefined} />
      <div className={s.toolbar}>
        <ZoneFilter value={zone} onChange={setZone} />
        <RefreshBar updatedAt={orders.updatedAt} loading={orders.loading} onRefresh={orders.reload} socket={socket} />
      </div>

      {orders.loading && !orders.data ? (
        <Loading label="Cargando pedidos…" />
      ) : orders.error && !orders.data ? (
        <ErrorState message={orders.error} onRetry={orders.reload} />
      ) : (
        <>
          <div className={s.stats}>
            <Card className={s.stat}>
              <span className={s.statLabel}>Almuerzos a preparar</span>
              <span className={s.statValue}>{orders.data?.totalLunches ?? 0}</span>
            </Card>
            <Card className={s.stat}>
              <span className={s.statLabel}>Por preparar</span>
              <span className={s.statValue}>{counts.PENDING}</span>
            </Card>
            <Card className={s.stat}>
              <span className={s.statLabel}>En preparación</span>
              <span className={s.statValue}>{counts.IN_PREPARATION}</span>
            </Card>
            <Card className={s.stat}>
              <span className={s.statLabel}>Listos para entregar</span>
              <span className={s.statValue}>{counts.READY}</span>
            </Card>
            <Card className={s.stat}>
              <span className={s.statLabel}>Esperando pago</span>
              <span className={s.statValue}>{counts.PENDING_PAYMENT}</span>
            </Card>
          </div>
          {orders.error && <ErrorState message={orders.error} onRetry={orders.reload} />}
          {list.length === 0 ? (
            <EmptyState icon="list" title={zone ? "No hay pedidos para esta zona hoy" : "Todavía no hay pedidos hoy"} />
          ) : (
            <ul className={s.grid}>
              {list.map((o) => (
                <li key={o.id}>
                  <Card className={s.item}>
                    <div className={s.itemHead}>
                      <div>
                        <p className={s.itemTitle}>
                          #{o.id} · {itemsText(o.items, o.dishName, o.quantity)}
                        </p>
                        <p className={s.itemMeta}>{o.customerName}</p>
                      </div>
                      <StatusChip status={o.status} />
                    </div>
                    <p className={s.itemMeta}>
                      <span className={s.metaPart}><Icon name="clock" size={16} /> {formatTime(o.deliveryTime)}</span>
                      <span className={s.metaPart}><Icon name="pin" size={16} /> {zoneLabel(o.deliveryZone, config.data)}{o.deliveryDetail ? ` · ${o.deliveryDetail}` : ""}</span>
                    </p>
                    <p className={s.itemMeta}>Total {formatCOP(o.total)}</p>
                    <StatusActions orderId={o.id} status={o.status} onChanged={orders.reload} />
                  </Card>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </>
  );
}
