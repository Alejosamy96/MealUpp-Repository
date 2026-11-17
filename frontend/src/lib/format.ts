// Formatos en español de Colombia: precios, horas y fechas.

const number = new Intl.NumberFormat("es-CO", { maximumFractionDigits: 0 });

/** 12000 → "$12.000" (como en el diseño). */
export function formatCOP(value: number): string {
  return "$" + number.format(Math.round(value));
}

/** "12:30:00" → "12:30 p.m." */
export function formatTime(hhmm: string): string {
  const [h, m] = hhmm.split(":").map(Number);
  if (Number.isNaN(h)) return hhmm;
  const suffix = h >= 12 ? "p.m." : "a.m.";
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${String(m ?? 0).padStart(2, "0")} ${suffix}`;
}

const DAYS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
const MONTHS = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];

/** "2026-05-12" → "Lun, 12 de mayo" */
export function formatDate(isoDate: string): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  if (!y || !m || !d) return isoDate;
  const date = new Date(Date.UTC(y, m - 1, d));
  return `${DAYS[date.getUTCDay()]}, ${d} de ${MONTHS[m - 1]}`;
}

/** Fecha y hora actuales en la zona del restaurante (America/Bogota por defecto). */
export function nowInZone(timeZone = "America/Bogota"): { date: string; time: string } {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date());
  const get = (t: string) => parts.find((p) => p.type === t)?.value ?? "00";
  return { date: `${get("year")}-${get("month")}-${get("day")}`, time: `${get("hour")}:${get("minute")}` };
}

/** "10:30:00" → minutos desde la medianoche. */
export function toMinutes(time: string): number {
  const [h, m] = time.split(":").map(Number);
  return h * 60 + (m || 0);
}

/** Minutos → "HH:mm:ss" (formato que espera el backend). */
export function fromMinutes(total: number): string {
  const h = Math.floor(total / 60);
  const m = total % 60;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:00`;
}

/** Horarios de entrega cada `step` minutos dentro de la ventana permitida (ambos extremos incluidos). */
export function deliverySlots(start: string, end: string, step = 15): string[] {
  const slots: string[] = [];
  for (let t = toMinutes(start); t <= toMinutes(end); t += step) slots.push(fromMinutes(t));
  return slots;
}

/** "Pollo a la plancha" → "pollo-a-la-plancha" (nombre del archivo de foto opcional). */
export function slugify(text: string): string {
  return text
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export function relativeSeconds(ms: number): string {
  const s = Math.max(0, Math.round((Date.now() - ms) / 1000));
  if (s < 10) return "justo ahora";
  if (s < 60) return `hace ${s} s`;
  const m = Math.round(s / 60);
  return `hace ${m} min`;
}
