import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../components/Button";
import { TextField } from "../../components/Field";
import { Notice, PageHeader } from "../../components/ui";
import { profileStore, useProfile } from "../../services/profileStore";
import styles from "./EditProfilePage.module.css";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function EditProfilePage() {
  const profile = useProfile();
  const navigate = useNavigate();
  const [name, setName] = useState(profile.name);
  const [email, setEmail] = useState(profile.email);
  const [errors, setErrors] = useState<{ name?: string; email?: string }>({});

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    const next: typeof errors = {};
    if (!name.trim()) next.name = "Escribe tu nombre.";
    if (email.trim() && !EMAIL.test(email.trim())) next.email = "Revisa el correo, parece incompleto.";
    setErrors(next);
    if (Object.keys(next).length) return;
    profileStore.save({ name, email });
    navigate("/perfil");
  };

  return (
    <>
      <PageHeader icon="user" title="Editar perfil" />
      <form className={styles.form} onSubmit={onSubmit} noValidate>
        <Notice tone="info">Tus datos se guardan solo en este dispositivo. Usaremos tu nombre para tus reservas.</Notice>
        <TextField label="Nombre" autoComplete="name" required maxLength={80} value={name} onChange={(e) => setName(e.target.value)} error={errors.name} />
        <TextField label="Correo (opcional)" type="email" autoComplete="email" maxLength={120} value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} />
        <div className={styles.actions}>
          <Button variant="ghost" onClick={() => navigate(-1)}>
            Cancelar
          </Button>
          <Button type="submit" variant="secondary">
            Guardar
          </Button>
        </div>
      </form>
    </>
  );
}
