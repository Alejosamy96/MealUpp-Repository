// Pantallas de las opciones del perfil: métodos de pago, notificaciones y ayuda ya están disponibles;
// las demás todavía muestran "Próximamente".
import { Link, useParams } from "react-router-dom";
import { ExternalLinkButton, LinkButton } from "../../components/Button";
import { Icon } from "../../components/Icon";
import { Card, ComingSoonBadge, PageHeader } from "../../components/ui";
import { NotFoundPage } from "../NotFoundPage";
import { PROFILE_OPTIONS } from "./ProfilePage";
import styles from "./ProfileOptionPage.module.css";

/** Línea de WhatsApp de la empresa (formato internacional para el enlace). */
const WHATSAPP_NUMBER = "573172454703";
const WHATSAPP_LABEL = "317 245 4703";
const WHATSAPP_URL = `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent("Hola, necesito ayuda con mi reserva de MealUp.")}`;

const SOON_TEXT: Record<string, string> = {
  "preferencias-de-comida": "Pronto podrás contarnos qué te gusta y qué prefieres evitar para recomendarte platos.",
};

function PaymentMethods() {
  return (
    <>
      <p>
        Pagas en línea con la <strong>pasarela de pagos de Mercado Pago</strong> cuando confirmas cada reserva. Te llevamos a su página segura y,
        al terminar, vuelves a MealUp con tu reserva confirmada.
      </p>
      <p>MealUp no guarda datos de tarjetas ni de cuentas: todo el pago ocurre dentro de la pasarela.</p>
      <p>Si el pago no se completa, tu reserva vence a los pocos minutos y puedes volver a intentarlo desde Mis reservas.</p>
      <LinkButton to="/mis-reservas" variant="outline">
        Ver mis reservas
      </LinkButton>
    </>
  );
}

function Notifications() {
  return (
    <>
      <p>
        <strong>Avisa cuando llegues.</strong> Si tu reserva es del <strong>centro comercial</strong>, entra a Mis reservas el día de tu pedido y pulsa
        <strong> «Ya llegué»</strong>. La cocina y el equipo de entrega reciben el aviso al instante para tener tu almuerzo listo.
      </p>
      <p>
        <strong>Sigue tu pedido.</strong> El estado de tu reserva (reservada, en preparación, lista, entregada) se actualiza solo en Mis reservas.
      </p>
      <LinkButton to="/mis-reservas" variant="outline">
        Ir a Mis reservas
      </LinkButton>
    </>
  );
}

function HelpAndSupport() {
  return (
    <>
      <p>
        ¿Tienes una duda o un problema con tu pedido? <strong>Contáctanos</strong> por la línea de WhatsApp de la empresa:{" "}
        <strong>{WHATSAPP_LABEL}</strong>.
      </p>
      <p>Para ayudarte más rápido, ten a mano el número y el código de tu reserva.</p>
      <ExternalLinkButton href={WHATSAPP_URL} icon={<Icon name="help" size={18} />}>
        Contáctanos por WhatsApp
      </ExternalLinkButton>
    </>
  );
}

export function ProfileOptionPage() {
  const { opcion = "" } = useParams();
  const option = PROFILE_OPTIONS.find((o) => o.slug === opcion);
  if (!option) return <NotFoundPage />;

  return (
    <>
      <Link to="/perfil" className={styles.back}>
        <Icon name="chevronLeft" size={18} /> Perfil
      </Link>
      <PageHeader icon={option.icon} title={option.label} />
      <Card className={styles.card}>
        {option.soon ? (
          <>
            <ComingSoonBadge />
            <p>{SOON_TEXT[option.slug]}</p>
          </>
        ) : option.slug === "metodos-de-pago" ? (
          <PaymentMethods />
        ) : option.slug === "notificaciones" ? (
          <Notifications />
        ) : (
          <HelpAndSupport />
        )}
      </Card>
    </>
  );
}
