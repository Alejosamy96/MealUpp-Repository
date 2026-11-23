// Flujo de reserva: platos y cantidades, nombre, hora, zona y detalle de entrega, resumen y confirmación.
// Un mismo pedido puede combinar varios platos (por ejemplo 2 almuerzos de uno y 1 de otro).
// Las reglas (hora límite, ventana de entrega, cantidades, zonas) vienen de GET /api/config.
import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, errorMessage } from "../../api/client";
import { publicApi } from "../../api/endpoints";
import type { DeliveryZone } from "../../api/types";
import { Button } from "../../components/Button";
import { DishImage } from "../../components/DishImage";
import { SelectField, TextAreaField, TextField } from "../../components/Field";
import { Icon } from "../../components/Icon";
import { Card, ErrorState, Loading, Notice, PageHeader } from "../../components/ui";
import { useAsync } from "../../hooks/useAsync";
import { useConfig } from "../../hooks/useConfig";
import { deliverySlots, formatCOP, formatTime, nowInZone, toMinutes } from "../../lib/format";
import { ordersStore } from "../../services/ordersStore";
import { profileStore, useProfile } from "../../services/profileStore";
import styles from "./ReservePage.module.css";

type Errors = Partial<Record<"quantity" | "name" | "time" | "zone" | "detail", string>>;

const NAME_MAX = 80;
const DETAIL_MAX = 160;

