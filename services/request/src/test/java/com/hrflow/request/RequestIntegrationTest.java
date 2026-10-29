package com.hrflow.request;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.request.entity.MaterialItem;
import com.hrflow.request.entity.RequestType;
import com.hrflow.request.repository.MaterialItemRepository;
import com.hrflow.request.repository.RequestTypeRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@Testcontainers
class RequestIntegrationTest extends RequestReferenceTestSupport {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("attachment.storage.dir", () -> "/tmp/hrflow-test-integ-" + System.currentTimeMillis());
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    RequestTypeRepository requestTypeRepository;

    @Autowired
    MaterialItemRepository materialItemRepository;

    @Autowired com.hrflow.request.repository.GeneralRequestRepository requestRows;
    @Autowired com.hrflow.request.repository.ApprovalStepRepository stepRows;

    @Test
    void separateAccountCannotApproveItsOwnHrStageAndAnIndependentHrDecisionRecordsTheAccount() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), account = UUID.randomUUID();
        String id = UUID.randomUUID().toString();
        requestRows.saveAndFlush(com.hrflow.request.entity.GeneralRequest.builder().id(id).tenantId(tenant.toString())
                .employeeId(employee.toString()).employeeName("Synthetic").type("OTHER").title("Private")
                .reason("Personal").quantity(1).status("PENDING_APPROVAL").version(0L).build());
        stepRows.saveAndFlush(com.hrflow.request.entity.ApprovalStep.builder().id(UUID.randomUUID().toString())
                .tenantId(tenant.toString()).requestId(id).stepOrder(1).approverId(UUID.randomUUID().toString())
                .approverRole("HR_ADMIN").status("PENDING").build());
        var ownHr = jwt().jwt(token -> token.subject(account.toString()).claim("tenantId", tenant.toString())
                .claim("employeeId", employee.toString()).claim("roles", List.of("HR_ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_HR_ADMIN"));
        mvc.perform(post("/requests/{id}/approve", id).with(ownHr).header("Idempotency-Key", UUID.randomUUID().toString())
                .header("If-Match", "\"0\"")).andExpect(status().isForbidden());
        UUID independentAccount = UUID.randomUUID(), independentEmployee = UUID.randomUUID();
        var otherHr = jwt().jwt(token -> token.subject(independentAccount.toString()).claim("tenantId", tenant.toString())
                .claim("employeeId", independentEmployee.toString()).claim("roles", List.of("HR_ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_HR_ADMIN"));
        mvc.perform(post("/requests/{id}/approve", id).with(otherHr).header("Idempotency-Key", UUID.randomUUID().toString())
                .header("If-Match", "\"0\"")).andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[0].decidedByAccount").value(independentAccount.toString()));
    }

    @Test
    void accountSubjectAndEmployeeIdentityStayDistinctForOwnPrivateRequestAccess() throws Exception {
        UUID tenant = UUID.randomUUID(), account = UUID.randomUUID(), employee = UUID.randomUUID();
        var linked = jwt().jwt(token -> token.subject(account.toString()).claim("tenantId", tenant.toString())
                .claim("employeeId", employee.toString()).claim("roles", List.of("EMPLOYEE")))
                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        String json = mvc.perform(post("/requests").with(linked).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"OTHER\",\"title\":\"Private\",\"reason\":\"Personal\",\"quantity\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.id");
        mvc.perform(get("/requests/{id}", id).with(linked)).andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employee.toString()));
        mvc.perform(get("/requests/{id}/comments", id).with(linked)).andExpect(status().isOk());
        mvc.perform(post("/requests/{id}/cancel", id).with(linked).header("Idempotency-Key", UUID.randomUUID().toString())
                .header("If-Match", "\"0\"")).andExpect(status().isOk());
        var missingEmployee = jwt().jwt(token -> token.subject(account.toString()).claim("tenantId", tenant.toString())
                .claim("roles", List.of("EMPLOYEE"))).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        mvc.perform(get("/requests/me").with(missingEmployee)).andExpect(status().isForbidden());
    }

    @Test
    void cachedDraftOutcomeCannotLeakAcrossAccountsAndPaths() throws Exception {
        UUID tenant = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        String body = """
                {"type":"OTHER","title":"Private request","reason":"Personal","quantity":1}
                """;
        String response = mvc.perform(post("/requests").with(employee(tenant, first)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/requests").with(employee(tenant, second)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(second.toString()));
        String id = JsonPath.read(response, "$.id");
        Number version = JsonPath.read(response, "$.version");
        mvc.perform(post("/requests/{id}/cancel", id).with(employee(tenant, first))
                .header("Idempotency-Key", key).header("If-Match", "\"" + version.longValue() + "\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/requests").with(employee(tenant, first)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPreconditionRequired());
    }

    @Test
    @DisplayName("Full general request workflow lifecycle: Draft -> Update -> Submit -> Multi-step Approval -> Attachment -> Fulfillment")
    void fullRequestWorkflowLifecycle() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID directorId = UUID.randomUUID();
        referenceTeam(tenantId.toString(), employeeId.toString(), managerId.toString());
        UUID hrId = UUID.randomUUID();

        // 1. Seed Catalogue: RequestType & MaterialItem
        requestTypeRepository.save(RequestType.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantId.toString())
                .code("MATERIAL")
                .name("Material Request")
                .category("MATERIAL")
                .active(true)
                .build());

        String materialId = UUID.randomUUID().toString();
        materialItemRepository.save(MaterialItem.builder()
                .id(materialId)
                .tenantId(tenantId.toString())
                .code("LAPTOP-M3")
                .name("MacBook Pro M3")
                .category("MATERIAL")
                .advisoryStock(8)
                .unit("UNIT")
                .active(true)
                .build());

        // 2. Query Catalogue via HTTP
        mvc.perform(get("/requests/types").with(employee(tenantId, employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MATERIAL"));

        mvc.perform(get("/materials").with(employee(tenantId, employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("LAPTOP-M3"));

        // 3. Create Draft Request with lines and approval steps
        String createDraftPayload = String.format("""
                {
                  "type": "MATERIAL",
                  "title": "Engineer Laptop Upgrade",
                  "reason": "Current machine out of warranty and battery swollen",
                  "quantity": 1,
                  "lines": [
                    {
                      "materialItemId": "%s",
                      "quantity": 1,
                      "notes": "Include power adapter"
                    }
                  ],
                  "approvers": [
                    {
                      "stepOrder": 1,
                      "approverId": "%s",
                      "approverRole": "MANAGER"
                    },
                    {
                      "stepOrder": 2,
                      "approverId": "%s",
                      "approverRole": "DIRECTOR"
                    }
                  ]
                }
                """, materialId, managerId, directorId);

        String draftJson = mvc.perform(post("/requests").with(employee(tenantId, employeeId))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDraftPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.quantity").value(1))
                .andExpect(header().exists("ETag"))
                .andReturn().getResponse().getContentAsString();

        String requestId = JsonPath.read(draftJson, "$.id");

        // 4. Update Draft (change quantity to 2) with If-Match: "0"
        String updatePayload = """
                {
                  "quantity": 2,
                  "reason": "Updated reason: need for test lab pair programming"
                }
                """;

        String updatedJson = mvc.perform(patch("/requests/{id}", requestId).with(employee(tenantId, employeeId))
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2))
                .andReturn().getResponse().getContentAsString();

        // 5. Submit Request
        String submittedJson = mvc.perform(post("/requests/{id}/submit", requestId).header("If-Match", "\"1\"").with(employee(tenantId, employeeId))
                .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();

        int version1 = JsonPath.read(submittedJson, "$.version");

        // 6. Check Manager Inbox
        mvc.perform(get("/requests/inbox").with(manager(tenantId, managerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId));

        // 7. Manager Approves Step 1 with If-Match
        String step1ApprovedJson = mvc.perform(post("/requests/{id}/approve", requestId).header("Idempotency-Key", UUID.randomUUID().toString()).with(manager(tenantId, managerId))
                .header("If-Match", "\"" + version1 + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Manager approved for project needs\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();

        int version2 = JsonPath.read(step1ApprovedJson, "$.version");

        // 8. Director Approves Step 2 with If-Match -> Transitions to APPROVED
        String approvedJson = mvc.perform(post("/requests/{id}/approve", requestId).header("Idempotency-Key", UUID.randomUUID().toString()).with(hr(tenantId, directorId))
                .header("If-Match", "\"" + version2 + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Director budget signed off\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn().getResponse().getContentAsString();

        int version3 = JsonPath.read(approvedJson, "$.version");

        // 9. Add Attachment (e.g. quote / receipt)
        byte[] pdfBytes = "%PDF-1.4 official hardware invoice details".getBytes(StandardCharsets.UTF_8);
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdfBytes));

        String presignPayload = String.format("""
                {
                  "fileName": "quote.pdf",
                  "contentType": "application/pdf",
                  "fileSize": %d,
                  "checksumSha256": "%s"
                }
                """, pdfBytes.length, sha256);

        String presignJson = mvc.perform(post("/requests/{id}/attachments/presign", requestId).header("Idempotency-Key", UUID.randomUUID().toString()).with(employee(tenantId, employeeId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(presignPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String attachmentId = JsonPath.read(presignJson, "$.attachmentId");

        // Upload attachment bytes
        mvc.perform(put("/requests/{id}/attachments/{attachmentId}/upload", requestId, attachmentId).with(employee(tenantId, employeeId))
                .contentType(MediaType.APPLICATION_PDF)
                .content(pdfBytes))
                .andExpect(status().isOk());

        // Complete attachment
        mvc.perform(post("/requests/{id}/attachments/{attachmentId}/complete", requestId, attachmentId).header("Idempotency-Key", UUID.randomUUID().toString()).with(employee(tenantId, employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZED"));

        // Download attachment metadata and bytes
        mvc.perform(get("/requests/{id}/attachments/{attachmentId}/file", requestId, attachmentId).with(employee(tenantId, employeeId)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    var disposition = org.springframework.http.ContentDisposition.parse(result.getResponse().getHeader("Content-Disposition"));
                    org.assertj.core.api.Assertions.assertThat(disposition.isAttachment()).isTrue();
                    org.assertj.core.api.Assertions.assertThat(disposition.getFilename()).isEqualTo("quote.pdf");
                });

        // 10. Add Comment
        mvc.perform(post("/requests/{id}/comments", requestId).header("Idempotency-Key", UUID.randomUUID().toString()).with(employee(tenantId, employeeId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"Ready for IT desk collection whenever available\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentText").value("Ready for IT desk collection whenever available"));

        // 11. HR Fulfills Material Handover with If-Match
        mvc.perform(post("/requests/{id}/fulfill", requestId).header("Idempotency-Key", UUID.randomUUID().toString()).with(hr(tenantId, hrId))
                .header("If-Match", "\"" + version3 + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"handoverReason\": \"Issued serial MBP-990022 at IT desk counter\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"))
                .andExpect(jsonPath("$.handoverNotes").value("Issued serial MBP-990022 at IT desk counter"));

        // 12. View Request Detail confirms all collections and status
        mvc.perform(get("/requests/{id}", requestId).with(employee(tenantId, employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"))
                .andExpect(jsonPath("$.lines[0].materialItemId").value(materialId))
                .andExpect(jsonPath("$.steps[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.steps[1].status").value("APPROVED"))
                .andExpect(jsonPath("$.attachments[0].fileName").value("quote.pdf"))
                .andExpect(jsonPath("$.comments[0].commentText").value("Ready for IT desk collection whenever available"));
    }

    private static JwtRequestPostProcessor employee(UUID tenantId, UUID employeeId) {
        return jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", employeeId.toString())
                .claim("name", "Alice " + employeeId)
                .claim("roles", List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }

    private static JwtRequestPostProcessor manager(UUID tenantId, UUID managerId) {
        return jwt().jwt(token -> token
                .subject(managerId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", managerId.toString())
                .claim("name", "Manager " + managerId)
                .claim("roles", List.of("MANAGER"))
        ).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
    }

    private static JwtRequestPostProcessor director(UUID tenantId, UUID directorId) {
        return jwt().jwt(token -> token
                .subject(directorId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", directorId.toString())
                .claim("name", "Director " + directorId)
                .claim("roles", List.of("DIRECTOR"))
        ).authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR"));
    }

    private static JwtRequestPostProcessor hr(UUID tenantId, UUID hrId) {
        return jwt().jwt(token -> token
                .subject(hrId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", hrId.toString())
                .claim("name", "HR " + hrId)
                .claim("roles", List.of("HR_ADMIN"))
        ).authorities(new SimpleGrantedAuthority("ROLE_HR_ADMIN"));
    }
}
