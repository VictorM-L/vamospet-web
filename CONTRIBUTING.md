# Cómo trabajamos en el repositorio

## Ramas

| Rama | Para qué | Quién la toca |
|---|---|---|
| `main` | Versión estable que se presenta | Solo por Pull Request desde `develop`, al cerrar una entrega |
| `develop` | Integración de lo terminado | Solo por Pull Request desde una `feature/*` |
| `feature/etapa-N-tema` | Trabajo en curso (ej. `feature/etapa-9-respaldos`) | Quien tiene asignado el Issue |

Nunca se hace `push` directo a `main` ni a `develop`.

## Commits

Mensaje corto en español con un prefijo:

| Prefijo | Uso |
|---|---|
| `feat:` | Función nueva |
| `fix:` | Corrección de un error |
| `test:` | Pruebas nuevas o ajustadas |
| `docs:` | Documentación |
| `chore:` | Configuración, scripts, dependencias |

Ejemplo: `fix: el login devuelve 429 al quinto intento fallido`.

## Pull Request

1. Sube tu rama y abre el PR hacia `develop` usando la plantilla.
2. Asigna al menos un revisor del equipo que no haya escrito el cambio.
3. El revisor marca la lista de la plantilla, prueba en su equipo y aprueba o pide cambios.
4. Se fusiona solo con `.\mvnw.cmd test` en verde y la aprobación.

## Reglas del proyecto

- Cero comentarios en el código (Java, JS, HTML, SQL).
- Nada de claves en el repositorio: van en `backend/.env.properties`, que está en `.gitignore`.
- Scripts SQL nuevos con el siguiente número en `db/`; nunca se edita uno ya entregado.
- Cada regla de negocio nueva lleva su prueba.
- No se muestran datos simulados en el frontend.
- Cada entrega suma una entrada en `CHANGELOG.md`.

## Issues y Milestones

Un Milestone por etapa (`herramientas/github_milestones.ps1` los crea). Cada tarea es un Issue con su plantilla y etiqueta; los errores usan la plantilla "Error encontrado".
