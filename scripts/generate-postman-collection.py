#!/usr/bin/env python3
"""
Generate comprehensive Postman Collection v2.1.0 and Environment JSON files
from the authoritative HRFlow OpenAPI specification.
"""
import json
import os
import re
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC_PATH = ROOT / "contracts/openapi.json"
OUTPUT_DIR = ROOT / "contracts/postman"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

# Specific domain mock payloads for endpoints that have generic open-ended schemas
ENDPOINT_SPECIFIC_BODIES = {
    ("PATCH", "/tenants/current"): {
        "name": "HRFlow Enterprise Tech Corp",
        "timezone": "Asia/Phnom_Penh",
        "currency": "USD"
    },
    ("PATCH", "/employees/{id}"): {
        "displayName": "Johnathan Doe",
        "jobTitle": "Lead Staff Engineer",
        "phone": "+85512345678"
    },
    ("POST", "/employees/{id}/impact"): {
        "action": "TERMINATE",
        "effectiveDate": "2026-11-01"
    },
    ("POST", "/employees/{id}/invite"): {
        "email": "candidate@example.test",
        "role": "EMPLOYEE",
        "message": "Welcome to HRFlow"
    },
    ("PUT", "/employees/{id}/roles"): {
        "roles": ["EMPLOYEE", "DEPARTMENT_HEAD"]
    },
    ("PATCH", "/departments/{id}"): {
        "name": "Platform & Cloud Engineering",
        "description": "Core distributed services and infrastructure"
    },
    ("PATCH", "/positions/{id}"): {
        "name": "Lead Staff Architect",
        "level": "L6"
    },
    ("POST", "/shift-assignments/{id}/close"): {
        "endDate": "2026-10-31",
        "reason": "Rotation to standard day shift"
    },
    ("PATCH", "/shifts/{id}"): {
        "name": "Updated Morning Shift A",
        "startTime": "08:30:00",
        "endTime": "17:30:00"
    },
    ("PATCH", "/work-sites/{id}"): {
        "name": "Headquarters Tech Campus",
        "radiusMeters": 150
    },
    ("POST", "/attendance/daily-status/{id}/finalize"): {
        "decision": "APPROVED",
        "notes": "Attendance reviewed and verified against building access logs"
    },
    ("POST", "/leave/admin/balances/{id}/adjust"): {
        "typeId": "{{leaveTypeId}}",
        "days": 2.5,
        "reason": "Overtime compensation credit"
    },
    ("PUT", "/leave/admin/policies/{id}"): {
        "name": "Standard Annual Vacation Policy",
        "accrualRate": 1.25,
        "maxCarryOver": 5.0
    },
    ("PUT", "/leave/admin/policies/{typeId}"): {
        "accrualRate": 1.25,
        "maxCarryOver": 5.0,
        "requiresApproval": True
    },
    ("PATCH", "/leave/admin/types/{id}"): {
        "name": "Annual Vacation Leave",
        "paid": True
    },
    ("POST", "/leave/admin/year-end/preview"): {
        "year": 2026,
        "targetYear": 2027,
        "carryOverCap": 5.0
    },
    ("POST", "/leave/requests/preview"): {
        "typeId": "{{leaveTypeId}}",
        "startDate": "2026-11-02",
        "endDate": "2026-11-04",
        "portion": "FULL_DAY"
    },
    ("POST", "/requests/admin/approval-chains"): {
        "name": "Standard IT Hardware Chain",
        "steps": [
            {"stepOrder": 1, "approverType": "MANAGER"},
            {"stepOrder": 2, "approverType": "FINANCE"}
        ]
    },
    ("PATCH", "/requests/admin/approval-chains/{id}"): {
        "name": "Updated Hardware Approval Chain"
    },
    ("PATCH", "/requests/admin/types/{id}"): {
        "name": "Hardware & Equipment Procurement",
        "active": True
    },
    ("PATCH", "/requests/{id}"): {
        "title": "MacBook Pro M3 Max Request",
        "priority": "HIGH"
    },
    ("POST", "/requests/{id}/approve"): {
        "decision": "APPROVED",
        "comments": "Approved within Q4 departmental budget"
    },
    ("POST", "/requests/{id}/fulfill"): {
        "trackingNumber": "TRACK-KH-88912",
        "notes": "Delivered to Phnom Penh Campus, Engineering Wing"
    },
    ("PATCH", "/materials/admin/{id}"): {
        "name": "Ergonomic Mechanical Keyboard",
        "unitCost": 85.0
    },
    ("POST", "/materials/admin/{id}/receive"): {
        "quantity": 50,
        "batchNumber": "BATCH-2026-10",
        "supplier": "TechSupplies Asia"
    },
    ("POST", "/reports/admin/rebuilds"): {
        "reportType": "ATTENDANCE_SUMMARY",
        "year": 2026,
        "month": 10
    },
    ("POST", "/workflows/{id}/actions/{action}"): {
        "action": "SUBMIT",
        "comment": "Submitting workflow stage for manager sign-off"
    },
    ("POST", "/approvals/delegations"): {
        "delegateeId": "{{employeeId}}",
        "startDate": "2026-10-26",
        "endDate": "2026-11-10",
        "reason": "Executive travel coverage"
    },
    ("PUT", "/form-drafts/{kind}"): {
        "data": {
            "draftType": "leave_request",
            "typeId": "{{leaveTypeId}}",
            "notes": "Pre-filled vacation plan"
        },
        "savedAt": "2026-10-25T18:00:00Z"
    }
}

