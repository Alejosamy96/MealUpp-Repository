// Piezas pequeñas reutilizables: tarjeta, chip, estados de carga/error/vacío, avisos y encabezado de página.
import type { HTMLAttributes, ReactNode } from "react";
import type { OrderStatus } from "../api/types";
import { STATUS_INFO, type ChipTone } from "../config/labels";
import { Button } from "./Button";
import { Icon, type IconName } from "./Icon";
import styles from "./ui.module.css";

export function Card({ className, children, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={[styles.card, className].filter(Boolean).join(" ")} {...rest}>
      {children}
    </div>
  );
}

export function Chip({ tone = "gray", children }: { tone?: ChipTone; children: ReactNode }) {
  return <span className={`${styles.chip} ${styles[`chip_${tone}`]}`}>{children}</span>;
}

export function StatusChip({ status }: { status: OrderStatus }) {
  const info = STATUS_INFO[status] ?? { label: status, tone: "gray" as const };
  return <Chip tone={info.tone}>{info.label}</Chip>;
}

export function Loading({ label = "Cargando…" }: { label?: string }) {
  return (
    <div className={styles.state} role="status" aria-live="polite">
      <span className={styles.loader} aria-hidden="true" />
      <p>{label}</p>
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className={`${styles.state} ${styles.error}`} role="alert">
      <Icon name="alert" size={30} />
      <p>{message}</p>
      {onRetry && (
        <Button variant="outline" size="sm" icon={<Icon name="refresh" size={18} />} onClick={onRetry}>
          Reintentar
        </Button>
      )}
    </div>
  );
}

export function EmptyState({ icon = "plate", title, children }: { icon?: IconName; title: string; children?: ReactNode }) {
  return (
    <div className={styles.state}>
      <span className={styles.emptyIcon}>
        <Icon name={icon} size={30} />
      </span>
      <p className={styles.emptyTitle}>{title}</p>
      {children}
    </div>
  );
}

type NoticeTone = "info" | "warning" | "error" | "success";
const NOTICE_ICON: Record<NoticeTone, IconName> = { info: "info", warning: "alert", error: "alert", success: "checkCircle" };

export function Notice({ tone = "info", children, title }: { tone?: NoticeTone; title?: string; children: ReactNode }) {
  return (
    <div className={`${styles.notice} ${styles[`notice_${tone}`]}`} role={tone === "error" ? "alert" : "status"}>
      <Icon name={NOTICE_ICON[tone]} size={20} />
      <div>
        {title && <p className={styles.noticeTitle}>{title}</p>}
        <div>{children}</div>
      </div>
    </div>
  );
}

export function PageHeader({ icon, title, subtitle, actions }: { icon?: IconName; title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <header className={styles.pageHeader}>
      <div className={styles.pageTitleRow}>
        <h1 className={styles.pageTitle}>
          {icon && <Icon name={icon} size={28} />}
          {title}
        </h1>
        {actions}
      </div>
      {subtitle && <p className={styles.pageSubtitle}>{subtitle}</p>}
    </header>
  );
}

export function ComingSoonBadge() {
  return <span className={styles.soon}>Próximamente</span>;
}
