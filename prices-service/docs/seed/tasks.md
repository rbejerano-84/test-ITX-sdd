# Tareas (orden TDD: rojo → verde → refactor)

Cada tarea debe terminar con la build en verde y un commit pequeño.

## Fase 0: Arranque
- [ ] **T-01** Generar proyecto Maven (Java 21, Spring Boot 4.1.x): web, data-jpa, validation, h2, actuator opcional. Añadir ArchUnit, Jacoco, Spotless. Wrapper incluido.
- [ ] **T-02** Crear estructura de paquetes y `README.md` base con supuestos S-1 a S-5.
- [ ] **T-03a** Redactar `src/main/resources/openapi/prices-api.yaml` a partir de la spec: parámetros, `PriceResponse`, `ProblemDetail`, `FieldError`, respuestas reutilizables 200/400/401/403/404/405/406/429/500/503, `securityScheme` y ejemplos. Hay un borrador ya escrito en `prices-api.yaml` que sirve de punto de partida. Revisarlo contra los escenarios de la spec y pasar un linter (por ejemplo Spectral) sin errores.
- [ ] **T-03b** Configurar `openapi-generator-maven-plugin` y generar el código (spike): fijar versión del plugin y opciones (`interfaceOnly`, `typeMappings` a `LocalDateTime`, paquetes `generated`). Comprobar que `mvn generate-sources` produce `PricesApi`, `PriceResponse`, `ProblemDetail` y `FieldError`, y que compilan con Boot 4.1 y Java 21. La generación queda enganchada a la build (`generate-sources`), sin versionar `target/`. Si falla sin remedio, activar plan B y documentarlo.

## Fase 1: Dominio (sin Spring)
- [ ] **T-04** (rojo) Tests de value objects: `BrandId`, `ProductId`, `PriceListId` (valores positivos), `Money` (escala 2, moneda válida), `DateRange` (inicio <= fin, `contains` inclusivo).
- [ ] **T-05** (verde) Implementar value objects como `record`.
- [ ] **T-06** (rojo) Tests de `PriceSelector`: prioridad mayor gana; tarifa única; lista vacía → vacío; límites inclusivos; desempate por `priceList` mayor.
- [ ] **T-07** (verde) Implementar `Price`, `PriceSelector` y comparador de desempate como estrategia. Decidir y documentar ADR 1/2.
- [ ] **T-08** Refactor y revisión de nombres (lenguaje ubicuo).

## Fase 2: Aplicación
- [ ] **T-09** (rojo) Test de `GetApplicablePriceService` con `PriceRepository` mockeado: devuelve la ganadora; lanza `PriceNotFoundException` si no hay.
- [ ] **T-10** (verde) Definir puertos `GetApplicablePriceUseCase` y `PriceRepository`; implementar el servicio.

## Fase 3: Persistencia
- [ ] **T-11** `schema.sql` y `data.sql` con los 4 registros e índice.
- [ ] **T-12** (rojo) `@DataJpaTest`: datos cargados; consulta devuelve candidatas correctas para cada fecha de los escenarios; respeta rango inclusivo.
- [ ] **T-13** (verde) `PriceEntity`, `PriceJpaRepository`, `PriceEntityMapper`, `PricePersistenceAdapter`.

## Fase 4: REST
- [ ] **T-14** (rojo) `@WebMvcTest` del controlador y del advice, una prueba por fila de la tabla del plan 5.1: 200 con JSON esperado; 400 por parámetro ausente, fecha inválida, id no numérico e id menor que 1 (con `errors[]`); 404 con `ProblemDetail`; 405 con POST; 406 con `Accept: application/xml`; 500 con excepción inesperada sin filtrar detalles. Todos con `Content-Type: application/problem+json`.
- [ ] **T-15** (verde) `PriceController` implementando la interfaz `PricesApi` generada, `PriceRestMapper` (dominio a DTO generado) y `GlobalExceptionHandler` (`@RestControllerAdvice`) con `ProblemDetail`.

## Fase 5: Aceptación (los 5 del enunciado)
- [ ] **T-16** (rojo) `@SpringBootTest` + `MockMvc`:
  - Test 1: 2020-06-14T10:00 → lista 1, 35.50
  - Test 2: 2020-06-14T16:00 → lista 2, 25.45
  - Test 3: 2020-06-14T21:00 → lista 1, 35.50
  - Test 4: 2020-06-15T10:00 → lista 3, 30.50
  - Test 5: 2020-06-16T21:00 → lista 4, 38.95
- [ ] **T-17** (verde) Ajustes de cableado (`BeanConfig`) hasta que pasen.
- [ ] **T-18** Escenarios adicionales 6 a 10 de la spec (404, límites, desempate) como tests de aceptación.

## Fase 6: Calidad
- [ ] **T-19** ArchUnit: dominio sin dependencias de Spring/JPA/Jackson; aplicación sin infraestructura; sin ciclos entre capas.
- [ ] **T-20** Jacoco con umbral (90% dominio y aplicación) y Spotless en `mvn verify`.
- [ ] **T-21** Test de contrato: las respuestas reales (200 y errores) validan contra `prices-api.yaml`. Publicar el YAML con Swagger UI si hay una integración compatible con Boot 4; si no, enlazarlo en el README.
- [ ] **T-22** README final: ejecución, `curl` de ejemplo, ADRs, supuestos, cómo correr tests.
- [ ] **T-23** (extra) Dockerfile multi-stage y plantilla de PR.

## Fase 7: Seguridad (opcional)
- [ ] **T-24** (rojo) Tests: sin `X-API-Key` → 401 `ProblemDetail`; clave inválida → 401; clave sin rol → 403; clave válida → 200.
- [ ] **T-25** (verde) Spring Security mínima con filtro de API key y `AuthenticationEntryPoint` / `AccessDeniedHandler` que emitan `ProblemDetail`. Los 5 tests del enunciado envían una clave de test configurada en `application-test.yml`.

## Definición de hecho
- `mvn clean verify` en verde (tests, Spotless, Jacoco, ArchUnit).
- Los 5 tests del enunciado pasan con los valores exactos.
- Historial de git que muestre ciclos TDD.
- README con decisiones y supuestos.
