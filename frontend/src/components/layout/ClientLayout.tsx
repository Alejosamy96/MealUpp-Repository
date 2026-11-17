// Estructura de las pantallas del cliente: encabezado con logo y menú, contenido y barra inferior.
import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { Icon, type IconName } from "../Icon";
import { Logo } from "../Logo";
import styles from "./ClientLayout.module.css";

const DRAWER_LINKS: { to: string; label: string; icon: IconName }[] = [
  { to: "/", label: "Inicio", icon: "home" },
  { to: "/reservar", label: "Hacer una reserva", icon: "calendarClock" },
  { to: "/menu", label: "Menú del día", icon: "utensils" },
  { to: "/mis-reservas", label: "Mis reservas", icon: "calendar" },
  { to: "/perfil", label: "Perfil", icon: "user" },
  { to: "/perfil/ayuda-y-soporte", label: "Ayuda y soporte", icon: "help" },
];

export function ClientLayout() {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const toggleRef = useRef<HTMLButtonElement>(null);
  const firstLinkRef = useRef<HTMLAnchorElement>(null);

  useEffect(() => {
    setOpen(false);
    window.scrollTo(0, 0);
  }, [location.pathname]);

  useEffect(() => {
    if (!open) return;
    firstLinkRef.current?.focus();
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpen(false);
        toggleRef.current?.focus();
      }
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open]);

  return (
    <div className={styles.shell}>
      <a href="#contenido" className="skip-link">Saltar al contenido</a>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link to="/" aria-label="MealUp, ir al inicio" className={styles.logoLink}>
            <Logo height={34} />
          </Link>
          <button
            ref={toggleRef}
            type="button"
            className={styles.menuButton}
            aria-label={open ? "Cerrar menú" : "Abrir menú"}
            aria-expanded={open}
            aria-controls="menu-principal"
            onClick={() => setOpen((o) => !o)}
          >
            <Icon name={open ? "close" : "menu"} size={26} />
          </button>
        </div>
      </header>

      {open && <div className={styles.backdrop} onClick={() => setOpen(false)} aria-hidden="true" />}
      <nav id="menu-principal" className={`${styles.drawer} ${open ? styles.drawerOpen : ""}`} aria-label="Menú principal" hidden={!open}>
        <ul>
          {DRAWER_LINKS.map((link, i) => (
            <li key={link.to}>
              <NavLink ref={i === 0 ? firstLinkRef : undefined} to={link.to} end className={({ isActive }) => (isActive ? styles.drawerActive : undefined)}>
                <Icon name={link.icon} size={20} />
                {link.label}
              </NavLink>
            </li>
          ))}
        </ul>
        <Link to="/staff" className={styles.staffLink}>
          <Icon name="users" size={18} />
          Acceso del personal
        </Link>
      </nav>

      <main id="contenido" className={styles.main} tabIndex={-1}>
        <Outlet />
      </main>

      <BottomNav />
    </div>
  );
}

const TABS: { to: string; label: string; icon: IconName; activeIcon: IconName; match: (p: string) => boolean }[] = [
  { to: "/", label: "Inicio", icon: "home", activeIcon: "homeFilled", match: (p) => p === "/" || p.startsWith("/menu") || p.startsWith("/reservar") },
  { to: "/mis-reservas", label: "Mis reservas", icon: "calendar", activeIcon: "calendar", match: (p) => p.startsWith("/mis-reservas") || p.startsWith("/pedido") || p.startsWith("/pago-simulado") },
  { to: "/perfil", label: "Perfil", icon: "user", activeIcon: "userFilled", match: (p) => p.startsWith("/perfil") },
];

export function BottomNav() {
  const { pathname } = useLocation();
  return (
    <nav className={styles.bottomNav} aria-label="Navegación inferior">
      <ul>
        {TABS.map((tab) => {
          const active = tab.match(pathname);
          return (
            <li key={tab.to}>
              <Link to={tab.to} className={active ? styles.tabActive : styles.tab} aria-current={active ? "page" : undefined}>
                <Icon name={active ? tab.activeIcon : tab.icon} size={24} />
                <span>{tab.label}</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