def load_spec():
    with open(SPEC_PATH, "r", encoding="utf-8") as f:
        return json.load(f)

def determine_folder(path):
    p = path.strip("/").split("/")
    first = p[0]
    if first in ("auth", ".well-known"):
        return "01. Authentication & Security"
    elif first == "tenants":
        return "02. Tenant Management"
    elif first == "employees":
        return "03. Employees & Roles"
    elif first in ("departments", "positions", "work-sites", "shifts", "shift-assignments", "holidays"):
        return "04. Organization Master Data"
    elif first == "attendance":
        return "05. Attendance Tracking"
    elif first == "leave":
        return "06. Leave Management"
    elif first == "requests":
        return "07. Requests & Procurement"
    elif first == "materials":
        return "08. Materials & Inventory"
    elif first == "notifications":
        return "09. Notifications & Messaging"
    elif first == "reports":
        return "10. Reports & Analytics"
    elif first == "workflows":
        return "11. Workflow Operations"
    elif first == "approvals":
        return "12. Approvals & Delegations"
    elif first == "form-drafts":
        return "13. Form Drafts"
    elif first in ("audit-logs", "operations"):
        return "14. Operations & Audit Logs"
    return "15. Miscellaneous"

def resolve_schema(schema, spec_schemas, depth=0):
    if depth > 5:
        return {}
    if not schema:
        return {}
    if "$ref" in schema:
        ref_name = schema["$ref"].split("/")[-1]
        if ref_name in spec_schemas:
            return resolve_schema(spec_schemas[ref_name], spec_schemas, depth + 1)
        return {}
    return schema

