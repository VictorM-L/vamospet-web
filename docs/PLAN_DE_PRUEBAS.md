# Plan de pruebas — Vamos Pet

## 1. Alcance

Se prueba la plataforma completa (API, web y base de datos) tal como queda al cierre de la Etapa 9: registro y sesión, roles, citas y servicios clínicos, tienda y venta con receta, cobros y comprobantes, contacto y configuración, seguridad, operación y respaldos. No se prueba la publicación en internet, porque el sistema todavía no se publica.

## 2. Niveles y herramientas

| Nivel | Qué cubre | Herramienta | Quién lo ejecuta |
|---|---|---|---|
| Unitarias | Reglas de negocio de cada servicio, con dependencias simuladas | JUnit 5 + Mockito (`*Test`) | `.\mvnw.cmd test` |
| Integración | Endpoints completos con base H2, token y matriz de acceso | MockMvc (`*IT`) | `.\mvnw.cmd test` |
| API manual | Flujos principales y casos de seguridad contra el backend real | Postman (`postman/`) | Equipo |
| Funcionales | Casos de esta matriz, en la web | Navegador | Equipo |
| Aceptación (UAT) | Cada rol confirma que el sistema sirve para su trabajo | `docs/pruebas/UAT.md` | Usuarios de cada rol |
| Usabilidad | Facilidad de uso con tareas cronometradas | `docs/pruebas/USABILIDAD.md` | Equipo con 3 a 5 personas |
| Accesibilidad | Contraste, teclado, etiquetas | `docs/pruebas/ACCESIBILIDAD.md` | Equipo |
| Compatibilidad | Chrome, Edge, Firefox y móvil | `docs/pruebas/NAVEGADORES.md` | Equipo |
| Seguridad | Inyección SQL, XSS, CSRF, acceso | `SeguridadIT`, Postman, OWASP ZAP (`docs/SEGURIDAD.md`) | Automático y equipo |
| Respaldo | Respaldo y restauración | `docs/RESPALDOS.md` | Equipo |

## 3. Entorno

- Backend con perfil `dev` en `http://localhost:8080`, base `vamospet_db` con los scripts de `db/` aplicados.
- Cuentas de prueba (clave `Demo1234`): `admin@vamospet.test`, `vet1@vamospet.test`, `vet2@vamospet.test`, `aux@vamospet.test`, `cliente1@vamospet.test`, `cliente2@vamospet.test`.
- Para correos (códigos, comprobantes) se usa una cuenta de Gmail del equipo configurada en `.env.properties`.

## 4. Criterios

- **Entrada:** `.\mvnw.cmd test` en verde y la colección de Postman sin fallos.
- **Aprobado:** el resultado obtenido coincide con el esperado. Se anota la fecha y quién lo probó.
- **Fallido:** se abre un Issue con la plantilla "Error encontrado", con su ID de caso y captura.
- **Salida:** todos los casos de prioridad alta aprobados y ningún error abierto que impida usar un flujo principal.

Estados posibles en la columna Estado: `Pendiente`, `Aprobado`, `Fallido (#issue)`, `Bloqueado`.

## 5. Matriz de casos

### Etapa 1 — Registro, inicio de sesión, recuperación y sesión

| ID | Módulo | Rol | Precondición | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|---|---|
| E1-01 | Registro | Visitante | Ninguna | Escribir letras en el DNI | El campo no las acepta y aparece la alerta en vivo | Pendiente |
| E1-02 | Registro | Visitante | Ninguna | Escribir una contraseña y mirar la lista de requisitos | Se marcan "Entre 8 y 64 caracteres", "Al menos una letra" y "Al menos un número" a medida que se cumplen | Pendiente |
| E1-03 | Registro | Visitante | DNI ya registrado | Escribir ese DNI | Aviso en vivo de DNI ya registrado | Pendiente |
| E1-04 | Registro | Visitante | Datos válidos | Completar el formulario y enviar | Cuenta creada; mensaje "Ahora inicia sesión" | Pendiente |
| E1-05 | Inicio de sesión | Cliente | Cuenta activa | Entrar con correo y clave correctos | Entra a su perfil; el menú muestra sus opciones | Pendiente |
| E1-06 | Inicio de sesión | Cliente | Cuenta activa | Escribir 4 claves erróneas seguidas | El 3.º y 4.º intento avisan cuántos quedan | Pendiente |
| E1-07 | Inicio de sesión | Cliente | Caso E1-06 | Un 5.º intento erróneo y luego la clave correcta | Bloqueo de 15 minutos; ni con la clave correcta entra | Pendiente |
| E1-08 | Recuperación | Cliente | Cuenta con correo real | "¿Olvidaste tu contraseña?" → correo → código → nueva clave | Llega el código, se cambia la clave y llega el correo de confirmación | Pendiente |
| E1-09 | Recuperación | Cliente | Código enviado hace más de 15 minutos | Usar ese código | "Código incorrecto o vencido" | Pendiente |
| E1-10 | Recuperación | Admin | Veterinario con correo | Personal → "Enviar restablecimiento" | El veterinario recibe el código y crea su clave; el Admin nunca la ve | Pendiente |
| E1-11 | Sesión | Cliente | Sesión iniciada | No tocar la página 19 minutos | Aparece "¿Sigues ahí?"; "Seguir conectado" mantiene la sesión | Pendiente |
| E1-12 | Sesión | Cliente | Sesión iniciada | No tocar la página 20 minutos | Se cierra la sesión; el carrito se conserva | Pendiente |
| E1-13 | Sesión | Cliente | Modal de pago abierto | Esperar 20 minutos con el modal abierto | No se cierra la sesión mientras se paga | Pendiente |

