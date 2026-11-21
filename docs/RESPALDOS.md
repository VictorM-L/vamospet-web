# Respaldos y restauración

## Qué se respalda

| Qué | Dónde está | Cómo se copia |
|---|---|---|
| Base de datos `vamospet_db` | PostgreSQL 18 | `pg_dump` en formato personalizado (`.backup`), con el usuario de solo lectura `app_lectura` |
| Imágenes de productos, banners y recetas | `uploads/` de la carpeta de la aplicación | `uploads.zip` |
| Archivos de exámenes | `uploads_clinicos/` | `uploads_clinicos.zip` |

Cada respaldo queda en una carpeta con fecha y hora, por ejemplo `C:\Respaldos\VamosPet\vamospet_2026-11-21_230000\`, con un `manifiesto.txt` que lista cada archivo con su tamaño y su huella SHA-256. Se guardan los últimos 14 días; los más antiguos se borran solos. El historial de cada ejecución queda en `C:\Respaldos\VamosPet\respaldo.log`.

La contraseña nunca está en los scripts: `pg_dump` la lee de `pgpass.conf`.

## 1. Preparación (una sola vez)

### 1.1 Archivo `pgpass.conf`

1. Abre la carpeta `%APPDATA%\postgresql` (pégalo en la barra del Explorador). Si no existe, créala.
2. Crea o edita `pgpass.conf` con una línea:
   ```
   localhost:5432:vamospet_db:app_lectura:LA_CLAVE_DE_APP_LECTURA
   ```
   El formato es `servidor:puerto:base:usuario:contraseña`. Si la clave tiene `:` o `\`, escríbelos como `\:` y `\\`.
3. Deja el archivo visible solo para tu usuario: clic derecho → Propiedades → Seguridad → Opciones avanzadas → "Deshabilitar herencia" → quita a todos menos a tu usuario.

Este archivo está fuera del proyecto, así que nunca llega a Git ni al ZIP.

### 1.2 Comprobar que `app_lectura` puede respaldar

```powershell
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -h localhost -U app_lectura -d vamospet_db -w -F c -f "$env:TEMP\prueba.backup"
```

Si no muestra nada y crea el archivo, está listo. Si falla, revisa la sección 6.

### 1.3 Primer respaldo a mano

Desde la carpeta del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File herramientas\respaldo.ps1
```

Por defecto respalda los archivos de `backend\` (donde se crean `uploads/` al arrancar con `mvnw`). Si usas el JAR en otra carpeta:

```powershell
powershell -ExecutionPolicy Bypass -File herramientas\respaldo.ps1 -CarpetaApp C:\VamosPet
```

Con una segunda carpeta (disco externo, USB o carpeta sincronizada con la nube):

```powershell
powershell -ExecutionPolicy Bypass -File herramientas\respaldo.ps1 -SegundaCarpeta E:\RespaldosVamosPet
```

| Parámetro | Valor por defecto |
|---|---|
| `-CarpetaApp` | `backend\` del proyecto |
| `-Destino` | `C:\Respaldos\VamosPet` |
| `-SegundaCarpeta` | vacío (no copia) |
| `-Dias` | `14` |
| `-PgBin` | `C:\Program Files\PostgreSQL\18\bin` |
| `-BaseDatos` / `-Usuario` | `vamospet_db` / `app_lectura` |

### 1.4 Tarea programada a las 23:00

En PowerShell, desde la carpeta del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File herramientas\registrar_tarea_respaldo.ps1
```

Con JAR y segunda carpeta:

```powershell
powershell -ExecutionPolicy Bypass -File herramientas\registrar_tarea_respaldo.ps1 -CarpetaApp C:\VamosPet -SegundaCarpeta E:\RespaldosVamosPet
```

- La tarea se llama **VamosPet - Respaldo diario** y la ves en el Programador de tareas de Windows.
- Corre con tu usuario, así encuentra tu `pgpass.conf`.
- Si el equipo está apagado a las 23:00, corre en cuanto se encienda.
- Por defecto solo corre si tu sesión de Windows está abierta. Para que corra también con la sesión cerrada, abre PowerShell **como administrador** y agrega `-SinSesion`.
- Para probarla ahora: `Start-ScheduledTask -TaskName 'VamosPet - Respaldo diario'` y revisa `respaldo.log`.
- Para quitarla: `herramientas\registrar_tarea_respaldo.ps1 -Quitar`.

## 2. Si un respaldo falla

- `respaldo.log` dice `ERROR:` con el motivo y la carpeta queda renombrada como `..._fallido` (no se confunde con un respaldo bueno; se borra sola con la rotación).
- La tarea programada termina con resultado `1`; en el Programador de tareas se ve en "Resultado de la última ejecución".
- Mensajes comunes:

| Mensaje | Causa | Solución |
|---|---|---|
| `No se encontró ...pgpass.conf` | Falta el archivo | Paso 1.1 |
| `fe_sendauth: no password supplied` | La línea de `pgpass.conf` no coincide | Revisa servidor `localhost`, puerto, base y usuario exactos |
| `password authentication failed` | Clave equivocada | Corrige la clave en `pgpass.conf` |
| `permission denied for table ...` o `for sequence ...` | A `app_lectura` le falta permiso | Sección 6 |
| `No se encontró ...pg_dump.exe` | PostgreSQL en otra carpeta | Usa `-PgBin` |

## 3. Prueba de restauración (hacerla al menos una vez al mes)

La restauración **no toca** tu base actual: crea una base nueva, `vamospet_restaurada`, y compara las filas de cada tabla.

```powershell
powershell -ExecutionPolicy Bypass -File herramientas\restaurar.ps1 -Respaldo C:\Respaldos\VamosPet\vamospet_2026-11-21_230000
```