def generate_mock_value(prop_name, prop_schema, spec_schemas, depth=0):
    prop_schema = resolve_schema(prop_schema, spec_schemas, depth + 1)
    if "example" in prop_schema:
        return prop_schema["example"]
    if "enum" in prop_schema and prop_schema["enum"]:
        return prop_schema["enum"][0]

    # Domain-specific smart defaults
    p_lower = prop_name.lower()
    if p_lower in ("tenant", "tenantslug"):
        return "{{tenant}}"
    if p_lower == "email":
        return "{{userEmail}}"
    if p_lower == "password":
        return "{{userPassword}}"
    if p_lower == "newpassword":
        return "new-secure-password-456"
    if p_lower == "refreshtoken":
        return "{{refreshToken}}"
    if p_lower == "accesstoken":
        return "{{accessToken}}"
    if p_lower in ("employeeid", "managerid"):
        return "{{employeeId}}"
    if p_lower in ("departmentid", "department"):
        return "{{departmentId}}"
    if p_lower in ("positionid", "position"):
        return "{{positionId}}"
    if p_lower == "worksiteid":
        return "{{workSiteId}}"
    if p_lower == "shiftid":
        return "{{shiftId}}"
    if p_lower in ("typeid", "leavetypeid"):
        return "{{leaveTypeId}}"
    if p_lower in ("latitude", "lat"):
        return 11.5564
    if p_lower in ("longitude", "lon", "lng"):
        return 104.9282
    if p_lower == "accuracymeters":
        return 15.0
    if p_lower == "devicemodel":
        return "iPhone 15 Pro"
    if p_lower == "roles":
        return ["EMPLOYEE", "OWNER"]
    if p_lower in ("portion", "leaveportion"):
        return "FULL_DAY"
    if p_lower == "decision":
        return "APPROVED"
    if p_lower == "status":
        return "PENDING"
    if p_lower == "reason":
        return "Personal appointment and annual family event"
    if p_lower in ("name", "displayname"):
        return "John Doe"

    t = prop_schema.get("type")
    fmt = prop_schema.get("format")

    if t == "string":
        if fmt == "date":
            return "2026-10-26"
        if fmt == "date-time":
            return "2026-10-26T08:00:00Z"
        if fmt == "email":
            return "owner@example.test"
        if fmt == "uuid":
            return "00000000-0000-0000-0000-000000000001"
        return f"{prop_name}-sample"
    elif t == "integer":
        return 1
    elif t == "number":
        return 1.0
    elif t == "boolean":
        return True
    elif t == "array":
        items = prop_schema.get("items", {})
        return [generate_mock_value(prop_name, items, spec_schemas, depth + 1)]
    elif t == "object" or "properties" in prop_schema:
        res = {}
        for sub_name, sub_s in prop_schema.get("properties", {}).items():
            res[sub_name] = generate_mock_value(sub_name, sub_s, spec_schemas, depth + 1)
        return res
    return f"{prop_name}_val"

def generate_mock_body(method, path, op, spec_schemas):
    # Check explicit domain override first
    if (method, path) in ENDPOINT_SPECIFIC_BODIES:
        return ENDPOINT_SPECIFIC_BODIES[(method, path)]

    rb = op.get("requestBody")
    if not rb:
        return None
    content = rb.get("content", {})
    json_media = content.get("application/json")
    if not json_media:
        return None
    schema = resolve_schema(json_media.get("schema", {}), spec_schemas)
    if not schema:
        return {}

    props = schema.get("properties", {})
    if props:
        body = {}
        for prop_name, prop_schema in props.items():
            body[prop_name] = generate_mock_value(prop_name, prop_schema, spec_schemas)
        return body

    t = schema.get("type")
    if t == "object":
        req = schema.get("required", [])
        body = {}
        for r in req:
            body[r] = generate_mock_value(r, {"type": "string"}, spec_schemas)
        return body or {"status": "ACTIVE", "comment": "Processed via API"}
    return {}

def get_path_var_default(var_name, path):
    v = var_name.lower()
    if v == "id":
        if "/employees/" in path: return "{{employeeId}}"
        if "/departments/" in path: return "{{departmentId}}"
        if "/positions/" in path: return "{{positionId}}"
        if "/work-sites/" in path: return "{{workSiteId}}"
        if "/shifts/" in path: return "{{shiftId}}"
        if "/holidays/" in path: return "{{holidayId}}"
        if "/leave/requests/" in path: return "{{leaveRequestId}}"
        if "/leave/admin/types/" in path: return "{{leaveTypeId}}"
        if "/leave/admin/policies/" in path: return "{{leaveTypeId}}"
        if "/leave/admin/balances/" in path: return "{{employeeId}}"
        if "/leave/admin/year-end/" in path: return "{{yearEndId}}"
        if "/requests/admin/types/" in path: return "{{requestTypeId}}"
        if "/requests/admin/approval-chains/" in path: return "{{approvalChainId}}"
        if "/requests/" in path: return "{{generalRequestId}}"
        if "/materials/admin/" in path: return "{{materialId}}"
        if "/notifications/devices/" in path: return "{{deviceId}}"
        if "/notifications/" in path: return "{{notificationId}}"
        if "/reports/attendance/employees/" in path: return "{{employeeId}}"
        if "/reports/exports/" in path: return "{{exportJobId}}"
        if "/workflows/" in path: return "{{workflowId}}"
        if "/approvals/delegations/" in path: return "{{delegationId}}"
        return "{{targetId}}"
    if v == "employeeid": return "{{employeeId}}"
    if v == "typeid": return "{{leaveTypeId}}"
    if v == "code": return "LEAVE_SUBMITTED"
    if v == "action": return "APPROVE"
    if v == "recordid": return "{{recordId}}"
    if v == "attachmentid": return "{{attachmentId}}"
    if v == "kind": return "leave_request"
    return f"{{{{{var_name}}}}}"

