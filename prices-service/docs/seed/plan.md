# Plan técnico

## 1. Arquitectura: hexagonal con DDD ligero

```
com.example.prices
├── domain
│   ├── model        Price, PriceQuery, BrandId, ProductId, PriceListId, Money, DateRange
│   ├── service      PriceSelector  (elige la tarifa ganadora)
│   └── exception    PriceNotFoundException
├── application
│   ├── port.in      GetApplicablePriceUseCase
│   ├── port.out     PriceRepository
│   └── service      GetApplicablePriceService
└── infrastructure
    ├── adapter.in.rest     PriceController (implementa PricesApi generada), PriceRestMapper, GlobalExceptionHandler  [generados: PricesApi, PriceResponse, ProblemDetail, FieldError]
    ├── adapter.out.persistence  PriceEntity, PriceJpaRepository, PricePersistenceAdapter, PriceEntityMapper
    └── config              BeanConfig
```

Regla de dependencias: `infrastructure → application → domain`. El dominio no importa Spring, JPA ni Jackson.

## 2. Decisiones de diseño (ADRs cortos, replicar en README)

| ADR | Decisión | Motivo |
|---|---|---|
| 1 | Selección de tarifa en la consulta (`start <= d AND end >= d ORDER BY priority DESC, price_list DESC LIMIT 1`) | No traer todas las tarifas a memoria; el índice resuelve el filtro |
| 2 | Además, `PriceSelector` en dominio con la regla de prioridad | La regla de negocio vive en el dominio y es testeable sin BD; el adaptador puede devolver candidatas y el dominio decide. Ver nota |
| 3 | `LocalDateTime` sin zona | El enunciado no define zona (supuesto S-1) |
| 4 | `BigDecimal` + `Currency` en `Money` | Evitar errores de coma flotante |
| 5 | Value objects como `record` con validación en constructor | Inmutabilidad y lenguaje ubicuo |
| 6 | Criterio de desempate como estrategia (`Comparator<Price>`) | Abierto a extensión (O de SOLID) |
| 7 | `ProblemDetail` (RFC 9457) | Estándar de errores en Spring |
| 8 | `schema.sql` + `data.sql` | Simple y explícito para H2 en memoria; Flyway sería sobreingeniería aquí |
| 9 | Contract-first con OpenAPI Generator (`interfaceOnly`) | El contrato es la fuente de verdad; el controlador no puede divergir de él |
| 10 | Un único `@RestControllerAdvice` que produce `ProblemDetail` | Respuestas de error uniformes y documentadas en el YAML |
| 11 | `LocalDateTime` en el código generado vía `typeMappings` | `format: date-time` genera `OffsetDateTime` por defecto; el supuesto S-1 exige hora local sin zona |

**Nota ADR 1 vs 2:** para que la regla de prioridad sea del dominio y no del SQL, el puerto expone `findApplicable(brandId, productId, date)` que devuelve las candidatas ordenadas, y `PriceSelector` aplica el comparador. Con tan pocos registros por producto el coste es nulo. Si se prefiere minimizar datos, el adaptador usa `LIMIT 1` y el dominio conserva el comparador como fuente de verdad testeada. Decidir en la tarea T-07 y dejarlo escrito.

## 3. Principios SOLID aplicados
- **S** Un caso de uso, una responsabilidad; mapeadores separados del controlador.
- **O** Desempate intercambiable sin tocar el selector.
- **L** Value objects sustituibles, sin herencia innecesaria.
- **I** Puertos pequeños y específicos (un caso de uso, un repositorio).
- **D** Aplicación depende de puertos; la infraestructura los implementa; ensamblado en `BeanConfig`.

## 4. Estrategia de pruebas (pirámide)
| Nivel | Herramienta | Qué cubre |
|---|---|---|
| Dominio | JUnit 5 + AssertJ, sin Spring | `PriceSelector`, value objects, límites inclusivos, desempate |
| Aplicación | Mockito sobre `PriceRepository` | `GetApplicablePriceService`: delega, lanza `PriceNotFoundException` |
| Persistencia | `@DataJpaTest` + H2 | Consulta, orden por prioridad, carga de `data.sql` |
| Web | `@WebMvcTest` + caso de uso mockeado | Mapeo JSON, validación 400, 404 como `ProblemDetail` |
| Aceptación | `@SpringBootTest` + `MockMvc` (o `RestTestClient`) | Los 5 escenarios del enunciado + 6 a 13 de la spec |
| Arquitectura | ArchUnit | Dominio sin Spring/JPA; reglas de capas |

