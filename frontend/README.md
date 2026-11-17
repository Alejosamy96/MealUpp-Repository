# MealUp · Frontend web

"Tu almuerzo, a tu ritmo". Front-end de MealUp para el restaurante Ferxxos (Villavicencio): reservas de almuerzo para clientes y panel para el personal.

Hecho con React + Vite + TypeScript, React Router y CSS Modules. Está pensado primero para celular, pero funciona igual en escritorio. Consume la API del backend de este mismo repositorio (ver [`../docs/API.md`](../docs/API.md)); el backend no se modifica desde aquí.

## Requisitos

- Node.js 20 o superior (LTS) y npm.
- El backend corriendo en `http://localhost:8080` (ver el `README.md` de la raíz).

## Cómo correrlo junto al backend

1. **Backend** (terminal 1, en la raíz del repositorio, PowerShell):

   ```
   $env:DB_PASSWORD = "tu-clave-de-postgres"
   $env:STAFF_PASSWORD = "una-clave-segura"
   $env:JWT_SECRET = "una-frase-larga-de-mas-de-32-caracteres-aleatorios"
   $env:PAYMENT_WEBHOOK_SECRET = "otra-frase-larga-para-los-avisos-de-pago"
   .\mvnw spring-boot:run
   ```

2. **Frontend** (terminal 2):

   ```
   cd frontend
   npm install
   npm run dev
   ```

3. Abre **http://localhost:5173** (usa `localhost`, no `127.0.0.1`: la pasarela de pago simulada devuelve direcciones con `localhost` y las reservas se guardan por dominio).

En desarrollo, Vite reenvía `/api`, `/media` y `/ws` al backend (`vite.config.ts`), así que no hace falta configurar CORS. Si el backend está en otro puerto: `MEALUP_BACKEND=http://localhost:9090 npm run dev` (en PowerShell: `$env:MEALUP_BACKEND="http://localhost:9090"; npm run dev`).

El personal entra en **http://localhost:5173/staff** con el usuario `ferxxos` y la contraseña definida en `STAFF_PASSWORD`.

## Compilar para producción

```
npm run build
```

Deja los archivos en `frontend/dist/`.

- **Con el despliegue del repositorio (Docker + Caddy):** copia el contenido de `frontend/dist/` a la carpeta `frontend-dist/` de la raíz. Caddy sirve el sitio y reenvía `/api`, `/media` y `/ws` al backend en el mismo dominio. No hace falta `VITE_API_URL`.
- **Si el backend vive en otro dominio:** compila con `VITE_API_URL=https://api.tu-dominio.com npm run build` (ver `.env.example`) y agrega el dominio del front a `CORS_ALLOWED_ORIGINS` en el backend.

El servidor que publique el sitio debe devolver `index.html` para cualquier ruta (el `try_files` del `Caddyfile` ya lo hace), para que recargar `/mis-reservas` o `/staff/cocina` funcione.

## Pantallas y rutas

| Ruta | Pantalla |
|---|---|
| `/` | Inicio: hero, menú del día deslizable y beneficios |
| `/menu` | Menú del día completo |
| `/reservar?plato=ID` | Reserva: plato, cantidad, nombre, hora, zona, detalle y resumen con total |
| `/pago-simulado?reference=...` | Pasarela de **prueba** (aprobar o rechazar; no se piden datos de tarjeta) |
| `/pedido/:id` | Seguimiento de la reserva (y pantalla de éxito con el número de pedido) |
| `/mis-reservas` | Próximas, Históricas y Canceladas |
| `/perfil`, `/perfil/editar`, `/perfil/:opcion` | Perfil y opciones "Próximamente" |
| `/staff/login` | Inicio de sesión del personal |
| `/staff/pedidos` · `cocina` · `caja` · `entrega` · `menu` · `personal` | Panel según el rol (`sections` del login) |

## Cómo está organizado

```
src/
  api/         client.ts (fetch central: token, errores { message }, 401/403), endpoints.ts, types.ts
  services/    session.ts (sesión del personal), ordersStore.ts (reservas del cliente), profileStore.ts
  hooks/       useAsync (carga/error/recarga), useConfig (GET /api/config), useStompTopic (tiempo real)
  config/      labels.ts (textos de estados, roles, secciones y qué botones ve cada rol)
  components/  Button, Card, Chip, Tabs, Field, DishCard, DishImage, Logo, Icon, layout/ClientLayout (+ BottomNav)
  pages/       client/ y staff/
  styles/      tokens.css (paleta y medidas: único lugar con colores) y global.css
```

## Decisiones importantes

- **Reglas del negocio:** la hora límite, la ventana de entrega, las cantidades y las zonas vienen de `GET /api/config`, no están escritas en el front. El front valida antes de enviar, pero el backend es el que decide y su mensaje se muestra tal cual.
- **Reservas del cliente:** el cliente no tiene cuenta. Cada reserva se guarda en `localStorage` con su `id` y `accessCode`, y el estado real se consulta con `GET /api/orders/{id}?code=...` (cada 10 s en el detalle y cada 20 s en Mis reservas). Todo está en `services/ordersStore.ts`, por si luego existe un listado por cliente.
- **Sesión del personal:** el token se guarda en `sessionStorage` (no en `localStorage`, como recomienda `docs/API.md`): sobrevive a recargar, pero se borra al cerrar la pestaña. Ante un `401` se cierra la sesión y se vuelve al login; ante un `403` se muestra el mensaje del backend. Las contraseñas nunca se guardan.
- **Tiempo real:** Cocina escucha `/topic/comandas` y Pedidos, Caja y Entrega escuchan `/topic/pedidos` (STOMP en `/ws`). Si no conecta, cada pantalla se actualiza sola cada 30 s (cocina cada 15 s) y con el botón "Actualizar".
- **Fotos de platos:** se usa la foto del backend (`thumbnailUrl`). Si no hay, se intenta `public/images/dishes/<nombre-con-guiones>.jpg` (ej. `pollo-a-la-plancha.jpg`) y, si tampoco existe, un placeholder con la paleta.
- **Logo:** `public/logo-mealup.png` es el oficial (fondo arena) y se usa como base del favicon. `public/logo-mealup-horizontal.png` usa las mismas piezas en una fila y con el fondo arena vuelto transparente, para el encabezado. Si se consigue una versión oficial con fondo transparente, basta con reemplazar ese archivo.

## Lo que todavía no tiene soporte en el backend

- **Perfil del cliente:** nombre y correo se guardan solo en el navegador; "Cerrar sesión" los borra de ese dispositivo.
- **"Próximamente":** Métodos de pago, Notificaciones, Preferencias de comida, Ayuda y soporte y el botón "Notificar llegada".
- **Cupos por plato:** no se muestran (el backend no los maneja).
- **Pago:** hoy es la pasarela simulada del backend (`PAYMENT_PROVIDER=sandbox`). Cuando se conecte una pasarela real, el backend devolverá su `paymentUrl` y el front redirige allá sin cambios.

## Prueba manual sugerida

1. Inicio muestra el menú del día → elige un plato → completa la reserva → "Confirmar e ir a pagar".
2. En la pasarela de prueba, "Rechazar" → "Ver mi reserva" → "Pagar ahora" → "Aprobar": aparece "¡Reserva confirmada!" con el número.
3. "Mis reservas" muestra la reserva; recarga la página y sigue ahí.
4. `/staff` → entra con `ferxxos` → Cocina: "Empezar preparación" → Entrega: "Marcar entregado" → Caja muestra el valor; el estado del cliente cambia en "Mis reservas".
