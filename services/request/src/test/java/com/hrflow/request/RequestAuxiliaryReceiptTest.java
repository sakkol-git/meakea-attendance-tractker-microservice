package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.service.RequestService;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RequestAuxiliaryReceiptTest {
    @org.junit.jupiter.api.io.TempDir static java.nio.file.Path storage;
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("attachment.storage.dir", () -> storage.toString());
        p.add("spring.datasource.username", () -> "request_app");
        p.add("spring.datasource.password", () -> "test-pass");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired RequestService requests;

    @Test void aRetriedCommentCommitsOnceAndDoesNotShareAReceiptAcrossAccountsOrRequests() throws Exception {
        String tenant = id(), employee = id(), account = id(), key = id();
        String request = draft(tenant, employee), otherRequest = draft(tenant, employee);
        var actor = actor(tenant, employee, account);
        String body = "{\"text\":\"Please review this private request.\"}";
        mvc.perform(post("/requests/{id}/comments", request).with(actor).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPreconditionRequired());
        String first = mvc.perform(post("/requests/{id}/comments", request).with(actor).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String retry = mvc.perform(post("/requests/{id}/comments", request).with(actor).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(retry).isEqualTo(first);
        mvc.perform(post("/requests/{id}/comments", request).with(actor).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Changed\"}")).andExpect(status().isConflict());
        mvc.perform(post("/requests/{id}/comments", otherRequest).with(actor).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        String accountResult = mvc.perform(post("/requests/{id}/comments", request).with(actor(tenant, employee, id())).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(accountResult, "$.id")).isNotEqualTo((String) JsonPath.read(first, "$.id"));
        mvc.perform(post("/requests/{id}/comments", request).with(actor(tenant, id(), id())).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/requests/{id}/comments", request).with(actor(id(), employee, account)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        assertThat(admin.queryForObject("SELECT count(*) FROM request.request_comment WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(3);
        assertThat(admin.queryForObject("SELECT count(*) FROM request.command_receipt_v2 WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(3);
    }

    @Test void aRetriedAttachmentReservationKeepsOnePrivateUploadTarget() throws Exception {
        String tenant = id(), employee = id(), account = id(), key = id(), request = draft(tenant, employee);
        var actor = actor(tenant, employee, account);
        byte[] bytes = "%PDF-evidence".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String checksum = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        String metadata = "{\"fileName\":\"evidence.pdf\",\"contentType\":\"application/pdf\",\"fileSize\":" + bytes.length + ",\"checksumSha256\":\"" + checksum + "\"}";
        String first = mvc.perform(post("/requests/{id}/attachments/presign", request).with(actor).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(metadata)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String retry = mvc.perform(post("/requests/{id}/attachments/presign", request).with(actor).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(metadata)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(retry).isEqualTo(first);
        String attachment = JsonPath.read(first, "$.attachmentId");
        mvc.perform(post("/requests/{id}/attachments/presign", request).with(actor).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(metadata.replace("evidence.pdf", "changed.pdf"))).andExpect(status().isConflict());
        mvc.perform(post("/requests/{id}/attachments/presign", request).with(actor)
                .contentType(MediaType.APPLICATION_JSON).content(metadata)).andExpect(status().isPreconditionRequired());
        mvc.perform(post("/requests/{id}/attachments/{file}/complete", request, attachment).with(actor)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isPreconditionRequired());
        // A failed completion must roll back its receipt so the same command can be retried after upload.
        mvc.perform(post("/requests/{id}/attachments/{file}/complete", request, attachment).with(actor).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/requests/{id}/attachments/{file}/upload", request, attachment).with(actor)
                .contentType(MediaType.APPLICATION_PDF).content(bytes)).andExpect(status().isOk());
        String completed = mvc.perform(post("/requests/{id}/attachments/{file}/complete", request, attachment).with(actor).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String completedRetry = mvc.perform(post("/requests/{id}/attachments/{file}/complete", request, attachment).with(actor).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(completedRetry).isEqualTo(completed);
        mvc.perform(get("/requests/{id}/attachments/{file}/file", request, attachment).with(actor)).andExpect(status().isOk());
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        assertThat(admin.queryForObject("SELECT count(*) FROM request.request_attachment WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
        assertThat(admin.queryForObject("SELECT count(*) FROM request.command_receipt_v2 WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(2);
    }

    @Test void canonicalMetadataAndCompletionRemainCompatibleWithPrivateBinaryTransfer() throws Exception {
        String tenant = id(), employee = id(), request = draft(tenant, employee);
        var actor = actor(tenant, employee, id());
        byte[] bytes = "%PDF-canonical".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String checksum = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        String metadata = "{\"fileName\":\"canonical.pdf\",\"contentType\":\"application/pdf\",\"size\":" + bytes.length + ",\"sha256\":\"" + checksum + "\"}";
        String reserved = mvc.perform(post("/requests/{id}/attachments/presign", request).with(actor).header("Idempotency-Key", id())
                .contentType(MediaType.APPLICATION_JSON).content(metadata)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String attachment = JsonPath.read(reserved, "$.attachmentId");
        mvc.perform(put("/requests/{id}/attachments/{file}/upload", request, attachment).with(actor)
                .contentType(MediaType.APPLICATION_OCTET_STREAM).content(bytes)).andExpect(status().isOk());
        mvc.perform(post("/requests/{id}/attachments/{file}/complete", request, attachment).with(actor).header("Idempotency-Key", id())
                .contentType(MediaType.APPLICATION_JSON).content("{\"sha256\":\"" + "0".repeat(64) + "\"}"))
                .andExpect(status().isUnprocessableEntity());
        String completed = mvc.perform(post("/requests/{id}/attachments/{file}/complete", request, attachment).with(actor).header("Idempotency-Key", id())
                .contentType(MediaType.APPLICATION_JSON).content("{\"sha256\":\"" + checksum + "\",\"size\":" + bytes.length + "}"))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.title").value("canonical.pdf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.version").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.details.sha256").value(checksum))
                .andReturn().getResponse().getContentAsString();
        String again = mvc.perform(post("/requests/{id}/attachments/{file}/complete", request, attachment).with(actor).header("Idempotency-Key", id())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(again).isEqualTo(completed);
        mvc.perform(put("/requests/{id}/attachments/{file}/upload", request, attachment).with(actor)
                .contentType(MediaType.APPLICATION_PDF).content(bytes)).andExpect(status().isOk());
        mvc.perform(put("/requests/{id}/attachments/{file}/upload", request, attachment).with(actor)
                .contentType(MediaType.APPLICATION_PDF).content("%PDF-tampered".getBytes(java.nio.charset.StandardCharsets.UTF_8))).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/requests/{id}/attachments/{file}/file", request, attachment).with(actor)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "no-store"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/requests/{id}/attachments/{file}/file", request, attachment).with(actor(tenant, id(), id()))).andExpect(status().isForbidden());
        mvc.perform(get("/requests/{id}/attachments/{file}/file", request, attachment).with(actor(id(), employee, id()))).andExpect(status().isNotFound());
    }

    @Test void spoofedBytesAndUnsafeNamesNeverBecomePrivateFilesAndFinalizedUploadsStayStable() throws Exception {
        String tenant = id(), employee = id(), request = draft(tenant, employee);
        var actor = actor(tenant, employee, id());
        byte[] spoof = "#!/bin/sh malicious".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String checksum = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(spoof));
        String metadata = "{\"fileName\":\"evidence.pdf\",\"contentType\":\"application/pdf\",\"size\":" + spoof.length + ",\"sha256\":\"" + checksum + "\"}";
        String reserved = mvc.perform(post("/requests/{id}/attachments/presign", request).with(actor).header("Idempotency-Key", id())
                .contentType(MediaType.APPLICATION_JSON).content(metadata)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String attachment = JsonPath.read(reserved, "$.attachmentId");
        mvc.perform(put("/requests/{id}/attachments/{file}/upload", request, attachment).with(actor)
                .contentType(MediaType.APPLICATION_PDF).content(spoof)).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/requests/{id}/attachments/{file}/complete", request, attachment).with(actor).header("Idempotency-Key", id())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnprocessableEntity());
        for (String fileName : java.util.List.of("evidence.exe", "quote\\r\\nX-Evil.pdf")) {
            mvc.perform(post("/requests/{id}/attachments/presign", request).with(actor).header("Idempotency-Key", id())
                    .contentType(MediaType.APPLICATION_JSON).content(metadata.replace("evidence.pdf", fileName)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is4xxClientError());
        }
    }

    private String draft(String tenant, String employee) {
        return requests.createDraftRequest(tenant, employee, "Synthetic", CreateGeneralRequestDto.builder()
                .title("Private").type("OTHER").reason("UAT").quantity(1).build()).getId();
    }
    private static String id() { return UUID.randomUUID().toString(); }
    private static JwtRequestPostProcessor actor(String tenant, String employee, String account) {
        return jwt().jwt(token -> token.subject(account).claim("tenantId", tenant).claim("employeeId", employee)
                .claim("roles", List.of("EMPLOYEE")));
    }
}
