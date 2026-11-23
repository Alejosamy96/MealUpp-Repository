// Entrega: dos pestañas. «Por entregar» tiene los pedidos que cocina dejó listos, con a quién, dónde y a
// qué hora llevarlos. «Entregados» tiene los que ya se entregaron hoy, con los datos del cliente y del pedido.
import { useState } from "react";
import { staffApi } from "../../api/endpoints";
import type { DeliveryOrder, DeliveryZone } from "../../api/types";
import { Icon } from "../../components/Icon";
import { Tabs } from "../../components/Tabs";
import { Card, Chip, EmptyState, ErrorState, Loading, PageHeader, StatusChip } from "../../components/ui";
import { useAsync } from "../../hooks/useAsync";
import { useConfig, zoneLabel } from "../../hooks/useConfig";
import { useStompTopic } from "../../hooks/useStompTopic";
import { formatTime, itemsText } from "../../lib/format";
import { REFRESH_MS, RefreshBar, StatusActions, ZoneFilter, staffStyles as s } from "./shared";

type TabKey = "por-entregar" | "entregados";

export function DeliveryPage() {
  const [zone, setZone] = useState<DeliveryZone | "">("");
  const [tab, setTab] = useState<TabKey>("por-entregar");
  const config = useConfig();
  const orders = useAsync((signal) => staffApi.deliveryOrders(zone, signal), [zone], { intervalMs: REFRESH_MS });
  const delivered = useAsync((signal) => staffApi.deliveredOrders(zone, signal), [zone], { intervalMs: REFRESH_MS });
  const reloadAll = () => {
    orders.reload();
    delivered.reload();
  };
  const socket = useStompTopic("/topic/pedidos", reloadAll);
  const list = [...(orders.data ?? [])].sort((a, b) => Number(b.customerArrived) - Number(a.customerArrived) || a.deliveryTime.localeCompare(b.deliveryTime));
  const deliveredList = [...(delivered.data ?? [])].sort((a, b) => a.deliveryTime.localeCompare(b.deliveryTime));
  const current = tab === "por-entregar" ? orders : delivered;

  const card = (o: DeliveryOrder, withActions: boolean) => (
    <li key={o.id}>
      <Card className={s.item}>
        <div className={s.itemHead}>
          <p className={s.big}>
            <Icon name="clock" size={18} /> {formatTime(o.deliveryTime)}
          </p>
          <div>
            {withActions && o.customerArrived && <Chip tone="green">Cliente llegó</Chip>} <StatusChip status={o.status} />
          </div>
        </div>
        <p className={s.itemTitle}>{o.customerName}</p>
        <p className={s.itemMeta}>
          <Icon name="pin" size={16} /> {zoneLabel(o.deliveryZone, config.data)}{o.deliveryDetail ? ` · ${o.deliveryDetail}` : ""}
        </p>
        <p className={s.itemMeta}>
          #{o.id} · {itemsText(o.items, o.dishName, o.quantity)}
        </p>
        {withActions && <StatusActions orderId={o.id} status={o.status} onChanged={reloadAll} />}
      </Card>
    </li>
  );

  return (
    <>
      <PageHeader icon="bike" title="Entrega" subtitle="Pedidos que cocina ya dejó listos, en orden de hora." />
      <Tabs
        idPrefix="entrega"
        label="Pedidos de entrega"
        value={tab}
        onChange={setTab}
        tabs={[
          { key: "por-entregar", label: "Por entregar", count: list.length },
          { key: "entregados", label: "Entregados", count: deliveredList.length },
        ]}
      />
      <div className={s.toolbar}>
        <ZoneFilter value={zone} onChange={setZone} />
        <RefreshBar updatedAt={current.updatedAt} loading={orders.loading || delivered.loading} onRefresh={reloadAll} socket={socket} />
      </div>
      <div id="entrega-panel" role="tabpanel" aria-labelledby={`entrega-tab-${tab}`}>
        {current.loading && !current.data ? (
          <Loading label={tab === "por-entregar" ? "Cargando entregas…" : "Cargando pedidos entregados…"} />
        ) : current.error && !current.data ? (
          <ErrorState message={current.error} onRetry={current.reload} />
        ) : tab === "por-entregar" ? (
          list.length === 0 ? (
            <EmptyState icon="bike" title="No hay pedidos por entregar">
              <p>Cuando cocina deje un pedido listo, aparecerá aquí.</p>
            </EmptyState>
          ) : (
            <ul className={s.grid}>{list.map((o) => card(o, true))}</ul>
          )
        ) : deliveredList.length === 0 ? (
          <EmptyState icon="bike" title="Todavía no hay pedidos entregados">
            <p>Cuando marques un pedido como entregado, aparecerá aquí.</p>
          </EmptyState>
        ) : (
          <ul className={s.grid}>{deliveredList.map((o) => card(o, false))}</ul>
        )}
      </div>
    </>
  );
}
