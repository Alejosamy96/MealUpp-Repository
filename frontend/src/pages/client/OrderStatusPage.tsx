// Detalle y seguimiento de una reserva (también es la pantalla de éxito tras confirmar o pagar).
// Consulta GET /api/orders/{id}?code=... cada 10 s mientras el pedido no esté terminado.
import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { errorMessage } from "../../api/client";
import { publicApi } from "../../api/endpoints";
import type { OrderStatus } from "../../api/types";
import { Button, LinkButton } from "../../components/Button";
import { DishImage } from "../../components/DishImage";
import { Icon } from "../../components/Icon";
import { Card, EmptyState, Notice, StatusChip } from "../../components/ui";
import { FINAL_STATUSES } from "../../config/labels";
import { useConfig, zoneLabel } from "../../hooks/useConfig";
import { formatCOP, formatDate, formatTime, itemsText, relativeSeconds } from "../../lib/format";
import { ordersStore, useStoredOrders } from "../../services/ordersStore";
import styles from "./OrderStatusPage.module.css";

const STEPS: { status: OrderStatus; label: string }[] = [
  { status: "PENDING", label: "Reservada" },
  { status: "IN_PREPARATION", label: "En preparación" },
  { status: "READY", label: "Lista" },
  { status: "DELIVERED", label: "Entregada" },
];

export function OrderStatusPage() {
  const { id } = useParams();
  const orderId = Number(id);
  const location = useLocation();
  const navigate = useNavigate();
  const fresh = Boolean((location.state as { fresh?: boolean } | null)?.fresh);
  const orders = useStoredOrders();
  const order = orders.find((o) => o.id === orderId);
  const config = useConfig();
  const [refreshError, setRefreshError] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);
  const [payError, setPayError] = useState<string | null>(null);
  const [paying, setPaying] = useState(false);

  const done = !order || order.notFound || FINAL_STATUSES.includes(order.status);

  const refresh = async (signal?: AbortSignal) => {
    setChecking(true);
    try {
      await ordersStore.refresh(orderId, signal);
      setRefreshError(null);
    } catch (err) {
      if ((err as Error).name !== "AbortError") setRefreshError(errorMessage(err));
    } finally {
      setChecking(false);
    }
  };

  useEffect(() => {
    if (!ordersStore.get(orderId)) return;
    const controller = new AbortController();
    void refresh(controller.signal);
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderId]);

  useEffect(() => {
    if (done) return;
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible") void refresh();
    }, 10_000);
    return () => window.clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [done, orderId]);

  if (!order) {
    return (
      <EmptyState icon="calendar" title="No encontramos esta reserva en este dispositivo">
        <p>Las reservas se guardan en el navegador donde las hiciste.</p>
        <LinkButton to="/mis-reservas" variant="secondary" size="sm">
          Ver mis reservas
        </LinkButton>
      </EmptyState>
    );
  }

  const pay = async () => {
    setPaying(true);
    setPayError(null);
    try {
      const { paymentUrl } = await publicApi.retryPayment(order.id, order.accessCode);
      ordersStore.setPaymentUrl(order.id, paymentUrl);
      const target = new URL(paymentUrl, window.location.origin);
      if (target.origin === window.location.origin) navigate(target.pathname + target.search);
      else window.location.assign(paymentUrl);
    } catch (err) {
      setPayError(errorMessage(err));
      void refresh();
    } finally {
      setPaying(false);
    }
  };

  const stepIndex = STEPS.findIndex((s) => s.status === order.status);
  const confirmed = order.status === "PENDING" || order.status === "IN_PREPARATION" || order.status === "READY" || order.status === "DELIVERED";

  return (
    <div className={styles.page}>
      <Link to="/mis-reservas" className={styles.back}>
        <Icon name="chevronLeft" size={18} /> Mis reservas
      </Link>

      {fresh && confirmed && (
        <div className={styles.success} role="status">
          <span className={styles.successIcon}>
            <Icon name="checkCircle" size={34} />
          </span>
          <h1>¡Reserva confirmada!</h1>
          <p>
            Tu número de pedido es <strong className={styles.orderNumber}>#{order.id}</strong>
          </p>
        </div>
      )}
      {!(fresh && confirmed) && <h1 className={styles.title}>Pedido #{order.id}</h1>}

      {order.notFound && (
        <Notice tone="warning" title="No pudimos consultar este pedido">
          El restaurante ya no lo tiene registrado. Si crees que es un error, comunícate con Ferxxos.
        </Notice>
      )}

      {order.status === "PENDING_PAYMENT" && !order.notFound && (
        <Notice tone="warning" title="Falta el pago">
          Tu reserva queda confirmada cuando se apruebe el pago
          {config.data ? `. Si no pagas en ${config.data.paymentExpirationMinutes} minutos, vence.` : "."}
        </Notice>
      )}
      {order.status === "EXPIRED" && <Notice tone="info">Esta reserva venció porque no se completó el pago a tiempo.</Notice>}
      {order.status === "CANCELLED" && <Notice tone="info">Esta reserva fue cancelada por el restaurante.</Notice>}

      <Card className={styles.card}>
        <div className={styles.dishRow}>
          <DishImage name={order.dishName} className={styles.thumb} iconSize={26} />
          <div>
            <h2 className={styles.dishName}>{order.dishName}</h2>
            <p className={styles.muted}>
              {formatDate(order.orderDate)} · {formatTime(order.deliveryTime)}
            </p>
            <StatusChip status={order.status} />
          </div>
        </div>

        {stepIndex >= 0 && (
          <ol className={styles.steps} aria-label="Progreso del pedido">
            {STEPS.map((s, i) => (
              <li key={s.status} className={i <= stepIndex ? styles.stepDone : styles.step} aria-current={i === stepIndex ? "step" : undefined}>
                <span className={styles.dot} aria-hidden="true">{i <= stepIndex && <Icon name="check" size={14} />}</span>
                {s.label}
              </li>
            ))}
          </ol>
        )}

        <dl className={styles.details}>
          {order.items && order.items.length > 1 && (
            <div><dt>Platos</dt><dd>{itemsText(order.items, order.dishName, order.quantity)}</dd></div>
          )}
          <div><dt>Cantidad</dt><dd>{order.quantity}</dd></div>
          <div><dt>Total</dt><dd>{formatCOP(order.total)}</dd></div>
          <div><dt>A nombre de</dt><dd>{order.customerName}</dd></div>
          <div><dt>Zona</dt><dd>{zoneLabel(order.deliveryZone, config.data)}</dd></div>
          {order.deliveryDetail && <div><dt>Detalle</dt><dd>{order.deliveryDetail}</dd></div>}
          <div><dt>Código</dt><dd className={styles.code}>{order.accessCode}</dd></div>
        </dl>
      </Card>

      {order.status === "PENDING_PAYMENT" && !order.notFound && (
        <Button size="lg" block loading={paying} onClick={pay} icon={<Icon name="card" size={20} />}>
          Pagar ahora
        </Button>
      )}
      {payError && <Notice tone="error">{payError}</Notice>}

      <div className={styles.refreshRow}>
        <span className={styles.muted} aria-live="polite">
          {refreshError ? refreshError : `Estado actualizado ${relativeSeconds(order.lastCheckedAt)}`}
        </span>
        <Button variant="ghost" size="sm" onClick={() => void refresh()} loading={checking} icon={<Icon name="refresh" size={16} />}>
          Actualizar
        </Button>
      </div>

      <LinkButton to="/" variant="outline" block>
        Volver al inicio
      </LinkButton>
    </div>
  );
}
