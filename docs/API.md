# Contrato de la API de MealUpp

Esta es la guía para quien hace el frontend. Todo el backend está construido: aquí está cada ruta, qué recibe y qué devuelve.

- Base: la misma dirección del sitio (`/api/...`). En desarrollo, `http://localhost:8080`.
- Formato: JSON con codificación UTF-8. Las horas van como `"HH:mm"` (por ejemplo `"12:30"`) y las fechas como `"2026-11-04"`.
- Los errores siempre llegan así: `{ "message": "texto para mostrarle a la persona" }`.
- Códigos: `400` datos o regla incumplida, `401` sin sesión o token vencido, `403` el rol no puede, `404` no existe.

## Cómo entra cada persona

| Quién | Cómo |
|---|---|
| Cliente | No inicia sesión. Hace su pedido y guarda el `accessCode` que recibe para consultarlo. |
| Personal | Inicia sesión y manda `Authorization: Bearer <token>` en cada petición a `/api/staff/**`. El token vence a las 8 horas. |

### Roles del personal

| Rol | Qué ve | Qué cambios de estado puede hacer |
|---|---|---|
| `ADMIN` | Todo | Cualquier cambio válido |
| `KITCHEN` | Comandas y pedidos por preparar | `PENDING` → `IN_PREPARATION` |
| `CASHIER` | Pedidos del día con su valor | `PENDING_PAYMENT` o `PENDING` → `CANCELLED` |
| `DELIVERY` | Pedidos en preparación, con a quién y dónde | `IN_PREPARATION` → `DELIVERED` |

El login devuelve `sections`: las opciones que debe mostrar el menú de esa persona (`pedidos`, `cocina`, `caja`, `entrega`, `menu`, `personal`).

### Estados de un pedido

```
PENDING_PAYMENT ──pago aprobado──▶ PENDING ──▶ IN_PREPARATION ──▶ DELIVERED
      │                               │
      ├──15 min sin pagar──▶ EXPIRED  └──▶ CANCELLED   (también desde PENDING_PAYMENT)
```

Si el pago en línea está desactivado (`paymentsRequired: false` en `/api/config`), el pedido nace directamente en `PENDING`.

---

## 1. Rutas públicas (sin token)

### `GET /api/config`
Reglas del negocio para armar el formulario. No las escribas a mano en el frontend.

```json
{
  "timeZone": "America/Bogota",
  "cutoffTime": "10:30:00",
  "deliveryStart": "11:30:00",
  "deliveryEnd": "14:00:00",
  "minQuantity": 1,
  "maxQuantity": 10,
  "maxOrdersPerDay": 100,
  "paymentsRequired": true,
  "currency": "COP",
  "paymentExpirationMinutes": 15,
  "zones": [
    { "code": "SHOPPING_CENTER", "label": "Centro comercial" },
    { "code": "FURNITURE_STREET", "label": "Calle de los Muebles" }
  ]
}
```

### `GET /api/menu`
Platos disponibles. Si no hay ninguno, `dishes` viene vacío y `message` trae el aviso.
`thumbnailUrl` (400 px, para listados) e `imageUrl` (1200 px, para el detalle) son `null` si el plato no tiene foto.

```json
{
  "dishes": [
    { "id": 1, "name": "Pollo a la plancha", "description": "Con arroz", "price": 12000,
      "thumbnailUrl": "/media/dishes/1/a1b2c3d4e5f6_thumb.jpg",
      "imageUrl": "/media/dishes/1/a1b2c3d4e5f6_detail.jpg" }
  ],
  "message": null
}
```

### `POST /api/orders`
Crea un pedido. Responde `201`.

```json
{ "dishId": 1, "quantity": 2, "customerName": "Valentina",
  "deliveryTime": "12:00", "deliveryZone": "SHOPPING_CENTER", "deliveryDetail": "Local 12" }
```

Respuesta:

```json
{ "id": 7, "dishName": "Pollo a la plancha", "quantity": 2, "customerName": "Valentina",
  "orderDate": "2026-11-04", "deliveryTime": "12:00:00", "deliveryZone": "SHOPPING_CENTER",
  "deliveryDetail": "Local 12", "status": "PENDING_PAYMENT", "total": 24000,
  "accessCode": "K7M2XQ9PTD",
  "paymentUrl": "https://mealupp.com/pago-simulado?reference=SBX-..." }
```

