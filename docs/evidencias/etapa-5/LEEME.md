# Evidencias — Etapa 5: Contacto, configuración y comprobantes

Guarda aquí las capturas con estos nombres. Cada una corresponde a casos de la matriz `docs/PLAN_DE_PRUEBAS.md` (IDs E5-xx) y se usa en el informe.

| Archivo | Qué debe verse |
|---|---|
| `01_bandeja_contacto.png` | Bandeja de mensajes del Auxiliar |
| `02_correo_respuesta.png` | Correo de respuesta citando el mensaje original |
| `03_configuracion.png` | Configuración de la clínica y el cambio reflejado en el sitio |
| `04_comprobante_pdf.png` | Comprobante PDF con su código QR |
| `05_verificacion_qr.png` | Página de verificación del comprobante |

Reglas para todas las capturas:

- Que se vea la barra con la dirección (`localhost:8080/...`) y la hora del sistema.
- Nunca captures claves, tokens completos, el `.env.properties` real ni `pgpass.conf`. Si aparece un token, tápalo.
- Si un caso falla, captura el error y anota el número de Issue en el nombre (por ejemplo `04_stock_vencido_issue12.png`).
