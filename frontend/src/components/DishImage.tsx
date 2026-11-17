// Foto del plato. Orden de preferencia:
// 1) la foto que guarda el backend (thumbnailUrl / imageUrl),
// 2) /images/dishes/<nombre-con-guiones>.jpg dentro de public/ (opcional),
// 3) un placeholder con la paleta y un icono de cubiertos.
import { useEffect, useMemo, useState } from "react";
import { mediaUrl } from "../api/client";
import { slugify } from "../lib/format";
import { Icon } from "./Icon";
import styles from "./DishImage.module.css";

interface Props {
  name: string;
  src?: string | null;
  className?: string;
  /** Texto alternativo; vacío si la imagen es decorativa (el nombre ya aparece al lado). */
  alt?: string;
  iconSize?: number;
  /** Oculta el icono del placeholder (por ejemplo, cuando hay texto encima). */
  hideIcon?: boolean;
}

export function DishImage({ name, src, className, alt = "", iconSize = 34, hideIcon = false }: Props) {
  const candidates = useMemo(
    () => [mediaUrl(src), `/images/dishes/${slugify(name)}.jpg`].filter((c): c is string => Boolean(c)),
    [name, src],
  );
  const [index, setIndex] = useState(0);
  useEffect(() => {
    setIndex(0);
  }, [candidates]);

  const current = candidates[index];
  return (
    <div className={[styles.frame, className].filter(Boolean).join(" ")}>
      <div className={styles.placeholder} aria-hidden="true">
        {!hideIcon && <Icon name="utensils" size={iconSize} />}
      </div>
      {current && (
        <img
          key={current}
          src={current}
          alt={alt}
          loading="lazy"
          decoding="async"
          className={styles.img}
          onError={() => setIndex((i) => i + 1)}
        />
      )}
    </div>
  );
}
