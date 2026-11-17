import { useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { errorMessage } from "../../api/client";
import { staffApi } from "../../api/endpoints";
import { Button } from "../../components/Button";
import { TextField } from "../../components/Field";
import { Logo } from "../../components/Logo";
import { Card, Notice } from "../../components/ui";
import { SECTION_INFO } from "../../config/labels";
import { session, useStaffSession } from "../../services/session";
import styles from "./StaffLoginPage.module.css";

export function StaffLoginPage() {
  const current = useStaffSession();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;
  const [endReason] = useState(() => session.takeEndReason());
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ username?: string; password?: string }>({});
  const [loading, setLoading] = useState(false);

  if (current) return <Navigate to="/staff" replace />;

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const errs: typeof fieldErrors = {};
    if (!username.trim()) errs.username = "Escribe tu usuario.";
    if (!password) errs.password = "Escribe tu contraseña.";
    setFieldErrors(errs);
    if (Object.keys(errs).length) return;
    setLoading(true);
    try {
      const login = await staffApi.login(username.trim(), password);
      setPassword("");
      session.start(login, username.trim());
      // Solo se vuelve a la pantalla anterior si el rol de esta cuenta tiene esa sección.
      const allowed = login.sections.some((sec) => SECTION_INFO[sec]?.path === from);
      navigate(allowed && from ? from : "/staff", { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className={styles.page}>
      <Card className={styles.card}>
        <div className={styles.head}>
          <Logo height={38} />
          <h1>Acceso del personal</h1>
          <p>Ferxxos · Panel de pedidos</p>
        </div>
        {endReason === "expired" && <Notice tone="warning">Tu sesión venció. Vuelve a iniciar sesión.</Notice>}
        {endReason === "logout" && <Notice tone="success">Cerraste sesión correctamente.</Notice>}
        <form className={styles.form} onSubmit={onSubmit} noValidate>
          <TextField label="Usuario" autoComplete="username" autoCapitalize="none" spellCheck={false} required value={username} onChange={(e) => setUsername(e.target.value)} error={fieldErrors.username} />
          <TextField label="Contraseña" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} error={fieldErrors.password} />
          {error && <Notice tone="error">{error}</Notice>}
          <Button type="submit" variant="secondary" size="lg" block loading={loading}>
            Iniciar sesión
          </Button>
        </form>
        <Link to="/" className={styles.back}>
          Volver a MealUp
        </Link>
      </Card>
    </main>
  );
}