Nota Boot 4: los starters de test están modularizados (por ejemplo `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`) y algunas anotaciones de test cambiaron de paquete. Comprobar con la guía de migración al fijar el `pom.xml`.

## 5. Contrato OpenAPI
`src/main/resources/openapi/prices-api.yaml` (contrato OpenAPI 3.0.3, ya redactado) es la fuente de verdad y se escribe antes del controlador. Se genera el código con `openapi-generator-maven-plugin`:
1. Configuración objetivo (a validar en el spike T-03): generador `spring`, `interfaceOnly=true`, `useSpringBoot3=true` (o la opción equivalente para Boot 4), `useTags=true`, `openApiNullable=false`, `useBeanValidation=true`, `skipDefaultInterface=true`, `typeMappings=DateTime=java.time.LocalDateTime`, `apiPackage` y `modelPackage` bajo `infrastructure.adapter.in.rest.generated`. Salida en `target/generated-sources`, nunca versionada. El controlador implementa `PricesApi`.
2. Plan B solo si el spike demuestra incompatibilidad con Boot 4 / Jackson 3 que no se resuelva con otra versión del plugin: controlador manual y un test que valide las respuestas contra el contrato. Se documenta el motivo en el README.

Si hay Spring Security (T-24), el generador no añade seguridad: se cablea en `SecurityConfig` con un filtro de `X-API-Key`.

## 5.1 Gestión de errores (`@RestControllerAdvice`)
`GlobalExceptionHandler` extiende `ResponseEntityExceptionHandler` (que ya cubre 405, 406, 400 por cuerpo/parámetro ausente) y sobrescribe lo necesario para devolver siempre `ProblemDetail` con `instance` = ruta de la petición.

| Excepción | Estado | Notas |
|---|---|---|
| `PriceNotFoundException` | 404 | `detail` con brand, producto y fecha |
| `MissingServletRequestParameterException` | 400 | Parámetro ausente |
| `MethodArgumentTypeMismatchException` | 400 | Tipo inválido (fecha o id) |
| `ConstraintViolationException` / `HandlerMethodValidationException` | 400 | Rellena `errors[]` con campo y mensaje |
| `HttpRequestMethodNotSupportedException` | 405 | Heredado de la clase base |
| `HttpMediaTypeNotAcceptableException` | 406 | Heredado de la clase base |
| `Exception` | 500 | Mensaje genérico; el detalle va solo al log |

Ningún `detail` expone trazas, SQL ni nombres de clases. Cada fila lleva su test en `@WebMvcTest` y, para el 404, otro de aceptación.

## 6. Calidad y herramientas
- **Spotless** (google-java-format o palantir) en `verify`.
- **Jacoco** con umbral mínimo en `verify`.
- **Maven Wrapper** incluido.
- **README**: cómo ejecutar, decisiones (ADRs), supuestos, cómo correr tests, ejemplo `curl`.
- **Git**: commits pequeños siguiendo TDD (`test:` rojo, `feat:` verde, `refactor:`), plantilla de PR.
- **Dockerfile** multi-stage opcional (extra).

## 7. Datos iniciales (`data.sql`)
```
(1, '2020-06-14 00:00:00', '2020-12-31 23:59:59', 1, 35455, 0, 35.50, 'EUR')
(1, '2020-06-14 15:00:00', '2020-06-14 18:30:00', 2, 35455, 1, 25.45, 'EUR')
(1, '2020-06-15 00:00:00', '2020-06-15 11:00:00', 3, 35455, 1, 30.50, 'EUR')
(1, '2020-06-15 16:00:00', '2020-12-31 23:59:59', 4, 35455, 1, 38.95, 'EUR')
```
Índice compuesto sugerido: `(brand_id, product_id, start_date, end_date)`.

## 8. Riesgos
- Cambios de paquetes/starters en Spring Boot 4 en las anotaciones de test.
- Compatibilidad de springdoc / openapi-generator con Boot 4 y Jackson 3.
- Interpretación de zona horaria (mitigado por supuesto S-1).
