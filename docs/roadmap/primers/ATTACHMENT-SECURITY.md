# Private file attachment security

> **Note:** Private file attachment security ensures that confidential documents (medical certificates, expense receipts, government IDs) are stored securely and accessed exclusively by authorized users with valid permissions.

## Purpose and core concept

Workforce management platforms frequently handle sensitive, legally protected documents. Storing these files in publicly accessible web directories or using predictable sequential URLs creates extreme data leak risks.

Private attachment security separates file storage from web presentation, requiring authenticated token verification, tenant boundary validation, and short-lived signed URLs or streaming controller authorization for every download.

## Problem being solved

Naively serving file uploads creates critical security vulnerabilities:

- Direct object reference (IDOR): A user changes `attachment_id=101` to `102` in their browser address bar and views another employee's confidential medical diagnosis.
- Cross-tenant file access: A user from Tenant A accesses receipts uploaded by Tenant B because the static web server does not check tenant context.
- Malicious file execution: An attacker uploads an executable script disguised as a PDF (`exploit.php` or `exploit.sh`) to the web root, allowing Remote Code Execution (RCE) if the web server executes it.

## Simple mental model

Private attachment security is like an evidence vault at a municipal courthouse.

Evidence bags are kept in a windowless locked room behind secure doors. When an attorney needs to inspect an item, they present a court order signed by the presiding judge. The vault clerk checks the attorney's credentials, opens the vault, and brings the item into a supervised viewing room. The attorney is never handed the keys to the vault.

## How it works

The attachment lifecycle implements secure storage and authorized retrieval:

1. Upload validation: When a file is uploaded, the server validates its MIME type, file size (e.g., maximum 10 MB), and file extension against an approved whitelist (PDF, PNG, JPEG).
1. Storage isolation: The file is stored outside the public web root in a private directory on the host filesystem (`/opt/hrflow/data/attachments/{tenant_id}/{uuid}`).
1. Metadata recording: The original filename, MIME type, file size, tenant ID, and owner ID are saved in the `request.request_attachments` table.
1. Download authorization: When a download is requested (`GET /api/v1/requests/{id}/attachments/{fileId}`), the application checks that the caller's JWT tenant matches the file tenant and verifies role access (the employee themselves, their direct manager, or an HR admin).
1. Stream response: The application streams the file bytes directly to the HTTP response with `Content-Disposition: attachment; filename="safe_name.pdf"` and `X-Content-Type-Options: nosniff`.

## Why HRFlow needs it

In HRFlow, privacy and compliance are non-negotiable requirements:

- Sick leave certificates: Employees upload medical notes containing private health diagnoses protected by privacy regulations.
- Material and purchase receipts: Equipment purchase requests include sensitive vendor invoices and price quotes.
- Self-hosted file governance: Avoids the complexity and recurring bill of third-party S3 cloud buckets while maintaining strict local disk access control.

## Minimal example

Attachment metadata table in PostgreSQL:

```sql
CREATE TABLE request.request_attachments (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    request_id UUID NOT NULL,
    uploaded_by UUID NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachment_request FOREIGN KEY (tenant_id, request_id)
        REFERENCES request.general_requests (tenant_id, id)
);

CREATE INDEX idx_attachments_tenant_request
ON request.request_attachments (tenant_id, request_id);
```

## Application-specific implementation

Authorized streaming download controller in Java:

```java
@RestController
@RequestMapping("/api/v1/requests/{requestId}/attachments")
public class AttachmentDownloadController {

    private final AttachmentSecurityService securityService;
    private final FileStorageService storageService;

    public AttachmentDownloadController(AttachmentSecurityService securityService,
                                        FileStorageService storageService) {
        this.securityService = securityService;
        this.storageService = storageService;
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID requestId,
                                             @PathVariable UUID attachmentId) {
        String tenantId = TenantContext.getTenantId();
        AttachmentMeta meta = securityService.verifyDownloadAccess(tenantId,
                                                                  requestId,
                                                                  attachmentId);

        Resource fileResource = storageService.loadAsResource(meta.getStoragePath());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getSafeFilename() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(fileResource);
    }
}
```

## Common mistakes and pitfalls

Avoid these critical attachment security vulnerabilities:

> **Pitfall:** Storing uploaded files using their original user-supplied filenames. An attacker can upload a file named `../../../../etc/passwd` or `malicious.jsp`. Always rename stored files to randomized UUIDs on disk, storing the original filename solely as metadata in the database.

- Trusting client-supplied MIME types: The browser sets the `Content-Type` header based on file extension. Always verify the file's magic bytes (file signature) on the server using Apache Tika or Java's `Files.probeContentType()`.
- Serving files with inline execution headers: Omitting `Content-Disposition: attachment` allows SVG files containing embedded `<script>` tags to execute JavaScript in the victim's browser session (Stored XSS).
- Missing antivirus or file size caps: Allowing multi-gigabyte uploads causes Denial of Service (DoS) by filling server disk partitions. Always enforce strict size limits.

## Testing and debugging

Validation test plan:

- Cross-tenant IDOR test: Authenticate as Tenant B. Attempt to download an attachment ID belonging to Tenant A. Assert that the server returns 404 Not Found or 403 Forbidden.
- Unauthenticated access test: Attempt direct HTTP download without an `Authorization` header. Assert that the server returns 401 Unauthorized.
- Directory traversal test: Submit an upload request with filename `../../boot.ini`. Confirm that the file is stored under its randomized UUID and the dangerous filename is sanitized.

## Trade-offs and alternatives

File storage architectures:

- Local filesystem with streaming controllers (HRFlow choice): Zero external cost, simple self-hosted backups, complete code-level access control; consumes application server bandwidth during large file transfers.
- Cloud object storage with signed URLs (AWS S3 / MinIO): Offloads file transfer bandwidth to cloud infrastructure; introduces cloud provider dependencies and external subscription costs.
- Database BLOB storage: Simplest backup alignment; severely bloats database size, degrades query cache performance, and exhausts database memory.

## When not to use it

Do not use authenticated private streaming for:

- Public static assets: Company logos, application CSS, and public favicon images should be served directly by Caddy or a public CDN with aggressive browser caching.
- Ephemeral test outputs: Temporary CSV export files that are generated and streamed in a single request do not require permanent disk persistence.

## Related documentation

- [Multi-tenancy primer](MULTI-TENANCY.md)
- [Reverse proxy and HTTPS primer](REVERSE-PROXY-HTTPS.md)
- [Week 07 requests and approvals build guide](../07-REQUESTS-AND-APPROVALS/BUILD-GUIDE.md)
- [API and database design](../../REST-API-DATABASE-DESIGN.md)
