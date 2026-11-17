// Menú (ADMIN): crear, editar, marcar agotado o disponible, eliminar y subir la foto de cada plato.
import { useRef, useState, type FormEvent } from "react";
import { errorMessage } from "../../api/client";
import { staffApi } from "../../api/endpoints";
import type { StaffDish } from "../../api/types";
import { Button } from "../../components/Button";
import { DishImage } from "../../components/DishImage";
import { TextAreaField, TextField } from "../../components/Field";
import { Icon } from "../../components/Icon";
import { Card, Chip, EmptyState, ErrorState, Loading, Notice, PageHeader } from "../../components/ui";
import { useAsync } from "../../hooks/useAsync";
import { formatCOP } from "../../lib/format";
import { RefreshBar, staffStyles as s } from "./shared";
import styles from "./DishesAdminPage.module.css";

const MAX_PHOTO = 5 * 1024 * 1024;

interface DishDraft {
  name: string;
  description: string;
  price: string;
}

function validateDraft(d: DishDraft): Partial<Record<keyof DishDraft, string>> {
  const e: Partial<Record<keyof DishDraft, string>> = {};
  if (!d.name.trim()) e.name = "Escribe el nombre del plato.";
  const price = Number(d.price);
  if (!d.price.trim() || !Number.isFinite(price) || price <= 0) e.price = "Escribe un precio mayor que cero, sin puntos (ej.: 12000).";
  return e;
}

function DishForm({ initial, submitLabel, onSubmit, onCancel }: { initial: DishDraft; submitLabel: string; onSubmit: (d: DishDraft) => Promise<void>; onCancel?: () => void }) {
  const [draft, setDraft] = useState(initial);
  const [errors, setErrors] = useState<Partial<Record<keyof DishDraft, string>>>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const found = validateDraft(draft);
    setErrors(found);
    if (Object.keys(found).length) return;
    setSaving(true);
    setError(null);
    try {
      await onSubmit(draft);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className={styles.form} onSubmit={submit} noValidate>
      <TextField label="Nombre" required maxLength={80} value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} error={errors.name} />
      <TextAreaField label="Descripción" maxLength={160} value={draft.description} onChange={(e) => setDraft({ ...draft, description: e.target.value })} />
      <TextField label="Precio (COP)" required inputMode="numeric" value={draft.price} onChange={(e) => setDraft({ ...draft, price: e.target.value.replace(/[^\d]/g, "") })} error={errors.price} hint={draft.price ? formatCOP(Number(draft.price)) : undefined} />
      {error && <Notice tone="error">{error}</Notice>}
      <div className={styles.formActions}>
        {onCancel && (
          <Button variant="ghost" onClick={onCancel}>
            Cancelar
          </Button>
        )}
        <Button type="submit" variant="secondary" loading={saving}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}

