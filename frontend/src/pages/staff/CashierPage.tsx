// Caja: valor de cada pedido del día y totales.
import { staffApi } from "../../api/endpoints";
import type { OrderStatus } from "../../api/types";
import { Card, EmptyState, ErrorState, Loading, PageHeader, StatusChip } from "../../components/ui";
import { useAsync } from "../../hooks/useAsync";
import { useStompTopic } from "../../hooks/useStompTopic";
import { formatCOP } from "../../lib/format";
import { REFRESH_MS, RefreshBar, StatusActions, staffStyles as s } from "./shared";
import styles from "./CashierPage.module.css";

/** Pedidos confirmados que cuentan para el total (igual que OrderStatus.countsForTotals en el backend). */
const COUNTS: OrderStatus[] = ["PENDING", "IN_PREPARATION", "READY", "DELIVERED"];

export function CashierPage() {
  const orders = useAsync((signal) => staffApi.cashierOrders(signal), [], { intervalMs: REFRESH_MS });
  const socket = useStompTopic("/topic/pedidos", () => orders.reload());
  const list = orders.data ?? [];
  const confirmed = list.filter((o) => COUNTS.includes(o.status));
  const totalConfirmed = confirmed.reduce((sum, o) => sum + Number(o.total), 0);
  const pendingPayment = list.filter((o) => o.status === "PENDING_PAYMENT").reduce((sum, o) => sum + Number(o.total), 0);

  return (
    <>
      <PageHeader icon="cash" title="Caja" subtitle="Valor de cada pedido de hoy." />
      <div className={s.toolbar}>
        <span />
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
              <span className={s.statLabel}>Total confirmado</span>
              <span className={s.statValue}>{formatCOP(totalConfirmed)}</span>
            </Card>
            <Card className={s.stat}>
              <span className={s.statLabel}>Pedidos confirmados</span>
              <span className={s.statValue}>{confirmed.length}</span>
            </Card>
            <Card className={s.stat}>
              <span className={s.statLabel}>Esperando pago</span>
              <span className={s.statValue}>{formatCOP(pendingPayment)}</span>
            </Card>
          </div>
          {orders.error && <ErrorState message={orders.error} onRetry={orders.reload} />}
          {list.length === 0 ? (
            <EmptyState icon="cash" title="Todavía no hay pedidos hoy" />
          ) : (
            <Card className={styles.tableWrap}>
              <table className={styles.table}>
                <caption className="visually-hidden">Pedidos del día con su valor</caption>
                <thead>
                  <tr>
                    <th scope="col">#</th>
                    <th scope="col">Cliente</th>
                    <th scope="col">Plato</th>
                    <th scope="col" className={styles.num}>Cant.</th>
                    <th scope="col" className={styles.num}>Unitario</th>
                    <th scope="col" className={styles.num}>Total</th>
                    <th scope="col">Estado</th>
                    <th scope="col"><span className="visually-hidden">Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((o) => (
                    <tr key={o.id}>
                      <td>{o.id}</td>
                      <td>{o.customerName}</td>
                      <td>{o.dishName}</td>
                      <td className={styles.num}>{o.quantity}</td>
                      <td className={styles.num}>{formatCOP(Number(o.unitPrice))}</td>
                      <td className={`${styles.num} ${styles.strong}`}>{formatCOP(Number(o.total))}</td>
                      <td><StatusChip status={o.status} /></td>
                      <td><StatusActions orderId={o.id} status={o.status} onChanged={orders.reload} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
          )}
        </>
      )}
    </>
  );
}
