# Pruebas de seguridad

Resumen de cómo se protege la API y cómo se comprueba. Las pruebas automáticas están en `backend/src/test` y corren con `.\mvnw.cmd test`.

## 1. Inyección SQL

**Cómo se evita.** Todas las consultas pasan por Spring Data JPA con parámetros enlazados (consultas derivadas como `findAllByCorreoIgnoreCase` o `@Query` con `:parametro`). En ningún lugar se arma SQL concatenando texto del usuario. Los identificadores numéricos se convierten a `Integer` antes de llegar al servicio: si llega texto, Spring responde 400.

**Cómo se prueba** (`SeguridadIT`):

| Caso | Entrada | Resultado esperado |
|---|---|---|
| Login, contraseña | `' OR '1'='1`, `' OR 1=1; DROP TABLE usuario; --` y otras | 401, la tabla `usuario` queda igual |
| Login, correo | `admin@seguro.test' --` y otras | 401 |
| Consulta de duplicados | `?correo=' OR '1'='1`, `?dni=1' OR '1'='1` | `{"existe": false}` |
| Búsqueda por DNI | `/buscar-dni/63000003' OR '1'='1` | 404 |
| Filtro numérico | `?mascotaId=1 OR 1=1`, `/mascotas/usuario/1 OR 1=1` | 400 |
| Filtro de texto | `?estados=Pendiente') OR ('1'='1` | 200 con lista vacía |

La colección de Postman repite el caso del login (carpeta `6. Seguridad`).

## 2. XSS (cross-site scripting)

**Cómo se evita.**
- El servidor guarda el texto tal cual y lo devuelve como JSON (`application/json`, con `X-Content-Type-Options: nosniff`), nunca como HTML.
- El frontend pinta todo dato del usuario con `esc()` (`app.js`), que convierte `< > & " '` en entidades: el texto se ve, no se ejecuta.
- Nombres de personas solo aceptan letras y espacios (`ReglasCuenta.NOMBRE`), en la página y en el servidor.
- La política de contenido (`Content-Security-Policy`) solo permite scripts del propio servidor, sin orígenes externos, y bloquea `object` y el uso de la página dentro de un iframe ajeno.

**Cómo se prueba.**
- `SeguridadIT.xssContacto`: se envía `<script>…</script><img src=x onerror=…>` por el formulario de contacto; se guarda como texto y la bandeja responde JSON.
- `SeguridadIT.xssNombre`: el registro rechaza `<b>Ana</b>`.
- `smoke10.js` (jsdom): la bandeja de mensajes muestra ese texto sin crear etiquetas ni ejecutar el script.

**Límite conocido.** La política permite `'unsafe-inline'` en scripts y estilos porque las vistas usan `<script>` dentro del HTML y atributos `onclick` y `style`. Retirarlo exigiría mover todo el JavaScript de las vistas a archivos `.js`; queda como mejora.

## 3. CSRF (justificación de dejarlo desactivado)

Un ataque CSRF funciona cuando el navegador **añade solo** una credencial (una cookie de sesión) a una petición que dispara otro sitio. En Vamos Pet:

1. La API no crea sesión ni cookies (`SessionCreationPolicy.STATELESS`). El login responde el token en el cuerpo JSON, sin `Set-Cookie`.
2. El token viaja en la cabecera `Authorization: Bearer`, que el navegador **no** agrega por su cuenta: solo la pone nuestro `app.js`, leyendo el token de `localStorage`, que otro sitio no puede leer.
3. Una petición desde otro sitio llega sin token y recibe 401.

Por eso la protección CSRF de Spring (pensada para formularios con sesión) se deja desactivada **solo** en esta API sin estado. Si algún día se usara una cookie para la sesión, habría que activarla.

**Cómo se prueba.** `SeguridadIT.sinSesionNiCookies`: el login no envía cookies, una petición autenticada no crea sesión y una compra enviada con `Origin: https://sitio-ajeno.example` sin token responde 401.

## 4. Sesión y acceso

| Medida | Prueba |
|---|---|
| Token vencido, alterado, revocado o ausente: 401 | `JwtServiceTest`, `SesionIT`, `MatrizAccesoIT` |
| Rol sin permiso o dato ajeno: 403 (183 operaciones) | `MatrizAccesoIT`, `IdentidadPorTokenIT` |
| La identidad nunca sale de un parámetro de la URL | `MatrizAccesoIT.ningunaIdentidadPorParametro` |
| Bloqueo del login: 5 intentos fallidos → 15 minutos | `IntentosLoginServiceTest`, `SeguridadIT.bloqueoLogin` |
| `/actuator/health` solo Admin | `SeguridadIT.actuatorSoloAdmin` |
| Contraseñas con BCrypt | `ClaveServiceTest`, `CuentaControllerIT` |

El bloqueo del login se guarda en memoria: reiniciar el backend lo libera. Recuperar la contraseña también libera el correo.

## 5. Análisis con OWASP ZAP (lo ejecuta el equipo)

1. Instala ZAP (https://www.zaproxy.org) y arranca el backend con el perfil `dev`.
2. **Automated Scan** sobre `http://localhost:8080`. Guarda el reporte HTML en `docs/evidencias/etapa-9/zap-sin-sesion.html`.
3. Para revisar la API con sesión: en ZAP, **Tools → Options → Replacer**, agrega una regla que ponga la cabecera `Authorization` con el valor `Bearer <token>` (cópialo de la respuesta del login en Postman). Repite el escaneo y guarda `zap-con-token.html`.
4. Anota en el informe cada alerta Media o Alta: si se corrigió, o por qué se acepta (por ejemplo, `'unsafe-inline'` en la política de contenido, explicado arriba).

Alertas esperadas y su explicación:
- *CSP: script-src unsafe-inline* — ver el límite conocido de la sección 2.
- *Cross-Domain Misconfiguration* — los controladores aceptan peticiones de cualquier origen (`@CrossOrigin("*")`), pero sin token no obtienen nada. Si se publica, se puede restringir al dominio propio.