### Etapa 2 — Roles y permisos

| ID | Módulo | Rol | Precondición | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|---|---|
| E2-01 | Menú | Admin | Sesión de Admin | Recorrer el menú | No ve opciones de comprar, agendar ni atender | Pendiente |
| E2-02 | Citas | Veterinario | Cita pendiente asignada | Clic en "Confirmar" | La cita pasa a Confirmada con un clic; no hay botón de cancelar | Pendiente |
| E2-03 | Citas | Veterinario | Cita asignada | "No puedo atender" | La cita aparece en la cola "por reasignar" del Auxiliar | Pendiente |
| E2-04 | Citas | Auxiliar | Caso E2-03 | Reasignar a otro veterinario libre | El cliente recibe aviso del cambio | Pendiente |
| E2-05 | Citas | Cliente | Cita a más de 24 h | Cancelar | Se cancela directamente | Pendiente |
| E2-06 | Citas | Cliente | Cita a menos de 24 h | Cancelar | Se envía una solicitud que resuelve el Auxiliar | Pendiente |
| E2-07 | API | Cliente | Token de cliente (Postman) | `GET /api/usuarios` | 403 | Pendiente |
| E2-08 | API | Cliente | Token de cliente | `GET /api/mascotas/usuario/{id de otro}` | 403 | Pendiente |
| E2-09 | MOF | Admin | Ninguna | Comparar `docs/MOF_VamosPet.md` con lo que cada rol puede hacer en la web | Coinciden | Pendiente |

### Etapa 3 — Citas con duración, cirugías, grooming, hospedaje y viajes

| ID | Módulo | Rol | Precondición | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|---|---|
| E3-01 | Agenda | Cliente | Servicio de 120 minutos | Agendar a las 09:00 y luego intentar las 09:30 con el mismo veterinario | La franja de 09:30 no aparece disponible | Pendiente |
| E3-02 | Cirugías | Veterinario | Examen prequirúrgico apto | Programar la cirugía | Aparece en "Por confirmar" del tablero | Pendiente |
| E3-03 | Cirugías | Veterinario | Caso E3-02 | Confirmar → Iniciar → Finalizar en 3 pasos | Avanza una columna por clic; al finalizar se crea la receta y el cobro | Pendiente |
| E3-04 | Cirugías | Veterinario | Examen no apto | Intentar programar la cirugía | No lo permite | Pendiente |
| E3-05 | Cirugías | Auxiliar | Cirugía programada | Abrir el tablero | Solo ve estado y cobro, sin datos clínicos | Pendiente |
| E3-06 | Grooming | Cliente | Veterinario con grooming | Reservar baño | Solo aparecen veterinarios que hacen grooming | Pendiente |
| E3-07 | Hospedaje | Cliente | Cuartos libres | Reservar guardería por día y hospedaje por noche | Cada uno muestra su precio y no se mezclan los cupos | Pendiente |
| E3-08 | Viajes | Cliente | Mascota registrada | Solicitar viaje subiendo los 3 documentos | La solicitud queda en revisión | Pendiente |
| E3-09 | Viajes | Auxiliar | Caso E3-08 | Marcar un documento como observado | El cliente ve cuál reenviar y lo reenvía | Pendiente |

### Etapa 4 — Tienda y banners

