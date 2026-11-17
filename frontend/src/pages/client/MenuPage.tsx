import { publicApi } from "../../api/endpoints";
import { useAsync } from "../../hooks/useAsync";
import { DishCard } from "../../components/DishCard";
import { EmptyState, ErrorState, Loading, PageHeader } from "../../components/ui";
import styles from "./MenuPage.module.css";

export function MenuPage() {
  const menu = useAsync((signal) => publicApi.menu(signal), []);
  return (
    <>
      <PageHeader icon="utensils" title="Menú del día" subtitle="Elige un plato para hacer tu reserva." />
      {menu.loading && !menu.data ? (
        <Loading label="Cargando el menú…" />
      ) : menu.error ? (
        <ErrorState message={menu.error} onRetry={menu.reload} />
      ) : !menu.data?.dishes.length ? (
        <EmptyState icon="utensils" title="Hoy no hay platos disponibles">
          <p>{menu.data?.message ?? "Vuelve más tarde para ver el menú del día."}</p>
        </EmptyState>
      ) : (
        <ul className={styles.grid}>
          {menu.data.dishes.map((dish) => (
            <li key={dish.id}>
              <DishCard dish={dish} wide />
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
