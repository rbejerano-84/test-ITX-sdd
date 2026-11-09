# Tareas: Consulta de precios por producto, cadena y fecha

**Entrada**: documentos de diseño en `specs/001-consulta-precios/`  
**Prerrequisitos**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `quickstart.md`

## Reglas de ejecución

- Seguir el orden indicado. Para cada comportamiento, completar primero la tarea **roja** (`test:`), comprobar que falla por el motivo esperado y después la tarea **verde** (`feat:`).
- Cada tarea debe dejar la build en verde y terminar en un commit pequeño. Usar `test:` para pruebas, `feat:` para implementación y `refactor:` para refactorizaciones sin cambio de comportamiento.
- No incluir las tareas opcionales de seguridad T-24 y T-25 del seed: autenticación y limitación de peticiones están fuera de alcance.

## Fase 0: Arranque y contrato

- [X] **T-01** Inicializar el proyecto Maven con Java 21, Spring Boot 4.1.x y Maven Wrapper; añadir web, data-jpa, validation, H2 y las dependencias/plugins de ArchUnit, JaCoCo y Spotless. Configurar la estructura base de paquetes. Commit `feat:`.
- [X] **T-02** Crear `README.md` base y reflejar los supuestos S-1 a S-5 del plan, incluyendo hora local sin zona, rangos inclusivos y resolución determinista. Commit `feat:`.
- [X] **T-03a** Revisar y completar `src/main/resources/openapi/prices-api.yaml` como contrato fuente de verdad: parámetros, `PriceResponse`, `ProblemDetail`, `FieldError`, las diez respuestas 200/400/401/403/404/405/406/429/500/503, ejemplos y respuestas de gateway/infraestructura. Validar el YAML con un linter (por ejemplo, Spectral) sin errores. Commit `feat:`.
- [X] **T-03b** Hacer el spike de OpenAPI Generator: fijar versión y opciones compatibles con Boot 4.1/Java 21; configurar `interfaceOnly`, generación en `target/generated-sources`, paquetes `generated` y mapeo de `DateTime` a `LocalDateTime`. Ejecutar `mvn generate-sources` y compilar `PricesApi`, `PriceResponse`, `ProblemDetail` y `FieldError`. Comprobar y resolver explícitamente el posible choque entre el `ProblemDetail` generado y `org.springframework.http.ProblemDetail` (paquete/imports o mapeo apropiado), sin versionar `target/`. Si no hay solución compatible, activar el plan B del plan y documentarlo en README. Commit `feat:`.

## Fase 1: Dominio

- [X] **T-04 (rojo)** Escribir pruebas unitarias para `BrandId`, `ProductId` y `PriceListId` (solo positivos), `Money` (escala de dos decimales y moneda válida) y `DateRange` (inicio no posterior al fin y `contains` inclusivo). Ejecutar y comprobar que fallan. Commit `test:`.
- [X] **T-05 (verde)** Implementar los value objects anteriores como `record`, con validación en sus constructores, hasta pasar T-04. Commit `feat:`.
- [X] **T-06 (rojo)** Escribir pruebas de `PriceSelector`: gana mayor prioridad; tarifa única; colección vacía; inicio y fin inclusivos; empate resuelto por `priceList` mayor. Comprobar el fallo esperado. Commit `test:`.
- [X] **T-07 (verde)** Implementar `Price`, `PriceSelector` y el comparador/estrategia de desempate. Resolver la decisión entre selección en dominio y consulta SQL y documentar ADR 1/2 en `research.md` o README, manteniendo el dominio como fuente de la regla. Commit `feat:`.
- [X] **T-08 (refactor)** Revisar nombres y lenguaje ubicuo del dominio sin cambiar comportamiento; mantener todas las pruebas verdes. Commit `refactor:`.

## Fase 2: Aplicación

- [X] **T-09 (rojo)** Escribir pruebas de `GetApplicablePriceService` con `PriceRepository` mockeado y `PriceSelector` real: devuelve la tarifa ganadora y lanza `PriceNotFoundException` cuando no hay candidatas. Comprobar que fallan. Commit `test:`.
- [X] **T-10 (verde)** Definir `GetApplicablePriceUseCase` y `PriceRepository` como puertos e implementar `GetApplicablePriceService` para pasar T-09; incluir los datos de consulta en `PriceNotFoundException`. Commit `feat:`.

## Fase 3: Persistencia

- [X] **T-11a (rojo)** Escribir un `@DataJpaTest` que compruebe la carga de los cuatro registros iniciales y la estructura necesaria para consultarlos. Ejecutar y comprobar el fallo esperado antes de crear el esquema y los datos. Commit `test:`.
- [X] **T-11b (verde)** Crear `schema.sql` y `data.sql` con los cuatro registros de referencia y el índice compuesto previsto; pasar T-11a. Commit `feat:`.
- [X] **T-12 (rojo)** Ampliar `@DataJpaTest` para comprobar que la consulta devuelve solo candidatas por producto, cadena y fecha en cada escenario, incluidos los extremos del periodo. Comprobar el fallo esperado antes de implementar el adaptador. Commit `test:`.
- [X] **T-13 (verde)** Implementar `PriceEntity`, `PriceJpaRepository`, `PriceEntityMapper` y `PricePersistenceAdapter`, sin trasladar la regla final de prioridad/desempate fuera del dominio. Pasar T-12. Commit `feat:`.

