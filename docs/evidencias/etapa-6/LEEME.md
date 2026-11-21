# Evidencias — Etapa 6: Venta con receta y borrados seguros

Guarda aquí las capturas con estos nombres. Cada una corresponde a casos de la matriz `docs/PLAN_DE_PRUEBAS.md` (IDs E6-xx) y se usa en el informe.

| Archivo | Qué debe verse |
|---|---|
| `01_carrito_bloqueado.png` | Carrito bloqueado sin receta aprobada |
| `02_receta_elegida.png` | Carrito con la receta vigente elegida |
| `03_receta_usada.png` | La misma receta ya no aparece en una segunda compra |
| `04_stock_vencido.png` | Mensaje de stock con unidades vencidas en venta en mostrador |
| `05_borrado_409.png` | Mensaje al eliminar una categoría con productos |
| `06_huerfanos_cero.png` | pgAdmin: 14_verificar_huerfanos.sql devuelve 0 filas |

Reglas para todas las capturas:

- Que se vea la barra con la dirección (`localhost:8080/...`) y la hora del sistema.
- Nunca captures claves, tokens completos, el `.env.properties` real ni `pgpass.conf`. Si aparece un token, tápalo.
- Si un caso falla, captura el error y anota el número de Issue en el nombre (por ejemplo `04_stock_vencido_issue12.png`).
