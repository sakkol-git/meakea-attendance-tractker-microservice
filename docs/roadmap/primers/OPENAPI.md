# Contract-first APIs with OpenAPI specification

> **Note:** The OpenAPI Specification (OAS) establishes a machine-readable, language-agnostic contract defining API endpoints, request parameters, response bodies, and security schemas before or alongside implementation.

## Purpose and core concept

The OpenAPI Specification provides a standardized format (YAML or JSON) to describe REST APIs. It establishes a single source of truth for both human documentation and automated code generation.

Instead of writing backend controllers first and manually updating client libraries or wiki pages, contract-first design aligns frontend and backend teams around a shared, unambiguous API contract.

## Problem being solved

Developing frontend clients and backend services without a machine-readable specification creates major integration friction:

- Frontend blocked by backend: Mobile and web developers cannot start writing views until backend engineers finish implementing and deploying real endpoints.
- Integration drift: Backend developers change a JSON field name from `employeeId` to `emp_id` without notifying frontend developers, causing production runtime parsing errors.
- Outdated documentation: Hand-written API markdown tables quickly become outdated as code evolves, misleading new developers.

## Simple mental model

An OpenAPI specification is like an architectural blueprint for a prefabricated building.

Before construction workers pour the concrete foundation or assemble the drywall, the blueprint defines the exact dimensions of every doorframe and window opening. Window manufacturers can fabricate the windows in their factory simultaneously, confident that the windows will fit into the doorframes when delivered to the job site.

## How it works

The contract-driven development lifecycle:

1. Contract definition: Engineers author `contracts/openapi.yaml`, defining paths (e.g., `/api/v1/attendance/check-in`), HTTP methods, request bodies, and status codes.
1. Schema modeling: Reusable components (such as `AttendanceSessionDto` and `ErrorResponse`) are defined under the `components/schemas` section.
1. Client code generation: Mobile engineers use `openapi-generator-cli` to generate strongly-typed Dart client models and API service classes.
1. Mock server generation: Frontend engineers generate a local mock server using Prism or MSW, enabling full UI development and unit testing before backend endpoints exist.
1. Backend validation: Backend integration tests validate that real Spring Boot controller responses conform strictly to the OpenAPI schema.

## Why HRFlow needs it

In HRFlow's cross-functional team structure:

- Parallel work streams: Contributor A builds Spring Boot backend services while Contributor B develops the Flutter mobile client.
- Week 01 mock milestone: In Week 01, Contributor B launches the Flutter mobile shell using generated mock models from `contracts/openapi.yaml` without waiting for backend database migrations.
- Single wire authority: Eliminates conflicting endpoint definitions between legacy markdown documentation and active code.

## Minimal example

A concise OpenAPI 3.1 specification snippet:

```yaml
openapi: 3.1.0
info:
  title: HRFlow Attendance API
  version: 1.0.0
paths:
  /api/v1/attendance/check-in:
    post:
      summary: Record employee check-in
      operationId: recordCheckIn
      security:
        - bearerAuth: []
      parameters:
        - in: header
          name: Idempotency-Key
          schema:
            type: string
            format: uuid
          required: false
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CheckInRequest'
      responses:
        '201':
          description: Attendance session created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AttendanceSession'
        '409':
          description: Duplicate check-in or state conflict

components:
  schemas:
    CheckInRequest:
      type: object
      required:
        - workDate
      properties:
        workDate:
          type: string
          format: date
```

## Application-specific implementation

Generating Flutter client models via OpenAPITools in `pubspec.yaml`:

```yaml
dev_dependencies:
  openapi_generator_cli: ^5.0.0
```

Generation command:

```bash
# Generate Dart client SDK from canonical contract
openapi-generator-cli generate \
  -i contracts/openapi.yaml \
  -g dart-dio \
  -o clients/mobile/lib/api \
  --additional-properties=pubName=hrflow_api
```

The generated `hrflow_api` package provides strongly-typed models (`CheckInRequest`, `AttendanceSession`) and an `AttendanceApi` client that serializes JSON and handles authentication tokens automatically.

## Common mistakes and pitfalls

Avoid these OpenAPI specification mistakes:

> **Pitfall:** Defining ambiguous response schemas with missing status codes. Omitting standard error responses (`401`, `403`, `404`, `409`, `422`) leaves frontend developers unprepared for error handling. Always define complete response status codes.

- Modifying generated code directly: Never manually edit generated Dart or TypeScript files in `clients/mobile/lib/api`. Any subsequent re-generation will overwrite manual edits. Customize behavior via wrapper classes.
- Contract drift: Updating backend controller annotations without updating `contracts/openapi.yaml`. In a contract-first workflow, the YAML specification is the authoritative source of truth.
- Overusing untyped `object` schemas: Defining schemas with `type: object` and no property definitions destroys the benefit of strong typing. Always define explicit properties and types.

## Testing and debugging

Validation routine:

- Specification linter: Run `npx @stoplight/spectral-cli lint contracts/openapi.yaml` to detect broken `$ref` pointers and missing descriptions.
- Mock server verification: Launch Prism mock server: `npx @stoplight/prism-cli mock contracts/openapi.yaml -p 4010`. Verify that `curl http://localhost:4010/api/v1/attendance/check-in` returns the documented schema.
- Schema conformity assertion: Use `open-api-spec-validator` in integration tests to verify that live HTTP responses match OpenAPI schema definitions.

## Trade-offs and alternatives

API design approaches:

- Contract-first OpenAPI (HRFlow choice): Enables parallel frontend/backend development, language-agnostic code generation, single source of truth; requires authoring and maintaining YAML files.
- Code-first (Springdoc / Swagger annotations): Generates OpenAPI YAML from Java annotations; however, requires backend code to exist before frontend can begin and tightly couples contracts to Java types.
- GraphQL: Single endpoint with flexible client queries; requires complex query depth protection, caching workarounds, and steep learning curves.

## When not to use it

Do not use OpenAPI for:

- Binary streaming protocols: Video streaming, WebRTC, or raw socket connections cannot be modeled effectively in standard OpenAPI.
- Internal event envelopes: Event payloads published to outbox tables or message queues should use JSON Schema or Avro, rather than OpenAPI HTTP path contracts.

## Related documentation

- [Offline command queue primer](OFFLINE-QUEUE.md)
- [API and database design](../../REST-API-DATABASE-DESIGN.md)
- [Week 01 foundation build guide](../01-FOUNDATION/BUILD-GUIDE.md)
- [Week 08 web and mobile integration build guide](../08-WEB-AND-MOBILE-INTEGRATION/BUILD-GUIDE.md)
