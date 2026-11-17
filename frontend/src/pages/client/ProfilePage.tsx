// Perfil del cliente. No hay cuentas de cliente en el backend: nombre y correo se guardan en este
// navegador y el nombre se usa al hacer una reserva.
import { useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "../../components/Button";
import { Icon, type IconName } from "../../components/Icon";
import { Card, ComingSoonBadge, Notice } from "../../components/ui";
import { profileStore, useProfile } from "../../services/profileStore";
import styles from "./ProfilePage.module.css";

export const PROFILE_OPTIONS: { slug: string; label: string; icon: IconName; soon: boolean }[] = [
  { slug: "metodos-de-pago", label: "Métodos de pago", icon: "card", soon: true },
  { slug: "notificaciones", label: "Notificaciones", icon: "bell", soon: true },
  { slug: "preferencias-de-comida", label: "Preferencias de comida", icon: "utensils", soon: true },
  { slug: "ayuda-y-soporte", label: "Ayuda y soporte", icon: "help", soon: true },
];

export function ProfilePage() {
  const profile = useProfile();
  const [confirming, setConfirming] = useState(false);
  const [closed, setClosed] = useState(false);
  const hasData = Boolean(profile.name || profile.email);

  return (
    <div className={styles.page}>
      <h1 className="visually-hidden">Perfil</h1>
      <section className={styles.identity}>
        <span className={styles.avatar} aria-hidden="true">
          <Icon name="userFilled" size={52} />
        </span>
        <div className={styles.who}>
          <p className={styles.name}>{profile.name || "¡Hola!"}</p>
          <p className={styles.email}>{profile.email || "Agrega tu nombre y correo"}</p>
          <Link to="/perfil/editar" className={styles.edit}>
            <Icon name="edit" size={15} /> {hasData ? "Editar perfil" : "Completar perfil"}
          </Link>
        </div>
      </section>

      {closed && <Notice tone="success">Borramos tu nombre y correo de este dispositivo.</Notice>}

      <Card>
        <ul className={styles.options}>
          <li>
            <Link to="/mis-reservas" className={styles.option}>
              <Icon name="calendar" size={22} />
              <span>Mis reservas</span>
              <Icon name="chevronRight" size={20} />
            </Link>
          </li>
          {PROFILE_OPTIONS.map((opt) => (
            <li key={opt.slug}>
              <Link to={`/perfil/${opt.slug}`} className={styles.option}>
                <Icon name={opt.icon} size={22} />
                <span>
                  {opt.label} {opt.soon && <ComingSoonBadge />}
                </span>
                <Icon name="chevronRight" size={20} />
              </Link>
            </li>
          ))}
          <li>
            {!confirming ? (
              <button type="button" className={styles.option} onClick={() => setConfirming(true)} disabled={!hasData}>
                <Icon name="logout" size={22} />
                <span>Cerrar sesión</span>
                <span />
              </button>
            ) : (
              <div className={styles.confirm} role="group" aria-label="Confirmar cierre de sesión">
                <p>Se borrarán tu nombre y correo de este dispositivo. Tus reservas guardadas se mantienen.</p>
                <div className={styles.confirmActions}>
                  <Button variant="ghost" size="sm" onClick={() => setConfirming(false)}>
                    Cancelar
                  </Button>
                  <Button
                    variant="danger"
                    size="sm"
                    autoFocus
                    onClick={() => {
                      profileStore.clear();
                      setConfirming(false);
                      setClosed(true);
                    }}
                  >
                    Sí, cerrar sesión
                  </Button>
                </div>
              </div>
            )}
          </li>
        </ul>
      </Card>

      <aside className={styles.banner}>
        <Icon name="leaf" size={30} className={styles.leaf} />
        <p>Disfruta de un almuerzo más organizado, rápido y sin filas.</p>
        <Icon name="leaf" size={44} className={styles.leafBig} />
      </aside>

      <Link to="/staff" className={styles.staff}>
        ¿Trabajas en Ferxxos? Acceso del personal
      </Link>
    </div>
  );
}