- **Guarda `id` y `accessCode`** (por ejemplo en `localStorage`): son la única forma de que el cliente consulte su pedido.
- Si `paymentUrl` no es `null`, lleva al cliente a esa dirección para que pague. Con pago desactivado viene `null` y el estado es `PENDING`.
- Errores `400`: pasó la hora límite, cantidad fuera de rango, hora de entrega fuera de la ventana, plato agotado, se alcanzó el máximo de pedidos del día, o faltan datos. `404`: el plato no existe.

### `GET /api/orders/{id}?code=ACCESSCODE`
Estado del pedido para el cliente. Consúltalo cada pocos segundos (por ejemplo cada 10 s) mientras espera el pago o la entrega.
Con un código incorrecto responde `404` (igual que si no existiera).

```json
{ "id": 7, "status": "PENDING", "dishName": "Pollo a la plancha", "quantity": 2,
  "deliveryTime": "12:00:00", "deliveryZone": "SHOPPING_CENTER", "deliveryDetail": "Local 12",
  "total": 24000, "paymentPending": false }
```

### `POST /api/orders/{id}/pay?code=ACCESSCODE`
Nuevo intento de pago de un pedido que sigue en `PENDING_PAYMENT` (por ejemplo si el pago fue rechazado). Devuelve `{ "paymentUrl": "..." }`.

---

## 2. Pago en línea

El pedido queda en `PENDING_PAYMENT` y el cliente paga en la página de la pasarela. Cuando el pago se aprueba, la pasarela avisa al backend y el pedido pasa a `PENDING`: el frontend lo ve consultando `GET /api/orders/{id}`.

Si en 15 minutos no se paga, el pedido pasa solo a `EXPIRED`.

### Pasarela simulada (desarrollo y demostraciones)
Con `PAYMENT_PROVIDER=sandbox` no se cobra nada. `paymentUrl` apunta a una página **de tu frontend**, `/pago-simulado?reference=SBX-...`. Esa página solo necesita dos botones:

| Botón | Llamada | Efecto |
|---|---|---|
| Aprobar | `POST /api/payments/sandbox/{reference}/approve` | El pedido pasa a `PENDING` y la comanda llega a cocina |
| Rechazar | `POST /api/payments/sandbox/{reference}/decline` | El pedido sigue en `PENDING_PAYMENT` (se puede reintentar) |

Estas dos rutas existen solo si `SANDBOX_ENABLED=true`. En producción se apagan.

### Mercado Pago (`PAYMENT_PROVIDER=mercadopago`)
Al crear el pedido, el backend crea una preferencia de Checkout Pro y devuelve su dirección en `paymentUrl`; el frontend lleva al cliente allí. Al terminar, Mercado Pago lo devuelve a `FRONTEND_URL/pedido/{id}` y avisa por webhook a:

`POST /api/payments/mercadopago/webhook?data.id=...&type=payment` con los encabezados `x-signature` (`ts=...,v1=...`) y `x-request-id`. El backend valida la firma con `MERCADOPAGO_WEBHOOK_SECRET`, consulta el pago en la API de Mercado Pago y aplica el resultado: `approved` confirma el pedido; `rejected` y `cancelled` rechazan el intento; los demás estados se ignoran. Responde `200`.

### Otra pasarela
Se escribe un adaptador nuevo en el backend y el frontend no cambia. El aviso de la pasarela llega a:

`POST /api/payments/webhook` con el encabezado `X-Signature` = HMAC-SHA256 en hexadecimal del cuerpo, con la clave `PAYMENT_WEBHOOK_SECRET`.

```json
{ "reference": "SBX-...", "status": "APPROVED", "amount": 24000 }
```

`status` puede ser `APPROVED` o `DECLINED`. Si la firma no es válida responde `401`. Un aviso repetido no confirma el pedido dos veces.

---

## 3. Sesión del personal

### `POST /api/auth/login`
```json
{ "username": "ferxxos", "password": "..." }
```
Respuesta `200`:
```json
{ "token": "eyJ...", "expiresAt": "2026-11-04T17:00:00", "fullName": "Administradora",
  "role": "ADMIN", "sections": ["pedidos", "cocina", "caja", "entrega", "menu", "personal"] }
```
Con datos incorrectos: `401` con "Usuario o contraseña incorrectos". Guarda el token en memoria (no en `localStorage` si puedes evitarlo) y mándalo como `Authorization: Bearer <token>`.

### `POST /api/auth/logout`
Con el token. Responde `204` y el token deja de servir.

---

## 4. Pedidos (personal)

