# Evidencias — Etapa 9: Git, pruebas, operación y respaldos

Guarda aquí las capturas con estos nombres. Cada una corresponde a casos de la matriz `docs/PLAN_DE_PRUEBAS.md` (IDs E9-xx) y se usa en el informe.

| Archivo | Qué debe verse |
|---|---|
| `01_mvn_test_verde.png` | Consola con .\mvnw.cmd test: número de pruebas, 0 fallos, BUILD SUCCESS |
| `02_postman_runner.png` | Resultado del Runner de Postman |
| `03_zap_reporte.png` | Resumen del reporte de OWASP ZAP (y los HTML zap-sin-sesion.html y zap-con-token.html) |
| `04_jar_prod.png` | Consola con java -jar vamospet.jar en perfil prod |
| `05_actuator.png` | Postman: /actuator/health del Admin con status UP |
| `06_logs.png` | Carpeta logs con vamospet.log y vamospet-errores.log |
| `07_respaldo_log.png` | respaldo.log con "Respaldo terminado sin errores" y la carpeta con fecha |
| `08_tarea_programada.png` | Programador de tareas con "VamosPet - Respaldo diario" a las 23:00 |
| `09_restauracion.png` | Tabla de conteos de restaurar.ps1 (o el verificacion_restauracion_*.txt) |
| `10_pull_request.png` | GitHub: Pull Request con la plantilla y la aprobación de un revisor |
| `11_milestones.png` | GitHub: Milestones por etapa |
| `12_uat_firmado.png` | Hoja de UAT firmada (docs/pruebas/UAT.md impresa o en PDF) |

Reglas para todas las capturas:

- Que se vea la barra con la dirección (`localhost:8080/...`) y la hora del sistema.
- Nunca captures claves, tokens completos, el `.env.properties` real ni `pgpass.conf`. Si aparece un token, tápalo.
- Si un caso falla, captura el error y anota el número de Issue en el nombre (por ejemplo `04_stock_vencido_issue12.png`).
