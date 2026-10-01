# Estándares del equipo - MealUpp

Proyecto: sistema de reserva de almuerzos.
Equipo: Danna Giselle Aguilar García y Alejandro López Rodríguez.
Redactado por: Danna Giselle Aguilar García.

Trabajamos con Java y Spring Boot 4.1.1, usamos Maven para compilar y manejar las dependencias, PostgreSQL como base de datos y Spotless con google-java-format como formateador.

## 1. Guía de estilo y nombres

Adoptamos la [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

El código (clases, métodos, variables y paquetes) va en inglés. Los comentarios y la documentación del repositorio van en español.

El formateador está configurado en el `pom.xml`. Para formatear el código usamos `./mvnw spotless:apply` y para revisar que todo cumpla usamos `./mvnw spotless:check`. Usamos el Maven Wrapper (`mvnw`) que viene dentro del repositorio, así que no hace falta instalar Maven. En Windows PowerShell se escribe `.\mvnw`.

Nuestras tres reglas de nombres:

1. Cada clase termina con el nombre de su capa: `ReservationController`, `ReservationService`, `ReservationRepository`. Las entidades van en singular y sin sufijo, por ejemplo `Reservation`.
2. Los objetos que se envían o reciben por la API terminan en `Request` o `Response` (`CreateReservationRequest`, `ReservationResponse`) y van en el paquete `dto`.
3. Los métodos que devuelven `boolean` empiezan con `is`, `has` o `can`, por ejemplo `isReservationOpen` o `hasArrived`.

## 2. Convención de commits y ramas

El mensaje del commit tiene este formato: `tipo: descripción`. La descripción va en español, en minúscula, en imperativo y con máximo 72 caracteres. Si hace falta se puede agregar el módulo entre paréntesis: `tipo(módulo): descripción`.

Tipos permitidos: `feat`, `fix`, `docs`, `refactor`, `test` y `chore`.

Ejemplos:

```
feat(reservas): bloquear reservas después de las 12:00 p.m.
fix(auth): corregir el rol del cocinero en el login
docs: agregar estándares del equipo
```

Ramas:

- `main`: la versión estable. Solo recibe cambios que vienen de `develop`.
- `develop`: donde se junta el trabajo en curso.
- `feature/<nombre-corto>`: para una funcionalidad nueva, por ejemplo `feature/aviso-llegada`.
- `fix/<nombre-corto>`: para corregir un error.

Las ramas `feature/` y `fix/` salen de `develop` y vuelven a `develop` con un pull request. No se hacen commits directos a `main`.

## 3. Definition of Ready

Una tarea se puede empezar cuando cumple estas cinco condiciones:

1. Está creada como issue en el repositorio, con título y descripción.
2. Dice qué requerimiento del documento técnico implementa (por ejemplo `RF-05` o `RNF-13`).
3. Tiene criterios de aceptación escritos como lista de casillas (`- [ ]`).
4. Tiene una persona asignada.
5. Si depende de otra tarea, el issue la nombra y esa tarea ya está terminada.

## 4. Definition of Done

Una tarea está terminada cuando cumple estas siete condiciones. Cualquier persona puede comprobarlas abriendo el repositorio:

1. En un clon limpio del repositorio, `./mvnw clean verify` termina en `BUILD SUCCESS`, es decir, las pruebas pasan.
2. `./mvnw spotless:check` termina en `BUILD SUCCESS`.
3. Hay un pull request hacia `develop` aprobado por la otra persona del equipo, y la aprobación se ve en el historial del PR.
4. Cada criterio de aceptación del issue tiene al menos una prueba automatizada en `src/test`, y el PR menciona su nombre.
5. Todos los commits de la rama siguen la convención de la sección 2, lo que se comprueba con `git log`.
6. El PR enlaza el issue y el código del requerimiento (`RF-xx` o `RNF-xx`), y todas las casillas del issue están marcadas.
7. No hay contraseñas ni llaves escritas en el código ni en `src/main/resources/application.properties`; las credenciales se leen de variables de entorno. Se comprueba abriendo ese archivo.

## 5. Política de revisión

Como somos dos, cada pull request lo revisa la otra persona. Nadie aprueba ni integra su propio PR.

El plazo para revisar es de 24 horas hábiles desde que se abre el PR. Si pasa ese tiempo sin respuesta, quien abrió el PR le escribe a la otra persona por el chat del equipo y no integra hasta tener la aprobación.

Lo que bloquea el PR:

- Las pruebas fallan con `./mvnw clean verify`.
- `./mvnw spotless:check` falla.
- Algún commit no sigue la convención de la sección 2.
- El PR no enlaza el requerimiento (`RF-xx` o `RNF-xx`).
- Un criterio de aceptación no tiene prueba automatizada.
- Hay contraseñas o llaves dentro del código.
- Un valor que debe poder cambiarse queda escrito fijo en el código, como la hora de cierre de las reservas (RNF-13).

Lo que no bloquea el PR: preferencias personales sobre nombres o estructura que no rompen ninguna regla de la sección 1, y sugerencias de mejora que no afectan los criterios de aceptación. Se dejan como comentario y el PR se puede integrar.

Los comentarios se escriben en la línea que corresponde del PR y empiezan con un prefijo: `bloqueante:` si hay que corregirlo, `sugerencia:` si es opcional o `nit:` si es un detalle menor. Quien revisa marca la conversación como resuelta cuando el cambio ya está hecho.

## 6. Aceptación

| Nombre completo | Aceptación |
|---|---|
| Danna Giselle Aguilar García | Conozco y acepto estos estándares |
| Alejandro López Rodríguez | Conozco y acepto estos estándares |

código de sesión: `______`
