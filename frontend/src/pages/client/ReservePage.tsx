// Flujo de reserva: plato, cantidad, nombre, hora, zona y detalle de entrega, resumen y confirmación.
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

type Errors = Partial<Record<"dish" | "quantity" | "name" | "time" | "zone" | "detail", string>>;

const NAME_MAX = 80;
const DETAIL_MAX = 160;

export function ReservePage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const profile = useProfile();
  const config = useConfig();
  const menu = useAsync((signal) => publicApi.menu(signal), []);

  const [dishId, setDishId] = useState<number | null>(() => {
    const p = Number(params.get("plato"));
    return Number.isFinite(p) && p > 0 ? p : null;
  });
  const [quantity, setQuantity] = useState(1);
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
  const dish = dishes.find((d) => d.id === dishId);
  const slots = useMemo(() => (cfg ? deliverySlots(cfg.deliveryStart, cfg.deliveryEnd) : []), [cfg]);
  const now = nowInZone(cfg?.timeZone);
  const pastCutoff = cfg ? toMinutes(now.time) >= toMinutes(cfg.cutoffTime) : false;
  const total = dish ? dish.price * quantity : 0;

  // Si el plato de la URL no está en el menú de hoy, no se deja preseleccionado.
  useEffect(() => {
    if (menu.data && dishId !== null && !menu.data.dishes.some((d) => d.id === dishId)) setDishId(null);
  }, [menu.data, dishId]);

  useEffect(() => {
    if (cfg) setQuantity((q) => Math.min(Math.max(q, cfg.minQuantity), cfg.maxQuantity));
  }, [cfg]);

  if ((config.loading && !cfg) || (menu.loading && !menu.data)) {
    return <Loading label="Preparando tu reserva…" />;
  }
  if (config.error || !cfg) {
    return <ErrorState message={config.error ?? "No pudimos cargar las reglas de pedido."} onRetry={config.reload} />;
  }
  if (menu.error) return <ErrorState message={menu.error} onRetry={menu.reload} />;

  const validate = (): Errors => {
    const e: Errors = {};
    if (!dish) e.dish = "Elige un plato.";
    if (!Number.isInteger(quantity) || quantity < cfg.minQuantity || quantity > cfg.maxQuantity)
      e.quantity = `La cantidad debe estar entre ${cfg.minQuantity} y ${cfg.maxQuantity}.`;
    if (!name.trim()) e.name = "Escribe tu nombre para saber a quién entregar.";
    if (!time) e.time = `Elige una hora entre ${formatTime(cfg.deliveryStart)} y ${formatTime(cfg.deliveryEnd)}`;
    if (!zone) e.zone = "Elige dónde te entregamos.";
    if (!detail.trim()) e.detail = "Cuéntanos el local, oficina o punto exacto de entrega.";
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
    if (!dish || !zone) return;
    setSubmitting(true);
    try {
      const created = await publicApi.createOrder({
        dishId: dish.id,
        quantity,
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
        <fieldset className={styles.fieldset} data-invalid={Boolean(errors.dish) || undefined} aria-describedby={errors.dish ? "dish-error" : undefined}>
          <legend className={styles.legend}>1. Elige tu plato</legend>
          {dishes.length === 0 ? (
            <Notice tone="info">{menu.data?.message ?? "Hoy no hay platos disponibles."}</Notice>
          ) : (
            <div className={styles.dishList}>
              {dishes.map((d) => (
                <label key={d.id} className={`${styles.dishOption} ${dishId === d.id ? styles.dishSelected : ""}`}>
                  <input
                    type="radio"
                    name="dish"
                    value={d.id}
                    checked={dishId === d.id}
                    onChange={() => { setDishId(d.id); clearError("dish"); }}
                    className="visually-hidden"
                    aria-invalid={Boolean(errors.dish) || undefined}
                  />
                  <DishImage name={d.name} src={d.thumbnailUrl} className={styles.dishThumb} iconSize={22} />
                  <span className={styles.dishText}>
                    <span className={styles.dishName}>{d.name}</span>
                    {d.description && <span className={styles.dishDesc}>{d.description}</span>}
                  </span>
                  <span className={styles.dishPrice}>{formatCOP(d.price)}</span>
                  <span className={styles.radioMark} aria-hidden="true">
                    {dishId === d.id && <Icon name="check" size={16} />}
                  </span>
                </label>
              ))}
            </div>
          )}
          {errors.dish && <p id="dish-error" className={styles.error} role="alert">{errors.dish}</p>}
        </fieldset>

        <fieldset className={styles.fieldset}>
          <legend className={styles.legend}>2. ¿Cuántos almuerzos?</legend>
          <div className={styles.stepper} role="group" aria-label="Cantidad de almuerzos">
            <button type="button" onClick={() => setQuantity((q) => Math.max(cfg.minQuantity, q - 1))} disabled={quantity <= cfg.minQuantity} aria-label="Quitar uno">
              <Icon name="minus" size={20} />
            </button>
            <output aria-live="polite" aria-label={`${quantity} almuerzos`}>{quantity}</output>
            <button type="button" onClick={() => setQuantity((q) => Math.min(cfg.maxQuantity, q + 1))} disabled={quantity >= cfg.maxQuantity} aria-label="Agregar uno">
              <Icon name="plus" size={20} />
            </button>
          </div>
          <p className={styles.hint}>
            Entre {cfg.minQuantity} y {cfg.maxQuantity} por pedido.
          </p>
          {errors.quantity && <p className={styles.error} role="alert">{errors.quantity}</p>}
        </fieldset>

        <fieldset className={styles.fieldset}>
          <legend className={styles.legend}>3. Datos de entrega</legend>
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
              label="Detalle de entrega"
              name="deliveryDetail"
              required
              maxLength={DETAIL_MAX}
              placeholder="Ej.: Local 12, segundo piso"
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
            <div>
              <dt>Plato</dt>
              <dd>{dish?.name ?? "—"}</dd>
            </div>
            <div>
              <dt>Cantidad</dt>
              <dd>
                {quantity} × {dish ? formatCOP(dish.price) : "—"}
              </dd>
            </div>
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
