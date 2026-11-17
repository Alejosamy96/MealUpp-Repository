// Entrega: pedidos en preparación, con a quién, dónde y a qué hora llevarlos.
import { useState } from "react";
import { staffApi } from "../../api/endpoints";
import type { DeliveryZone } from "../../api/types";
import { Icon } from "../../components/Icon";
import { Card, EmptyState, ErrorState, Loading, PageHeader, StatusChip } from "../../components/ui";
import { useAsync } from "../../hooks/useAsync";
import { useConfig, zoneLabel } from "../../hooks/useConfig";
import { useStompTopic } from "../../hooks/useStompTopic";
import { formatTime } from "../../lib/format";
import { REFRESH_MS, RefreshBar, StatusActions, ZoneFilter, staffStyles as s } from "./shared";

export function DeliveryPage() {
  const [zone, setZone] = useState<DeliveryZone | "">("");
  const config = useConfig();
  const orders = useAsync((signal) => staffApi.deliveryOrders(zone, signal), [zone], { intervalMs: REFRESH_MS });
  const socket = useStompTopic("/topic/pedidos", () => orders.reload());
  const list = [...(orders.data ?? [])].sort((a, b) => a.deliveryTime.localeCompare(b.deliveryTime));

  return (
    <>
      <PageHeader icon="bike" title="Entrega" subtitle="Pedidos listos para llevar, en orden de hora." />
      <div className={s.toolbar}>
        <ZoneFilter value={zone} onChange={setZone} />
        <RefreshBar updatedAt={orders.updatedAt} loading={orders.loading} onRefresh={orders.reload} socket={socket} />
      </div>
      {orders.loading && !orders.data ? (
        <Loading label="Cargando entregas…" />
      ) : orders.error && !orders.data ? (
        <ErrorState message={orders.error} onRetry={orders.reload} />
      ) : list.length === 0 ? (
        <EmptyState icon="bike" title="No hay pedidos por entregar" />
      ) : (
        <ul className={s.grid}>
          {list.map((o) => (
            <li key={o.id}>
              <Card className={s.item}>
                <div className={s.itemHead}>
                  <p className={s.big}>
                    <Icon name="clock" size={18} /> {formatTime(o.deliveryTime)}
                  </p>
                  <StatusChip status={o.status} />
                </div>
                <p className={s.itemTitle}>{o.customerName}</p>
                <p className={s.itemMeta}>
                  <Icon name="pin" size={16} /> {zoneLabel(o.deliveryZone, config.data)} · {o.deliveryDetail}
                </p>
                <p className={s.itemMeta}>
                  #{o.id} · {o.quantity} × {o.dishName}
                </p>
                <StatusActions orderId={o.id} status={o.status} onChanged={orders.reload} />
              </Card>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
