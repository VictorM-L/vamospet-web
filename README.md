# VMP: Plataforma web para la clínica veterinaria Vamos Pet
 
Proyecto del curso **Curso Integrador I: Sistemas Software** de la Universidad Tecnológica del Perú (UTP).
Docente: Danila Raquel Neira Sacaski. Lima, Perú, 2026.
 
## De qué trata este proyecto
 
Vamos Pet es una clínica veterinaria real que atiende en Miraflores, Lima. Tiene página web, pero la usa sobre todo para mostrar sus servicios y dar información de contacto. Las citas, las consultas y otros trámites se resuelven por WhatsApp.
 
VMP es la plataforma web que hicimos para que esas tareas se puedan hacer en un solo lugar. El dueño de la mascota puede registrarse, sacar citas, revisar la historia de su mascota y comprar productos. El personal de la clínica puede atender, cobrar, controlar el inventario y ver reportes.
 
## El problema que queremos resolver
 
Hoy en Vamos Pet:
 
- Las citas se piden por WhatsApp. Ese canal no muestra qué horarios están libres ni deja un registro ordenado.
- La información de cada mascota está repartida. El veterinario depende de papeles o de lo que el dueño recuerde.
- No hay un hilo claro entre la receta, el pago y la entrega del medicamento. Es difícil saber qué receta permitió cada venta o cuánto stock queda.
- Sin registros juntos, es difícil sacar reportes que ayuden a decidir.
 
## Objetivo
 
Reunir en una sola plataforma la gestión de mascotas, citas, historial clínico, recetas, productos, pagos y administración de Vamos Pet, para que los clientes y el personal tengan la información al alcance.
 
## Para quién es
 
La plataforma tiene cuatro tipos de usuario:
 
| Rol | Qué hace |
|---|---|
| Cliente | Se registra, agrega a sus mascotas, saca citas, revisa su historial y compra productos |
| Veterinario | Atiende las citas, registra consultas, vacunas y recetas |
| Auxiliar de Veterinaria | Registra clientes en la clínica, procesa cobros y ventas en mostrador |
| Administrador | Maneja usuarios, horarios, servicios, inventario, compras y reportes |
 
Un cliente puede crear su cuenta desde la web o ser registrado por la auxiliar en la clínica. Se identifica siempre con su DNI, así no se duplica su historial.
 
## Qué incluye
 
- **Clientes y mascotas:** registro con DNI, inicio de sesión, perfil del cliente y ficha de cada mascota.
- **Citas:** elegir veterinario, fecha y hora entre los horarios libres, con aviso por correo antes de la cita.
- **Atención clínica:** consultas, vacunas, historial de la mascota y recetas digitales.
- **Farmacia y accesorios:** catálogo con carrito de compras, pedidos y control de los productos que piden receta.
- **Pagos y comprobantes:** registro del pago y comprobante en PDF con código QR que se puede verificar.
- **Inventario:** control de stock, movimientos (kardex), proveedores, compras y devoluciones.
- **Reportes:** ingresos, atenciones por veterinario, asistencia a citas, inventario y comprobantes, que se pueden bajar en PDF.
- **Extras:** asistente virtual que da orientación general sobre el cuidado de mascotas (no reemplaza al veterinario) y un blog con artículos.
 
## Cómo está organizado el repositorio
 
- `backend/`: la parte que guarda y procesa los datos.
- `frontend/`: las pantallas que ve el usuario.
- `database/`: los scripts de la base de datos.
- `docs/`: diagramas, capturas y documentos del proyecto.
 
## Estado del proyecto
 
Este repositorio corresponde al **Avance 3** del curso. El sistema sigue en desarrollo, así que algunas funciones pueden estar incompletas.
 
Cosas que no están en esta versión y quedan para más adelante:
 
- Avisos por WhatsApp. Por ahora las notificaciones salen por correo.
- Pago en línea real. El pago se registra en el sistema, pero no se conecta a un banco.
- Boleta electrónica de SUNAT. El comprobante de VMP es de uso interno.
- Aplicación móvil. La plataforma funciona en el navegador.
 
## Integrantes
 
| Integrante | Rol en el proyecto |
|---|---|
| Ramirez Chicoma, Angela Anschely | Líder y analista de negocio |
| García Valera, Alec Adriano | Analista de sistemas |
| López Alzamora, Víctor Manuel | Responsable de base de datos |
| Zanabria Arones, Stefano Fernando | Desarrollador |
| Maldonado Caycho, Yllary Vayolet | Analista de sistemas |



## Requisitos
 
- JDK 25 [confirmar versión exacta]
- PostgreSQL [versión]
- Un servidor estático para el frontend, por ejemplo la extensión Live Server de VS Code
- No hace falta instalar Maven: el proyecto incluye el wrapper (mvnw)
 
## Configuración
 
El backend lee sus claves de variables de entorno. Hay que definirlas antes de ejecutar:
 
| Variable | Para qué sirve |
|---|---|
| DB_PASSWORD | Contraseña del usuario de PostgreSQL |
| MAIL_PASSWORD | Contraseña de aplicación del correo que envía las notificaciones |
| GEMINI_API_KEY | Clave del asistente virtual |
 
[Indicar a quién pedir los valores de prueba. No escribirlos aquí.]
 
## Base de datos
 
1. Crear una base de datos llamada vamospet_db.
2. Ejecutar database/01_esquema.sql.
3. Ejecutar database/02_datos_prueba.sql.
 
[Aclarar si el esquema ya incluye las migraciones de database/MIGRACION_pendiente.sql.]
 
## Cómo ejecutar el backend
 
    cd backend
    ./mvnw spring-boot:run
 
En Windows: mvnw.cmd spring-boot:run. El backend queda en http://localhost:8080.
 
## Cómo ejecutar el frontend
 
Abrir frontend/index.html con un servidor estático (por ejemplo Live Server en 127.0.0.1:5500).
El frontend llama al backend en http://localhost:8080, por lo que el backend debe estar encendido.
 
## Usuarios de prueba
 
| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | [ficticio] | [de prueba] |
| Cliente | [ficticio] | [de prueba] |
| Veterinario | [ficticio] | [de prueba] |
| Auxiliar de Veterinaria | [ficticio] | [de prueba] |
 
## Pruebas
 
    cd backend
    ./mvnw test
 
## Cómo trabajamos en el repositorio
 
- main es la versión estable. No se trabaja directo en ella.
- Cada tarea se hace en una rama (feature/... o docs/...) y se fusiona con un Pull Request.
- Mensajes de commit: feat, fix, docs, refactor, test y chore.
- No se suben credenciales, archivos de configuración local ni datos reales.

