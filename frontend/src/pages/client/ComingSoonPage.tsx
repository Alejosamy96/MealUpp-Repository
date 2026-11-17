// Pantallas informativas para las opciones del perfil que el backend todavía no soporta.
import { Link, useParams } from "react-router-dom";
import { Icon } from "../../components/Icon";
import { Card, ComingSoonBadge, PageHeader } from "../../components/ui";
import { NotFoundPage } from "../NotFoundPage";
import { PROFILE_OPTIONS } from "./ProfilePage";
import styles from "./ComingSoonPage.module.css";

const TEXT: Record<string, string> = {
  "metodos-de-pago": "Pronto podrás guardar tus métodos de pago. Por ahora el pago se hace en la pasarela al confirmar cada reserva; MealUp no guarda datos de tarjetas.",
  notificaciones: "Pronto podrás elegir cómo te avisamos cuando tu almuerzo esté listo o en camino.",
  "preferencias-de-comida": "Pronto podrás contarnos qué te gusta y qué prefieres evitar para recomendarte platos.",
  "ayuda-y-soporte": "Pronto tendrás aquí un canal de ayuda. Mientras tanto, si tienes un problema con tu pedido, comunícate directamente con el restaurante Ferxxos y ten a mano el número y el código de tu reserva.",
};

export function ComingSoonPage() {
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
        <ComingSoonBadge />
        <p>{TEXT[option.slug]}</p>
      </Card>
    </>
  );
}
