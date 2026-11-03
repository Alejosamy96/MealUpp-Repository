# MealUpp-Repository

MealUpp es una startup con la intención de facilitar la gestión de restaurantes mediante pedidos y ordenes directos entre usuarios y cocina

## Inicio de sesión del personal (HU 5)

El personal del restaurante entra con usuario y contraseña. El sistema devuelve un token que se manda en la cabecera `Authorization: Bearer <token>` en cada petición a las rutas internas (`/api/staff/**`). El token vence a las 8 horas (`mealup.staff.token-hours`) o al cerrar sesión.

| Acción | Ruta | Resultado |
|---|---|---|
| Iniciar sesión | `POST /api/auth/login` con `{"username": "...", "password": "..."}` | 200 con token, nombre, rol y opciones del menú. Con datos incorrectos o vacíos: 401 o 400 con mensaje |
| Cerrar sesión | `POST /api/auth/logout` con el token | 204. El token deja de servir |
| Ruta interna sin token | cualquier `/api/staff/**` | 401 |

Roles (HU 6): `ADMIN` (todo), `KITCHEN` (pedidos que hay que preparar) y `CASHIER` (pedidos con su valor). Un rol que abre la ruta de otro recibe 403 con un mensaje.

## Variables de entorno

Ninguna contraseña va escrita en el código (Definition of Done, punto 7). Antes de correr la aplicación se definen estas variables:

| Variable | Para qué sirve | Valor por defecto |
|---|---|---|
| `DB_URL` | Dirección de la base de datos PostgreSQL | `jdbc:postgresql://localhost:5432/mealup` |
| `DB_USERNAME` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de la base de datos | Ninguno, es obligatoria |
| `STAFF_USERNAME` | Usuario de la primera administradora | `ferxxos` |
| `STAFF_PASSWORD` | Contraseña de la primera administradora | Ninguno, es obligatoria |

La cuenta de la administradora se crea sola la primera vez que arranca la aplicación, si todavía no hay personal registrado.

## Cómo correrlo y probarlo

En PowerShell, dentro de la carpeta del repositorio:

```
$env:DB_PASSWORD = "tu-clave-de-postgres"
$env:STAFF_PASSWORD = "una-clave-segura"
.\mvnw spring-boot:run
```

Las pruebas no necesitan PostgreSQL (usan una base H2 en memoria):

```
.\mvnw clean verify
```

Este comando también revisa que la cobertura de líneas sea de 70 % o más. El reporte queda en `target/site/jacoco/index.html`.

Los estándares del equipo están en [ESTANDARES.md](ESTANDARES.md).
