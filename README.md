# MealUpp-Repository

MealUpp es una startup con la intención de facilitar la gestión de restaurantes mediante pedidos y ordenes directos entre usuarios y cocina. Este repositorio es el **backend** (API REST) del proyecto para Ferxxos de Almuerzos.

## Qué hace el backend

| Módulo | Qué incluye |
|---|---|
| Menú y fotos | Menú público, administración de platos y foto de cada plato (se guardan reducidas, en dos tamaños) |
| Pedidos | Crear un pedido con sus reglas (hora límite, cantidad, hora de entrega, máximo de 100 por día), consultarlo con un código y seguir su estado |
| Pagos | Pago en línea con webhook firmado, reintentos y vencimiento a los 15 minutos. Hoy con una pasarela simulada; el proveedor real se conecta con un adaptador |
| Comandas | Cuando un pedido se confirma, la cocina recibe su comanda por WebSocket, en tiempo real |
| Personal | Inicio de sesión con token JWT, cuatro roles (administradora, cocina, caja y entrega), alta y baja de cuentas |

La guía completa de rutas, datos y WebSocket para quien haga el frontend está en **[docs/API.md](docs/API.md)**.

## Variables de entorno

Ninguna contraseña va escrita en el código (Definition of Done, punto 7). Las obligatorias son estas cuatro; el resto tiene valor por defecto (la lista completa está en [docs/API.md](docs/API.md#9-variables-de-entorno) y en [.env.example](.env.example)).

| Variable | Para qué sirve | Valor por defecto |
|---|---|---|
| `DB_PASSWORD` | Contraseña de la base de datos PostgreSQL | Ninguno, es obligatoria |
| `STAFF_PASSWORD` | Contraseña de la primera administradora | Ninguno, es obligatoria |
| `JWT_SECRET` | Clave que firma los tokens (mínimo 32 caracteres) | Ninguno, es obligatoria |
| `PAYMENT_WEBHOOK_SECRET` | Clave con la que la pasarela firma sus avisos | Ninguno, es obligatoria |
| `DB_URL` / `DB_USERNAME` | Dirección y usuario de la base de datos | `jdbc:postgresql://localhost:5432/mealup` / `postgres` |
| `STAFF_USERNAME` | Usuario de la primera administradora | `ferxxos` |

La cuenta de la administradora se crea sola la primera vez que arranca la aplicación, si todavía no hay personal registrado.

## Cómo correrlo en tu computador

En PowerShell, dentro de la carpeta del repositorio (con PostgreSQL funcionando):

```
$env:DB_PASSWORD = "tu-clave-de-postgres"
$env:STAFF_PASSWORD = "una-clave-segura"
$env:JWT_SECRET = "una-frase-larga-de-mas-de-32-caracteres-aleatorios"
$env:PAYMENT_WEBHOOK_SECRET = "otra-frase-larga-para-los-avisos-de-pago"
.\mvnw spring-boot:run
```

La API queda en `http://localhost:8080`. Revisa que esté viva en `http://localhost:8080/actuator/health`. Las fotos subidas se guardan en la carpeta `media/`.

## Cómo desplegarlo (servidor con Docker)

Una máquina Linux con Docker. El proxy (Caddy) consigue el certificado HTTPS solo, reparte el tráfico y sirve el frontend; la base de datos nunca queda expuesta a Internet.

```
cp .env.example .env          # llenar los valores, sobre todo las claves
docker compose up -d --build
```

- Frontend: se copia ya compilado a la carpeta `frontend-dist/` (la sirve el proxy).
- Copia de seguridad de la base de datos: `ops/backup.sh` (programarla con `cron` cada día; guarda 14 días).
- Fotos en la nube: cambiar `STORAGE_TYPE=s3` y llenar las variables `S3_*` (sirve con AWS S3, Cloudflare R2, Backblaze B2 o MinIO).
- En producción con una pasarela real, poner `SANDBOX_ENABLED=false`.

## Pruebas

Las pruebas no necesitan PostgreSQL (usan una base H2 en memoria):

```
.\mvnw spotless:apply
.\mvnw clean verify
```

`verify` también revisa que la cobertura de líneas sea de 70 % o más. El reporte queda en `target/site/jacoco/index.html`. En GitHub, el flujo `CI` (`.github/workflows/ci.yml`) corre lo mismo en cada pull request y, al integrar a `main`, construye la imagen de Docker.

Los estándares del equipo están en [ESTANDARES.md](ESTANDARES.md).
