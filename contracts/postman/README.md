# HRFlow Complete Postman API Test Suite

Comprehensive Postman Collection v2.1.0 schema and environments generated directly from the authoritative HRFlow OpenAPI specification.

## Scope and features

- Total routes and operations: generated from the current OpenAPI inventory (currently 144 paths and 177 HTTP operations across 14 categories). Pending routes are not provider acceptance.
- Microservices gateway routing: designed to target the unified Spring Cloud Gateway at baseUrl api v1 defaulting to port 8080.
- Full realistic mock payloads: every POST, PUT, and PATCH endpoint is pre-populated with realistic, schema-valid JSON bodies including domain-specific models for employee invitations, role assignments, department structures, leave requests, approval chains, material receipts, and GPS attendance coordinates.
- Dynamic token and secret extraction:
    - Automatically extracts CSRF tokens from XSRF-TOKEN cookie and Set-Cookie header and injects X-XSRF-TOKEN.
    - Automatically extracts JWT access token and refresh token upon login and refresh, propagating them to both Environment and Collection variables.
    - Automatically extracts authenticated user metadata including user ID, employee ID, tenant ID, and user roles from auth me.
    - Automatically captures HTTP ETag response headers to enable optimistic locking on conditional updates.
- Chained entity ID propagation: every creation endpoint captures the resulting entity ID into environment variables feeding downstream update, fetch, and delete requests.
- Standalone execution ready: every path variable is mapped to standard fallback UUIDs in the environment files, allowing any single request to be tested independently in isolation.

## Directory layout

```text
contracts/postman/
├── HRFlow_Complete_API.postman_collection.json
├── HRFlow_Local.postman_environment.json
├── HRFlow_Staging.postman_environment.json
└── README.md
```

## Categories and operation breakdown

| Category | Requests | Key operations and endpoints |
| --- | --- | --- |
| 01. Authentication and Security | 10 | CSRF bootstrap, login, profile inspection, public JWKS keys, profile update, token rotation, password change/reset, session logout |
| 02. Tenant Management | 3 | Workspace tenant retrieval, tenant configuration patch, current tenant status |
| 03. Employees and Roles | 17 | Employee listing, creation (v1 and v2), single profile, profile patch, termination, shift assignments, role assignment, impact analysis, invitations |
| 04. Organization Master Data | 32 | Departments, Positions, Work-sites (v1 and v2), Shifts (v1 and v2), Shift-assignments (v1 and v2), and Holidays (v1 and v2) CRUD workflows |
| 05. Attendance Tracking | 21 | Mobile check-in with GPS coordinates, check-out, manual correction requests, evidence uploads, daily status finalization, approval/rejection |
| 06. Leave Management | 29 | Leave types, leave policies, balance adjustments, vacation requests, request conflict resolution, presigned attachment uploads, approvals/rejections |
| 07. Requests and Procurement | 22 | General requests, procurement request types, approval chains, comments, presigned upload URLs, fulfillment and sign-offs |
| 08. Materials and Inventory | 6 | Inventory master catalog, stock receiving, material updates, and administrative listings |
| 09. Notifications and Messaging | 13 | Notification center, device registration APNs and FCM tokens, notification preferences, template administration, mark-read actions |
| 10. Reports and Analytics | 12 | Attendance reports, export jobs CSV and Excel, report cache rebuilding, operational metrics |
| 11. Workflow Operations | 3 | Workflow instances, step execution actions, and lifecycle status |
| 12. Approvals and Delegations | 4 | Delegations registry, delegation creation, approval actions, and authority handovers |
| 13. Form Drafts | 3 | Draft persistence, retrieval by kind, and draft clearance |
| 14. Operations and Audit Logs | 2 | Administrative audit logs, security event queries |

## Environment variables reference

