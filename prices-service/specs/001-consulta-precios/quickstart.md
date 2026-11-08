# Quickstart de desarrollo

## Requisitos

- JDK 21.
- Maven Wrapper incluido en el proyecto al implementarlo.
- Git.

## Build y pruebas

Desde la raíz del proyecto, en Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

`verify` debe ejecutar pruebas unitarias, de aplicación, persistencia, web y aceptación; reglas ArchUnit; Spotless; y JaCoCo con cobertura mínima del 80% de líneas. La generación OpenAPI se engancha a `generate-sources`, por lo que `verify` comprueba también que la interfaz y los DTOs generados compilan.

## Ejecución local

```powershell
.\mvnw.cmd spring-boot:run
```

H2 se crea en memoria y carga el esquema y los cuatro registros de prueba desde `schema.sql` y `data.sql`.

## Consulta de ejemplo

```powershell
curl.exe "http://localhost:8080/api/v1/prices?applicationDate=2020-06-14T10:00:00&productId=35455&brandId=1"
```

El resultado esperado incluye `priceList: 1`, `price: 35.50` y `currency: EUR`. El contrato OpenAPI está en `src/main/resources/openapi/prices-api.yaml`; la hora no lleva offset ni zona.