## Fase 4: REST

- [X] **T-14 (rojo)** Escribir pruebas `@WebMvcTest` del controlador y del advice, una por cada respuesta de aplicación del plan: 200 con JSON esperado; 400 por parámetro ausente, fecha inválida, ID no numérico o menor que 1, con `errors[]`; 404 con `ProblemDetail`; POST → 405; `Accept: application/xml` → 406; excepción inesperada → 500 genérico sin filtrar detalles. Verificar `application/problem+json` en errores y comprobar que las pruebas fallan. Commit `test:`.
- [X] **T-15 (verde)** Implementar `PriceController` sobre la interfaz `PricesApi` generada, `PriceRestMapper` hacia DTOs generados y `GlobalExceptionHandler` como único `@RestControllerAdvice`, usando `ProblemDetail` sin ambigüedad con el tipo generado. Pasar T-14. Commit `feat:`.

## Fase 5: Aceptación

- [ ] **T-16 (rojo)** Escribir `@SpringBootTest` con MockMvc para los cinco casos del enunciado y sus resultados exactos: 2020-06-14T10:00 → lista 1, 35.50; 2020-06-14T16:00 → lista 2, 25.45; 2020-06-14T21:00 → lista 1, 35.50; 2020-06-15T10:00 → lista 3, 30.50; 2020-06-16T21:00 → lista 4, 38.95. Comprobar el fallo esperado. Commit `test:`.
- [ ] **T-17 (verde)** Ajustar el cableado de `BeanConfig` y la integración hasta pasar los cinco tests de aceptación. Commit `feat:`.
- [ ] **T-18a (rojo)** Añadir tests de aceptación para los escenarios adicionales de la spec: producto inexistente y fechas sin tarifa → 404; límites inclusivos; desempate por `priceList` mayor; parámetros ausentes/inválidos; método y formato no admitidos; error inesperado sin detalles internos. Cubrir los escenarios adicionales 6 a 10 previstos en el seed y comprobar fallos de regresión cuando corresponda. Commit `test:`.
- [ ] **T-18b (verde)** Implementar cualquier comportamiento faltante que revelen T-18a sin duplicar reglas entre capas; pasar los tests de aceptación y MVC relacionados. Commit `feat:`.

## Fase 6: Arquitectura y calidad

- [ ] **T-19a (rojo)** Escribir reglas ArchUnit que comprueben que el dominio no depende de Spring/JPA/Jackson, que aplicación no depende de infraestructura y que no hay ciclos entre capas. Confirmar que detectan una violación de forma controlada y comprobar el estado inicial. Commit `test:`.
- [ ] **T-19b (verde/refactor)** Corregir cualquier violación arquitectónica detectada por T-19a y dejar las reglas verdes, preservando los límites hexagonales. Commit `refactor:`.
- [ ] **T-20** Configurar JaCoCo para exigir al menos 80% de cobertura de líneas global y Spotless para comprobar formato durante `mvn verify`; ejecutar `mvn clean verify` y ajustar el formato/cobertura mediante pruebas relevantes, no reduciendo el umbral. Commit `feat:`.
- [ ] **T-21a (rojo)** Crear un test de contrato que valide las respuestas reales 200 y de error contra `src/main/resources/openapi/prices-api.yaml`; comprobar que detecta discrepancias y que pasa con la implementación actual. Commit `test:`.
- [ ] **T-21b (verde/refactor)** Corregir cualquier discrepancia de contrato que revele T-21a; publicar el YAML con Swagger UI solo si existe una integración compatible con Boot 4, o enlazarlo desde README si no. Commit `feat:` o `refactor:` según el cambio.
- [ ] **T-22** Completar `README.md` con ejecución, ejemplo `curl`, ADRs, supuestos y comandos de pruebas; validar el quickstart. Commit `feat:`.
- [ ] **T-23 (extra)** Añadir Dockerfile multi-stage y plantilla de PR del seed solo si se desea entregar el extra; no bloquea la definición de hecho. Commit `feat:`.

## Dependencias y orden

- T-01 a T-03b establecen el proyecto y el contrato antes del código dependiente de OpenAPI.
- T-04 → T-05, T-06 → T-07 y T-09 → T-10 son ciclos rojo-verde explícitos; T-08 refactoriza el dominio ya probado.
- T-11a → T-11b → T-12 → T-13 prepara datos y luego implementa la consulta de persistencia contra pruebas previas.
- T-14 → T-15, T-16 → T-17 y T-18a → T-18b mantienen el orden TDD en REST y aceptación.
- T-19a → T-19b conserva ArchUnit del seed y corrige lo detectado; T-20 configura la calidad y T-21a → T-21b mantiene el orden TDD del contrato antes de su publicación.
- T-22 y T-23 completan la documentación y el extra. Omitir T-24 y T-25 del seed (API key, autenticación y `SecurityConfig`), que son opcionales y están fuera de alcance.

## Definición de hecho

- `mvn clean verify` está verde, incluidos tests, Spotless, JaCoCo con mínimo global de líneas del 80%, ArchUnit y contrato.
- Los cinco escenarios del enunciado pasan con los valores exactos; también pasan los casos de error, ausencia de tarifa, límites y desempate.
- El historial contiene ciclos TDD con commits pequeños `test:`, `feat:` y `refactor:`.
- README documenta ejecución, decisiones y supuestos; no hay código generado de `target/` versionado.
