# Ficha de ejecución — Vamos Pet

| Elemento | Valor |
|---|---|
| Java | JDK 25 (`java -version` debe decir 25) |
| Maven | No hay que instalarlo: el proyecto trae `mvnw` / `mvnw.cmd` (necesita internet la primera vez) |
| Base de datos | PostgreSQL, base `vamospet_db`, puerto 5432, localhost |
| Backend | Puerto **8080** — API en `http://localhost:8080/api` |
| Frontend | Lo sirve el mismo backend: `http://localhost:8080`. En el perfil `dev` lee `frontend/` del disco, así que basta guardar y recargar |
| Enlace front → back | `app.js` usa la dirección del propio servidor (`window.location.origin`), sin `localhost:8080` escrito a mano |
| CORS | Los controladores usan `@CrossOrigin(origins = "*")` |
| Archivos clínicos | Se guardan en `backend/uploads_clinicos/` (NO se sirven como estáticos; solo se descargan por la API con permiso) |
| Credenciales | Fuera del código: archivo local `backend/.env.properties` (no se sube a Git ni va en el ZIP). Modelo sin valores: `backend/.env.properties.example` |
| Fuentes e íconos | Locales en `frontend/vendor/` (Poppins, Fraunces, Font Awesome 6.4.0, con sus licencias). El sitio funciona sin internet, fotos incluidas (`frontend/img/fotos/`); solo el mapa de Google necesita conexión |

## Pasos

```bash
# 1) Base de datos (una sola vez) — ver db/LEEME.txt
createdb -U postgres vamospet_db
psql -U postgres -d vamospet_db -f db/01_esquema_completo.sql
psql -U postgres -d vamospet_db -f db/03_datos_prueba.sql
psql -U postgres -d vamospet_db -f db/03b_datos_prueba_complemento.sql
psql -U postgres -d vamospet_db -f db/06_migracion_mk16.sql      # exámenes, cirugías, emergencias, lotes (también en una base que ya tienes)

psql -U postgres -d vamospet_db -f db/16_bcrypt.sql
psql -U postgres -d vamospet_db -f db/17_roles_postgres.sql
psql -U postgres -d vamospet_db -f db/18_cifrar_claves_pendientes.sql
psql -U postgres -d vamospet_db -f db/19_rutas_relativas.sql
psql -U postgres -d vamospet_db          # dentro de psql: \password app_escritura   y luego \q

# 2) Credenciales: copia el modelo y llena los valores
copy backend\.env.properties.example backend\.env.properties    # Windows
cp backend/.env.properties.example backend/.env.properties       # Linux / macOS

# 3) Fotos del home (una sola vez, con internet)
powershell -ExecutionPolicy Bypass -File herramientas\descargar_fotos.ps1

# 4) Backend  ->  http://localhost:8080
cd backend
.\mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run            # Linux / macOS

# 5) Abre http://localhost:8080 en el navegador (ya no hace falta Live Server)

# 6) JAR para producción: .\mvnw.cmd clean package  y luego
#    java -jar target\vamospet.jar --spring.profiles.active=prod   (ver docs/OPERACION.md)

# Pruebas unitarias (*Test) y de integración (*IT, con H2); no necesitan PostgreSQL
cd backend
.\mvnw.cmd test
```

## Archivo backend/.env.properties

| Variable | Qué va |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/vamospet_db` |
| `DB_USERNAME` | `app_escritura` (o `postgres` mientras no ejecutes el 17) |
| `DB_PASSWORD` | La clave que pusiste con `\password app_escritura` |
| `MAIL_USERNAME` | Correo de Gmail que envía las notificaciones |
| `MAIL_PASSWORD` | Contraseña de aplicación de Gmail (16 letras, sin espacios) |
| `GEMINI_API_KEY` | API key de Google AI Studio |
| `JWT_SECRET` | Texto aleatorio de 32 caracteres o más. Genéralo una vez en PowerShell con `$b = New-Object byte[] 48; [Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($b); [Convert]::ToBase64String($b)` y copia el resultado; no lo compartas. Si lo cambias, todas las sesiones abiertas se cierran |

Una variable por línea, `NOMBRE=valor`, sin comillas. Si un valor tiene `\`, escríbelo como `\\`. Spring lo lee igual desde la terminal, IntelliJ o VS Code: busca `.env.properties` en la carpeta desde donde arranca y en `backend/`. También sirven variables de entorno del sistema con los mismos nombres.

Cuentas de prueba (clave `Demo1234`): `admin@vamospet.test`, `vet1@vamospet.test`, `aux@vamospet.test`, `cliente1@vamospet.test`.

## Problemas comunes

| Síntoma | Causa y solución |
|---|---|
| `release version 25 not supported` | JDK anterior. Instala JDK 25 y revisa `JAVA_HOME`. |
| `JWT_SECRET debe tener al menos 32 caracteres` | Falta `JWT_SECRET` en `backend/.env.properties` o es muy corto. |
| Todo responde 401 después de actualizar | Normal la primera vez: cierra sesión y vuelve a entrar para recibir el token. |
| `Could not resolve placeholder 'DB_URL'` (u otra variable) | Falta `backend/.env.properties` o esa línea. Cópialo del `.example` y llénalo. |
| `Connection refused` al arrancar | PostgreSQL apagado, o `DB_URL` / `DB_PASSWORD` no coinciden. |
| `password authentication failed for user "app_escritura"` | Falta poner la clave con `\password app_escritura` o no coincide con `DB_PASSWORD`. |
| `permission denied for table ...` | La aplicación intentó algo que `app_escritura` no tiene permitido; anota qué hacías y avísame. |
| `Port 8080 was already in use` | Otro programa usa el 8080. Ciérralo o arranca con `--server.port=8081` y cambia las URLs de `app.js`. |
| El sitio carga pero sin datos | El backend no corre, o abriste el HTML con doble clic (`file://`) o con Live Server. Ábrelo en `http://localhost:8080`. |
| `Demasiados intentos fallidos` al entrar | 5 claves erradas seguidas bloquean ese correo 15 minutos. Espera, recupera la contraseña o reinicia el backend. |
| `relation "xxx" does not exist` | No cargaste `db/01_esquema_completo.sql` (Hibernate no crea tablas: `ddl-auto=none`). |
| No llegan correos / el asistente no responde | Faltan o son inválidas las credenciales de Gmail / Gemini. |
| Logs | Carpeta `backend/logs/` (`vamospet.log` y `vamospet-errores.log`). |
