// Cliente HTTP central: arma la URL, envía el token del personal y traduce los errores
// del backend ({ message }) a ApiError con un texto listo para mostrar.
import { session } from "../services/session";

const BASE = ((import.meta.env.VITE_API_URL as string | undefined) ?? "").replace(/\/$/, "");

export class ApiError extends Error {
  readonly status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "ApiError";
  }
}

export const NETWORK_ERROR =
  "No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.";

const DEFAULT_MESSAGES: Record<number, string> = {
  400: "Revisa los datos e inténtalo de nuevo.",
  401: "Tu sesión venció. Vuelve a iniciar sesión.",
  403: "No tienes permiso para entrar a esta sección.",
  404: "No encontramos lo que buscas.",
};

/** Dirección completa de una ruta del backend. */
export function apiUrl(path: string): string {
  return BASE + path;
}

/** Las fotos llegan como "/media/..." (servidor) o como dirección completa (S3/CDN). */
export function mediaUrl(url: string | null | undefined): string | null {
  if (!url) return null;
  return /^https?:\/\//.test(url) ? url : BASE + url;
}

/** Dirección del WebSocket (STOMP) del backend. */
export function wsUrl(): string {
  const origin = BASE || window.location.origin;
  return origin.replace(/^http/, "ws") + "/ws";
}

interface RequestOptions {
  body?: unknown;
  form?: FormData;
  auth?: boolean;
  signal?: AbortSignal;
}

async function readMessage(res: Response): Promise<string | null> {
  try {
    const text = await res.text();
    if (!text) return null;
    const data = JSON.parse(text);
    return typeof data?.message === "string" && data.message.trim() ? data.message : null;
  } catch {
    return null;
  }
}

export async function request<T>(method: string, path: string, opts: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (opts.body !== undefined) headers["Content-Type"] = "application/json";
  if (opts.auth) {
    const token = session.token();
    if (!token) {
      session.clear("expired");
      throw new ApiError(401, DEFAULT_MESSAGES[401]);
    }
    headers.Authorization = `Bearer ${token}`;
  }

  let res: Response;
  try {
    res = await fetch(apiUrl(path), {
      method,
      headers,
      body: opts.form ?? (opts.body !== undefined ? JSON.stringify(opts.body) : undefined),
      signal: opts.signal,
    });
  } catch (err) {
    if ((err as Error).name === "AbortError") throw err;
    throw new ApiError(0, NETWORK_ERROR);
  }

  if (res.ok) {
    if (res.status === 204) return undefined as T;
    const text = await res.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }

  const message = (await readMessage(res)) ?? DEFAULT_MESSAGES[res.status] ?? "Algo salió mal. Inténtalo de nuevo.";
  // 401 en una ruta del personal = token vencido o inválido: se cierra la sesión y el guard lleva al login.
  if (res.status === 401 && opts.auth) session.clear("expired");
  throw new ApiError(res.status, message);
}

export function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  return "Algo salió mal. Inténtalo de nuevo.";
}
