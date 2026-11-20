# Operación

## Perfiles

| Perfil | Cuándo | Qué cambia |
|---|---|---|
| `dev` (por defecto) | `.\mvnw.cmd spring-boot:run` o la IDE | Lee `frontend/` del disco sin caché; logs `DEBUG` de `com.vamospet` y del SQL |
| `prod` | `java -jar vamospet.jar --spring.profiles.active=prod` | Usa la copia del frontend dentro del JAR con caché de 1 hora; logs `INFO` y `WARN` para Spring e Hibernate |
| `test` | Lo activan solas las pruebas | Base H2 en memoria; logs `WARN` |

Los tres leen las claves de `.env.properties` (o de variables de entorno): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `GEMINI_API_KEY`, `JWT_SECRET`.

## Empaquetado

```
cd backend
.\mvnw.cmd clean package
```

Corre todas las pruebas y deja `backend/target/vamospet.jar`, que lleva dentro la API y el frontend (`frontend/` se copia a `static/` al compilar). Para saltar las pruebas en una prueba rápida: `.\mvnw.cmd clean package -DskipTests`.

Para ejecutarlo en otra carpeta (por ejemplo `C:\VamosPet`):

```
C:\VamosPet\vamospet.jar
C:\VamosPet\.env.properties
C:\VamosPet\uploads\            (se crea sola; cópiala del equipo anterior si ya tienes imágenes)
C:\VamosPet\uploads_clinicos\   (archivos de exámenes)
C:\VamosPet\logs\               (se crea sola)

cd C:\VamosPet
java -jar vamospet.jar --spring.profiles.active=prod
```

## Estado del sistema (Actuator)

`GET /actuator/health` responde el estado de la aplicación, la base de datos y el disco. Solo el Administrador puede verlo: sin token responde 401 y con otro rol 403. Ningún otro endpoint de Actuator está publicado. En Postman está en la carpeta `5. Admin`.

## Logs

Configuración en `backend/src/main/resources/logback-spring.xml`.

| Archivo | Contenido | Se guarda |
|---|---|---|
| `logs/vamospet.log` | Todo desde `INFO` | 14 días, máximo 10 MB por archivo y 200 MB en total, comprimidos por día |
| `logs/vamospet-errores.log` | Solo `WARN` y `ERROR` | 30 días |

La carpeta se crea donde se ejecuta el backend. Se puede cambiar con la variable `LOG_DIR`. `logs/` está en `.gitignore` y no va en el ZIP de entrega.
