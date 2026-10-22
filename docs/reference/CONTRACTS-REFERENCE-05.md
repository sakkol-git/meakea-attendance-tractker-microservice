# Contracts reference 05

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## 3. Endpoint catalog

All relative paths in entries are shown as full public paths. Every endpoint is planned. The preserved catalog is extended only where a necessary workflow/status/security gap is identified explicitly.

### API-001: POST /api/v1/auth/login

- Owner: `identity-org`; status: planned.
- Purpose and validation: Authenticate within tenant; generic invalid-credential error; no account enumeration.
- Authorization and tenant scope: public; tenantSlug required; shared token/tenant/object rules apply.
- Profile: AUTH; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Login; response schema: Tokens.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"tenantSlug":"demo","email":"employee@example.test","password":"example-only-secret"}`
- Response example: `{"accessToken":"example-only-jwt","tokenType":"Bearer","expiresIn":600,"refreshToken":"example-only-rotated-token"}`

### API-002: POST /api/v1/auth/refresh

- Owner: `identity-org`; status: planned.
- Purpose and validation: Rotate token family once under lock; replay revokes family; serialize client refresh.
- Authorization and tenant scope: public possession of valid refresh secret; shared token/tenant/object rules apply.
- Profile: AUTH; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Refresh; response schema: Tokens.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"refreshToken":"example-only-opaque-token"}`
- Response example: `{"accessToken":"example-only-jwt","tokenType":"Bearer","expiresIn":600,"refreshToken":"example-only-rotated-token"}`

### API-003: POST /api/v1/auth/logout

- Owner: `identity-org`; status: planned.
- Purpose and validation: Revoke family and clear web cookie; success 204 overrides MARK; idempotent revocation; secret response not cached.
- Authorization and tenant scope: authenticated own token family; shared token/tenant/object rules apply.
- Profile: MARK; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Refresh; response schema: Empty.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"refreshToken":"example-only-opaque-token"}`
- Response example: no body.

### API-004: GET /api/v1/auth/me

- Owner: `identity-org`; status: planned.
- Purpose and validation: Return own identity plus freshness-labeled local assignment.
- Authorization and tenant scope: any authenticated user; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Empty; response schema: Profile.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"userId":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","tenantId":"019a0000-0000-7000-8000-000000000020","roles":["EMPLOYEE"],"locale":"en","shift":null}`

### API-005: POST /api/v1/auth/change-password

- Owner: `identity-org`; status: planned.
- Purpose and validation: No If-Match required; lock credential; revoke other refresh families; 204 override; credential key stores only safe result.
- Authorization and tenant scope: own user; recent authentication; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: PasswordChange; response schema: Empty.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"currentPassword":"example-old","newPassword":"example-new-long-password"}`
- Response example: no body.

### API-006: GET /.well-known/jwks.json

- Owner: `identity-org`; status: planned.
- Purpose and validation: Root public path; cache headers; public keys only; no tenant lookup.
- Authorization and tenant scope: public; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Empty; response schema: JWKS.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"keys":[{"kty":"RSA","kid":"example-key","use":"sig","alg":"RS256","n":"example-modulus","e":"AQAB"}]}`

### API-007: POST /api/v1/tenants

- Owner: `identity-org`; status: planned.
- Purpose and validation: Provision tenant and initial OWNER atomically; separate platform key scope; no initial password in response.
- Authorization and tenant scope: platform control-plane administrator; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: TenantInput; response schema: Tenant.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.employee.v1 if employee provisioned.
- Request example: `{"name":"Demo Company","slug":"demo","timezone":"Asia/Phnom_Penh","country":"KH","ownerEmail":"owner@example.test"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","name":"Demo Company","slug":"demo","timezone":"Asia/Phnom_Penh","country":"KH","status":"ACTIVE","version":1}`

### API-008: GET /api/v1/tenants/current

- Owner: `identity-org`; status: planned.
- Purpose and validation: Read current company settings.
- Authorization and tenant scope: OWNER own tenant; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Empty; response schema: Tenant.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","name":"Demo Company","slug":"demo","timezone":"Asia/Phnom_Penh","country":"KH","status":"ACTIVE","version":1}`

### API-009: PATCH /api/v1/tenants/current

- Owner: `identity-org`; status: planned.
- Purpose and validation: Change settings; timezone changes affect future assignments only unless reviewed recalculation.
- Authorization and tenant scope: OWNER own tenant; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: TenantPatch; response schema: Tenant.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"name":"Demo Company Updated"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","name":"Demo Company","slug":"demo","timezone":"Asia/Phnom_Penh","country":"KH","status":"ACTIVE","version":1}`

### API-010: GET /api/v1/employees