function DishItem({ dish, onUpdated, onDeleted }: { dish: StaffDish; onUpdated: (d: StaffDish) => void; onDeleted: (id: number) => void }) {
  const [editing, setEditing] = useState(false);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const act = async (key: string, fn: () => Promise<void>) => {
    setBusy(key);
    setError(null);
    try {
      await fn();
    } catch (err) {
      setError(errorMessage(err));
      setConfirmDelete(false);
    } finally {
      setBusy(null);
    }
  };

  const onFile = (file: File | undefined) => {
    if (!file) return;
    if (!["image/jpeg", "image/png"].includes(file.type)) {
      setError("La foto debe ser JPG o PNG.");
      return;
    }
    if (file.size > MAX_PHOTO) {
      setError("La imagen supera el tamaño máximo de 5 MB.");
      return;
    }
    void act("photo", async () => onUpdated(await staffApi.uploadDishPhoto(dish.id, file)));
    if (fileRef.current) fileRef.current.value = "";
  };

  return (
    <Card className={`${s.item} ${dish.available ? "" : styles.soldOut}`}>
      <div className={styles.dishHead}>
        <DishImage name={dish.name} src={dish.thumbnailUrl} className={styles.thumb} iconSize={26} />
        <div className={styles.dishInfo}>
          <p className={s.itemTitle}>{dish.name}</p>
          {dish.description && <p className={s.itemMeta}>{dish.description}</p>}
          <p className={styles.price}>{formatCOP(Number(dish.price))}</p>
        </div>
        <Chip tone={dish.available ? "green" : "gray"}>{dish.available ? "Disponible" : "Agotado"}</Chip>
      </div>

      {editing ? (
        <DishForm
          initial={{ name: dish.name, description: dish.description ?? "", price: String(dish.price) }}
          submitLabel="Guardar cambios"
          onCancel={() => setEditing(false)}
          onSubmit={async (d) => {
            onUpdated(await staffApi.updateDish(dish.id, { name: d.name.trim(), description: d.description.trim(), price: Number(d.price) }));
            setEditing(false);
          }}
        />
      ) : (
        <div className={styles.actions}>
          <Button size="sm" variant={dish.available ? "outline" : "secondary"} loading={busy === "avail"} disabled={busy !== null} onClick={() => act("avail", async () => onUpdated(await staffApi.setDishAvailable(dish.id, !dish.available)))}>
            {dish.available ? "Marcar agotado" : "Marcar disponible"}
          </Button>
          <Button size="sm" variant="ghost" icon={<Icon name="edit" size={16} />} disabled={busy !== null} onClick={() => setEditing(true)}>
            Editar
          </Button>
          <input ref={fileRef} type="file" accept="image/jpeg,image/png" className="visually-hidden" id={`photo-${dish.id}`} onChange={(e) => onFile(e.target.files?.[0])} tabIndex={-1} />
          <Button size="sm" variant="ghost" icon={<Icon name="camera" size={16} />} loading={busy === "photo"} disabled={busy !== null} onClick={() => fileRef.current?.click()}>
            {dish.thumbnailUrl ? "Cambiar foto" : "Subir foto"}
          </Button>
          {dish.thumbnailUrl && (
            <Button size="sm" variant="ghost" loading={busy === "nophoto"} disabled={busy !== null} onClick={() => act("nophoto", async () => onUpdated(await staffApi.deleteDishPhoto(dish.id)))}>
              Quitar foto
            </Button>
          )}
          <Button
            size="sm"
            variant="danger"
            icon={<Icon name="trash" size={16} />}
            loading={busy === "delete"}
            disabled={busy !== null}
            onClick={() => {
              if (!confirmDelete) return setConfirmDelete(true);
              void act("delete", async () => {
                await staffApi.deleteDish(dish.id);
                onDeleted(dish.id);
              });
            }}
          >
            {confirmDelete ? "¿Confirmas eliminar?" : "Eliminar"}
          </Button>
          {confirmDelete && (
            <Button size="sm" variant="ghost" onClick={() => setConfirmDelete(false)}>
              No
            </Button>
          )}
        </div>
      )}
      {error && <Notice tone="error">{error}</Notice>}
    </Card>
  );
}

export function DishesAdminPage() {
  const dishes = useAsync((signal) => staffApi.dishes(signal), []);
  const [creating, setCreating] = useState(false);
  const list = dishes.data ?? [];

  const update = (d: StaffDish) => dishes.setData((prev) => prev?.map((x) => (x.id === d.id ? d : x)));
  const remove = (id: number) => dishes.setData((prev) => prev?.filter((x) => x.id !== id));

  return (
    <>
      <PageHeader
        icon="utensils"
        title="Menú"
        subtitle="Los platos disponibles aparecen en el menú del día de los clientes."
        actions={
          !creating && (
            <Button variant="primary" size="sm" icon={<Icon name="plus" size={18} />} onClick={() => setCreating(true)}>
              Nuevo plato
            </Button>
          )
        }
      />
      <div className={s.toolbar}>
        <span />
        <RefreshBar updatedAt={dishes.updatedAt} loading={dishes.loading} onRefresh={dishes.reload} />
      </div>

      {creating && (
        <Card className={`${s.item} ${styles.createCard}`}>
          <h2 className={styles.createTitle}>Nuevo plato</h2>
          <DishForm
            initial={{ name: "", description: "", price: "" }}
            submitLabel="Crear plato"
            onCancel={() => setCreating(false)}
            onSubmit={async (d) => {
              const created = await staffApi.createDish({ name: d.name.trim(), description: d.description.trim(), price: Number(d.price), available: true });
              dishes.setData((prev) => [...(prev ?? []), created]);
              setCreating(false);
            }}
          />
        </Card>
      )}

      {dishes.loading && !dishes.data ? (
        <Loading label="Cargando platos…" />
      ) : dishes.error && !dishes.data ? (
        <ErrorState message={dishes.error} onRetry={dishes.reload} />
      ) : list.length === 0 ? (
        <EmptyState icon="utensils" title="Todavía no hay platos registrados" />
      ) : (
        <ul className={s.grid}>
          {list.map((d) => (
            <li key={d.id}>
              <DishItem dish={d} onUpdated={update} onDeleted={remove} />
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