export function ReservePage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const profile = useProfile();
  const config = useConfig();
  const menu = useAsync((signal) => publicApi.menu(signal), []);

  // Cuántos almuerzos de cada plato (clave: id del plato). Solo se guardan los mayores que 0.
  const [counts, setCounts] = useState<Record<number, number>>(() => {
    const p = Number(params.get("plato"));
    return Number.isFinite(p) && p > 0 ? { [p]: 1 } : {};
  });
  const [name, setName] = useState(profile.name);
  const [time, setTime] = useState("");
  const [zone, setZone] = useState<DeliveryZone | "">("");
  const [detail, setDetail] = useState("");
  const [errors, setErrors] = useState<Errors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const errorRef = useRef<HTMLDivElement>(null);
  const clearError = (key: keyof Errors) => setErrors((e) => (e[key] ? { ...e, [key]: undefined } : e));

  const cfg = config.data;
  const dishes = menu.data?.dishes ?? [];
  const chosen = dishes.filter((d) => !d.soldOut && (counts[d.id] ?? 0) > 0);
  const quantity = chosen.reduce((sum, d) => sum + (counts[d.id] ?? 0), 0);
  const slots = useMemo(() => (cfg ? deliverySlots(cfg.deliveryStart, cfg.deliveryEnd) : []), [cfg]);
  const now = nowInZone(cfg?.timeZone);
  const pastCutoff = cfg ? toMinutes(now.time) >= toMinutes(cfg.cutoffTime) : false;
  const total = chosen.reduce((sum, d) => sum + d.price * (counts[d.id] ?? 0), 0);
  const changeCount = (id: number, delta: number) => {
    setCounts((current) => {
      const next = Math.max(0, (current[id] ?? 0) + delta);
      const { [id]: _removed, ...rest } = current;
      return next > 0 ? { ...rest, [id]: next } : rest;
    });
    clearError("quantity");
  };

  // Si un plato elegido (por ejemplo, el de la URL) no está en el menú de hoy, se quita.
  useEffect(() => {
    const menuDishes = menu.data?.dishes;
    if (!menuDishes) return;
    setCounts((current) => {
      const kept = Object.entries(current).filter(([id]) => menuDishes.some((d) => d.id === Number(id) && !d.soldOut));
      return kept.length === Object.keys(current).length ? current : Object.fromEntries(kept);
    });
  }, [menu.data]);

  if ((config.loading && !cfg) || (menu.loading && !menu.data)) {
    return <Loading label="Preparando tu reserva…" />;
  }
  if (config.error || !cfg) {
    return <ErrorState message={config.error ?? "No pudimos cargar las reglas de pedido."} onRetry={config.reload} />;
  }
  if (menu.error) return <ErrorState message={menu.error} onRetry={menu.reload} />;

  const validate = (): Errors => {
    const e: Errors = {};
    if (chosen.length === 0) e.quantity = "Elige al menos un almuerzo.";
    else if (quantity < cfg.minQuantity || quantity > cfg.maxQuantity)
      e.quantity = `La cantidad debe estar entre ${cfg.minQuantity} y ${cfg.maxQuantity}.`;
    if (!name.trim()) e.name = "Escribe tu nombre para saber a quién entregar.";
    if (!time) e.time = `Elige una hora entre ${formatTime(cfg.deliveryStart)} y ${formatTime(cfg.deliveryEnd)}`;
    if (!zone) e.zone = "Elige dónde te entregamos.";
    if (zone === "FURNITURE_STREET" && !detail.trim()) e.detail = "Cuéntanos el local, oficina o punto exacto de entrega.";
    return e;
  };

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);
    const found = validate();
    setErrors(found);
    if (Object.keys(found).length > 0) {
      const first = document.querySelector<HTMLElement>("[aria-invalid='true'], [data-invalid='true'] input");
      first?.focus();
      return;
    }
    if (chosen.length === 0 || !zone) return;
    setSubmitting(true);
    try {
      const created = await publicApi.createOrder({
        items: chosen.map((d) => ({ dishId: d.id, quantity: counts[d.id] ?? 0 })),
        customerName: name.trim(),
        deliveryTime: time,
        deliveryZone: zone,
        deliveryDetail: detail.trim(),
      });
      ordersStore.add(created);
      if (!profile.name) profileStore.save({ ...profile, name: name.trim() });

      if (created.paymentUrl) {
        const target = new URL(created.paymentUrl, window.location.origin);
        if (target.origin === window.location.origin) navigate(target.pathname + target.search);
        else window.location.assign(created.paymentUrl);
      } else {
        navigate(`/pedido/${created.id}`, { state: { fresh: true } });
      }
    } catch (err) {
      setFormError(errorMessage(err));
      if (err instanceof ApiError && (err.status === 400 || err.status === 404)) menu.reload();
      requestAnimationFrame(() => errorRef.current?.focus());
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader icon="calendarClock" title="Haz tu reserva" subtitle={`Pide antes de las ${formatTime(cfg.cutoffTime)} y recibe tu almuerzo entre ${formatTime(cfg.deliveryStart)} y ${formatTime(cfg.deliveryEnd)}`} />

      {pastCutoff && (
        <div className={styles.gap}>
          <Notice tone="warning" title="Por hoy ya cerramos los pedidos">
            Los pedidos del día se reciben antes de las {formatTime(cfg.cutoffTime)}; puedes ver el menú y volver mañana temprano.
          </Notice>
        </div>
      )}

      <form className={styles.form} onSubmit={onSubmit} noValidate>
        <fieldset className={styles.fieldset} data-invalid={Boolean(errors.quantity) || undefined} aria-describedby={errors.quantity ? "quantity-error" : "quantity-hint"}>
          <legend className={styles.legend}>1. Elige tus almuerzos</legend>
          {dishes.length === 0 ? (
            <Notice tone="info">{menu.data?.message ?? "Hoy no hay platos disponibles."}</Notice>
          ) : (
            <>
              <p id="quantity-hint" className={styles.hint}>
                Pide uno o varios, y combina los platos que quieras: por ejemplo 2 de uno y 1 de otro. Entre {cfg.minQuantity} y {cfg.maxQuantity} almuerzos por pedido.
              </p>
              <div className={styles.dishList}>
                {dishes.map((d) => {
                  const count = counts[d.id] ?? 0;
                  return (
                    <div key={d.id} className={`${styles.dishRow} ${count > 0 ? styles.dishSelected : ""} ${d.soldOut ? styles.dishSoldOut : ""}`}>
                      <DishImage name={d.name} src={d.thumbnailUrl} className={styles.dishThumb} iconSize={22} />
                      <span className={styles.dishText}>
                        <span className={styles.dishName}>{d.name}</span>
                        {d.description && <span className={styles.dishDesc}>{d.description}</span>}
                        <span className={styles.dishPrice}>{formatCOP(d.price)}</span>
                        {d.soldOut ? (
                          <span className={styles.stockNote}>Agotado</span>
                        ) : (
                          d.remaining !== null && <span className={styles.stockNote}>Quedan {d.remaining}</span>
                        )}
                      </span>
                      <div className={`${styles.stepper} ${styles.miniStepper}`} role="group" aria-label={`Cantidad de ${d.name}`}>
                        <button type="button" onClick={() => changeCount(d.id, -1)} disabled={d.soldOut || count <= 0} aria-label={`Quitar uno de ${d.name}`}>
                          <Icon name="minus" size={18} />
                        </button>
                        <output aria-live="polite" aria-label={`${count} de ${d.name}`}>{count}</output>
                        <button type="button" onClick={() => changeCount(d.id, 1)} disabled={d.soldOut || quantity >= cfg.maxQuantity || (d.remaining !== null && count >= d.remaining)} aria-label={`Agregar uno de ${d.name}`}>
                          <Icon name="plus" size={18} />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
              <p className={styles.count} aria-live="polite">
                {quantity === 0 ? "Aún no has elegido almuerzos." : `Llevas ${quantity} ${quantity === 1 ? "almuerzo" : "almuerzos"}.`}
              </p>
            </>
          )}
          {errors.quantity && <p id="quantity-error" className={styles.error} role="alert">{errors.quantity}</p>}
        </fieldset>

        <fieldset className={styles.fieldset}>
          <legend className={styles.legend}>2. Datos de entrega</legend>
          <div className={styles.fields}>
            <TextField
              label="Tu nombre"
              name="customerName"
              autoComplete="name"
              required
              maxLength={NAME_MAX}
              value={name}
              onChange={(e) => { setName(e.target.value); clearError("name"); }}
              error={errors.name}
            />
            <SelectField label="Hora de entrega" name="deliveryTime" required value={time} onChange={(e) => { setTime(e.target.value); clearError("time"); }} error={errors.time}>
              <option value="">Elige una hora</option>
              {slots.map((s) => (
                <option key={s} value={s}>
                  {formatTime(s)}
                </option>
              ))}
            </SelectField>

            <div role="radiogroup" aria-labelledby="zone-label" aria-describedby={errors.zone ? "zone-error" : undefined} className={styles.zoneGroup} data-invalid={Boolean(errors.zone) || undefined}>
              <span id="zone-label" className={styles.subLabel}>
                Zona de entrega<span aria-hidden="true" className={styles.req}> *</span>
              </span>
              <div className={styles.zones}>
                {cfg.zones.map((z) => (
                  <label key={z.code} className={`${styles.zone} ${zone === z.code ? styles.zoneSelected : ""}`}>
                    <input type="radio" name="zone" value={z.code} checked={zone === z.code} onChange={() => { setZone(z.code); clearError("zone"); }} className="visually-hidden" aria-invalid={Boolean(errors.zone) || undefined} />
                    <Icon name="pin" size={18} />
                    {z.label}
                  </label>
                ))}
              </div>
              {errors.zone && <p id="zone-error" className={styles.error} role="alert">{errors.zone}</p>}
            </div>

            <TextAreaField
              label={zone === "SHOPPING_CENTER" ? "Detalle de entrega (opcional)" : "Detalle de entrega"}
              name="deliveryDetail"
              required={zone !== "SHOPPING_CENTER"}
              maxLength={DETAIL_MAX}
              placeholder={zone === "SHOPPING_CENTER" ? "Ej.: restaurante o local dentro del centro comercial" : "Ej.: Local 12, segundo piso"}
              hint={`${detail.length}/${DETAIL_MAX}`}
              value={detail}
              onChange={(e) => { setDetail(e.target.value); clearError("detail"); }}
              error={errors.detail}
            />
          </div>
        </fieldset>

        <Card className={styles.summary} aria-labelledby="summary-title">
          <h2 id="summary-title" className={styles.summaryTitle}>Resumen</h2>
          <dl className={styles.summaryList}>
            {chosen.length === 0 ? (
              <div>
                <dt>Almuerzos</dt>
                <dd>—</dd>
              </div>
            ) : (
              chosen.map((d) => (
                <div key={d.id}>
                  <dt>{counts[d.id]} × {d.name}</dt>
                  <dd>{formatCOP(d.price * (counts[d.id] ?? 0))}</dd>
                </div>
              ))
            )}
            <div>
              <dt>Entrega</dt>
              <dd>{time ? formatTime(time) : "—"}</dd>
            </div>
            <div className={styles.totalRow}>
              <dt>Total</dt>
              <dd>{formatCOP(total)}</dd>
            </div>
          </dl>
          {cfg.paymentsRequired && (
            <p className={styles.hint}>
              Al confirmar te llevamos a pagar. Tienes {cfg.paymentExpirationMinutes} minutos; si no se paga, la reserva vence.
            </p>
          )}
        </Card>

        {formError && (
          <div ref={errorRef} tabIndex={-1} className={styles.formError}>
            <Notice tone="error" title="No pudimos registrar tu reserva">
              {formError}
            </Notice>
          </div>
        )}

        <Button type="submit" size="lg" block loading={submitting} disabled={pastCutoff || dishes.length === 0}>
          {cfg.paymentsRequired ? "Confirmar e ir a pagar" : "Confirmar reserva"}
        </Button>
      </form>
    </>
  );
}
