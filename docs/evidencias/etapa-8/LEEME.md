# Evidencias — Etapa 8: Seguridad

Guarda aquí las capturas con estos nombres. Cada una corresponde a casos de la matriz `docs/PLAN_DE_PRUEBAS.md` (IDs E8-xx) y se usa en el informe.

| Archivo | Qué debe verse |
|---|---|
| `01_bcrypt_pgadmin.png` | pgAdmin: contraseñas que empiezan con $2a$ y la consulta de claves sin cifrar en 0 |
| `02_sin_token_401.png` | Navegador o Postman: /api/pagos sin token responde 401 |
| `03_bearer_red.png` | F12 → Red: petición con la cabecera Authorization: Bearer |
| `04_renovar.png` | F12 → Red: llamada a /api/sesion/renovar |
| `05_token_cerrado_401.png` | Postman: token después de cerrar sesión responde 401 |
| `06_env_example.png` | backend/.env.properties.example sin valores (nunca el .env.properties real) |
| `07_sin_internet.png` | La web sin internet con fuentes e íconos |
| `08_bloqueo_login.png` | Mensaje de bloqueo tras 5 intentos fallidos |

Reglas para todas las capturas:

- Que se vea la barra con la dirección (`localhost:8080/...`) y la hora del sistema.
- Nunca captures claves, tokens completos, el `.env.properties` real ni `pgpass.conf`. Si aparece un token, tápalo.
- Si un caso falla, captura el error y anota el número de Issue en el nombre (por ejemplo `04_stock_vencido_issue12.png`).
