# Feature Specification: Consulta de precios por producto, cadena y fecha

**Feature Branch**: `001-consulta-precios`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: Servicio REST de consulta de precios de un producto de una cadena en una fecha dada.

## User Scenarios & Testing

### User Story 1 - Consultar el precio aplicable (Priority: P1)

Como consumidor del servicio, quiero consultar el precio de un producto de una cadena para una fecha y hora concretas, para conocer el precio que corresponde aplicar.

**Why this priority**: Es el propósito principal del servicio y aporta valor en cada consulta válida.

**Independent Test**: Consultar los datos de producto y cadena indicados para cada fecha de los cinco escenarios y comprobar el precio y la lista devueltos.

**Acceptance Scenarios**:

Datos comunes: producto `35455`, cadena `1` y los cuatro registros de precios del enunciado.

| # | Given | When (`applicationDate`) | Then (`priceList`) | Then (`price`) |
|---|---|---|---:|---:|
| 1 | Los cuatro registros están disponibles. | `2020-06-14T10:00:00` | 1 | 35.50 |
| 2 | Los cuatro registros están disponibles. | `2020-06-14T16:00:00` | 2 | 25.45 |
| 3 | Los cuatro registros están disponibles. | `2020-06-14T21:00:00` | 1 | 35.50 |
| 4 | Los cuatro registros están disponibles. | `2020-06-15T10:00:00` | 3 | 30.50 |
| 5 | Los cuatro registros están disponibles. | `2020-06-16T21:00:00` | 4 | 38.95 |

En cada respuesta satisfactoria se identifican el producto, la cadena, la lista aplicada, su periodo de vigencia, el precio y la moneda (`EUR`).

### User Story 2 - Reconocer consultas sin resultado (Priority: P2)

Como consumidor del servicio, quiero recibir una respuesta inequívoca cuando no exista un precio aplicable, para no confundir la ausencia de precio con un precio válido.

**Why this priority**: Evita aplicar o interpretar un precio incorrecto cuando faltan datos coincidentes.

**Independent Test**: Consultar un producto inexistente y fechas fuera de los periodos disponibles y comprobar que cada consulta comunica que no existe un precio aplicable.

**Acceptance Scenarios**:

1. **Given** un producto que no existe, **When** se consulta para una fecha y cadena, **Then** la respuesta indica que no se encontró un precio (`404`).
2. **Given** un producto y cadena con precios disponibles, **When** se consulta `2020-06-13T23:59:59`, **Then** la respuesta indica que no se encontró un precio (`404`).
3. **Given** un producto y cadena con precios disponibles, **When** se consulta una fecha posterior a todos sus periodos de vigencia, **Then** la respuesta indica que no se encontró un precio (`404`).

### User Story 3 - Recibir resultados claros en entradas inválidas y límites (Priority: P3)

Como consumidor del servicio, quiero que se indiquen las entradas inválidas y se respeten los límites de vigencia, para corregir la consulta y obtener el precio correcto en los extremos de cada periodo.

**Why this priority**: Aclara los casos límite de las consultas y permite tratar errores sin confundirlos con resultados válidos.

**Independent Test**: Probar cada parámetro obligatorio ausente o inválido, y comprobar consultas en los extremos de vigencia y con tarifas de igual prioridad.

**Acceptance Scenarios**:

1. **Given** una tarifa cuya vigencia comienza en la fecha consultada, **When** se solicita el precio en ese instante exacto, **Then** la tarifa es aplicable.
2. **Given** una tarifa cuya vigencia termina en la fecha consultada, **When** se solicita el precio en ese instante exacto, **Then** la tarifa es aplicable.
3. **Given** dos tarifas aplicables con la misma prioridad, **When** se consulta su periodo coincidente, **Then** se devuelve la tarifa cuyo `priceList` sea mayor.
4. **Given** falta `applicationDate`, `productId` o `brandId`, **When** se realiza la consulta, **Then** se indica el parámetro ausente y la respuesta es `400`.
5. **Given** `applicationDate` no representa una fecha y hora válida, **When** se realiza la consulta, **Then** se indica el error de formato y la respuesta es `400`.
6. **Given** `productId` o `brandId` no es numérico o está fuera del rango admitido, **When** se realiza la consulta, **Then** se indica el parámetro inválido y la respuesta es `400`.
7. **Given** se usa un método no admitido para la consulta, **When** se solicita el recurso, **Then** la respuesta es `405`.
8. **Given** el cliente solicita un formato de respuesta no admitido, **When** se realiza la consulta, **Then** la respuesta es `406`.
9. **Given** ocurre un error inesperado, **When** se realiza la consulta, **Then** se comunica un error genérico `500` sin exponer detalles internos.

