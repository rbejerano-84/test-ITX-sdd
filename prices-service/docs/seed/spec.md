# Spec: Servicio de consulta de precios (PRICES)

## 1. Objetivo
Servicio Spring Boot que, dada una fecha de aplicación, un producto y una cadena (brand), devuelve la tarifa y el precio final aplicable según las tarifas almacenadas en la tabla `PRICES`.

## 2. Stack
- Java 21, Maven, Spring Boot 4.1.x (versión estable actual; confirmar en start.spring.io)
- H2 en memoria, inicializada con los 4 registros del enunciado
- OpenAPI Generator (plugin Maven, generador `spring`, `interfaceOnly`)
- JUnit 5, AssertJ, Mockito, ArchUnit, Jacoco

## 3. Requisitos funcionales
- **RF-1** Endpoint `GET /api/v1/prices` con parámetros `applicationDate`, `productId`, `brandId`.
- **RF-2** La respuesta incluye `productId`, `brandId`, `priceList`, `startDate`, `endDate`, `price` (y `currency`).
- **RF-3** Si varias tarifas cubren la fecha, se aplica la de mayor `priority`.
- **RF-4** Si no hay tarifa aplicable, 404 con `ProblemDetail`.
- **RF-5** Parámetros ausentes o con formato inválido, 400 con `ProblemDetail`.
- **RF-6** Datos iniciales cargados al arrancar (los 4 registros del ejemplo).
- **RF-7** Todo error se devuelve como `ProblemDetail` (RFC 9457) con `title`, `status`, `detail` e `instance`; los errores de validación incluyen `errors[]` por parámetro.
- **RF-8** La interfaz REST y los DTOs se generan desde `prices-api.yaml`; el controlador solo implementa la interfaz generada.

## 4. Requisitos no funcionales
- Arquitectura hexagonal, dominio sin dependencias de framework (verificado con ArchUnit).
- TDD: cada comportamiento nace de un test en rojo.
- Contrato API definido en `openapi.yaml`.
- Cobertura mínima Jacoco: 90% en dominio y aplicación.
- Formato y estilo automatizados (Spotless).

## 5. Contrato de API
```
GET /api/v1/prices?applicationDate=2020-06-14T10:00:00&productId=35455&brandId=1
```
200:
```json
{
  "productId": 35455,
  "brandId": 1,
  "priceList": 1,
  "startDate": "2020-06-14T00:00:00",
  "endDate": "2020-12-31T23:59:59",
  "price": 35.50,
  "currency": "EUR"
}
```
El contrato completo vive en `prices-api.yaml` (fuente de verdad; el código REST se genera a partir de él).

Respuestas (los errores son siempre `application/problem+json`):

| Código | Cuándo | Quién lo produce |
|---|---|---|
| 200 | Tarifa encontrada | Controlador |
| 400 | Parámetro ausente, tipo inválido o fuera de rango | `@RestControllerAdvice` |
| 401 | API key ausente o inválida | Spring Security (fase opcional) o gateway |
| 403 | Credenciales sin permiso | Spring Security (fase opcional) o gateway |
| 404 | Sin tarifa aplicable (`PriceNotFoundException`) | `@RestControllerAdvice` |
| 405 | Método HTTP no soportado | `@RestControllerAdvice` |
| 406 | `Accept` no satisfacible | `@RestControllerAdvice` |
| 429 | Límite de peticiones | Gateway (documentado, no implementado) |
| 500 | Error inesperado, sin filtrar detalles internos | `@RestControllerAdvice` |
| 503 | No disponible | Gateway / infraestructura (documentado) |

Seguridad: el contrato declara `ApiKeyAuth` (cabecera `X-API-Key`). Si la fase opcional de Spring Security no se implementa, 401/403/429/503 quedan como respuestas del gateway.

## 6. Supuestos (decisiones documentadas)
- **S-1 Zona horaria:** `applicationDate` es hora local sin zona (`LocalDateTime`, ISO-8601). El enunciado no define zona; se asume la hora del sistema comercial.
- **S-2 Rango inclusivo:** `startDate <= applicationDate <= endDate`.
- **S-3 Desempate:** si coinciden fecha y prioridad, gana la `priceList` mayor. Determinista y documentado; el enunciado no lo define.
- **S-4 Tipos:** `BigDecimal` para precio (escala 2), moneda ISO-4217 como `Currency`, ids como `long`.
- **S-5 Nombres:** columnas en inglés y snake_case (`brand_id`, `start_date`, ...); puede variar respecto al enunciado, que lo permite.

## 7. Escenarios de aceptación (Given/When/Then)
Datos: los 4 registros del enunciado, producto 35455, brand 1.

| # | When (applicationDate) | Then priceList | Then price | Motivo |
|---|---|---|---|---|
| 1 | 2020-06-14T10:00:00 | 1 | 35.50 | Solo aplica la tarifa 1 |
| 2 | 2020-06-14T16:00:00 | 2 | 25.45 | Tarifa 2 (prio 1) gana a tarifa 1 (prio 0) |
| 3 | 2020-06-14T21:00:00 | 1 | 35.50 | La tarifa 2 ya terminó (18:30) |
| 4 | 2020-06-15T10:00:00 | 3 | 30.50 | Tarifa 3 (prio 1) hasta las 11:00 |
| 5 | 2020-06-16T21:00:00 | 4 | 38.95 | Tarifa 4 (prio 1) desde el 15 a las 16:00 |

Escenarios adicionales:
- **6** Producto inexistente → 404.
- **7** Fecha anterior a cualquier tarifa (2020-06-13T23:59:59) → 404.
- **8** `applicationDate` igual a `startDate` de una tarifa → se aplica (límite inclusivo).
- **9** `applicationDate` igual a `endDate` de una tarifa → se aplica (límite inclusivo).
- **10** Empate de fecha y prioridad → gana `priceList` mayor.
- **11** Falta un parámetro obligatorio → 400.
- **12** `applicationDate` con formato inválido → 400.
- **13** `brandId` o `productId` no numérico → 400.

## 8. Fuera de alcance
Caché, escritura/alta de tarifas, multimoneda con conversión, mensajería (Kafka descartado). La autenticación es una fase opcional (T-24).