| Ruta | Roles | Qué devuelve |
|---|---|---|
| `GET /api/staff/orders?zone=SHOPPING_CENTER` | `ADMIN` | `{ date, totalLunches, orders: [...] }`. `zone` es opcional. `totalLunches` cuenta solo pedidos confirmados |
| `PATCH /api/staff/orders/{id}/status` con `{ "status": "IN_PREPARATION" }` | todos los roles, según la tabla de arriba | El pedido actualizado. `403` si el rol no puede hacer ese cambio; `400` si el cambio no es válido |
| `GET /api/staff/kitchen/orders` | `KITCHEN`, `ADMIN` | `[{ id, dishName, quantity, deliveryTime, status }]` (sin datos del cliente) |
| `GET /api/staff/kitchen/tickets` | `KITCHEN`, `ADMIN` | Comandas pendientes (ver WebSocket). Úsalo al abrir o reconectar la pantalla de cocina |
| `GET /api/staff/cashier/orders` | `CASHIER`, `ADMIN` | `[{ id, customerName, dishName, quantity, unitPrice, total, status }]` |
| `GET /api/staff/delivery/orders?zone=...` | `DELIVERY`, `ADMIN` | `[{ id, customerName, dishName, quantity, deliveryTime, deliveryZone, deliveryDetail, status }]`, solo los que están en `IN_PREPARATION` |

Una persona que abre la pantalla de otro rol recibe `403` con "No tienes permiso para entrar a esta sección".

---

## 5. Menú y fotos (solo `ADMIN`)

| Ruta | Cuerpo | Notas |
|---|---|---|
| `GET /api/staff/dishes` | — | Todos los platos, también los agotados (`available`) |
| `POST /api/staff/dishes` | `{ "name", "description", "price", "available" }` | `201`. `description` y `available` son opcionales (por defecto disponible) |
| `PUT /api/staff/dishes/{id}` | `{ "name", "description", "price" }` | Edita el plato |
| `PATCH /api/staff/dishes/{id}/availability` | `{ "available": false }` | Marcar agotado o disponible |
| `DELETE /api/staff/dishes/{id}` | — | `204`. Si el plato ya tiene pedidos responde `400`: se debe marcar como agotado |
| `POST /api/staff/dishes/{id}/photo` | `multipart/form-data`, campo `file` | JPG o PNG, máximo 5 MB. Reemplaza la foto anterior |
| `DELETE /api/staff/dishes/{id}/photo` | — | Quita la foto |

Las respuestas de los platos traen `id, name, description, price, available, thumbnailUrl, imageUrl`.
Ejemplo de subida con `fetch`:

```js
const form = new FormData();
form.append("file", archivo);                       // un <input type="file">
await fetch(`/api/staff/dishes/${id}/photo`, {
  method: "POST",
  headers: { Authorization: `Bearer ${token}` },    // NO pongas Content-Type: el navegador lo arma
  body: form,
});
```

Las fotos se piden directamente a `thumbnailUrl` / `imageUrl` (son direcciones públicas, sin token).

---

## 6. Personal (solo `ADMIN`)

| Ruta | Cuerpo | Notas |
|---|---|---|
| `GET /api/staff/members` | — | `[{ id, username, fullName, role, active }]` (nunca la contraseña) |
| `POST /api/staff/members` | `{ "username", "fullName", "password", "role" }` | `201`. Usuario de 3 a 30 caracteres (minúsculas, números, `.`, `-`, `_`); contraseña de 8 o más |
| `PATCH /api/staff/members/{id}/role` | `{ "role": "KITCHEN" }` | Cambia el rol. Siempre debe quedar al menos una persona `ADMIN` activa |
| `PATCH /api/staff/members/{id}/active` | `{ "active": false }` | Desactiva o reactiva la cuenta. Una cuenta desactivada pierde el acceso de inmediato |

---

## 7. Tiempo real (WebSocket)

Sirve para que cocina vea las comandas al instante, sin recargar. Es STOMP sobre WebSocket.

- Dirección: `wss://TU-DOMINIO/ws` (en desarrollo `ws://localhost:8080/ws`).
- Al conectar hay que mandar el token en el encabezado STOMP `Authorization: Bearer <token>`. Sin token válido, la conexión se rechaza.
- Cada rol solo puede suscribirse a sus canales; otro canal se rechaza.

| Canal | Quién | Cuándo llega un mensaje |
|---|---|---|
| `/topic/comandas` | `KITCHEN`, `ADMIN` | Llega una comanda nueva, o una cambia (la tomaron, se canceló, se entregó) |
| `/topic/pedidos` | `ADMIN`, `CASHIER`, `DELIVERY` | Un pedido se confirma o cambia de estado |

