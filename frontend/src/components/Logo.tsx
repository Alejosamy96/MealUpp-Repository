// Logo oficial (public/logo-mealup.png, fondo arena). Para el encabezado se usa
// public/logo-mealup-horizontal.png: las mismas piezas del logo en una fila y con el fondo arena
// vuelto transparente, para que no se vea una caja sobre el crema. Si se consigue una versión
// oficial con fondo transparente, basta con reemplazar ese archivo.
import styles from "./Logo.module.css";

export function Logo({ height = 34 }: { height?: number }) {
  return (
    <img
      src="/logo-mealup-horizontal.png"
      alt="Meal Up"
      className={styles.logo}
      style={{ height }}
      width={Math.round(height * 5.03)}
      height={height}
    />
  );
}