| ID | Módulo | Rol | Precondición | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|---|---|
| E4-01 | Tienda | Visitante | Productos cargados | Ver Farmacia en PC, tablet y móvil | Cuadrícula ordenada sin barras de desplazamiento horizontal | Pendiente |
| E4-02 | Tienda | Visitante | Producto sin foto o con foto rota | Ver el catálogo | Aparece el marcador "Sin foto", no un ícono roto | Pendiente |
| E4-03 | Banners | Auxiliar | Ninguna | Subir un banner y ordenarlo | Se ve en la sección; avanza solo y se pausa con el mouse | Pendiente |
| E4-04 | Banners | Auxiliar | 6 banners en la sección | Subir otro | Se rechaza con mensaje del límite | Pendiente |
| E4-05 | Banners | Visitante | Sección sin banners | Ver Farmacia | No queda un hueco vacío | Pendiente |

### Etapa 5 — Contacto, configuración y comprobantes

| ID | Módulo | Rol | Precondición | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|---|---|
| E5-01 | Contacto | Visitante | Ninguna | Enviar el formulario | El mensaje aparece en la bandeja del Auxiliar | Pendiente |
| E5-02 | Contacto | Auxiliar | Caso E5-01 | Responder | Llega el correo citando el original; el estado pasa a Respondido | Pendiente |
| E5-03 | Contacto | Visitante | 5 mensajes en la última hora | Enviar otro | Se rechaza por límite | Pendiente |
| E5-04 | Configuración | Admin | Ninguna | Cambiar el teléfono de la clínica | El nuevo teléfono se ve en el sitio | Pendiente |
| E5-05 | Comprobantes | Cliente | Pago con tarjeta | Pagar una cita | Llega el PDF adjunto; el QR abre la verificación del comprobante | Pendiente |

### Etapa 6 — Venta con receta y borrados seguros

| ID | Módulo | Rol | Precondición | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|---|---|
| E6-01 | Carrito | Cliente | Producto con receta, sin receta aprobada | Intentar pagar | El pago queda bloqueado con la explicación | Pendiente |
| E6-02 | Carrito | Cliente | Receta aprobada y vigente | Elegirla y pagar | Compra registrada; la receta queda usada | Pendiente |
| E6-03 | Carrito | Cliente | Caso E6-02 | Comprar de nuevo con la misma receta | No aparece como opción | Pendiente |
| E6-04 | Recetas | Cliente | Receta aprobada hace 31 días | Intentar usarla | Ya no es vigente | Pendiente |
| E6-05 | Mostrador | Auxiliar | Producto con stock vigente | Vender y cerrar el modal sin cobrar | La venta se anula y el stock vuelve | Pendiente |
| E6-06 | Mostrador | Auxiliar | Producto con todo su stock en un lote vencido | Venderlo | "No hay stock suficiente. X: pediste 1 y hay 0 disponibles para la venta (N unidades vencidas no se venden)." | Pendiente |
| E6-07 | Inventario | Auxiliar | Categoría con productos activos | Eliminar la categoría | 409 con mensaje claro; no se borra | Pendiente |
| E6-08 | Pedidos | Auxiliar | Pedido pagado | Cambiar su estado a Cancelado | "Para este pedido usa una devolución" | Pendiente |

### Etapa 7 — Colores

| ID | Módulo | Rol | Precondición | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|---|---|
| E7-01 | Interfaz | Todos | Ninguna | Revisar grooming, hospedaje, viajes, laboratorio, dashboard del Admin, finanzas, inventario, proveedores, citas y home | Ningún botón índigo o azul; la etiqueta "Confirmada" sigue azul | Pendiente |
| E7-02 | Interfaz | Todos | Ninguna | Medir el contraste de los botones verdes (`ACCESIBILIDAD.md`) | Al menos 4.5:1 | Pendiente |

### Etapa 8 — Seguridad

| ID | Módulo | Rol | Precondición | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|---|---|
| E8-01 | Contraseñas | Admin | Acceso a pgAdmin | `SELECT count(*) FROM usuario WHERE contrasena NOT LIKE '$2%';` | 0 | Pendiente |
| E8-02 | Token | Visitante | Ninguna | Abrir `http://localhost:8080/api/pagos` en el navegador | 401 | Pendiente |
| E8-03 | Token | Cliente | Sesión iniciada | Cerrar sesión y reutilizar el token en Postman | 401 | Pendiente |
| E8-04 | Token | Cliente | Activo en la web | Revisar en F12 → Red | Cada pocos minutos se llama a `/api/sesion/renovar` | Pendiente |
| E8-05 | Token | Personal | Sesión iniciada | Esperar 8 horas o usar un token vencido | Pide volver a entrar | Pendiente |
| E8-06 | Arranque | Admin | `.env.properties` sin `JWT_SECRET` | Arrancar el backend | No arranca y dice qué falta | Pendiente |
| E8-07 | Base de datos | Admin | `app_escritura` configurado | Recorrer la web completa | Ningún "permission denied" | Pendiente |
| E8-08 | Archivos | Cliente | Comprobante propio | Abrir el PDF | Se abre en otra pestaña; un PDF ajeno responde 403 | Pendiente |
| E8-09 | Sin internet | Todos | Red desconectada | Recargar la web | Fuentes e íconos se ven bien | Pendiente |

