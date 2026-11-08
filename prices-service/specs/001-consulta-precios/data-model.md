# Modelo de dominio y persistencia

## Agregados y value objects

### PriceQuery

Representa los parámetros de consulta:

| Campo | Tipo previsto | Regla |
|---|---|---|
| `applicationDate` | `LocalDateTime` | Obligatorio, hora local sin zona |
| `productId` | `ProductId` | Obligatorio y positivo |
| `brandId` | `BrandId` | Obligatorio y positivo |

### Price

Tarifa ofrecida por una cadena para un producto y una lista:

| Campo | Tipo previsto | Regla |
|---|---|---|
| `brandId` | `BrandId` | Identificador positivo |
| `startDate` / `endDate` | `LocalDateTime` | Ambos extremos inclusivos; inicio no posterior al fin |
| `priceList` | `PriceListId` | Identificador positivo usado como desempate |
| `productId` | `ProductId` | Identificador positivo |
| `priority` | entero | Valor mayor gana |
| `money` | `Money` | Importe decimal y moneda ISO-4217 |

`Money` usa `BigDecimal` y `Currency`; no convierte importes. Los value objects se implementan como `record` inmutables con validación en constructor. `DateRange.contains(date)` representa `startDate <= date <= endDate`.

### PriceSelector

Servicio de dominio que recibe tarifas candidatas aplicables y selecciona de forma determinista:

1. mayor `priority`;
2. mayor `priceList` cuando la prioridad empata.

Una colección vacía no produce tarifa; el caso de uso convierte esa ausencia en `PriceNotFoundException`.

## Persistencia

H2 en memoria contiene los cuatro registros de referencia en `data.sql`. El esquema `schema.sql` representa los campos de tarifa como columnas tipadas; los importes se almacenan en decimal de dos posiciones y timestamps sin zona. Se recomienda un índice compuesto por `(brand_id, product_id, start_date, end_date)`. La consulta del puerto devuelve las candidatas para la fecha; la selección final permanece en `PriceSelector`.

Las clases `PriceEntity`, `PriceJpaRepository`, `PriceEntityMapper` y `PricePersistenceAdapter` quedan confinadas a `infrastructure.adapter.out.persistence`. No se expone JPA fuera de esa capa. La clave técnica de fila y cualquier restricción de unicidad se definirán al crear el esquema, sin atribuirles semántica de negocio.

## Contrato de salida

`PriceResponse` contiene `productId`, `brandId`, `priceList`, `startDate`, `endDate`, `price` y `currency`, todos requeridos. `ProblemDetail` proporciona la forma RFC 9457 y `FieldError` representa errores por parámetro. Los DTOs son generados desde el contrato; los mapeadores se sitúan en infraestructura.