1. Te pide la contraseña de `postgres` (solo mientras corre; no se guarda).
2. Crea `vamospet_restaurada` y carga el respaldo con `pg_restore`.
3. Muestra una tabla con las filas de cada tabla en la restaurada y en la actual, y la guarda en `verificacion_restauracion_<fecha>.txt` dentro de la carpeta del respaldo.
4. Descomprime `uploads.zip` y `uploads_clinicos.zip` en `archivos_restaurados\` dentro de la misma carpeta, para revisarlos.

**Qué revisar (evidencia para el informe):**
- Los conteos coinciden con la base actual, salvo lo que se haya creado después de la hora del respaldo.
- En pgAdmin, `vamospet_restaurada` tiene las 44 tablas y se pueden ver usuarios, citas y productos.
- Las imágenes de `archivos_restaurados\uploads` se abren.

Al terminar, borra la base de prueba desde pgAdmin (clic derecho sobre `vamospet_restaurada` → Delete) para la siguiente prueba.

### Restaurar de verdad (cuando se perdió la base)

1. Detén el backend.
2. Ejecuta:
   ```powershell
   powershell -ExecutionPolicy Bypass -File herramientas\restaurar.ps1 -Respaldo <carpeta> -CarpetaApp C:\VamosPet -Activar
   ```
3. Escribe `vamospet_db` cuando te lo pida. La base actual **no se borra**: queda renombrada como `vamospet_db_anterior_<fecha>` y la restaurada pasa a llamarse `vamospet_db`.
4. Con `-CarpetaApp`, los archivos de los zip se copian a `uploads\` y `uploads_clinicos\` de esa carpeta (reemplaza los que tengan el mismo nombre; no borra otros).
5. Ejecuta `db\17_roles_postgres.sql` sobre la base para asegurar los permisos de `app_escritura` y `app_lectura`.
6. Arranca el backend y revisa `/actuator/health` (Admin) e inicia sesión con cada rol.

## 4. Regla 3-2-1

| Regla | Qué pide | Cómo se cumple en Vamos Pet | Estado |
|---|---|---|---|
| **3** copias | Los datos originales y dos copias | 1) La base en PostgreSQL y las carpetas `uploads` · 2) `C:\Respaldos\VamosPet` · 3) La segunda carpeta (`-SegundaCarpeta`) | **Falta la tercera** hasta definir la segunda carpeta |
| **2** soportes distintos | Las copias no pueden estar todas en el mismo disco | Hoy la base y `C:\Respaldos` están en el **mismo disco**: si el disco falla se pierden ambas | **Falta**: la segunda carpeta debe estar en un disco externo o USB |
| **1** fuera del local | Una copia en otro lugar físico | Una carpeta sincronizada con OneDrive o Google Drive, o un USB que se lleva a casa una vez por semana | **Falta** |

Con una sola decisión se cubren las tres: usar como `-SegundaCarpeta` una carpeta de OneDrive o Google Drive para escritorio (copia fuera del local y en otro soporte), o un disco externo que una persona se lleve a casa cada semana.

Los respaldos incluyen datos personales y clínicos: la carpeta en la nube debe ser de una cuenta de la clínica, con verificación en dos pasos y sin compartir.

## 5. Plan de mantenimiento

| Frecuencia | Tarea | Responsable |
|---|---|---|
| Diaria (automática) | Respaldo de las 23:00 | Tarea programada |
| Semanal | Leer `respaldo.log`: debe terminar en "Respaldo terminado sin errores" cada día | Auxiliar o encargado de sistemas |
| Semanal | Si la segunda carpeta es un USB, llevarlo fuera del local | Encargado |
| Mensual | Prueba de restauración (sección 3) y guardar el `verificacion_restauracion_*.txt` | Encargado de sistemas |
| Mensual | Revisar el espacio libre del disco y el tamaño de `logs/` | Encargado de sistemas |
| Mensual | Dar de baja los lotes vencidos en Inventario | Auxiliar |
| Cada actualización (MK) | Respaldo a mano **antes** de ejecutar scripts nuevos de `db/` | Quien actualiza |
| Cada 6 meses | Cambiar las claves de `app_lectura`, `app_escritura` y Gmail; actualizar `pgpass.conf` y `.env.properties` | Administrador |

## 6. Si `pg_dump` falla por falta de permisos

`app_lectura` necesita permiso de lectura en el esquema, en todas las tablas y en todas las secuencias. `db/17_roles_postgres.sql` ya lo da y deja el permiso automático para las tablas nuevas que cree el usuario `postgres`. Si aparece `permission denied`, abre psql como `postgres` sobre la base y ejecuta:

```sql
GRANT CONNECT ON DATABASE vamospet_db TO app_lectura;
GRANT USAGE ON SCHEMA public TO app_lectura;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO app_lectura;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO app_lectura;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO app_lectura;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO app_lectura;
```

Casos particulares:

| Mensaje | Qué ejecutar |
|---|---|
| `permission denied for schema public` | `GRANT USAGE ON SCHEMA public TO app_lectura;` |
| `permission denied for table X` | `GRANT SELECT ON X TO app_lectura;` |
| `permission denied for sequence X` | `GRANT SELECT ON SEQUENCE X TO app_lectura;` |
| Vuelve a fallar con tablas nuevas | Esas tablas las creó otro usuario. Ejecuta las dos líneas `ALTER DEFAULT PRIVILEGES` agregando `FOR ROLE ese_usuario` después de `ALTER DEFAULT PRIVILEGES`, o crea las tablas siempre como `postgres` |

Para ver qué tablas o secuencias no puede leer `app_lectura`:

```sql
SELECT c.relname AS tabla
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind IN ('r', 'S') AND NOT has_table_privilege('app_lectura', c.oid, 'SELECT');
```

Debe devolver 0 filas.
