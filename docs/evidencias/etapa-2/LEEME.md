# Evidencias — Etapa 2: Roles y Manual de Funciones

Guarda aquí las capturas con estos nombres. Cada una corresponde a casos de la matriz `docs/PLAN_DE_PRUEBAS.md` (IDs E2-xx) y se usa en el informe.

| Archivo | Qué debe verse |
|---|---|
| `01_menu_admin.png` | Menú del Admin sin comprar, agendar ni atender |
| `02_confirmar_un_clic.png` | Citas del Veterinario con el botón Confirmar |
| `03_cola_por_reasignar.png` | Cola "por reasignar" del Auxiliar después de "No puedo atender" |
| `04_postman_403.png` | Postman: un cliente pidiendo /api/usuarios recibe 403 |
| `05_mof.png` | Una página de docs/MOF_VamosPet.md |

Reglas para todas las capturas:

- Que se vea la barra con la dirección (`localhost:8080/...`) y la hora del sistema.
- Nunca captures claves, tokens completos, el `.env.properties` real ni `pgpass.conf`. Si aparece un token, tápalo.
- Si un caso falla, captura el error y anota el número de Issue en el nombre (por ejemplo `04_stock_vencido_issue12.png`).
