# Prices Service

Servicio REST para consultar el precio aplicable a un producto y una cadena en una fecha y hora determinada. El contrato de la API se mantiene en `src/main/resources/openapi/prices-api.yaml`.

## Supuestos del producto

- **S-1 — Hora local:** `applicationDate` se interpreta como fecha y hora local, sin zona ni offset.
- **S-2 — Vigencia inclusiva:** el precio es aplicable tanto en `startDate` como en `endDate`.
- **S-3 — Datos y moneda:** se usan los cuatro registros de referencia del enunciado; los precios están expresados en EUR y deben producir los cinco resultados descritos en la especificación.
- **S-4 — Selección determinista:** gana la tarifa de mayor prioridad; si hay empate, gana el `priceList` mayor.
- **S-5 — Alcance:** el servicio solo consulta precios existentes; no crea tarifas, convierte monedas, autentica clientes ni limita peticiones.

Los identificadores de producto y cadena deben ser positivos. Si no hay una tarifa aplicable, la respuesta es `404`.
