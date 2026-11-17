// Página de la pasarela de pago SIMULADA (solo desarrollo y demostraciones).
// El backend, con PAYMENT_PROVIDER=sandbox, manda al cliente a /pago-simulado?reference=SBX-...
// Aquí no se piden datos de tarjeta ni se cobra dinero: solo se aprueba o rechaza el pago de prueba.
import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, errorMessage } from "../../api/client";
import { publicApi } from "../../api/endpoints";
import { Button } from "../../components/Button";
import { Icon } from "../../components/Icon";
import { Card, EmptyState, Notice, PageHeader } from "../../components/ui";
import { formatCOP, formatTime } from "../../lib/format";
import { ordersStore } from "../../services/ordersStore";
import styles from "./SandboxPaymentPage.module.css";

export function SandboxPaymentPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const reference = params.get("reference") ?? "";
  const order = reference ? ordersStore.findByReference(reference) : undefined;
  const [busy, setBusy] = useState<"approve" | "decline" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [declined, setDeclined] = useState(false);

  if (!reference) {
    return (
      <EmptyState icon="card" title="Falta la referencia del pago">
        <Link to="/mis-reservas">Ir a mis reservas</Link>
      </EmptyState>
    );
  }

  const approve = async () => {
    setBusy("approve");
    setError(null);
    try {
      await publicApi.sandboxApprove(reference);
      if (order) {
        await ordersStore.refresh(order.id).catch(() => undefined);
        navigate(`/pedido/${order.id}`, { replace: true, state: { fresh: true } });
      } else {
        navigate("/mis-reservas", { replace: true });
      }
    } catch (err) {
      setError(err instanceof ApiError && err.status === 404 ? "La pasarela de prueba no está activa en el servidor (SANDBOX_ENABLED)." : errorMessage(err));
    } finally {
      setBusy(null);
    }
  };

  const decline = async () => {
    setBusy("decline");
    setError(null);
    try {
      await publicApi.sandboxDecline(reference);
      setDeclined(true);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className={styles.page}>
      <PageHeader icon="card" title="Pago de tu reserva" />
      <Notice tone="warning" title="Pasarela de prueba">
        Este es un pago simulado para pruebas: no se cobra dinero y no se piden datos de tarjeta.
      </Notice>

      {order ? (
        <Card className={styles.card}>
          <p className={styles.muted}>Pedido #{order.id}</p>
          <p className={styles.dish}>
            {order.quantity} × {order.dishName}
          </p>
          <p className={styles.muted}>Entrega a las {formatTime(order.deliveryTime)}</p>
          <p className={styles.total}>{formatCOP(order.total)}</p>
        </Card>
      ) : (
        <Notice tone="info">No encontramos el detalle de este pedido en este dispositivo, pero puedes completar el pago de prueba.</Notice>
      )}

      {declined && (
        <Notice tone="error" title="Pago rechazado">
          Tu reserva sigue pendiente de pago. Puedes intentarlo de nuevo desde el detalle del pedido.
        </Notice>
      )}
      {error && <Notice tone="error">{error}</Notice>}

      {declined && order ? (
        <Button size="lg" block onClick={() => navigate(`/pedido/${order.id}`, { replace: true })}>
          Ver mi reserva
        </Button>
      ) : (
        <div className={styles.actions}>
          <Button size="lg" variant="secondary" block loading={busy === "approve"} disabled={busy !== null} onClick={approve} icon={<Icon name="check" size={20} />}>
            Aprobar pago
          </Button>
          <Button size="lg" variant="danger" block loading={busy === "decline"} disabled={busy !== null} onClick={decline}>
            Rechazar pago
          </Button>
        </div>
      )}
      <p className={styles.reference}>Referencia: {reference}</p>
    </div>
  );
}
