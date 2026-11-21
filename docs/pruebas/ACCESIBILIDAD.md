# Prueba de accesibilidad

Revisión basada en las pautas WCAG 2.1 nivel AA. Herramientas: la extensión **WAVE** o **axe DevTools**, Lighthouse (pestaña Accesibilidad de Chrome, opcional) y la calculadora de contraste de WebAIM (https://webaim.org/resources/contrastchecker/).

**Fecha:** ____ · **Revisó:** ____ · **Navegador:** ____

## 1. Contraste de color (mínimo 4.5:1 para texto normal y 3:1 para texto grande)

| N.° | Elemento | Texto / fondo | Relación medida | ¿Cumple? |
|---|---|---|---|---|
| AC1 | Botón principal verde (texto blanco sobre `#158A55`) | | | |
| AC2 | Botón de contorno (texto `#158A55` sobre blanco) | | | |
| AC3 | Ícono verde sobre verde claro (`#158A55` sobre `#EAF4EE`) | | | |
| AC4 | Texto secundario `#667085` sobre blanco | | | |
| AC5 | Etiqueta "Confirmada" (`#2563EB` sobre `#DBEAFE`) | | | |
| AC6 | Mensajes de error en rojo | | | |

## 2. Teclado (sin usar el mouse)

| N.° | Prueba | Resultado esperado | ¿Cumple? |
|---|---|---|---|
| T1 | Recorrer el inicio con Tab | Todos los enlaces y botones reciben foco en orden lógico y el foco se ve | |
| T2 | Iniciar sesión solo con teclado | Se puede escribir, mostrar la contraseña y enviar con Enter | |
| T3 | Abrir y cerrar un modal (pago, receta) | El foco entra al modal y Escape o el botón lo cierra | |
| T4 | Aviso "¿Sigues ahí?" | El foco va al botón "Seguir conectado" | |
| T5 | Agendar una cita | Se puede elegir servicio, fecha y hora sin mouse | |
| T6 | Menú del panel por rol | Cada opción se activa con Enter | |

## 3. Etiquetas y estructura

| N.° | Prueba | Resultado esperado | ¿Cumple? |
|---|---|---|---|
| E1 | Cada campo de formulario tiene una etiqueta asociada | WAVE no marca "Missing form label" | |
| E2 | Las imágenes tienen `alt` | WAVE no marca "Missing alternative text" | |
| E3 | Los botones que solo tienen ícono tienen `title` o `aria-label` | Se leen con lector de pantalla | |
| E4 | Los mensajes de error se anuncian | Los campos con error muestran el mensaje junto al campo | |
| E5 | El idioma de la página está declarado | `<html lang="es">` | |
| E6 | Títulos en orden (h1, h2, h3) | Sin saltos de nivel | |
| E7 | Zoom al 200 % | El contenido no se corta ni se superpone | |

## Hallazgos

| N.° | Pantalla | Problema | Pauta WCAG | Gravedad | Propuesta |
|---|---|---|---|---|---|
| | | | | | |
