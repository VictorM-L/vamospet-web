# Colección de Postman

1. En Postman: **Import** → elige `VamosPet.postman_collection.json` y `VamosPet_local.postman_environment.json`.
2. Arriba a la derecha selecciona el entorno **Vamos Pet local**.
3. Arranca el backend (`http://localhost:8080`).
4. Clic derecho sobre la colección → **Run collection** → **Run**. Las carpetas corren en orden: primero `1. Iniciar sesión` guarda el token de cada rol.

| Carpeta | Qué comprueba |
|---|---|
| 0. Público | Lo que se ve sin iniciar sesión |
| 1. Iniciar sesión | Login de los 4 roles; guarda token e id |
| 2. Cliente | Sus mascotas, citas, pedidos y pagos; renovar la sesión |
| 3. Veterinario | Citas, tablero de cirugías, exámenes por evaluar |
| 4. Auxiliar | Venta en mostrador, anulación y que el stock vuelva |
| 5. Admin | Personal, reportes y `/actuator/health` |
| 6. Seguridad | 401 sin token o con token alterado, 403 por rol o dato ajeno, inyección SQL en el login, token cerrado |

La carpeta 4 crea una venta y la anula enseguida: el stock queda igual. La carpeta 6 cuenta un intento fallido de login con un correo inventado, que no afecta a las cuentas de prueba.

Si corres la colección muchas veces seguidas con una clave equivocada, el correo se bloquea 15 minutos (5 intentos fallidos). Reinicia el backend o espera para desbloquearlo.

Con Newman (opcional): `npx newman run VamosPet.postman_collection.json -e VamosPet_local.postman_environment.json`
