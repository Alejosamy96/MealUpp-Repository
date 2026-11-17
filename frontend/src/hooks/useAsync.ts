import { useCallback, useEffect, useRef, useState } from "react";
import { errorMessage } from "../api/client";

export interface AsyncState<T> {
  data: T | undefined;
  error: string | null;
  loading: boolean;
  /** Hora (ms) de la última carga correcta. */
  updatedAt: number | null;
  reload: () => void;
  setData: (updater: (prev: T | undefined) => T | undefined) => void;
}

interface Options {
  /** Recarga automática cada N ms (sin mostrar el estado de carga de nuevo). */
  intervalMs?: number;
}

/** Carga datos con estados de carga y error, recarga manual y, opcional, automática. */
export function useAsync<T>(
  loader: (signal: AbortSignal) => Promise<T>,
  deps: unknown[],
  options: Options = {},
): AsyncState<T> {
  const [data, setDataState] = useState<T | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [updatedAt, setUpdatedAt] = useState<number | null>(null);
  const [tick, setTick] = useState(0);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  const reload = useCallback(() => setTick((t) => t + 1), []);
  const setData = useCallback((updater: (prev: T | undefined) => T | undefined) => setDataState(updater), []);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    loaderRef
      .current(controller.signal)
      .then((result) => {
        setDataState(result);
        setError(null);
        setUpdatedAt(Date.now());
      })
      .catch((err) => {
        if ((err as Error).name !== "AbortError") setError(errorMessage(err));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  useEffect(() => {
    if (!options.intervalMs) return;
    const id = window.setInterval(() => {
      if (document.visibilityState === "visible") reload();
    }, options.intervalMs);
    return () => window.clearInterval(id);
  }, [options.intervalMs, reload]);

  return { data, error, loading, updatedAt, reload, setData };
}
