# Evidencias — Etapa 1: Registro, inicio de sesión, recuperación y sesión

Guarda aquí las capturas con estos nombres. Cada una corresponde a casos de la matriz `docs/PLAN_DE_PRUEBAS.md` (IDs E1-xx) y se usa en el informe.

| Archivo | Qué debe verse |
|---|---|
| `01_dni_bloquea_letras.png` | Registro con la alerta en vivo al escribir una letra en el DNI |
| `02_requisitos_clave.png` | Lista de requisitos de contraseña marcándose mientras se escribe |
| `03_duplicado_en_vivo.png` | Aviso de DNI o correo ya registrado |
| `04_correo_codigo.png` | Correo recibido con el código de 6 dígitos (tapa el código si la captura se publica) |
| `05_codigo_vencido.png` | Mensaje al usar un código vencido |
| `06_restablecimiento_admin.png` | Vista Personal con el botón "Enviar restablecimiento" y el mensaje de envío |
| `07_aviso_sigues_ahi.png` | Aviso "¿Sigues ahí?" al minuto 19 |
| `08_cierre_por_inactividad.png` | Mensaje de cierre por 20 minutos sin actividad |

Reglas para todas las capturas:

- Que se vea la barra con la dirección (`localhost:8080/...`) y la hora del sistema.
- Nunca captures claves, tokens completos, el `.env.properties` real ni `pgpass.conf`. Si aparece un token, tápalo.
- Si un caso falla, captura el error y anota el número de Issue en el nombre (por ejemplo `04_stock_vencido_issue12.png`).
