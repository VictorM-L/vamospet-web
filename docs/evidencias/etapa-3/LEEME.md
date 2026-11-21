# Evidencias — Etapa 3: Citas con duración, cirugías, grooming, hospedaje y viajes

Guarda aquí las capturas con estos nombres. Cada una corresponde a casos de la matriz `docs/PLAN_DE_PRUEBAS.md` (IDs E3-xx) y se usa en el informe.

| Archivo | Qué debe verse |
|---|---|
| `01_franjas_bloqueadas.png` | Agenda: un servicio de 120 minutos bloquea las franjas siguientes |
| `02_tablero_cirugias.png` | Tablero con las columnas Por confirmar, Programadas, En curso y Postoperatorio |
| `03_finalizar_pasos.png` | Modal Finalizar en sus 3 pasos |
| `04_vista_auxiliar_tablero.png` | Tablero visto por el Auxiliar (solo estado y cobro) |
| `05_grooming.png` | Reserva de grooming con veterinarios habilitados |
| `06_guarderia_hospedaje.png` | Precio de guardería por día y hospedaje por noche |
| `07_documento_observado.png` | Viaje con un documento observado y el reenvío del cliente |

Reglas para todas las capturas:

- Que se vea la barra con la dirección (`localhost:8080/...`) y la hora del sistema.
- Nunca captures claves, tokens completos, el `.env.properties` real ni `pgpass.conf`. Si aparece un token, tápalo.
- Si un caso falla, captura el error y anota el número de Issue en el nombre (por ejemplo `04_stock_vencido_issue12.png`).
