# VamosPet - Sistema de Gestión Veterinaria
Proyecto web desarrollado para la administración de historias clínicas, citas, inventario y facturación de la clínica veterinaria VamosPet.

## Tecnologías
* **Backend:** Java, Spring Boot
* **Frontend:** HTML, CSS, JavaScript (Vanilla)
* **Base de Datos:** PostgreSQL

## Requisitos
- JDK 25
- PostgreSQL 16
- Un servidor estático para el frontend Live Server
- No hace falta instalar Maven: el proyecto incluye el wrapper (mvnw)

## Configuración
El backend lee sus claves de variables de entorno. Hay que definirlas antes de ejecutar:
| Variable | Para qué sirve |
|---|---|
| **** | Contraseña del usuario de PostgreSQL |
| **** | Contraseña de aplicación del correo que envía las notificaciones |
| **** | Clave del asistente virtual |

(Solicitar los valores de prueba al administrador del repositorio).

## Base de datos
1. Crear una base de datos llamada `vamospet_db`.
2. Ejecutar `db/01_esquema.sql`.
3. Ejecutar `db/02_datos_prueba.sql`.
(Nota: El esquema actual ya incluye todas las migraciones previas).

## Cómo ejecutar el backend
    cd backend
    ./mvnw spring-boot:run
(En Windows: `mvnw.cmd spring-boot:run`). El backend queda en http://localhost:8080.

## Cómo ejecutar el frontend
Abrir `frontend/index.html` con un servidor estático (por ejemplo Live Server en 127.0.0.1:5500). El frontend llama al backend en http://localhost:8080, por lo que el backend debe estar encendido.

## Usuarios de prueba
| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | [admin@vamospet.com] | [clave] |
| Cliente | [juan@gmail.com] | [123456] |
| Veterinario | [c@gmail.com] | [123456] |
| Auxiliar | [t@gmail.com] | [clave] |

## Pruebas
    cd backend
    ./mvnw test

## Cómo trabajamos en el repositorio
- `main` es la versión estable. No se trabaja directo en ella.
- Cada tarea se hace en una rama (`feature/...` o `docs/...`) y se fusiona con un Pull Request.
- Mensajes de commit: feat, fix, docs, refactor, test y chore.
- No se suben credenciales, archivos de configuración local ni datos reales.