import { Link } from "react-router-dom";
import type { Dish } from "../api/types";
import { formatCOP } from "../lib/format";
import { DishImage } from "./DishImage";
import styles from "./DishCard.module.css";

/** Tarjeta de plato del "Menú del día". Lleva a la reserva con el plato ya elegido. */
export function DishCard({ dish, wide = false }: { dish: Dish; wide?: boolean }) {
  return (
    <Link to={`/reservar?plato=${dish.id}`} className={`${styles.card} ${wide ? styles.wide : ""}`} aria-label={`${dish.name}, ${formatCOP(dish.price)}. Reservar este plato`}>
      <DishImage name={dish.name} src={dish.thumbnailUrl} className={styles.image} />
      <div className={styles.body}>
        <h3 className={styles.name}>{dish.name}</h3>
        {dish.description && <p className={styles.desc}>{dish.description}</p>}
        <p className={styles.price}>{formatCOP(dish.price)}</p>
      </div>
    </Link>
  );
}