- Owner: `identity-org`; status: planned.
- Purpose and validation: List employees; q bounded and tenant-scoped.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query departmentId,status,q.
- Request schema: Empty; response schema: Page`<Employee>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","employeeNo":"E001","fullName":"Demo Employee","departmentId":null,"positionId":null,"managerId":null,"hireDate":"2026-10-01","status":"ACTIVE","version":1}],"page":0,"size":20,"total":1}`

### API-011: POST /api/v1/employees

- Owner: `identity-org`; status: planned.
- Purpose and validation: Create employee; unique employeeNo and user link; no implicit password provisioning.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: EmployeeInput; response schema: Employee.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.employee.v1.
- Request example: `{"employeeNo":"E001","fullName":"Demo Employee","hireDate":"2026-10-01","employmentType":"FULL_TIME"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeNo":"E001","fullName":"Demo Employee","departmentId":null,"positionId":null,"managerId":null,"hireDate":"2026-10-01","status":"ACTIVE","version":1}`

### API-012: GET /api/v1/employees/{id}

- Owner: `identity-org`; status: planned.
- Purpose and validation: Read permitted employee detail; managers use team-specific views.
- Authorization and tenant scope: HR_ADMIN or self; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Employee.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeNo":"E001","fullName":"Demo Employee","departmentId":null,"positionId":null,"managerId":null,"hireDate":"2026-10-01","status":"ACTIVE","version":1}`

### API-013: PATCH /api/v1/employees/{id}

- Owner: `identity-org`; status: planned.
- Purpose and validation: Update employee; same-tenant references and acyclic manager graph.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: EmployeePatch; response schema: Employee.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.employee.v1.
- Request example: `{"fullName":"Updated Employee"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeNo":"E001","fullName":"Demo Employee","departmentId":null,"positionId":null,"managerId":null,"hireDate":"2026-10-01","status":"ACTIVE","version":1}`

### API-014: POST /api/v1/employees/{id}/terminate

- Owner: `identity-org`; status: planned.
- Purpose and validation: Terminate on effective endDate; revoke linked refresh families and future schedule eligibility.
- Authorization and tenant scope: HR_ADMIN; not self administrative bypass; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Termination; response schema: Employee.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.employee.v1.
- Request example: `{"endDate":"2026-10-31","reason":"Contract ended"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeNo":"E001","fullName":"Demo Employee","departmentId":null,"positionId":null,"managerId":null,"hireDate":"2026-10-01","status":"ACTIVE","version":1}`

### API-015: PUT /api/v1/employees/{id}/device

- Owner: `identity-org`; status: planned.
- Purpose and validation: Bind pseudonymous installation; replacement invalidates prior binding; no biometric template.
- Authorization and tenant scope: self; recent authentication; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: DeviceBinding; response schema: Employee.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.employee.v1.
- Request example: `{"deviceId":"installation-example","platform":"ANDROID"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeNo":"E001","fullName":"Demo Employee","departmentId":null,"positionId":null,"managerId":null,"hireDate":"2026-10-01","status":"ACTIVE","version":1}`

### API-016: GET /api/v1/employees/{id}/direct-reports

- Owner: `identity-org`; status: planned.
- Purpose and validation: List current direct reports; no recursive tenant-wide scope.
- Authorization and tenant scope: MANAGER where id=self, or HR_ADMIN; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; query status.
- Request schema: Empty; response schema: Page`<Employee>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","employeeNo":"E001","fullName":"Demo Employee","departmentId":null,"positionId":null,"managerId":null,"hireDate":"2026-10-01","status":"ACTIVE","version":1}],"page":0,"size":20,"total":1}`

### API-017: GET /api/v1/departments

- Owner: `identity-org`; status: planned.
- Purpose and validation: Read organization departments.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query q.
- Request schema: Empty; response schema: Page`<NamedResource>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","name":"Operations","parentId":null,"version":1}],"page":0,"size":20,"total":1}`

### API-018: POST /api/v1/departments

- Owner: `identity-org`; status: planned.
- Purpose and validation: Create department; acyclic parent; same-tenant FK.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: NamedInput; response schema: NamedResource.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.employee.v1 for affected display snapshots.
- Request example: `{"name":"Operations"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","name":"Operations","parentId":null,"version":1}`

### API-019: PATCH /api/v1/departments/{id}

- Owner: `identity-org`; status: planned.
- Purpose and validation: Update department; propagate labels asynchronously.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: NamedInput; response schema: NamedResource.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.employee.v1 for affected snapshots.
- Request example: `{"name":"Operations"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","name":"Operations","parentId":null,"version":1}`

### API-020: DELETE /api/v1/departments/{id}

- Owner: `identity-org`; status: planned.
- Purpose and validation: Soft delete; reject active assignments/children unless explicitly reassigned.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: REMOVE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Empty.
- Success: 204; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.employee.v1 for affected snapshots.
- Request example: no body.
- Response example: no body.

