# Compatibilidad entre navegadores

Se repite el mismo recorrido en cada navegador. Anota la versión (menú → Ayuda → Acerca de).

**Fecha:** ____ · **Revisó:** ____

| N.° | Recorrido | Chrome ___ | Edge ___ | Firefox ___ | Móvil Android (Chrome) ___ | Móvil iPhone (Safari), si hay ___ |
|---|---|---|---|---|---|---|
| N1 | Inicio: banners, tarjetas y menú se ven bien | | | | | |
| N2 | Registro con validaciones en vivo | | | | | |
| N3 | Inicio de sesión y aviso de bloqueo | | | | | |
| N4 | Tienda en cuadrícula y carrito | | | | | |
| N5 | Agendar cita (selector de fecha y hora) | | | | | |
| N6 | Pago con tarjeta y Yape (modal) | | | | | |
| N7 | Abrir un comprobante PDF en otra pestaña | | | | | |
| N8 | Descargar un reporte en Excel | | | | | |
| N9 | Subir un documento de viaje | | | | | |
| N10 | Aviso "¿Sigues ahí?" a los 19 minutos | | | | | |
| N11 | Menú del panel por rol en pantalla pequeña | | | | | |
| N12 | Fuentes e íconos sin internet | | | | | |

Marca **OK**, **Falla** (con N.° de Issue) o **N/A**.

## Notas

- En el móvil, la web se abre con la IP del computador que corre el backend (por ejemplo `http://192.168.1.20:8080`), con ambos en la misma red Wi-Fi. Si no carga, permite el puerto 8080 en el Firewall de Windows.
- Si un navegador bloquea la pestaña del PDF, permite las ventanas emergentes para `localhost:8080`.
