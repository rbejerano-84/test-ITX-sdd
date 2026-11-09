# Modelo de dominio y persistencia

## Bounded context: Aplicación de Precios

Este modelo pertenece únicamente al contexto `pricing`. Su lenguaje ubicuo define una **tarifa** como una oferta de precio para producto y cadena externos durante una vigencia, en una lista y con una prioridad. El **precio aplicable** es la tarifa ganadora para una consulta.

`productId` y `brandId` son identificadores opacos de otros contextos. Este servicio no carga entidades de catálogo ni replica reglas de productos o cadenas; solo valida que los identificadores de entrada sean positivos y los usa para buscar tarifas dentro de su propio contexto.

## Modelo de dominio

La capacidad descrita es de consulta y no modifica el estado. Por tanto, este modelo no define una raíz de agregado de escritura artificial. `Price` es una representación inmutable de una tarifa candidata; la clave técnica de persistencia no forma parte del modelo de dominio. `priceList` identifica la lista asociada y solo sirve como criterio de desempate, no como identidad única de la tarifa.

Si se añaden comandos para crear o modificar tarifas, deberá definirse antes la identidad de negocio y el agregado que protege las invariantes de escritura. Esa decisión queda fuera del alcance actual.

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
| `validityPeriod` | `DateRange` | Ambos extremos inclusivos; inicio no posterior al fin |
| `priceList` | `PriceListId` | Identificador positivo usado como desempate |
| `productId` | `ProductId` | Identificador positivo |
| `priority` | `Priority` | Value object; el valor mayor gana |
| `money` | `Money` | Importe decimal y moneda ISO-4217 |

`Money` usa `BigDecimal` y `Currency`; no convierte importes. Los value objects se implementan como `record` inmutables con validación en constructor. `DateRange.contains(date)` representa `startDate <= date <= endDate`.

### PriceSelector

Servicio de dominio que recibe tarifas candidatas aplicables y selecciona de forma determinista:

1. mayor `priority`;
2. mayor `priceList` cuando la prioridad empata.

Una colección vacía no produce tarifa; el caso de uso convierte esa ausencia en `PriceNotFoundException`. Esta decisión se ejecuta una sola vez dentro del bounded context, nunca en los adaptadores.

## Glosario de lenguaje ubicuo

| Término | Definición en el contexto `pricing` |
|---|---|
| `Price` | Tarifa candidata: precio de un producto para una marca, asociado a una lista, prioridad y vigencia. No tiene identidad de negocio en el alcance actual de solo consulta. |
| `PriceList` | Lista que agrupa tarifas y aporta el identificador usado como criterio de desempate; no es una entidad cargada desde otro contexto. |
| `Brand` | Marca externa identificada opacamente por `BrandId`; el contexto no carga ni replica su entidad o reglas. |
| `Product` | Producto externo identificado opacamente por `ProductId`; el contexto no carga ni replica su entidad o reglas. |
| `Priority` | Valor de orden de una tarifa; gana el valor mayor y, en empate, decide el identificador mayor de `PriceList`. |
| `DateRange` | Vigencia inclusiva de una tarifa: `startDate <= applicationDate <= endDate`. |
| `Money` | Importe decimal y moneda ISO-4217, sin conversión monetaria. |
| Tarifa aplicable | La tarifa (`Price`) que `PriceSelector` selecciona para una consulta, según la vigencia, prioridad y desempate definidos. |

Los nombres pueden adaptarse al idioma predominante del código, pero cada concepto debe conservar su definición y no variar de significado entre dominio, aplicación y adaptadores. El glosario es la referencia para el código y las pruebas.

## Clasificación de clases de dominio

Las clases del modelo de dominio pertenecen a `com.example.prices.domain`; los casos de uso pertenecen a `com.example.prices.application` y los adaptadores a `com.example.prices.infrastructure`. No se añaden subpaquetes para modelos, servicios, repositorios o excepciones. Los value objects son `record` inmutables y validan sus invariantes en el constructor. En este alcance de consulta, `Price` y los identificadores se comparan por valor, no son entidades; si se incorporan comandos, deberá definirse explícitamente la identidad de negocio y el agregado que proteja sus invariantes.

| Clase o concepto modelado | Clasificación | Paquete |
|---|---|---|
| `Price` | Value object | `com.example.prices.domain` |
| `PriceQuery` | Value object | `com.example.prices.domain` |
| `ProductId` (referencia a `Product`) | Value object | `com.example.prices.domain` |
| `BrandId` (referencia a `Brand`) | Value object | `com.example.prices.domain` |
| `PriceListId` (referencia a `PriceList`) | Value object | `com.example.prices.domain` |
| `Priority` | Value object | `com.example.prices.domain` |
| `DateRange` | Value object | `com.example.prices.domain` |
| `Money` | Value object | `com.example.prices.domain` |
| `PriceSelector` | Servicio de dominio | `com.example.prices.domain` |
| `PriceRepository` (interfaz) | Repositorio de dominio | `com.example.prices.domain` |
| `PriceNotFoundException` | Excepción de dominio | `com.example.prices.domain` |

`Price`, `PriceList`, `Brand` y `Product` no son entidades en este modelo: `Price` no requiere identidad para la consulta y los otros tres conceptos pertenecen a contextos externos (en el caso de `PriceList`, solo se usa el identificador como criterio de selección). No se crea una entidad ni un agregado de escritura artificial. `PriceSelector` y los objetos del dominio deben contener el comportamiento y las invariantes que les corresponden, evitando trasladar decisiones de negocio al caso de uso o a los adaptadores.

## Persistencia

H2 en memoria contiene los cuatro registros de referencia en `data.sql`. El esquema `schema.sql` representa los campos de tarifa como columnas tipadas; los importes se almacenan en decimal de dos posiciones y timestamps sin zona. Se recomienda un índice compuesto por `(brand_id, product_id, start_date, end_date)`. La consulta del puerto devuelve las candidatas para la fecha; la selección final permanece en `PriceSelector`.

El puerto `PriceRepository` pertenece a `com.example.prices.domain`; el caso de uso que lo coordina pertenece a `com.example.prices.application`. Las clases `PriceEntity`, `PriceJpaRepository`, `PriceEntityMapper` y `PricePersistenceAdapter` pertenecen a `com.example.prices.infrastructure`. No se expone JPA fuera de infraestructura. La clave técnica de fila y cualquier restricción de unicidad se definirán al crear el esquema, sin atribuirles semántica de negocio.

## Cambios realizados en el código

Se implementaron con TDD, cada uno en un paso aparte, el traslado de `PriceRepository` y `PriceNotFoundException` a `com.example.prices.domain`, y la introducción de `Priority` como `record` validado. También se incorporó `PriceQuery` como value object de los parámetros de consulta.

## Contrato de salida

`PriceResponse` contiene `productId`, `brandId`, `priceList`, `startDate`, `endDate`, `price` y `currency`, todos requeridos. `ProblemDetail` proporciona la forma RFC 9457 y `FieldError` representa errores por parámetro. Los DTOs son generados desde el contrato; los mapeadores se sitúan en infraestructura y traducen entre el lenguaje externo de la API y el modelo del contexto.
