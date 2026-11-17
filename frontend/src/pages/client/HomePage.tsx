import { Link } from "react-router-dom";
import { publicApi } from "../../api/endpoints";
import { useAsync } from "../../hooks/useAsync";
import { DishCard } from "../../components/DishCard";
import { DishImage } from "../../components/DishImage";
import { LinkButton } from "../../components/Button";
import { Icon, type IconName } from "../../components/Icon";
import { EmptyState, ErrorState, Loading } from "../../components/ui";
import styles from "./HomePage.module.css";

const FEATURES: { icon: IconName; text: string }[] = [
  { icon: "calendarClock", text: "Reserva con anticipación" },
  { icon: "card", text: "Paga de forma segura" },
  { icon: "bell", text: "Recibe tu aviso de llegada" },
];

export function HomePage() {
  const menu = useAsync((signal) => publicApi.menu(signal), []);
  const heroDish = menu.data?.dishes.find((d) => d.imageUrl) ?? menu.data?.dishes[0];

  return (
    <div className={styles.page}>
      <section className={styles.hero} aria-labelledby="hero-title">
        <DishImage
          name={heroDish?.name ?? "almuerzo"}
          src={heroDish?.imageUrl}
          className={styles.heroImage}
          hideIcon
        />
        {!heroDish?.imageUrl && (
          <span className={styles.heroPlate} aria-hidden="true">
            <Icon name="utensils" size={56} />
          </span>
        )}
        <div className={styles.heroShade} aria-hidden="true" />
        <div className={styles.heroText}>
          <h1 id="hero-title">
            Tu almuerzo,
            <br />a tu ritmo
          </h1>
          <p>Reserva, paga y disfruta sin filas ni complicaciones.</p>
          <LinkButton to="/reservar" iconAfter={<Icon name="arrowRight" size={18} />}>
            Haz tu reserva
          </LinkButton>
        </div>
      </section>

      <section aria-labelledby="menu-title" className={styles.section}>
        <div className={styles.sectionHead}>
          <h2 id="menu-title">Menú del día</h2>
          {menu.data && menu.data.dishes.length > 0 && (
            <Link to="/menu" className={styles.seeAll}>
              Ver todo
            </Link>
          )}
        </div>
        {menu.loading && !menu.data ? (
          <Loading label="Cargando el menú…" />
        ) : menu.error ? (
          <ErrorState message={menu.error} onRetry={menu.reload} />
        ) : menu.data && menu.data.dishes.length === 0 ? (
          <EmptyState icon="utensils" title="Hoy no hay platos disponibles">
            <p>{menu.data.message ?? "Vuelve más tarde para ver el menú del día."}</p>
          </EmptyState>
        ) : (
          <ul className={styles.carousel} aria-label="Platos del día (desliza para ver más)">
            {menu.data?.dishes.map((dish) => (
              <li key={dish.id}>
                <DishCard dish={dish} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className={styles.features} aria-label="Cómo funciona MealUp">
        {FEATURES.map((f) => (
          <div key={f.text} className={styles.feature}>
            <span className={styles.featureIcon}>
              <Icon name={f.icon} size={24} />
            </span>
            <p>{f.text}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
