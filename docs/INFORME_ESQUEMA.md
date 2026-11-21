# Esquema del informe final

Ocho secciones. Cada una dice qué contiene y qué evidencia de `docs/evidencias/` la respalda. Se redacta al final, con las evidencias ya tomadas.

## 1. Introducción y problema

- Presentación de la clínica Vamos Pet (Miraflores, Lima) y cómo trabajaba antes (agenda en papel o WhatsApp, inventario sin control de vencimientos, cobros sin comprobante verificable).
- Problema, objetivo general y objetivos específicos.
- Alcance: lo que hace el sistema y lo que queda fuera (publicación en internet, facturación electrónica ante SUNAT).
- **Evidencia:** fotos o descripción del proceso anterior (si la clínica lo permite); captura del inicio del sistema (`etapa-4/01`).

## 2. Requerimientos

- Actores: Administrador, Veterinario, Auxiliar y Cliente.
- Requerimientos funcionales agrupados por módulo (cuenta, citas, clínica, estancias, tienda, cobros, contacto, reportes).
- Requerimientos no funcionales: seguridad (BCrypt, token, roles), disponibilidad (respaldos), usabilidad, accesibilidad y compatibilidad.
- Reglas de negocio clave: receta obligatoria y de un solo uso con 30 días de vigencia, cancelación con 24 horas, sesión de cliente de 20 minutos, stock vencido no vendible, bloqueo del login.
- **Evidencia:** matriz de permisos y MOF (`docs/MOF_VamosPet.md`, `etapa-2/05`).

## 3. Diseño

- Arquitectura en capas (controlador, servicio, repositorio, modelo, permisos, seguridad) y su diagrama (`ARQUITECTURA.md`).
- Modelo de datos: diagrama entidad-relación de las 44 tablas, agrupado por módulo, y la justificación de la tercera forma normal.
- Diagramas de casos de uso por rol y de secuencia de los flujos principales (agendar cita, venta con receta, inicio de sesión con token).
- Diseño de la interfaz: paleta verde, tipografías, vistas por rol, criterios de accesibilidad.
- **Evidencia:** diagramas; capturas de las vistas por rol (`etapa-2/01`, `etapa-3/02`, `etapa-7/01`).

## 4. Implementación

- Tecnologías y por qué se eligieron (Spring Boot 4, Java 25, PostgreSQL 18, JavaScript sin frameworks).
- Recorrido por etapas 1 a 9 con lo que se construyó en cada una (`CHANGELOG.md`).
- Seguridad: BCrypt, token JWT, matriz de acceso, claves fuera del código, roles de base de datos, bloqueo del login, cabeceras.
- Control de versiones: ramas, Pull Request con revisión cruzada, Issues y Milestones.
- **Evidencia:** `etapa-8/01` a `08`, `etapa-9/10` y `11`.

## 5. Pruebas

- Plan de pruebas y matriz de casos (`docs/PLAN_DE_PRUEBAS.md`) con el resultado de cada caso.
- Pruebas automáticas: número de pruebas unitarias y de integración, y qué cubre cada grupo.
- Pruebas de API con Postman.
- Pruebas de seguridad: inyección SQL, XSS, CSRF (con la justificación), resultado de OWASP ZAP y alertas aceptadas.
- UAT por rol, usabilidad con el puntaje SUS, accesibilidad y compatibilidad entre navegadores, con sus hallazgos.
- **Evidencia:** `etapa-9/01`, `02`, `03`, `12`; las plantillas llenas de `docs/pruebas/`.

## 6. Despliegue y operación

- Plan de despliegue en un equipo de la clínica (`docs/DESPLIEGUE.md`): requisitos, variables, orden de scripts, arranque y verificación.
- Perfiles `dev` y `prod`, empaquetado en un JAR, Actuator y logs (`docs/OPERACION.md`).
- Respaldos: script diario, rotación de 14 días, prueba de restauración, regla 3-2-1 (qué copia falta) y plan de mantenimiento (`docs/RESPALDOS.md`).
- Plan de reversa.
- **Evidencia:** `etapa-9/04` a `09`.

## 7. Resultados

- Cumplimiento de cada objetivo específico, con su evidencia.
- Indicadores: casos aprobados de la matriz, número de pruebas automáticas, puntaje SUS, hallazgos de accesibilidad corregidos, alertas de ZAP resueltas.
- Comparación con el proceso anterior de la clínica.
- **Evidencia:** resumen de la matriz de casos; capturas que respaldan cada objetivo.

## 8. Conclusiones, limitaciones y trabajo futuro

- Conclusiones por objetivo.
- Limitaciones conocidas: bloqueo del login y tokens cerrados en memoria, `'unsafe-inline'` en la política de contenido, sin publicación en internet, sin facturación electrónica.
- Trabajo futuro: publicación con HTTPS, copia automática fuera del local, mover el JavaScript de las vistas a archivos para retirar `'unsafe-inline'`.
- Lecciones del trabajo en equipo (por ejemplo, el conflicto de fusión por copiar una carpeta antigua y la regla de partir siempre de `develop`).

## Anexos

- A. Manual de usuario por rol (capturas de `etapa-1` a `etapa-6`).
- B. Matriz de permisos completa (`docs/MOF_VamosPet.md`).
- C. Scripts de base de datos y su orden (`db/LEEME.txt`).
- D. Colección de Postman (`postman/`).
- E. Plantillas de prueba llenas (`docs/pruebas/`).
- F. Reportes de OWASP ZAP.