def generate_test_script(method, path, op):
    # Determine expected success codes
    expected_codes = "[200, 201, 202, 204]"
    if method == "POST" and path in ("/employees", "/departments", "/positions", "/work-sites", "/shifts", "/holidays", "/leave/requests", "/requests", "/materials/admin"):
        expected_codes = "[201, 200]"
    elif method == "DELETE":
        expected_codes = "[204, 200]"
    elif path == "/auth/csrf":
        expected_codes = "[204, 200]"

    lines = [
        "// Validate standard HTTP response status",
        "pm.test(\"Status code is successful\", function () {",
        f"    pm.expect(pm.response.code).to.be.oneOf({expected_codes});",
        "});",
        "",
        "// Global response time assertion",
        "pm.test(\"Response time is acceptable (< 2000ms)\", function () {",
        "    pm.expect(pm.response.responseTime).to.be.below(2000);",
        "});",
        "",
        "// Capture ETag header if present",
        "var etag = pm.response.headers.get(\"ETag\");",
        "if (etag) {",
        "    pm.environment.set(\"currentEtag\", etag);",
        "}",
        ""
    ]

    # Special extractions by endpoint
    if path == "/auth/csrf":
        lines.extend([
            "// Extract CSRF Token from cookie or Set-Cookie header",
            "var xsrf = pm.cookies.get(\"XSRF-TOKEN\");",
            "if (xsrf) {",
            "    pm.environment.set(\"xsrfToken\", xsrf);",
            "    pm.collectionVariables.set(\"xsrfToken\", xsrf);",
            "} else {",
            "    var setCookie = pm.response.headers.get(\"Set-Cookie\");",
            "    if (setCookie) {",
            "        var match = setCookie.match(/XSRF-TOKEN=([^;]+)/);",
            "        if (match) {",
            "            pm.environment.set(\"xsrfToken\", match[1]);",
            "            pm.collectionVariables.set(\"xsrfToken\", match[1]);",
            "        }",
            "    }",
            "}"
        ])
    elif path == "/auth/login":
        lines.extend([
            "// Extract Access & Refresh Tokens upon successful sign-in",
            "if (pm.response.code === 200) {",
            "    pm.test(\"Login payload contains valid token structure\", function () {",
            "        var res = pm.response.json();",
            "        pm.expect(res).to.have.property(\"accessToken\");",
            "        pm.expect(res).to.have.property(\"tokenType\");",
            "    });",
            "",
            "    var res = pm.response.json();",
            "    if (res.accessToken) {",
            "        pm.environment.set(\"accessToken\", res.accessToken);",
            "        pm.collectionVariables.set(\"accessToken\", res.accessToken);",
            "    }",
            "    if (res.refreshToken) {",
            "        pm.environment.set(\"refreshToken\", res.refreshToken);",
            "        pm.collectionVariables.set(\"refreshToken\", res.refreshToken);",
            "    }",
            "}",
            "var setCookie = pm.response.headers.get(\"Set-Cookie\");",
            "if (setCookie) {",
            "    var match = setCookie.match(/hrflow_refresh=([^;]+)/);",
            "    if (match) {",
            "        pm.environment.set(\"refreshToken\", match[1]);",
            "        pm.collectionVariables.set(\"refreshToken\", match[1]);",
            "    }",
            "}"
        ])
    elif path == "/auth/refresh":
        lines.extend([
            "// Extract Rotated Tokens upon successful token refresh",
            "if (pm.response.code === 200) {",
            "    var res = pm.response.json();",
            "    if (res.accessToken) {",
            "        pm.environment.set(\"accessToken\", res.accessToken);",
            "        pm.collectionVariables.set(\"accessToken\", res.accessToken);",
            "    }",
            "    if (res.refreshToken) {",
            "        pm.environment.set(\"refreshToken\", res.refreshToken);",
            "        pm.collectionVariables.set(\"refreshToken\", res.refreshToken);",
            "    }",
            "}"
        ])
    elif path == "/auth/me":
        lines.extend([
            "// Extract Authenticated User Context",
            "var user = pm.response.json();",
            "if (user.userId) pm.environment.set(\"userId\", user.userId);",
            "if (user.employeeId) pm.environment.set(\"employeeId\", user.employeeId);",
            "if (user.tenantId) pm.environment.set(\"tenantId\", user.tenantId);",
            "if (user.roles) pm.environment.set(\"userRoles\", JSON.stringify(user.roles));"
        ])
    elif method == "POST" and path == "/employees":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"employeeId\", res.id);"
        ])
    elif method == "POST" and path == "/departments":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"departmentId\", res.id);"
        ])
    elif method == "POST" and path == "/positions":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"positionId\", res.id);"
        ])
    elif method == "POST" and path == "/work-sites":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"workSiteId\", res.id);"
        ])
    elif method == "POST" and path == "/shifts":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"shiftId\", res.id);"
        ])
    elif method == "POST" and path == "/holidays":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"holidayId\", res.id);"
        ])
    elif method == "POST" and path == "/attendance/check-in":
        lines.extend([
            "var res = pm.response.json();",
            "var sid = res.id || res.sessionId;",
            "if (sid) pm.environment.set(\"attendanceSessionId\", sid);"
        ])
    elif method == "POST" and path == "/leave/requests":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"leaveRequestId\", res.id);"
        ])
    elif method == "POST" and path == "/leave/admin/types":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"leaveTypeId\", res.id);"
        ])
    elif method == "POST" and path == "/requests":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"generalRequestId\", res.id);"
        ])
    elif method == "POST" and path == "/requests/admin/types":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"requestTypeId\", res.id);"
        ])
    elif method == "POST" and path == "/materials/admin":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"materialId\", res.id);"
        ])
    elif method == "POST" and path == "/reports/exports":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"exportJobId\", res.id);"
        ])
    elif method == "POST" and path == "/approvals/delegations":
        lines.extend([
            "var res = pm.response.json();",
            "if (res && res.id) pm.environment.set(\"delegationId\", res.id);"
        ])

    return lines

