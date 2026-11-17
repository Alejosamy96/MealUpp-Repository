// Campos de formulario con etiqueta, ayuda y mensaje de error asociados (accesibles).
import { useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from "react";
import styles from "./Field.module.css";

interface Base {
  label: string;
  hint?: ReactNode;
  error?: string | null;
}

function describedBy(id: string, hint?: ReactNode, error?: string | null) {
  return [hint ? `${id}-hint` : "", error ? `${id}-error` : ""].filter(Boolean).join(" ") || undefined;
}

function Wrapper({ id, label, hint, error, required, children }: Base & { id: string; required?: boolean; children: ReactNode }) {
  return (
    <div className={styles.field}>
      <label htmlFor={id} className={styles.label}>
        {label}
        {required && <span aria-hidden="true" className={styles.required}> *</span>}
      </label>
      {children}
      {hint && <p id={`${id}-hint`} className={styles.hint}>{hint}</p>}
      {error && <p id={`${id}-error`} className={styles.error} role="alert">{error}</p>}
    </div>
  );
}

export function TextField({ label, hint, error, id: idProp, ...rest }: Base & InputHTMLAttributes<HTMLInputElement>) {
  const auto = useId();
  const id = idProp ?? auto;
  return (
    <Wrapper id={id} label={label} hint={hint} error={error} required={rest.required}>
      <input id={id} className={styles.control} aria-invalid={Boolean(error) || undefined} aria-describedby={describedBy(id, hint, error)} {...rest} />
    </Wrapper>
  );
}

export function SelectField({ label, hint, error, id: idProp, children, ...rest }: Base & SelectHTMLAttributes<HTMLSelectElement>) {
  const auto = useId();
  const id = idProp ?? auto;
  return (
    <Wrapper id={id} label={label} hint={hint} error={error} required={rest.required}>
      <select id={id} className={styles.control} aria-invalid={Boolean(error) || undefined} aria-describedby={describedBy(id, hint, error)} {...rest}>
        {children}
      </select>
    </Wrapper>
  );
}

export function TextAreaField({ label, hint, error, id: idProp, ...rest }: Base & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const auto = useId();
  const id = idProp ?? auto;
  return (
    <Wrapper id={id} label={label} hint={hint} error={error} required={rest.required}>
      <textarea id={id} className={`${styles.control} ${styles.textarea}`} aria-invalid={Boolean(error) || undefined} aria-describedby={describedBy(id, hint, error)} {...rest} />
    </Wrapper>
  );
}

export { styles as fieldStyles };
