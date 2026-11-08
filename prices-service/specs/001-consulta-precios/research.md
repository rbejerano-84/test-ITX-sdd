# Investigación y decisiones técnicas

**Feature**: `001-consulta-precios`

**Fecha**: 2026-11-08

## Decisiones

### R-01 — La decisión de precio pertenece al dominio

**Decisión**: Persistencia filtra candidatos por cadena, producto y vigencia; el dominio compara candidatos por prioridad descendente y, en empate, por `priceList` descendente.

**Motivo**: conserva la regla determinista en una única ubicación independiente de SQL y permite probarla sin Spring o H2. Con el volumen de cuatro registros de referencia, recuperar las tarifas aplicables es suficiente. La consulta puede usar el índice compuesto sugerido sin seleccionar la tarifa final.

### R-02 — Timestamps locales

**Decisión**: usar `LocalDateTime` sin zona en dominio, JDBC/JPA y DTO de REST. Mantener `format: date-time` en OpenAPI y ejemplos sin sufijo de zona.

**Motivo**: el enunciado y el supuesto S-1 no definen una zona horaria. El generador suele mapear `date-time` a `OffsetDateTime`; se configurará `typeMappings` para `DateTime=java.time.LocalDateTime` y se comprobará la conversión y compilación en un spike.

### R-03 — Contrato generado

**Decisión**: usar OpenAPI Generator Maven Plugin con generador Spring, `interfaceOnly=true`, `useSpringBoot3=true` o la opción compatible equivalente, `useTags=true`, `openApiNullable=false`, `useBeanValidation=true` y `skipDefaultInterface=true`. Generar bajo `target/generated-sources` en el paquete `infrastructure.adapter.in.rest.generated`.

**Motivo**: la interfaz generada fija el límite del contrato y evita divergencias del controlador. Deben verificarse la versión del plugin, las opciones y los paquetes frente a Boot 4.1.x y Jackson 3 antes de fijarlos. El controlador manual solo es un plan alternativo si el spike demuestra una incompatibilidad no resoluble; tal cambio deberá justificarse en README y validar contrato en pruebas.

### R-04 — Persistencia explícita en H2

**Decisión**: H2 en memoria inicializado por `schema.sql` y `data.sql`, sin migrador.

**Motivo**: el servicio tiene cuatro datos deterministas y no requiere migraciones evolutivas en esta feature. Un índice `(brand_id, product_id, start_date, end_date)` apoya el filtrado por candidato.

### R-05 — Respuestas de error

**Decisión**: un único `@RestControllerAdvice`, basado en `ResponseEntityExceptionHandler`, normaliza errores en Problem Details RFC 9457 con `title`, `status`, `detail`, `instance` y `errors[]` cuando corresponda.

**Motivo**: satisface la forma de error acordada y cubre validación, ausencia de precio, errores HTTP y excepciones inesperadas sin filtrar información interna.

### R-06 — Seguridad y estados de infraestructura

**Decisión**: eliminar del contrato el bloque global `security` y el esquema `ApiKeyAuth`, porque la spec deja autenticación fuera de alcance. Conservar las diez respuestas del seed: `200`, `400`, `401`, `403`, `404`, `405`, `406`, `429`, `500` y `503`. Las descripciones de `401`, `403`, `429` y `503` identifican al gateway o infraestructura como origen; la aplicación no implementa API keys, autorización ni rate limiting.

**Motivo**: respeta simultáneamente el alcance funcional y la petición de no simplificar el catálogo de respuestas. La ausencia de seguridad en el documento permite que las respuestas externas se documenten sin afirmar que el endpoint de aplicación exige credenciales.

### R-07 — Compatibilidad de Spring Boot 4.1.x

**Decisión**: conservar Boot 4.1.x y Java 21 como requisitos. En la configuración inicial, comprobar la modularización de starters y paquetes de tests (MVC, JPA, validación) contra la versión elegida; no fijar versiones por inferencia.

**Motivo**: la generación OpenAPI, Jackson 3 y las anotaciones de test pueden variar en Boot 4. El spike y el primer build verifican la combinación real, sin sustituir el stack requerido.

## Riesgos por validar durante implementación

- El plugin OpenAPI Generator puede requerir ajustes para Boot 4.1.x/Jackson 3, especialmente tipos de fecha y clases ProblemDetail generadas.
- Las anotaciones y starters de tests pueden diferir de versiones anteriores de Spring Boot.
- El formato OpenAPI `date-time` convencional puede implicar validaciones de zona horaria en herramientas externas; la prueba de generación y parsing debe asegurar que la solicitud local sin offset permanece soportada según la spec.
- Las respuestas `401`, `403`, `429` y `503` se documentan, pero su emisión corresponde a componentes externos a esta aplicación.
