# Cambios de Vamos Pet

Una entrada por entrega (MK). Cada una dice qué cambió, qué scripts SQL trae y qué pruebas agrega.

## MK32 — Etapa 9, parte 2: pruebas, respaldos, despliegue y documentación
- Plan de pruebas con matriz de 71 casos (etapas 1 a 9) y plantillas de UAT, usabilidad (SUS), accesibilidad y navegadores.
- Respaldos: `herramientas/respaldo.ps1` (pg_dump con `app_lectura` y `pgpass.conf`, uploads y uploads_clinicos, carpeta con fecha, manifiesto con SHA-256, rotación de 14 días, segunda carpeta opcional), `registrar_tarea_respaldo.ps1` (tarea diaria a las 23:00) y `restaurar.ps1` (restaura en una base nueva y compara filas; con `-Activar` reemplaza la actual sin borrarla).
- Documentos: `RESPALDOS.md`, `DESPLIEGUE.md`, `SUSTENTACION.md`, `INFORME_ESQUEMA.md` y un LEEME de capturas en cada `docs/evidencias/etapa-N`.
- Mensaje de falta de stock con el producto, lo pedido, lo disponible para la venta y las unidades vencidas.
- Postman: la venta en mostrador elige un producto con lote vigente y salta la venta si no hay ninguno.
- `db/07_datos_pa'rellenar.sql` pasa a `db/07_datos_para_rellenar.sql` (mismo contenido; revisado en `db/LEEME.txt`).
- `CONTRIBUTING.md` con el prefijo `refactor:`; scripts de PowerShell guardados en UTF-8 con BOM.
- Pruebas: `VentaServiceTest.sinStockPorVencimiento`, `InventarioServiceTest.todoVencido` y `mensajeFaltante`.

## MK31 — Etapa 9, parte 1: Git, pruebas de seguridad, Postman y operación
- El JAR sirve también el frontend: todo se abre en `http://localhost:8080`. `app.js` toma la dirección de la API del propio servidor (ya no tiene `localhost:8080` escrito).
- Perfiles `dev` (por defecto; lee `frontend/` en vivo) y `prod` (usa la copia dentro del JAR). `mvn clean package` genera `backend/target/vamospet.jar`.
- `/actuator/health` solo para el Admin con token (401 sin token, 403 con otro rol). El resto de Actuator no se publica.
- Bloqueo del login: 5 intentos fallidos bloquean ese correo 15 minutos, con aviso de los intentos que quedan. Se libera al recuperar la contraseña.
- Cabeceras: política de contenido ajustada a la web servida por el JAR, `X-Content-Type-Options: nosniff` activo.
- Logback con niveles por perfil (`dev`, `prod`, `test`).
- Colección y entorno de Postman (37 peticiones en 7 carpetas).
- Repositorio: README, CONTRIBUTING, plantillas de Pull Request e Issues, script para subir la entrega a `develop` y script de Milestones.
- SQL: `db/19_rutas_relativas.sql` (imágenes y recetas guardadas con `http://localhost:8080/uploads/...` pasan a `/uploads/...`).
- Pruebas: `SeguridadIT` (inyección SQL, XSS, CSRF sin sesión, cabeceras, bloqueo del login, Actuator, frontend servido por el JAR) e `IntentosLoginServiceTest`.

## MK30 — Identidad tomada del token en todos los controladores
- 30 endpoints de 12 controladores leían quién pregunta de `usuarioId` en la URL; ahora usan el token.
- Pruebas: `IdentidadPorTokenIT` y `MatrizAccesoIT.ningunaIdentidadPorParametro`.

## MK29 — Etapa 8, parte 2: token JWT y sesión en el servidor
- Token HS256 firmado con `JWT_SECRET`; cliente 20 minutos con renovación por actividad, personal 8 horas; cerrar sesión invalida el token.
- La matriz de acceso devuelve 401 sin token y 403 por rol; se retira la cabecera `X-Usuario-Id`.
- PDF y archivos se descargan con el token. Se retiró el video de laboratorio y la sección Equipo de Nosotros.
- SQL: `db/18_cifrar_claves_pendientes.sql`.
- Pruebas: `JwtServiceTest`, `SesionIT`; todas las pruebas de integración pasan a usar token.

## MK28 — Etapa 8, parte 1: contraseñas, claves y roles de base de datos
- BCrypt para todas las contraseñas; regla de 8 a 64 caracteres con letra y número, igual en la página y en el servidor.
- Claves fuera del código en `backend/.env.properties`.
- Fuentes e íconos locales; reemplazo automático de imágenes caídas.
- Cancelar por estado un pedido cobrado o Entregado pide usar una devolución.
- SQL: `db/16_bcrypt.sql`, `db/17_roles_postgres.sql`.

## MK27 — Etapa 7: paleta verde y cancelación con stock
- Botones índigo y azules pasan a la paleta verde.
- Cancelar un pedido devuelve el stock; listado de ventas de mostrador sin cobrar con botón Anular.
- `.\mvnw.cmd test` corre también las pruebas `*IT`.

## MK26 — Venta en mostrador sin cobrar
- Cerrar el cobro sin pagar anula la venta, devuelve el stock y libera la receta.

## MK25 — Etapa 6: venta con receta
- Venta atómica con precios de la base, una receta por compra, vigencia de 30 días y uso único.
- Borrados seguros con mensaje claro.
- SQL: `db/14_verificar_huerfanos.sql`, `db/15_receta_fk.sql`.

## MK24 — Etapa 5: contacto y configuración de la clínica
- Bandeja de mensajes de contacto con respuesta por correo; configuración de datos y horarios de la clínica.
- SQL: `db/13_contacto_config.sql`.

## MK23 — Etapa 4: banners de la tienda
- Banners de Farmacia y Accesorios; imagen de reemplazo para productos sin foto.
- SQL: `db/12_banner.sql`.

## MK22 — Etapa 3: cirugías, grooming y viajes
- Tablero de cirugías por pasos, agenda de grooming, requisitos y documentos de viaje.
- SQL: `db/11_duracion_cirugia.sql`.

## MK1–MK21: versiones previas