### API-021: GET /api/v1/positions

- Owner: `identity-org`; status: planned.
- Purpose and validation: Read positions; title required for position inputs; name compatibility is display label.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query q.
- Request schema: Empty; response schema: Page`<Position>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","title":"Operations officer","level":"L1","version":1}],"page":0,"size":20,"total":1}`

### API-022: POST /api/v1/positions

- Owner: `identity-org`; status: planned.
- Purpose and validation: Create position using title/level; unique active tenant title.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: PositionInput; response schema: Position.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"title":"Operations officer","level":"L1"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","title":"Operations officer","level":"L1","version":1}`

### API-023: GET /api/v1/work-sites

- Owner: `identity-org`; status: planned.
- Purpose and validation: Read permitted work sites and evidence configuration.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query q.
- Request schema: Empty; response schema: Page`<Site>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","name":"Office","latitude":11.56,"longitude":104.92,"radiusM":100,"timezone":"Asia/Phnom_Penh","version":1}],"page":0,"size":20,"total":1}`

### API-024: POST /api/v1/work-sites

- Owner: `identity-org`; status: planned.
- Purpose and validation: Create site; validate coordinates/radius/timezone.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: SiteInput; response schema: Site.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"name":"Office","latitude":11.56,"longitude":104.92,"radiusM":100,"timezone":"Asia/Phnom_Penh"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","name":"Office","latitude":11.56,"longitude":104.92,"radiusM":100,"timezone":"Asia/Phnom_Penh","version":1}`

### API-025: PATCH /api/v1/work-sites/{id}

- Owner: `identity-org`; status: planned.
- Purpose and validation: Update site; consumers receive embedded shift site context.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: SiteInput; response schema: Site.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.shift.v1 for affected shift snapshots.
- Request example: `{"name":"Office","latitude":11.56,"longitude":104.92,"radiusM":100,"timezone":"Asia/Phnom_Penh"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","name":"Office","latitude":11.56,"longitude":104.92,"radiusM":100,"timezone":"Asia/Phnom_Penh","version":1}`

### API-026: DELETE /api/v1/work-sites/{id}

- Owner: `identity-org`; status: planned.
- Purpose and validation: Soft delete only if no active shift depends on site.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: REMOVE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Empty.
- Success: 204; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: no body.

### API-027: GET /api/v1/shifts

- Owner: `identity-org`; status: planned.
- Purpose and validation: List shift templates.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query siteId.
- Request schema: Empty; response schema: Page`<Shift>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","name":"Day shift","startTime":"08:00","endTime":"17:00","graceMinutes":10,"workingDays":31,"siteId":"019a0000-0000-7000-8000-000000000020","timezone":"Asia/Phnom_Penh","version":1}],"page":0,"size":20,"total":1}`

### API-028: POST /api/v1/shifts

- Owner: `identity-org`; status: planned.
- Purpose and validation: Create shift with resolved site timezone; overnight disabled until O03.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: ShiftInput; response schema: Shift.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.shift.v1.
- Request example: `{"name":"Day shift","startTime":"08:00","endTime":"17:00","graceMinutes":10,"workingDays":31,"siteId":"019a0000-0000-7000-8000-000000000020","timezone":"Asia/Phnom_Penh"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","name":"Day shift","startTime":"08:00","endTime":"17:00","graceMinutes":10,"workingDays":31,"siteId":"019a0000-0000-7000-8000-000000000020","timezone":"Asia/Phnom_Penh","version":1}`

### API-029: PATCH /api/v1/shifts/{id}

- Owner: `identity-org`; status: planned.
- Purpose and validation: Version shift; do not silently recalculate finalized history.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: ShiftInput; response schema: Shift.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.shift.v1.
- Request example: `{"name":"Day shift","startTime":"08:00","endTime":"17:00","graceMinutes":10,"workingDays":31,"siteId":"019a0000-0000-7000-8000-000000000020","timezone":"Asia/Phnom_Penh"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","name":"Day shift","startTime":"08:00","endTime":"17:00","graceMinutes":10,"workingDays":31,"siteId":"019a0000-0000-7000-8000-000000000020","timezone":"Asia/Phnom_Penh","version":1}`

### API-030: POST /api/v1/employees/{id}/shifts

- Owner: `identity-org`; status: planned.
- Purpose and validation: Assign effective interval; employee guard lock prevents overlap; full assignment snapshot in employee event.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: AssignmentInput; response schema: Assignment.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.employee.v1.
- Request example: `{"shiftId":"019a0000-0000-7000-8000-000000000020","validFrom":"2026-10-22","validTo":null}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","shiftId":"019a0000-0000-7000-8000-000000000020","validFrom":"2026-10-22","validTo":null,"version":1}`
