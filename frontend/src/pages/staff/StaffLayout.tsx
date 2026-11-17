// Estructura del panel del personal: protege las rutas, dibuja el menú según `sections` del login
// y cierra sesión con POST /api/auth/logout.
import { useState, type ReactNode } from "react";
import { Link, Navigate, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { staffApi } from "../../api/endpoints";
import type { StaffSection } from "../../api/types";
import { Button } from "../../components/Button";
import { Icon, type IconName } from "../../components/Icon";
import { Logo } from "../../components/Logo";
import { EmptyState } from "../../components/ui";
import { ROLE_LABELS, SECTION_INFO } from "../../config/labels";
import { session, useStaffSession } from "../../services/session";
import styles from "./StaffLayout.module.css";

const SECTION_ICON: Record<StaffSection, IconName> = {
  pedidos: "list",
  cocina: "chef",
  caja: "cash",
  entrega: "bike",
  menu: "utensils",
  personal: "users",
};

export function StaffLayout() {
  const current = useStaffSession();
  const location = useLocation();
  const navigate = useNavigate();
  const [leaving, setLeaving] = useState(false);

  if (!current) {
    return <Navigate to="/staff/login" replace state={{ from: location.pathname }} />;
  }

  const logout = async () => {
    setLeaving(true);
    try {
      await staffApi.logout();
    } catch {
      /* aunque falle (token ya vencido), la sesión se cierra en este navegador */
    }
    session.clear("logout");
    navigate("/staff/login", { replace: true });
  };

  const sections = current.sections.filter((s): s is StaffSection => s in SECTION_INFO);

  return (
    <div className={styles.shell}>
      <a href="#contenido-staff" className="skip-link">Saltar al contenido</a>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link to="/staff" className={styles.brand} aria-label="Panel del personal">
            <Logo height={30} />
            <span className={styles.badge}>Personal</span>
          </Link>
          <div className={styles.user}>
            <span className={styles.userText}>
              <strong>{current.fullName}</strong>
              <span>{ROLE_LABELS[current.role] ?? current.role}</span>
            </span>
            <Button variant="ghost" size="sm" onClick={logout} loading={leaving} icon={<Icon name="logout" size={18} />}>
              Cerrar sesión
            </Button>
          </div>
        </div>
        {sections.length > 1 && (
          <nav aria-label="Secciones del personal" className={styles.nav}>
            <ul>
              {sections.map((s) => (
                <li key={s}>
                  <NavLink to={SECTION_INFO[s].path} className={({ isActive }) => (isActive ? styles.navActive : styles.navLink)}>
                    <Icon name={SECTION_ICON[s]} size={18} />
                    {SECTION_INFO[s].label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        )}
      </header>
      <main id="contenido-staff" className={styles.main} tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  );
}

/** /staff lleva a la primera sección que le corresponde al rol. */
export function StaffHome() {
  const current = useStaffSession();
  const first = current?.sections.find((s) => s in SECTION_INFO);
  if (!first) {
    return <EmptyState icon="alert" title="Tu rol no tiene secciones asignadas" />;
  }
  return <Navigate to={SECTION_INFO[first].path} replace />;
}

/** Protege una sección: si el rol no la tiene, muestra el aviso en vez de la pantalla. */
export function RequireSection({ section, children }: { section: StaffSection; children: ReactNode }) {
  const current = useStaffSession();
  if (!current?.sections.includes(section)) {
    return (
      <EmptyState icon="alert" title="No tienes permiso para entrar a esta sección">
        <Link to="/staff">Ir a mis secciones</Link>
      </EmptyState>
    );
  }
  return <>{children}</>;
}