El cuerpo de cada mensaje es **texto JSON** (haz `JSON.parse(frame.body)`).

`/topic/comandas`:
```json
{ "event": "CREATED", "ticketId": 4, "orderId": 7, "dishName": "Pollo a la plancha",
  "quantity": 2, "deliveryTime": "12:30", "orderStatus": "PENDING", "ticketStatus": "SENT" }
```
`event` es `CREATED` o `UPDATED`. Muestra la comanda mientras `orderStatus` sea `PENDING` o `IN_PREPARATION`, y quítala cuando sea `CANCELLED` o `DELIVERED`.

`/topic/pedidos`:
```json
{ "event": "ORDER_CHANGED", "id": 7, "dishName": "Pollo a la plancha", "quantity": 2,
  "customerName": "Valentina", "orderDate": "2026-11-04", "deliveryTime": "12:30",
  "deliveryZone": "SHOPPING_CENTER", "deliveryDetail": "Local 12", "status": "IN_PREPARATION", "total": 24000 }
```

Ejemplo con `@stomp/stompjs`:

```js
import { Client } from "@stomp/stompjs";

const client = new Client({
  brokerURL: "wss://TU-DOMINIO/ws",
  connectHeaders: { Authorization: `Bearer ${token}` },
  reconnectDelay: 3000,
  onConnect: async () => {
    // 1) Primero pide lo pendiente por REST: así no se pierde nada si la pantalla estuvo desconectada.
    const pendientes = await (await fetch("/api/staff/kitchen/tickets", {
      headers: { Authorization: `Bearer ${token}` },
    })).json();
    mostrar(pendientes);
    // 2) Luego escucha lo nuevo.
    client.subscribe("/topic/comandas", (frame) => actualizar(JSON.parse(frame.body)));
  },
});
client.activate();
```

Si el WebSocket no llega a conectarse, la pantalla puede consultar `GET /api/staff/kitchen/tickets` cada 15 segundos como respaldo.

---

## 8. Otras rutas

- `GET /media/**`: fotos (solo cuando las fotos se guardan en el servidor; con S3 las direcciones ya vienen completas).
- `GET /actuator/health`: `{ "status": "UP" }`, para revisar que el servicio está vivo.

## 9. Variables de entorno

| Variable | Para qué sirve | Obligatoria |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Base de datos PostgreSQL | `DB_PASSWORD` sí |
| `STAFF_USERNAME`, `STAFF_PASSWORD` | Primera cuenta de administradora | `STAFF_PASSWORD` sí |
| `JWT_SECRET` | Firma de los tokens (mínimo 32 caracteres) | Sí |
| `PAYMENT_WEBHOOK_SECRET` | Firma de los avisos de la pasarela | Sí |
| `CORS_ALLOWED_ORIGINS` | Dominios del frontend, separados por coma | No (por defecto `localhost:5173` y `:3000`) |
| `PAYMENTS_REQUIRED` | `true` exige pago en línea; `false` confirma el pedido al crearlo | No (`true`) |
| `PAYMENT_PROVIDER` | Proveedor de la pasarela: `sandbox` (simulada) o `mercadopago` | No (`sandbox`) |
| `MERCADOPAGO_ACCESS_TOKEN` | Credencial privada de la aplicación de Mercado Pago (`TEST-...` en pruebas) | Solo con `mercadopago` |
| `MERCADOPAGO_WEBHOOK_SECRET` | Clave secreta de los webhooks, la que muestra el panel de Mercado Pago | Solo con `mercadopago` |
| `MERCADOPAGO_NOTIFICATION_URL` | Dirección pública HTTPS del webhook (`.../api/payments/mercadopago/webhook`) | Solo con `mercadopago` |
| `FRONTEND_URL` | Dirección del frontend, a donde vuelve el cliente después de pagar | No (`http://localhost:5173`) |
| `SANDBOX_ENABLED` | Activa las rutas de la pasarela simulada | No (`true`; apagar en producción) |
| `SANDBOX_CHECKOUT_URL` | Página del frontend para el pago simulado | No |
| `STORAGE_TYPE` | `local` (carpeta del servidor) o `s3` | No (`local`) |
| `MEDIA_DIR`, `MEDIA_BASE_URL` | Carpeta de fotos y dirección pública | No |
| `S3_ENDPOINT`, `S3_BUCKET`, `S3_REGION`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` | Almacenamiento compatible con S3 | Solo con `STORAGE_TYPE=s3` |
