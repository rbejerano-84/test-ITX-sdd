# Plan de implementación: Consulta de precios por producto, cadena y fecha

**Rama**: `001-consulta-precios` | **Fecha**: 2026-11-08 | **Especificación**: [spec.md](spec.md)

**Entrada**: `specs/001-consulta-precios/spec.md`

## Resumen

Implementar `GET /api/v1/prices` para devolver la tarifa aplicable a un producto, cadena y fecha/hora local. La selección usa rangos inclusivos y gana la prioridad más alta; en caso de empate, gana el `priceList` mayor. El contrato OpenAPI es la fuente de verdad y genera la interfaz que implementará el controlador. La aplicación seguirá arquitectura hexagonal, con Java 21, Maven, Spring Boot 4.1.x y H2 en memoria inicializada por `schema.sql` y `data.sql`.

El contrato derivado de `docs/seed/prices-api.yaml` está en `src/main/resources/openapi/prices-api.yaml`. Conserva las diez respuestas documentadas y los modelos del seed, pero no declara autenticación: la especificación excluye autenticación. Las respuestas `401`, `403`, `429` y `503` describen respuestas posibles del gateway o de la infraestructura, no errores generados por esta aplicación.

## Contexto técnico

**Lenguaje/versión**: Java 21

**Build y framework**: Maven Wrapper, Spring Boot 4.1.x

**Dependencias principales**: Spring Web MVC, Spring Data JPA, Bean Validation, H2; OpenAPI Generator Maven Plugin

**Persistencia**: H2 en memoria, esquema y cuatro registros iniciales mediante `schema.sql` y `data.sql`

**Pruebas**: JUnit 5, AssertJ, Mockito, `@DataJpaTest`, `@WebMvcTest`, `@SpringBootTest`/MockMvc y ArchUnit

**Plataforma**: servicio REST Java

**Objetivo de rendimiento**: sin requisito cuantitativo en la especificación

**Restricciones**: timestamps locales sin zona; `BigDecimal`; fechas inclusivas; no exponer detalles internos en errores
**Alcance**: consulta de lectura de precios; no incluye escritura, conversión monetaria ni autenticación de la aplicación

El generador debe usar `interfaceOnly=true`; el mapeo de `format: date-time` debe producir `java.time.LocalDateTime` (`DateTime=java.time.LocalDateTime` en `typeMappings`). La versión y las opciones compatibles con Spring Boot 4.1.x se fijarán mediante el spike de generación antes de depender de los tipos generados. La salida estará bajo `target/generated-sources`, nunca versionada.

## Comprobación de constitución

| Principio | Comprobación del plan |
|---|---|
| Contract-first | El YAML precede al controlador; OpenAPI Generator crea la interfaz y DTOs; el controlador implementa `PricesApi`. |
| Dominio primero y hexagonal | Dependencias `infrastructure → application → domain`; dominio independiente de Spring, JPA, Jackson y HTTP; ArchUnit lo verifica. |
| Test-first | Las reglas y escenarios se desarrollan en ciclos rojo-verde-refactor, con pruebas unitarias, web, persistencia y aceptación. |
| Selección determinista | Rangos inclusivos; `PriceSelector` decide por prioridad y después por `priceList`. Un no-coincidente produce 404. |
| Validación y observabilidad seguras | Un único `@RestControllerAdvice` devuelve `ProblemDetail`; 400 identifica parámetros; el 500 no filtra detalles internos. |
| Calidad | `mvn verify` aplica Spotless, JaCoCo con mínimo global de líneas del 80% y ArchUnit. |

**Resultado**: conforme con la constitución. Los estados de gateway/infraestructura (`401`, `403`, `429`, `503`) quedan documentados en el contrato sin implementar seguridad ni limitación de peticiones en la aplicación.

## Decisiones de diseño

1. **Selección en dominio**: el puerto de persistencia devuelve las tarifas que coinciden con producto, cadena y rango. `PriceSelector` elige la ganadora en el dominio; así la regla tiene una única implementación de negocio y se prueba sin base de datos. Se filtran candidatos en SQL y se añade el índice compuesto `(brand_id, product_id, start_date, end_date)`, pero no se aplica `LIMIT 1` allí.
2. **Fecha/hora local**: `LocalDateTime` en dominio, persistencia y DTO generado. El contrato mantiene `format: date-time` y ejemplos sin offset conforme al supuesto de la spec.
3. **Importes**: `BigDecimal` y moneda ISO-4217; no hay conversión.
4. **Errores**: un `@RestControllerAdvice` basado en `ResponseEntityExceptionHandler` adapta errores HTTP y de negocio al formato Problem Details acordado; `instance` representa la ruta.
5. **Contrato externo**: se mantienen las diez respuestas `200`, `400`, `401`, `403`, `404`, `405`, `406`, `429`, `500` y `503`. Los cuatro estados originados externamente se documentan como gateway/infraestructura.

## Estructura del proyecto

```text
src/
├── main/
│   ├── java/.../prices/
│   │   ├── domain/
│   │   │   ├── model/                   # Price, PriceQuery, Money, DateRange e IDs
│   │   │   ├── service/                 # PriceSelector
│   │   │   └── exception/
│   │   ├── application/
│   │   │   ├── port/in/                 # GetApplicablePriceUseCase
│   │   │   ├── port/out/                # PriceRepository
│   │   │   └── service/
│   │   └── infrastructure/
│   │       ├── adapter/in/rest/         # Controller, mapper, advice y tipos generados
│   │       ├── adapter/out/persistence/ # Entidad, repositorio JPA, mapper y adapter
│   │       └── config/
│   └── resources/
│       ├── openapi/prices-api.yaml
│       ├── schema.sql
│       └── data.sql
└── test/
    └── java/.../prices/                 # Pruebas por capa y reglas ArchUnit
```

**Decisión de estructura**: un único servicio Maven; paquetes `domain`, `application` e `infrastructure` bajo el paquete raíz de precios.

## Secuencia de implementación

1. Crear el proyecto Maven, configurar Java 21, Boot 4.1.x, Wrapper, calidad y estructura hexagonal.
2. Validar el contrato YAML y configurar OpenAPI Generator; comprobar que `PricesApi`, `PriceResponse`, `ProblemDetail` y `FieldError` se generan y compilan con `LocalDateTime`.
3. Construir primero los tests y value objects de dominio; implementar `PriceSelector` y las reglas inclusivas/de desempate.
4. Definir los puertos y el caso de uso; después crear esquema, datos, adapter JPA y pruebas de persistencia.
5. Crear pruebas MVC para el contrato y sus errores; implementar controlador, mapper y el único advice.
6. Añadir los cinco escenarios de aceptación, límites y ausencia de precio; verificar integración completa.
7. Aplicar reglas ArchUnit, cobertura mínima, formato y validación de respuestas frente al YAML; completar README y guía de ejecución.

La secuencia detallada de tareas TDD se deriva en `docs/seed/tasks.md` y será trasladada a `tasks.md` en la fase de tareas, no en este plan.

## Archivos de diseño

- [Investigación y decisiones](research.md)
- [Modelo de datos y dominio](data-model.md)
- [Quickstart](quickstart.md)
- Contrato: `src/main/resources/openapi/prices-api.yaml`

## Registro de complejidad

Sin desviaciones de la constitución ni proyectos adicionales. La separación de capas y los adaptadores son requisitos explícitos de arquitectura hexagonal, no abstracciones añadidas fuera de alcance.