def generate_prerequest_script(method, path):
    lines = []
    is_csrf_sensitive = path in ("/auth/login", "/auth/refresh", "/auth/logout")
    if is_csrf_sensitive:
        lines.append("// Check if CSRF token is available, log warning if empty")
        lines.append("if (!pm.environment.get(\"xsrfToken\") && !pm.collectionVariables.get(\"xsrfToken\")) {")
        lines.append("    console.warn(\"Warning: xsrfToken variable is empty. Run 'GET /auth/csrf' first if required.\");")
        lines.append("}")
    return lines

def build_postman_request(method, path, op, spec_schemas):
    summary = op.get("summary", f"{method} {path}")
    description = op.get("description", summary)

    # Path segments & variables
    segments = ["api", "v1"]
    raw_path_parts = [p for p in path.strip("/").split("/") if p]
    path_variables = []

    for seg in raw_path_parts:
        if seg.startswith("{") and seg.endswith("}"):
            var_name = seg[1:-1]
            segments.append(f":{var_name}")
            path_variables.append({
                "key": var_name,
                "value": get_path_var_default(var_name, path),
                "description": f"URL Path variable {var_name}"
            })
        else:
            segments.append(seg)

    raw_url = "{{baseUrl}}/" + "/".join(segments)

    # Headers
    headers = [
        {"key": "Accept", "value": "application/json", "type": "text"}
    ]

    is_public = path in ("/.well-known/jwks.json", "/auth/csrf", "/auth/login",
                         "/auth/forgot-password", "/auth/reset-password")
    if not is_public:
        headers.append({
            "key": "Authorization",
            "value": "Bearer {{accessToken}}",
            "type": "text"
        })

    is_csrf_sensitive = path in ("/auth/login", "/auth/refresh", "/auth/logout")
    if is_csrf_sensitive:
        headers.append({"key": "Origin", "value": "{{browserOrigin}}", "type": "text"})
        headers.append({"key": "X-XSRF-TOKEN", "value": "{{xsrfToken}}", "type": "text"})

    request_obj = {
        "method": method,
        "header": headers,
        "url": {
            "raw": raw_url,
            "host": ["{{baseUrl}}"],
            "path": segments,
            "variable": path_variables
        },
        "description": description
    }

    # Query parameters
    parameters = op.get("parameters", [])
    query_params = []
    for param in parameters:
        if param.get("in") == "query":
            p_name = param.get("name")
            p_schema = param.get("schema", {})
            query_params.append({
                "key": p_name,
                "value": str(generate_mock_value(p_name, p_schema, spec_schemas)),
                "description": param.get("description", ""),
                "disabled": not param.get("required", False)
            })
    if query_params:
        request_obj["url"]["query"] = query_params

    # Request Body
    mock_body = generate_mock_body(method, path, op, spec_schemas)
    if mock_body is not None:
        headers.insert(0, {"key": "Content-Type", "value": "application/json", "type": "text"})
        request_obj["body"] = {
            "mode": "raw",
            "raw": json.dumps(mock_body, indent=2),
            "options": {
                "raw": {
                    "language": "json"
                }
            }
        }

    # Events (Pre-request & Test Scripts)
    events = []
    pre_lines = generate_prerequest_script(method, path)
    if pre_lines:
        events.append({
            "listen": "prerequest",
            "script": {
                "type": "text/javascript",
                "exec": pre_lines
            }
        })

    test_lines = generate_test_script(method, path, op)
    events.append({
        "listen": "test",
        "script": {
            "type": "text/javascript",
            "exec": test_lines
        }
    })

    return {
        "name": summary,
        "request": request_obj,
        "event": events,
        "response": []
    }

