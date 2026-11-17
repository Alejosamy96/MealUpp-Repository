import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Link } from "react-router-dom";
import styles from "./Button.module.css";

type Variant = "primary" | "secondary" | "outline" | "ghost" | "danger";
type Size = "sm" | "md" | "lg";

interface Common {
  variant?: Variant;
  size?: Size;
  block?: boolean;
  icon?: ReactNode;
  iconAfter?: ReactNode;
  children?: ReactNode;
}

function classes(variant: Variant, size: Size, block?: boolean, extra?: string) {
  return [styles.btn, styles[variant], styles[size], block ? styles.block : "", extra ?? ""].filter(Boolean).join(" ");
}

interface ButtonProps extends Common, ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean;
}

export function Button({ variant = "primary", size = "md", block, icon, iconAfter, loading, children, className, disabled, type = "button", ...rest }: ButtonProps) {
  return (
    <button
      type={type}
      className={classes(variant, size, block, className)}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading ? <span className={styles.spinner} aria-hidden="true" /> : icon}
      <span>{children}</span>
      {!loading && iconAfter}
    </button>
  );
}

interface LinkButtonProps extends Common {
  to: string;
  className?: string;
  state?: unknown;
}

export function LinkButton({ to, variant = "primary", size = "md", block, icon, iconAfter, children, className, state }: LinkButtonProps) {
  return (
    <Link to={to} state={state} className={classes(variant, size, block, className)}>
      {icon}
      <span>{children}</span>
      {iconAfter}
    </Link>
  );
}