## Requirements

### Functional Requirements

- **FR-001**: El servicio MUST permitir consultar un precio mediante `GET /api/v1/prices`, indicando `applicationDate`, `productId` y `brandId`.
- **FR-002**: Cuando exista una tarifa aplicable, el servicio MUST devolver el producto, la cadena, la lista de precios, el inicio y fin de vigencia, el precio y la moneda.
- **FR-003**: Una tarifa MUST ser aplicable cuando la fecha y hora consultadas estén dentro de su periodo de vigencia, incluidos ambos extremos.
- **FR-004**: Si varias tarifas son aplicables, el servicio MUST seleccionar la de mayor prioridad. Si también empatan en prioridad, MUST seleccionar la de mayor `priceList`.
- **FR-005**: Si no existe tarifa aplicable para el producto, la cadena y la fecha consultados, el servicio MUST responder `404` y no MUST devolver una tarifa alternativa.
- **FR-006**: Si falta un parámetro obligatorio, o si un parámetro no cumple el formato o rango admitido, el servicio MUST responder `400` e identificar el parámetro con el error.
- **FR-007**: Las respuestas de error MUST seguir el formato Problem Details acordado por el proyecto, con `title`, `status`, `detail` e `instance`; los errores de validación MUST incluir los errores asociados a cada parámetro cuando corresponda.
- **FR-008**: Los errores inesperados MUST responder `500` sin revelar detalles internos.
- **FR-009**: El resultado MUST incluir el precio y la moneda de la tarifa seleccionada sin conversión monetaria.
- **FR-010**: El servicio MUST permitir distinguir una solicitud con resultado, una solicitud inválida y una solicitud válida sin tarifa aplicable mediante sus respuestas correspondientes (`200`, `400` y `404`).

### Key Entities

- **Consulta de precio**: Producto, cadena y fecha/hora para los que se solicita el precio.
- **Tarifa**: Precio ofrecido para un producto y una cadena durante un periodo de vigencia, identificado por una lista y una prioridad, e indicado en una moneda.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Los cinco escenarios de aceptación devuelven respectivamente las listas `1`, `2`, `1`, `3` y `4`, y los precios `35.50`, `25.45`, `35.50`, `30.50` y `38.95`.
- **SC-002**: Todas las consultas sin tarifa aplicable de los escenarios descritos responden `404`.
- **SC-003**: Todas las entradas ausentes o inválidas de los escenarios descritos responden `400` e identifican el parámetro correspondiente.
- **SC-004**: Las consultas exactamente en el inicio o fin de vigencia incluyen la tarifa; los empates de prioridad se resuelven a favor de la lista mayor.

## Assumptions

- `applicationDate` representa una fecha y hora local sin información de zona horaria, conforme al supuesto del enunciado.
- Los extremos del periodo de vigencia son inclusivos.
- Los cuatro registros y los cinco resultados de referencia son los indicados en el enunciado; sus precios están expresados en euros.
- La selección por prioridad y el desempate por el identificador de lista siguen las reglas deterministas establecidas por la constitución del proyecto.
- La consulta solo lee precios existentes; crear o modificar tarifas, convertir monedas, autenticación y limitación de peticiones quedan fuera de esta funcionalidad.
- Los límites numéricos concretos de los identificadores no están definidos en el enunciado; cualquier valor fuera del rango admitido por el servicio se considera entrada inválida.
