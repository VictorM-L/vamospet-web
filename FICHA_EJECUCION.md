# Ficha de ejecución — Vamos Pet

| Elemento | Valor |
|---|---|
| Java | JDK 25 (`java -version` debe decir 25) |
| Maven | No hay que instalarlo: el proyecto trae `mvnw` / `mvnw.cmd` (necesita internet la primera vez) |
| Base de datos | PostgreSQL, base `vamospet_db`, puerto 5432, localhost |
| Backend | Puerto **8080** — API en `http://localhost:8080/api` |
| Frontend | Extensión **Live Server** de VS Code (puerto 5500): `http://127.0.0.1:5500/frontend/index.html` |
| Enlace front → back | `frontend/js/app.js` apunta a `http://localhost:8080` (`API_URL` y `API_BASE`; la subida de imágenes usa `API_BASE`) |
| CORS | Los controladores usan `@CrossOrigin(origins = "*")` |
| Archivos clínicos | Se guardan en `backend/uploads_clinicos/` (NO se sirven como estáticos; solo se descargan por la API con permiso) |
| Credenciales | Correo (Gmail) y Gemini se configuran en `backend/src/main/resources/application.properties` |

## Pasos

```bash
# 1) Base de datos (una sola vez) — ver db/LEEME.txt
createdb -U postgres vamospet_db
psql -U postgres -d vamospet_db -f db/01_esquema_completo.sql
psql -U postgres -d vamospet_db -f db/03_datos_prueba.sql
psql -U postgres -d vamospet_db -f db/03b_datos_prueba_complemento.sql
psql -U postgres -d vamospet_db -f db/06_migracion_mk16.sql      # exámenes, cirugías, emergencias, lotes (también en una base que ya tienes)

# 2) Backend  ->  http://localhost:8080
cd backend
.\mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run            # Linux / macOS

# 3) Frontend: abrir la carpeta del proyecto en VS Code,
#    clic derecho en frontend/index.html -> "Open with Live Server"

# Pruebas (no necesitan PostgreSQL)
cd backend
.\mvnw.cmd test
```

Cuentas de prueba (clave `Demo1234`): `admin@vamospet.test`, `vet1@vamospet.test`, `aux@vamospet.test`, `cliente1@vamospet.test`.

## Problemas comunes

| Síntoma | Causa y solución |
|---|---|
| `release version 25 not supported` | JDK anterior. Instala JDK 25 y revisa `JAVA_HOME`. |
| `Connection refused` al arrancar | PostgreSQL apagado, o puerto/clave de `application.properties` no coinciden. |
| `Port 8080 was already in use` | Otro programa usa el 8080. Ciérralo o arranca con `--server.port=8081` y cambia las URLs de `app.js`. |
| El sitio carga pero sin datos | El backend no corre, o abriste el HTML con doble clic (`file://`) en vez de Live Server. |
| `relation "xxx" does not exist` | No cargaste `db/01_esquema_completo.sql` (Hibernate no crea tablas: `ddl-auto=none`). |
| No llegan correos / el asistente no responde | Faltan o son inválidas las credenciales de Gmail / Gemini. |
| Logs | Carpeta `backend/logs/` (`vamospet.log` y `vamospet-errores.log`). |



*Nota: Documentación en proceso de actualización.*