### Etapa 9 — Operación, pruebas y respaldos

| ID | Módulo | Rol | Precondición | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|---|---|
| E9-01 | Pruebas | Equipo | JDK 25 | `.\mvnw.cmd test` | 0 fallos (357 pruebas o más) | Pendiente |
| E9-02 | Postman | Equipo | Backend encendido | Correr la colección completa | Todas las pruebas pasan | Pendiente |
| E9-03 | Seguridad | Equipo | Postman | Inyección SQL en el login (carpeta 6) | 401, sin token | Pendiente |
| E9-04 | Seguridad | Auxiliar | Mensaje con `<script>` en contacto | Abrir la bandeja | Se ve el texto, no se ejecuta | Pendiente |
| E9-05 | Operación | Equipo | Ninguna | `.\mvnw.cmd clean package` y `java -jar target\vamospet.jar --spring.profiles.active=prod` | La web y la API funcionan en `http://localhost:8080` | Pendiente |
| E9-06 | Actuator | Admin / Auxiliar | Tokens de ambos | `GET /actuator/health` | Admin 200 con `"status":"UP"`; Auxiliar 403; sin token 401 | Pendiente |
| E9-07 | Logs | Equipo | Backend corriendo | Provocar un error (login con clave errónea) | Aparece en `logs/vamospet.log`; las advertencias en `vamospet-errores.log` | Pendiente |
| E9-08 | Respaldo | Equipo | `.pgpass` configurado | `herramientas\respaldo.ps1` | Carpeta con fecha con `.backup`, `uploads.zip`, `uploads_clinicos.zip` y `manifiesto.txt` | Pendiente |
| E9-09 | Respaldo | Equipo | Tarea registrada | `Start-ScheduledTask -TaskName 'VamosPet - Respaldo diario'` | `respaldo.log` dice "Respaldo terminado sin errores" | Pendiente |
| E9-10 | Restauración | Equipo | Caso E9-08 | `herramientas\restaurar.ps1 -Respaldo <carpeta>` | Base `vamospet_restaurada` con los mismos conteos que `vamospet_db` | Pendiente |
| E9-11 | Git | Equipo | Rama `feature/*` | Abrir un PR a `develop` con revisor | La plantilla se llena y otro integrante aprueba | Pendiente |

## 6. Pruebas automáticas existentes

| Área | Clases |
|---|---|
| Cuenta y recuperación | `CodigoServiceTest`, `RecuperacionServiceTest`, `ReglasCuentaValidacionTest`, `DniValidoValidatorTest`, `CuentaControllerIT` |
| Roles y acceso | `PermisosApiTest`, `MatrizAccesoIT`, `IdentidadPorTokenIT`, `CitaRolesIT` |
| Agenda, cirugías, estancias, viajes | `AgendaServiceTest`, `CitaServiceTest`, `CirugiaServiceTest`, `ExamenServiceTest`, `EstanciaGuarderiaTest`, `EstanciaCalculosTest`, `ViajeDocumentosTest`, `Etapa3IT` |
| Atención clínica y emergencias | `AtencionMedicaServiceTest`, `EmergenciaServiceTest`, `ArchivoClinicoValidadorTest` |
| Tienda y ventas | `InventarioServiceTest`, `VentaServiceTest`, `VentaRecetaIT`, `BorradosSegurosIT`, `BannerServiceTest`, `BannerIT` |
| Contacto, configuración y PDF | `ContactoServiceTest`, `ConfiguracionServiceTest`, `ComprobantePdfServiceTest`, `PagoCorreoTest`, `ContactoConfiguracionIT` |
| Seguridad | `ClaveServiceTest`, `JwtServiceTest`, `IntentosLoginServiceTest`, `SesionIT`, `SeguridadIT` |
| Reportes | `ReporteExcelServiceTest`, `ReporteFormalControllerIT` |