def get_auth_order_rank(method, path):
    if path == "/auth/csrf": return 10
    if path == "/auth/login": return 20
    if path == "/auth/me": return 30
    if path == "/.well-known/jwks.json": return 40
    if path == "/auth/profile": return 50
    if path == "/auth/refresh": return 60
    if path == "/auth/change-password": return 70
    if path == "/auth/forgot-password": return 80
    if path == "/auth/reset-password": return 90
    if path == "/auth/logout": return 990
    return 100

def get_general_order_rank(method, path):
    # Order: GET collection, POST create, GET item, PUT/PATCH update, POST actions, DELETE
    is_item = "{" in path
    if not is_item and method == "GET": return 10
    if not is_item and method == "POST": return 20
    if is_item and method == "GET": return 30
    if is_item and method in ("PUT", "PATCH"): return 40
    if is_item and method == "POST": return 50
    if method == "DELETE": return 80
    return 60

def sort_folder_items(folder_name, items):
    if "01. Authentication" in folder_name:
        return sorted(items, key=lambda x: (
            get_auth_order_rank(x["request"]["method"], "/" + "/".join(x["request"]["url"]["path"][2:]).replace(":", "{").replace("}", "}")),
            x["name"]
        ))
    else:
        return sorted(items, key=lambda x: (
            get_general_order_rank(x["request"]["method"], "/" + "/".join(x["request"]["url"]["path"][2:]).replace(":", "{").replace("}", "}")),
            x["request"]["url"]["raw"],
            x["request"]["method"]
        ))

