# Prices Service

Servicio REST para consultar el precio aplicable a un producto y una cadena en una fecha y hora determinada. El contrato de la API se mantiene en `src/main/resources/openapi/prices-api.yaml`.

## Supuestos del producto

- **S-1 — Hora local:** `applicationDate` se interpreta como fecha y hora local, sin zona ni offset.
- **S-2 — Vigencia inclusiva:** el precio es aplicable tanto en `startDate` como en `endDate`.
- **S-3 — Datos y moneda:** se usan los cuatro registros de referencia del enunciado; los precios están expresados en EUR y deben producir los cinco resultados descritos en la especificación.
- **S-4 — Selección determinista:** gana la tarifa de mayor prioridad; si hay empate, gana el `priceList` mayor.
- **S-5 — Alcance:** el servicio solo consulta precios existentes; no crea tarifas, convierte monedas, autentica clientes ni limita peticiones.

Los identificadores de producto y cadena deben ser positivos. Si no hay una tarifa aplicable, la respuesta es `404`.

## Contrato API

El [contrato OpenAPI](src/main/resources/openapi/prices-api.yaml) es la fuente de verdad y no tiene Swagger UI integrado. Las pruebas de contrato validan respuestas reales con sus esquemas; omiten únicamente la validación RFC 3339 del formato `date-time`, porque S-1 define fechas locales sin zona ni offset.

## Contrato generado

`mvn generate-sources` genera la interfaz y los modelos en `target/generated-sources/openapi`; esos archivos no se versionan. El DTO del contrato `com.example.prices.infrastructure.adapter.in.rest.generated.model.ProblemDetail` y el tipo HTTP `org.springframework.http.ProblemDetail` conservan paquetes distintos. La interfaz generada usa el DTO del contrato; el manejo de errores REST debe referirse explícitamente al tipo de Spring, sin importar ambos nombres simples en una misma clase.
