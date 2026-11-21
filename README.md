# Vamos Pet — Sistema de gestión veterinaria

Plataforma web para la clínica veterinaria Vamos Pet (Miraflores, Lima): citas, historia clínica, exámenes y cirugías, hospedaje y viajes, tienda con farmacia, venta en mostrador, cobros con comprobante y reportes. Cada rol (Administrador, Veterinario, Auxiliar y Cliente) ve y hace solo lo que le corresponde.

## Equipo

| Integrante | GitHub |
|---|---|
| García Valera, Alec Adriano | [GodLie67](https://github.com/GodLie67) |
| Ramírez Chicoma, Angela Anschely | [angelaarc](https://github.com/angelaarc) |
| López Alzamora, Víctor Manuel | [VictorM-L](https://github.com/VictorM-L) |
| Zanabria Arones, Stefano Fernando | [StefanoZanabria](https://github.com/StefanoZanabria) |
| Maldonado Caycho, Yllary Vayolet | [yllarymaldonado898-beep](https://github.com/yllarymaldonado898-beep) |

| Curso | Docente | Sección |
|---|---|---|
| Integrador I | Danila Raquel Neira Sacaski | 49617 |
| Marcos de Desarrollo Web | Franz Bruce Mendoza Arce | 49616 |

Universidad Tecnológica del Perú · Ingeniería de Sistemas e Informática · 6.° ciclo.

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 25, Spring Boot 4.0.5 (Web MVC, Data JPA, Validation, Security, Mail, Actuator) |
| Seguridad | BCrypt, token JWT HS256, matriz de acceso por rol (`PermisosApi`) |
| Base de datos | PostgreSQL 18 (H2 solo en las pruebas) |
| Frontend | HTML, CSS y JavaScript sin frameworks, servido por el mismo JAR |
| Documentos | OpenPDF (comprobantes y reportes), Apache POI (Excel), ZXing (QR) |
| Pruebas | JUnit 5, Mockito, MockMvc, jsdom, Postman |

## Cómo ejecutarlo

Requisitos: JDK 25 y PostgreSQL 18. Maven no hace falta: el proyecto trae `mvnw`.

1. Base de datos: crea `vamospet_db` y ejecuta los scripts de `db/` en orden (ver `db/LEEME.txt`).
2. Credenciales: copia `backend/.env.properties.example` como `backend/.env.properties` y llena los valores. Ese archivo nunca se sube a Git.
3. Arranca en desarrollo:
   ```
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
   Abre `http://localhost:8080`. En desarrollo el servidor lee `frontend/` directamente: guardas un HTML o JS y recargas.
4. Pruebas: `.\mvnw.cmd test` (unitarias e integración, no necesitan PostgreSQL).
5. Producción (JAR):
   ```
   cd backend
   .\mvnw.cmd clean package
   java -jar target\vamospet.jar --spring.profiles.active=prod
   ```
   El `.env.properties` va en la carpeta desde donde ejecutas `java -jar`.

Más detalle en `FICHA_EJECUCION.md` y `docs/OPERACION.md`.

## Estructura

```
backend/      API Spring Boot (controlador, servicio, repositorio, modelo, permisos, seguridad)
frontend/     index.html, css/, js/app.js, vistas/ por rol, vendor/ (fuentes e íconos locales)
db/           scripts SQL numerados; nunca se edita uno ya entregado
docs/         MOF, seguridad, operación, evidencias por etapa
postman/      colección y entorno para probar la API
herramientas/ scripts de PowerShell (Git, Milestones, fotos, respaldo y restauración)
```

## Ramas y contribución

`main` (versión estable), `develop` (integración) y `feature/etapa-N-tema`. Todo cambio entra por Pull Request a `develop` con revisión de otro integrante. Reglas en `CONTRIBUTING.md`; historial en `CHANGELOG.md`.

## Documentación

| Archivo | Contenido |
|---|---|
| `ARQUITECTURA.md` | Capas, seguridad, pruebas y limitaciones |
| `FICHA_EJECUCION.md` | Pasos de instalación y problemas comunes |
| `docs/MOF_VamosPet.md` | Funciones por rol generadas desde la matriz de acceso |
| `docs/SEGURIDAD.md` | Pruebas de inyección SQL, XSS, CSRF, cabeceras y OWASP ZAP |
| `docs/OPERACION.md` | Perfiles, empaquetado, Actuator y logs |
| `postman/LEEME.md` | Cómo correr la colección |
| `docs/PLAN_DE_PRUEBAS.md` | Plan y matriz de casos de prueba de las etapas 1 a 9 |
| `docs/pruebas/` | Plantillas de UAT, usabilidad, accesibilidad y navegadores |
| `docs/RESPALDOS.md` | Respaldo diario, restauración, regla 3-2-1 y mantenimiento |
| `docs/DESPLIEGUE.md` | Instalación en la clínica, verificación y plan de reversa |
| `docs/SUSTENTACION.md` | Lista de verificación y guion de la demostración |
| `docs/INFORME_ESQUEMA.md` | Esquema del informe en 8 secciones |
| `docs/evidencias/` | Capturas por etapa (cada carpeta tiene su LEEME) |