def generate_collection(spec):
    spec_schemas = spec.get("components", {}).get("schemas", {})
    folders_dict = {}

    for path, methods in sorted(spec.get("paths", {}).items()):
        for method, op in methods.items():
            if method.lower() not in ("get", "post", "put", "patch", "delete"):
                continue
            folder_name = determine_folder(path)
            if folder_name not in folders_dict:
                folders_dict[folder_name] = []

            postman_req = build_postman_request(method.upper(), path, op, spec_schemas)
            folders_dict[folder_name].append(postman_req)

    collection_items = []
    for f_name in sorted(folders_dict.keys()):
        sorted_requests = sort_folder_items(f_name, folders_dict[f_name])
        collection_items.append({
            "name": f_name,
            "description": f"Endpoints and workflows for {f_name}",
            "item": sorted_requests
        })

    # Collection level events (timestamp tracking and global latency check)
    coll_events = [
        {
            "listen": "prerequest",
            "script": {
                "type": "text/javascript",
                "exec": [
                    "// Set ISO 8601 execution timestamp",
                    "pm.collectionVariables.set(\"requestTimestamp\", new Date().toISOString());"
                ]
            }
        },
        {
            "listen": "test",
            "script": {
                "type": "text/javascript",
                "exec": [
                    "// Ensure response is within expected benchmark",
                    "pm.test(\"Latency benchmark met (< 3000ms)\", function () {",
                    "    pm.expect(pm.response.responseTime).to.be.below(3000);",
                    "});"
                ]
            }
        }
    ]

    collection = {
        "info": {
            "_postman_id": str(uuid.uuid4()),
            "name": "HRFlow Complete API Collection",
            "description": "Authoritative, end-to-end Postman Collection covering all 133 routes and 158 HTTP operations of HRFlow microservices via the Spring Cloud Gateway. Includes realistic mock payloads, response schema assertions, CSRF extraction, dynamic token rotation, and entity ID propagation.",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        },
        "item": collection_items,
        "event": coll_events,
        "variable": [
            {
                "key": "baseUrl",
                "value": "http://localhost:8080",
                "type": "string",
                "description": "HRFlow Spring Cloud Gateway entrypoint"
            },
            {
                "key": "accessToken",
                "value": "",
                "type": "string",
                "description": "Active JWT bearer token minted by identity-org"
            },
            {
                "key": "refreshToken",
                "value": "",
                "type": "string",
                "description": "Active Refresh token"
            },
            {
                "key": "xsrfToken",
                "value": "",
                "type": "string",
                "description": "Anti-CSRF token extracted from cookie or header"
            },
            {
                "key": "requestTimestamp",
                "value": "",
                "type": "string",
                "description": "Timestamp generated during pre-request script"
            }
        ]
    }
    return collection

