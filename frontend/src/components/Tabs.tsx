// Pestañas tipo píldora (como "Próximas / Históricas / Canceladas"). Accesibles con flechas.
import { useRef, type KeyboardEvent } from "react";
import styles from "./Tabs.module.css";

interface Tab<K extends string> {
  key: K;
  label: string;
  count?: number;
}

interface Props<K extends string> {
  tabs: Tab<K>[];
  value: K;
  onChange: (key: K) => void;
  label: string;
  idPrefix: string;
}

export function Tabs<K extends string>({ tabs, value, onChange, label, idPrefix }: Props<K>) {
  const refs = useRef<(HTMLButtonElement | null)[]>([]);

  const onKeyDown = (e: KeyboardEvent, index: number) => {
    let next = -1;
    if (e.key === "ArrowRight") next = (index + 1) % tabs.length;
    if (e.key === "ArrowLeft") next = (index - 1 + tabs.length) % tabs.length;
    if (e.key === "Home") next = 0;
    if (e.key === "End") next = tabs.length - 1;
    if (next >= 0) {
      e.preventDefault();
      onChange(tabs[next].key);
      refs.current[next]?.focus();
    }
  };

  return (
    <div role="tablist" aria-label={label} className={styles.list}>
      {tabs.map((tab, i) => {
        const selected = tab.key === value;
        return (
          <button
            key={tab.key}
            ref={(el) => {
              refs.current[i] = el;
            }}
            role="tab"
            type="button"
            id={`${idPrefix}-tab-${tab.key}`}
            aria-selected={selected}
            aria-controls={`${idPrefix}-panel`}
            tabIndex={selected ? 0 : -1}
            className={selected ? styles.active : styles.tab}
            onClick={() => onChange(tab.key)}
            onKeyDown={(e) => onKeyDown(e, i)}
          >
            {tab.label}
            {tab.count !== undefined && tab.count > 0 && <span className={styles.count}>{tab.count}</span>}
          </button>
        );
      })}
    </div>
  );
}
