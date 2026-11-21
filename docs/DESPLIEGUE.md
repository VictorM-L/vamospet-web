# Plan de despliegue

Vamos Pet todavía **no se publica en internet**. Este plan describe cómo instalarlo en un equipo de la clínica (servidor local) para usarlo dentro de la red del local, y qué haría falta para publicarlo más adelante.

## 1. Requisitos del equipo servidor

| Elemento | Mínimo |
|---|---|
| Sistema operativo | Windows 10/11 de 64 bits |
| Memoria | 8 GB de RAM (el backend usa unos 500 MB) |
| Disco | 20 GB libres para la base, los archivos y 14 días de respaldos |
| Java | JDK 25 (`java -version` debe decir 25) |
| Base de datos | PostgreSQL 18 con pgAdmin |
| Red | IP fija dentro del local (configurada en el router) si otros equipos van a entrar |
| Respaldo | Un disco externo o una carpeta sincronizada con la nube para la segunda copia |

## 2. Variables de entorno

Van en `C:\VamosPet\.env.properties`, en la misma carpeta que el JAR. El modelo sin valores es `backend/.env.properties.example`.

| Variable | Valor en el servidor |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/vamospet_db` |
| `DB_USERNAME` | `app_escritura` |
| `DB_PASSWORD` | Clave de `app_escritura` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Correo de la clínica y su contraseña de aplicación de Gmail |
| `GEMINI_API_KEY` | API key del asistente |
| `JWT_SECRET` | Texto aleatorio de 48 caracteres o más, distinto del de desarrollo |

El archivo se protege igual que `pgpass.conf`: solo lo puede leer el usuario que corre el backend. Nunca se copia a Git, al ZIP ni a la carpeta de respaldos.

## 3. Instalación paso a paso

### 3.1 Base de datos

1. Instala PostgreSQL 18 y crea la base:
   ```
   createdb -U postgres vamospet_db
   ```
2. Ejecuta los scripts de `db/` **en este orden**, como `postgres`:

   | Orden | Script | Para qué |
   |---|---|---|
   | 1 | `01_esquema_completo.sql` | Tablas |
   | 2 | `03_datos_prueba.sql`, `03b_datos_prueba_complemento.sql` | Solo si se quieren datos de demostración |
   | 3 | `06_migracion_mk16.sql` | Exámenes, cirugías, emergencias, lotes |
   | 4 | `08_migracion_mk19.sql` a `15_receta_fk.sql`, uno por uno | Migraciones de las etapas 0 a 6 (ejecuta `14_verificar_huerfanos.sql` antes del 15; debe devolver 0 filas) |
   | 5 | `16_bcrypt.sql` | Columna de contraseñas |
   | 6 | `17_roles_postgres.sql` | Usuarios `app_escritura` y `app_lectura`; luego `\password` a cada uno |
   | 7 | `07_datos_para_rellenar.sql` | Solo en una base de **demostración** (ver `db/LEEME.txt`) |
   | 8 | `18_cifrar_claves_pendientes.sql` | Cifra cualquier clave en texto plano de los datos de prueba |
   | 9 | `19_rutas_relativas.sql` | Rutas de imágenes |

   En una instalación real **sin** datos de demostración se omiten los pasos 2 y 7, y el primer Administrador se crea con `10_roles_y_cuentas_prueba.sql` (y luego se le cambia la clave) o desde pgAdmin.

### 3.2 Aplicación

1. En el equipo de desarrollo, genera el JAR:
   ```
   cd backend
   .\mvnw.cmd clean package
   ```
   Queda `backend\target\vamospet.jar` (API y web juntas).
2. En el servidor crea `C:\VamosPet\` y copia ahí `vamospet.jar` y `.env.properties`.
3. Si ya había imágenes, copia también `uploads\` y `uploads_clinicos\`.
4. Arranca:
   ```
   cd C:\VamosPet
   java -jar vamospet.jar --spring.profiles.active=prod
   ```
5. Para que arranque solo al encender el equipo, crea una tarea en el Programador de tareas con el desencadenador "Al iniciar el sistema" que ejecute `java` con esos argumentos y la carpeta `C:\VamosPet` como directorio de inicio.

### 3.3 Respaldos

Sigue `docs/RESPALDOS.md` con `-CarpetaApp C:\VamosPet` y registra la tarea de las 23:00.

## 4. Verificación después de instalar

| N.° | Comprobación | Resultado esperado |
|---|---|---|
| 1 | En la consola del backend | `Started BackendApplication` y el perfil `prod` activo, sin errores |
| 2 | `http://localhost:8080` | Se ve la página de inicio con fuentes e íconos |
| 3 | Login del Admin en Postman y `GET /actuator/health` con su token | `{"status":"UP"}`, con `db` y `diskSpace` en `UP` |
| 4 | `GET /actuator/health` sin token | 401 |
| 5 | Iniciar sesión con cada rol | Cada uno ve su panel |
| 6 | Vender en mostrador y anular | El stock vuelve |
| 7 | `C:\VamosPet\logs\vamospet.log` | Se está escribiendo |
| 8 | Primer respaldo a mano | `respaldo.log` termina sin errores |
| 9 | Desde otro equipo de la red: `http://IP-del-servidor:8080` | Carga la web (si no, permite el puerto 8080 en el Firewall de Windows) |

## 5. Actualizar a una versión nueva (MK)

1. Avisar a los usuarios y elegir un horario sin atención.
2. **Respaldo a mano** con `herramientas\respaldo.ps1` y comprobar que terminó sin errores.
3. Guardar el JAR actual como `vamospet_anterior.jar`.
4. Detener el backend (cerrar su ventana o detener la tarea).
5. Ejecutar solo los scripts nuevos de `db/` que trae la versión (ver `CHANGELOG.md`).
6. Copiar el JAR nuevo y arrancar.
7. Repetir la verificación de la sección 4.

## 6. Plan de reversa

Si después de actualizar algo no funciona y no se puede corregir en el momento:

| Situación | Qué hacer |
|---|---|
| El backend no arranca y no hubo scripts nuevos de `db/` | Detener, volver a `vamospet_anterior.jar` y arrancar |
| El backend no arranca por una variable | Revisar `.env.properties` con el mensaje de la consola; no tocar la base |
| Se ejecutó un script de `db/` y falla | Detener el backend y restaurar el respaldo del paso 2 con `herramientas\restaurar.ps1 -Respaldo <carpeta> -CarpetaApp C:\VamosPet -Activar`. La base con problemas queda guardada como `vamospet_db_anterior_<fecha>` para revisarla. Luego volver al JAR anterior |
| Faltan imágenes o archivos | Descomprimir `uploads.zip` y `uploads_clinicos.zip` del último respaldo en `C:\VamosPet` |

Después de una reversa se anota en un Issue qué falló, con la hora y el mensaje del log.

## 7. Si más adelante se publica en internet

No forma parte de este proyecto, pero haría falta:

- Un servidor o VPS con dominio y certificado HTTPS (por ejemplo con un proxy como Nginx o Caddy delante del JAR).
- Cerrar el puerto 5432 de PostgreSQL al exterior.
- Limitar `@CrossOrigin` al dominio propio.
- Guardar el bloqueo del login y los tokens cerrados en la base (hoy están en memoria).
- Copia fuera del local automática (regla 3-2-1) y monitoreo de `/actuator/health`.
- Revisar el escaneo de OWASP ZAP contra el dominio publicado.