| Variable Name | Type | Description |
| --- | --- | --- |
| baseUrl | URL | Spring Cloud Gateway entrypoint http localhost 8080 |
| tenant | String | Workspace tenant slug demo |
| userEmail | String | Authenticated user email owner at example dot test |
| userPassword | Secret | Authenticated user password |
| browserOrigin | URL | Allowed browser origin for CORS and CSRF http localhost 3000 |
| xsrfToken | String | Anti-CSRF token extracted from XSRF-TOKEN cookie |
| accessToken | Secret | JWT bearer token minted by identity service |
| refreshToken | Secret | Refresh token for rotation |
| userId | UUID | Authenticated user ID |
| employeeId | UUID | Employee ID extracted from auth me or created employee |
| tenantId | UUID | Organization Tenant ID |
| departmentId | UUID | Active Department ID |
| positionId | UUID | Active Position ID |
| workSiteId | UUID | Active Work Site ID |
| shiftId | UUID | Active Shift Schedule ID |
| holidayId | UUID | Active Holiday Record ID |
| shiftAssignmentId | UUID | Active Shift Assignment ID |
| leaveTypeId | UUID | Active Leave Type ID |
| leaveRequestId | UUID | Active Leave Request ID |
| generalRequestId | UUID | Active General Request ID |
| attendanceSessionId | UUID | Active Attendance Session ID from check-in |
| materialId | UUID | Active Material Inventory ID |
| notificationId | UUID | Notification ID |
| exportJobId | UUID | Async Report Export Job ID |
| delegationId | UUID | Delegation Assignment ID |
| currentEtag | String | ETag captured from response headers for optimistic locking |

## Security and extraction scripts

### Anti-CSRF cookie extraction

```ts
var xsrf = pm.cookies.get("XSRF-TOKEN");
if (xsrf) {
    pm.environment.set("xsrfToken", xsrf);
    pm.collectionVariables.set("xsrfToken", xsrf);
} else {
    var setCookie = pm.response.headers.get("Set-Cookie");
    if (setCookie) {
        var match = setCookie.match(/XSRF-TOKEN=([^;]+)/);
        if (match) {
            pm.environment.set("xsrfToken", match[1]);
            pm.collectionVariables.set("xsrfToken", match[1]);
        }
    }
}
```

### Access and refresh token extraction

```ts
if (pm.response.code === 200) {
    var res = pm.response.json();
    if (res.accessToken) {
        pm.environment.set("accessToken", res.accessToken);
        pm.collectionVariables.set("accessToken", res.accessToken);
    }
    if (res.refreshToken) {
        pm.environment.set("refreshToken", res.refreshToken);
        pm.collectionVariables.set("refreshToken", res.refreshToken);
    }
}
var setCookie = pm.response.headers.get("Set-Cookie");
if (setCookie) {
    var match = setCookie.match(/hrflow_refresh=([^;]+)/);
    if (match) {
        pm.environment.set("refreshToken", match[1]);
        pm.collectionVariables.set("refreshToken", match[1]);
    }
}
```

### Authenticated identity and context propagation

```ts
var user = pm.response.json();
if (user.userId) pm.environment.set("userId", user.userId);
if (user.employeeId) pm.environment.set("employeeId", user.employeeId);
if (user.tenantId) pm.environment.set("tenantId", user.tenantId);
if (user.roles) pm.environment.set("userRoles", JSON.stringify(user.roles));
```

## Running the tests

### Option 1 via Postman CLI

Run the full collection against local environment:

```bash
postman collection run contracts/postman/HRFlow_Complete_API.postman_collection.json \
  -e contracts/postman/HRFlow_Local.postman_environment.json \
  --timeout-request 5000
```

Run only a specific category folder:

```bash
postman collection run contracts/postman/HRFlow_Complete_API.postman_collection.json \
  -e contracts/postman/HRFlow_Local.postman_environment.json \
  --folder "01. Authentication & Security"
```

### Option 2 via Newman CLI

```bash
npx -y newman run contracts/postman/HRFlow_Complete_API.postman_collection.json \
  -e contracts/postman/HRFlow_Local.postman_environment.json \
  --reporters cli,json \
  --reporter-json-export contracts/postman/test-run-report.json
```

### Option 3 via Postman Desktop App

1. Open Postman desktop application.
1. Click Import in the top-left navigation panel.
1. Select and import the collection and local and staging environments from contracts postman.
1. Select the HRFlow Local Development environment in the top-right environment selector.
1. In the Collection panel, click Run collection to run the automated test flow.

## Regenerating the collection

To synchronize changes from updated OpenAPI specifications run:

```bash
python3 scripts/generate-postman-collection.py
```