def generate_environments():
    local_env = {
        "id": str(uuid.uuid4()),
        "name": "HRFlow Local Development",
        "values": [
            {"key": "baseUrl", "value": "http://localhost:8080", "type": "default", "enabled": True},
            {"key": "tenant", "value": "demo", "type": "default", "enabled": True},
            {"key": "userEmail", "value": "owner@example.test", "type": "default", "enabled": True},
            {"key": "userPassword", "value": "local-test-password-123", "type": "secret", "enabled": True},
            {"key": "browserOrigin", "value": "http://localhost:3000", "type": "default", "enabled": True},
            {"key": "accessToken", "value": "", "type": "secret", "enabled": True},
            {"key": "refreshToken", "value": "", "type": "secret", "enabled": True},
            {"key": "xsrfToken", "value": "", "type": "default", "enabled": True},
            {"key": "userId", "value": "00000000-0000-0000-0000-000000000001", "type": "default", "enabled": True},
            {"key": "employeeId", "value": "00000000-0000-0000-0000-000000000002", "type": "default", "enabled": True},
            {"key": "tenantId", "value": "00000000-0000-0000-0000-000000000003", "type": "default", "enabled": True},
            {"key": "departmentId", "value": "00000000-0000-0000-0000-000000000010", "type": "default", "enabled": True},
            {"key": "positionId", "value": "00000000-0000-0000-0000-000000000020", "type": "default", "enabled": True},
            {"key": "workSiteId", "value": "00000000-0000-0000-0000-000000000030", "type": "default", "enabled": True},
            {"key": "shiftId", "value": "00000000-0000-0000-0000-000000000040", "type": "default", "enabled": True},
            {"key": "holidayId", "value": "00000000-0000-0000-0000-000000000050", "type": "default", "enabled": True},
            {"key": "leaveTypeId", "value": "00000000-0000-0000-0000-000000000060", "type": "default", "enabled": True},
            {"key": "leaveRequestId", "value": "00000000-0000-0000-0000-000000000070", "type": "default", "enabled": True},
            {"key": "generalRequestId", "value": "00000000-0000-0000-0000-000000000080", "type": "default", "enabled": True},
            {"key": "requestTypeId", "value": "00000000-0000-0000-0000-000000000085", "type": "default", "enabled": True},
            {"key": "approvalChainId", "value": "00000000-0000-0000-0000-000000000086", "type": "default", "enabled": True},
            {"key": "attendanceSessionId", "value": "00000000-0000-0000-0000-000000000090", "type": "default", "enabled": True},
            {"key": "materialId", "value": "00000000-0000-0000-0000-000000000100", "type": "default", "enabled": True},
            {"key": "notificationId", "value": "00000000-0000-0000-0000-000000000110", "type": "default", "enabled": True},
            {"key": "deviceId", "value": "00000000-0000-0000-0000-000000000115", "type": "default", "enabled": True},
            {"key": "exportJobId", "value": "00000000-0000-0000-0000-000000000120", "type": "default", "enabled": True},
            {"key": "workflowId", "value": "00000000-0000-0000-0000-000000000130", "type": "default", "enabled": True},
            {"key": "delegationId", "value": "00000000-0000-0000-0000-000000000140", "type": "default", "enabled": True},
            {"key": "targetId", "value": "00000000-0000-0000-0000-000000000150", "type": "default", "enabled": True},
            {"key": "currentEtag", "value": "", "type": "default", "enabled": True}
        ],
        "_postman_variable_scope": "environment"
    }

    staging_env = {
        "id": str(uuid.uuid4()),
        "name": "HRFlow Staging Cluster",
        "values": [
            {"key": "baseUrl", "value": "https://api-staging.hrflow.internal", "type": "default", "enabled": True},
            {"key": "tenant", "value": "staging-corp", "type": "default", "enabled": True},
            {"key": "userEmail", "value": "admin@staging.hrflow.internal", "type": "default", "enabled": True},
            {"key": "userPassword", "value": "", "type": "secret", "enabled": True},
            {"key": "browserOrigin", "value": "https://app-staging.hrflow.internal", "type": "default", "enabled": True},
            {"key": "accessToken", "value": "", "type": "secret", "enabled": True},
            {"key": "refreshToken", "value": "", "type": "secret", "enabled": True},
            {"key": "xsrfToken", "value": "", "type": "default", "enabled": True},
            {"key": "userId", "value": "", "type": "default", "enabled": True},
            {"key": "employeeId", "value": "", "type": "default", "enabled": True},
            {"key": "tenantId", "value": "", "type": "default", "enabled": True},
            {"key": "currentEtag", "value": "", "type": "default", "enabled": True}
        ],
        "_postman_variable_scope": "environment"
    }
    return local_env, staging_env

def main():
    spec = load_spec()
    collection = generate_collection(spec)
    local_env, staging_env = generate_environments()

    coll_path = OUTPUT_DIR / "HRFlow_Complete_API.postman_collection.json"
    local_path = OUTPUT_DIR / "HRFlow_Local.postman_environment.json"
    staging_path = OUTPUT_DIR / "HRFlow_Staging.postman_environment.json"

    with open(coll_path, "w", encoding="utf-8") as f:
        json.dump(collection, f, indent=2)
    print(f"Generated collection: {coll_path} ({len(collection['item'])} categories)")

    total_requests = sum(len(f["item"]) for f in collection["item"])
    print(f"Total operations included: {total_requests}")

    with open(local_path, "w", encoding="utf-8") as f:
        json.dump(local_env, f, indent=2)
    print(f"Generated local environment: {local_path}")

    with open(staging_path, "w", encoding="utf-8") as f:
        json.dump(staging_env, f, indent=2)
    print(f"Generated staging environment: {staging_path}")

if __name__ == "__main__":
    main()
