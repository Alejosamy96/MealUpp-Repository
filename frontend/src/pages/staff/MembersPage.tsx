// Personal (ADMIN): listar cuentas, crear nuevas, cambiar rol y activar o desactivar.
// La contraseña de una cuenta nueva solo viaja en la petición: no se guarda en el navegador.
import { useState, type FormEvent } from "react";
import { errorMessage } from "../../api/client";
import { staffApi } from "../../api/endpoints";
import type { StaffMember, StaffRole } from "../../api/types";
import { Button } from "../../components/Button";
import { SelectField, TextField } from "../../components/Field";
import { Icon } from "../../components/Icon";
import { Card, Chip, EmptyState, ErrorState, Loading, Notice, PageHeader } from "../../components/ui";
import { ROLE_LABELS } from "../../config/labels";
import { useAsync } from "../../hooks/useAsync";
import { useStaffSession } from "../../services/session";
import { RefreshBar, staffStyles as s } from "./shared";
import styles from "./MembersPage.module.css";

const ROLES: StaffRole[] = ["ADMIN", "KITCHEN", "CASHIER", "DELIVERY"];
const USERNAME = /^[a-z0-9._-]{3,30}$/;

function CreateMemberForm({ onCreated, onCancel }: { onCreated: (m: StaffMember) => void; onCancel: () => void }) {
  const [username, setUsername] = useState("");
  const [fullName, setFullName] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<StaffRole>("KITCHEN");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const found: Record<string, string> = {};
    if (!USERNAME.test(username)) found.username = "De 3 a 30 caracteres: minúsculas, números, punto, guion o guion bajo.";
    if (!fullName.trim()) found.fullName = "Escribe el nombre completo.";
    if (password.length < 8) found.password = "La contraseña debe tener 8 caracteres o más.";
    setErrors(found);
    if (Object.keys(found).length) return;
    setSaving(true);
    setError(null);
    try {
      const created = await staffApi.createMember({ username, fullName: fullName.trim(), password, role });
      setPassword("");
      onCreated(created);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className={styles.form} onSubmit={submit} noValidate>
      <TextField label="Usuario" required autoComplete="off" autoCapitalize="none" spellCheck={false} value={username} onChange={(e) => setUsername(e.target.value.toLowerCase())} error={errors.username} />
      <TextField label="Nombre completo" required value={fullName} onChange={(e) => setFullName(e.target.value)} error={errors.fullName} />
      <TextField label="Contraseña inicial" type="password" required autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} hint="Mínimo 8 caracteres. Compártela con la persona por un medio seguro." />
      <SelectField label="Rol" value={role} onChange={(e) => setRole(e.target.value as StaffRole)}>
        {ROLES.map((r) => (
          <option key={r} value={r}>
            {ROLE_LABELS[r]}
          </option>
        ))}
      </SelectField>
      {error && <Notice tone="error">{error}</Notice>}
      <div className={styles.formActions}>
        <Button variant="ghost" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" variant="secondary" loading={saving}>
          Crear cuenta
        </Button>
      </div>
    </form>
  );
}

function MemberRow({ member, isMe, onUpdated }: { member: StaffMember; isMe: boolean; onUpdated: (m: StaffMember) => void }) {
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const act = async (key: string, fn: () => Promise<StaffMember>) => {
    setBusy(key);
    setError(null);
    try {
      onUpdated(await fn());
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(null);
    }
  };

  return (
    <Card className={`${s.item} ${member.active ? "" : styles.inactive}`}>
      <div className={s.itemHead}>
        <div>
          <p className={s.itemTitle}>
            {member.fullName} {isMe && <span className={styles.me}>(tú)</span>}
          </p>
          <p className={s.itemMeta}>@{member.username}</p>
        </div>
        <Chip tone={member.active ? "green" : "gray"}>{member.active ? "Activa" : "Desactivada"}</Chip>
      </div>
      <div className={styles.row}>
        <SelectField label="Rol" value={member.role} disabled={busy !== null} onChange={(e) => act("role", () => staffApi.changeRole(member.id, e.target.value as StaffRole))}>
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {ROLE_LABELS[r]}
            </option>
          ))}
        </SelectField>
        <Button size="sm" variant={member.active ? "danger" : "secondary"} loading={busy === "active"} disabled={busy !== null} onClick={() => act("active", () => staffApi.setMemberActive(member.id, !member.active))}>
          {member.active ? "Desactivar" : "Reactivar"}
        </Button>
      </div>
      {error && <Notice tone="error">{error}</Notice>}
    </Card>
  );
}

export function MembersPage() {
  const current = useStaffSession();
  const members = useAsync((signal) => staffApi.members(signal), []);
  const [creating, setCreating] = useState(false);
  const [created, setCreated] = useState<string | null>(null);
  const list = members.data ?? [];

  const update = (m: StaffMember) => members.setData((prev) => prev?.map((x) => (x.id === m.id ? m : x)));

  return (
    <>
      <PageHeader
        icon="users"
        title="Personal"
        subtitle="Cuentas del equipo y su rol. Siempre debe quedar al menos una administradora activa."
        actions={
          !creating && (
            <Button size="sm" icon={<Icon name="plus" size={18} />} onClick={() => { setCreating(true); setCreated(null); }}>
              Nueva cuenta
            </Button>
          )
        }
      />
      <div className={s.toolbar}>
        <span />
        <RefreshBar updatedAt={members.updatedAt} loading={members.loading} onRefresh={members.reload} />
      </div>
      {created && (
        <div className={styles.gap}>
          <Notice tone="success">Se creó la cuenta de {created}.</Notice>
        </div>
      )}
      {creating && (
        <Card className={`${s.item} ${styles.createCard}`}>
          <h2 className={styles.createTitle}>Nueva cuenta</h2>
          <CreateMemberForm
            onCancel={() => setCreating(false)}
            onCreated={(m) => {
              members.setData((prev) => [...(prev ?? []), m]);
              setCreating(false);
              setCreated(m.fullName);
            }}
          />
        </Card>
      )}
      {members.loading && !members.data ? (
        <Loading label="Cargando personal…" />
      ) : members.error && !members.data ? (
        <ErrorState message={members.error} onRetry={members.reload} />
      ) : list.length === 0 ? (
        <EmptyState icon="users" title="No hay cuentas registradas" />
      ) : (
        <ul className={s.grid}>
          {list.map((m) => (
            <li key={m.id}>
              <MemberRow member={m} isMe={m.username === current?.username} onUpdated={update} />
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
