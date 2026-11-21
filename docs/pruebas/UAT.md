# Pruebas de aceptación (UAT) por rol

Cada persona usa la web con su rol, sin ayuda, siguiendo solo la tarea escrita. Quien observa anota el resultado y no explica cómo hacerlo. Al final la persona firma si acepta o no el módulo.

**Fecha:** ____ · **Versión (MK):** ____ · **Observador:** ____ · **Navegador:** ____

## Administrador (`admin@vamospet.test`)

| N.° | Tarea | ¿La completó? (Sí / Con ayuda / No) | Tiempo | Comentario |
|---|---|---|---|---|
| A1 | Dar de alta a un veterinario nuevo y enviarle su restablecimiento de contraseña | | | |
| A2 | Asignarle especialidades y un horario de atención | | | |
| A3 | Cambiar el precio de un servicio | | | |
| A4 | Cambiar el horario de atención de la clínica en Configuración | | | |
| A5 | Ver el reporte de ingresos del último mes y descargarlo en Excel | | | |
| A6 | Revisar el estado del sistema en `/actuator/health` (con Postman) | | | |

## Veterinario (`vet1@vamospet.test`)

| N.° | Tarea | ¿La completó? | Tiempo | Comentario |
|---|---|---|---|---|
| V1 | Confirmar una cita pendiente | | | |
| V2 | Registrar la atención de una cita con diagnóstico, tratamiento y vacuna | | | |
| V3 | Pedir un examen prequirúrgico y, con el resultado, marcarlo apto | | | |
| V4 | Programar la cirugía y llevarla en el tablero hasta el postoperatorio | | | |
| V5 | Aprobar una receta subida por un cliente | | | |

## Auxiliar (`aux@vamospet.test`)

| N.° | Tarea | ¿La completó? | Tiempo | Comentario |
|---|---|---|---|---|
| X1 | Vender en mostrador a un cliente buscado por DNI y cobrar en efectivo | | | |
| X2 | Registrar la entrada de un lote con fecha de vencimiento | | | |
| X3 | Dar de baja unidades vencidas con un ajuste | | | |
| X4 | Reasignar una cita marcada "No puedo atender" | | | |
| X5 | Responder un mensaje de contacto | | | |
| X6 | Revisar los documentos de un viaje y observar uno | | | |
| X7 | Registrar el ingreso y la salida de una mascota en hospedaje | | | |

## Cliente (`cliente1@vamospet.test`)

| N.° | Tarea | ¿La completó? | Tiempo | Comentario |
|---|---|---|---|---|
| C1 | Registrar una mascota | | | |
| C2 | Agendar una consulta con un especialista | | | |
| C3 | Comprar un producto de farmacia con receta | | | |
| C4 | Pagar una cita y descargar el comprobante | | | |
| C5 | Cancelar una cita con más de 24 horas de anticipación | | | |
| C6 | Recuperar su contraseña con el código del correo | | | |

## Aceptación

| Rol | Persona | ¿Acepta el módulo? | Observaciones | Firma |
|---|---|---|---|---|
| Administrador | | | | |
| Veterinario | | | | |
| Auxiliar | | | | |
| Cliente | | | | |

Cada tarea marcada "No" o "Con ayuda" se registra como Issue con la etiqueta correspondiente.
