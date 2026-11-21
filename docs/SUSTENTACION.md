# Sustentación — lista de verificación y guion de demostración

## 1. Un día antes

- [ ] `develop` fusionado en `main` con su Pull Request aprobado; etiqueta de versión creada (`git tag v1.0` y `git push origin v1.0`).
- [ ] `.\mvnw.cmd test` en verde; captura en `docs/evidencias/etapa-9`.
- [ ] JAR generado con `.\mvnw.cmd clean package` y probado en perfil `prod`.
- [ ] Respaldo a mano hecho y probado con `restaurar.ps1` (por si la demostración daña datos).
- [ ] Base con datos de demostración: cuentas `*@vamospet.test`, productos con lote vigente, al menos una cita pendiente, una confirmada y una receta aprobada.
- [ ] Lotes vencidos dados de baja (para que la venta no falle en vivo).
- [ ] Postman con la colección y el entorno importados, y el Runner probado.
- [ ] Correo de Gmail del equipo funcionando (códigos y comprobantes).
- [ ] Fotos del home descargadas (`herramientas\descargar_fotos.ps1`).
- [ ] Informe y presentación con las evidencias de cada etapa.
- [ ] Cada integrante sabe qué parte presenta y qué rol maneja en la demostración.

## 2. El día de la sustentación (15 minutos antes)

- [ ] PostgreSQL encendido; backend arrancado con `java -jar vamospet.jar --spring.profiles.active=prod`.
- [ ] Abrir `http://localhost:8080` en una ventana normal y otra de incógnito (para tener dos roles a la vez).
- [ ] Zoom del navegador al 125 % para que se lea en el proyector.
- [ ] Notificaciones del sistema desactivadas.
- [ ] Ninguna pestaña o ventana con `.env.properties`, `pgpass.conf` o claves.
- [ ] Plan B: video corto grabado de la demostración completa, por si falla la red o el equipo.

## 3. Guion de demostración (unos 15 minutos)

| Min. | Quién | Rol en pantalla | Qué se muestra | Qué se dice |
|---|---|---|---|---|
| 0–1 | Integrante 1 | Visitante | Inicio, servicios, tienda en cuadrícula, banners | El problema de la clínica y lo que resuelve el sistema |
| 1–3 | Integrante 1 | Cliente | Registro con validaciones en vivo, inicio de sesión, registrar mascota, agendar una cita con especialista | Validación igual en la página y en el servidor; agenda sin choques de horario |
| 3–5 | Integrante 2 | Veterinario | Confirmar la cita con un clic, registrar la atención, pedir examen, tablero de cirugías | Cada rol ve solo lo suyo; la cirugía avanza paso a paso |
| 5–7 | Integrante 3 | Auxiliar | Venta en mostrador con receta, cobro y comprobante PDF con QR; bandeja de contacto | La receta es obligatoria y de un solo uso; el stock vencido no se vende |
| 7–8 | Integrante 3 | Cliente | Comprobante en "Mis pagos" y verificación por QR | Comprobante verificable |
| 8–10 | Integrante 4 | Admin | Personal (restablecimiento), configuración, reporte de ingresos en Excel | El Admin supervisa; nunca ve contraseñas |
| 10–12 | Integrante 4 | Postman | Runner con la carpeta 6 (401, 403, inyección SQL), `/actuator/health` | Token JWT, matriz de 183 operaciones, pruebas de seguridad |
| 12–14 | Integrante 5 | Consola y GitHub | `.\mvnw.cmd test` (o su captura), Pull Request con revisión, respaldo y su log | 357 pruebas, flujo con ramas, respaldo diario y prueba de restauración |
| 14–15 | Integrante 5 | — | Cierre: lo logrado, límites conocidos y siguientes pasos | Lo que haría falta para publicarlo (`docs/DESPLIEGUE.md`, sección 7) |

Asigna los nombres reales en la columna "Quién" según el reparto del equipo.

## 4. Preguntas probables y dónde está la respuesta

| Pregunta | Respuesta corta | Documento |
|---|---|---|
| ¿Cómo guardan las contraseñas? | BCrypt; nunca en texto plano | `ARQUITECTURA.md`, `docs/SEGURIDAD.md` |
| ¿Por qué CSRF está desactivado? | La API no usa cookies ni sesión; el token va en una cabecera que otro sitio no puede poner | `docs/SEGURIDAD.md` §3 |
| ¿Cómo evitan la inyección SQL? | Consultas con parámetros de Spring Data JPA; probado en `SeguridadIT` | `docs/SEGURIDAD.md` §1 |
| ¿Qué pasa si se pierde la base? | Respaldo diario con `pg_dump` y restauración probada | `docs/RESPALDOS.md` |
| ¿Cumplen la regla 3-2-1? | Falta definir la segunda carpeta fuera del disco y del local; está documentado qué copia falta | `docs/RESPALDOS.md` §4 |
| ¿Cómo saben que cada rol solo hace lo suyo? | Matriz de acceso probada endpoint por endpoint y rol por rol | `MatrizAccesoIT`, `docs/MOF_VamosPet.md` |
| ¿Cómo lo instalarían en la clínica? | JAR con perfil `prod`, variables en `.env.properties`, scripts de `db/` en orden | `docs/DESPLIEGUE.md` |
| ¿Cómo trabajaron en equipo? | Ramas `feature/*`, Pull Request con revisión, Milestones por etapa | `CONTRIBUTING.md`, `CHANGELOG.md` |

## 5. Después

- [ ] Detener el backend.
- [ ] Guardar en `docs/evidencias/etapa-9` las observaciones del jurado.
- [ ] Abrir Issues con lo que pidan corregir.
