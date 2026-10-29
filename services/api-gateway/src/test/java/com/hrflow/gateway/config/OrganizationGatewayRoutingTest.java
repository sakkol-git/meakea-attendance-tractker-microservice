package com.hrflow.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.hrflow.gateway.ApiGatewayApplication;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = ApiGatewayApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrganizationGatewayRoutingTest {

    private static HttpServer mockDownstream;
    private static RSAKey rsaKey;
    private static final AtomicInteger downstreamCallCount = new AtomicInteger();
    private static final List<RecordedRequest> recordedRequests = new CopyOnWriteArrayList<>();

    record RecordedRequest(String path, HttpHeaders headers) {}

    @LocalServerPort
    private int port;

    private WebTestClient webClient;


    @BeforeAll
    static void startMockDownstream() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("gw-route-key").generate();
        mockDownstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockDownstream.createContext("/.well-known/jwks.json", exchange -> {
            byte[] body = new JWKSet(rsaKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        mockDownstream.createContext("/", exchange -> {
            if (exchange.getRequestURI().getPath().equals("/.well-known/jwks.json")) {
                return;
            }
            downstreamCallCount.incrementAndGet();
            HttpHeaders headers = new HttpHeaders();
            exchange.getRequestHeaders().forEach(headers::addAll);
            recordedRequests.add(new RecordedRequest(exchange.getRequestURI().getPath(), headers));
            byte[] body = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        mockDownstream.start();
    }

    @AfterAll
    static void stopMockDownstream() {
        if (mockDownstream != null) {
            mockDownstream.stop(0);
        }
    }

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry properties) {
        int port = mockDownstream.getAddress().getPort();
        properties.add("IDENTITY_URI", () -> "http://127.0.0.1:" + port);
        properties.add("ATTENDANCE_URI", () -> "http://127.0.0.1:" + port);
        properties.add("REPORTING_URI", () -> "http://127.0.0.1:" + port);
        properties.add("hrflow.auth.jwks-uri", () -> "http://127.0.0.1:" + port + "/.well-known/jwks.json");
        properties.add("hrflow.auth.issuer", () -> "hrflow-local");
    }

    @BeforeEach
    void resetRecordingsAndClient() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build();
        downstreamCallCount.set(0);
        recordedRequests.clear();
    }

    @Test
    void unauthenticatedOrganizationRequestFailsWithProblemJsonAndNeverReachesDownstream() {
        webClient.get().uri("/api/v2/employees")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("GATEWAY-UNAUTHENTICATED")
                .jsonPath("$.status").isEqualTo(401);

        assertEquals(0, downstreamCallCount.get());
    }

    @Test
    void expiredTokenFailsWithProblemJsonAndNeverReachesDownstream() throws Exception {
        String expiredToken = createToken(Instant.now().minusSeconds(1000), Instant.now().minusSeconds(100));

        webClient.get().uri("/api/v2/employees")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("GATEWAY-UNAUTHENTICATED")
                .jsonPath("$.status").isEqualTo(401);

        assertEquals(0, downstreamCallCount.get());
    }

    @Test
    void validTokenRoutesToDownstreamStrippingV2PrefixAndStrippingIdentityHeaders() throws Exception {
        String validToken = createToken(Instant.now().minusSeconds(10), Instant.now().plusSeconds(900));

        webClient.get().uri("/api/v2/employees")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .header("X-Tenant-Id", "forged-tenant-id")
                .header("X-Employee-Id", "forged-emp-id")
                .header("X-Roles", "ROLE_OWNER")
                .header("X-Role", "ROLE_OWNER")
                .header("X-User-Id", "forged-user-id")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok");

        assertEquals(1, downstreamCallCount.get());
        RecordedRequest recorded = recordedRequests.getFirst();
        assertEquals("/employees", recorded.path());
        assertEquals("Bearer " + validToken, recorded.headers().getFirst(HttpHeaders.AUTHORIZATION));
        assertNull(recorded.headers().getFirst("X-Tenant-Id"));
        assertNull(recorded.headers().getFirst("X-Employee-Id"));
        assertNull(recorded.headers().getFirst("X-Roles"));
        assertNull(recorded.headers().getFirst("X-Role"));
        assertNull(recorded.headers().getFirst("X-User-Id"));
    }

    @Test
    void validTokenRoutesV1DepartmentsStrippingV1Prefix() throws Exception {
        String validToken = createToken(Instant.now().minusSeconds(10), Instant.now().plusSeconds(900));

        webClient.get().uri("/api/v1/departments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .exchange()
                .expectStatus().isOk();

        assertEquals(1, downstreamCallCount.get());
        assertEquals("/departments", recordedRequests.getFirst().path());
    }

    @Test
    void validTokenRoutesV2HolidaysStrippingV2Prefix() throws Exception {
        String validToken = createToken(Instant.now().minusSeconds(10), Instant.now().plusSeconds(900));

        webClient.get().uri("/api/v2/holidays")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .exchange()
                .expectStatus().isOk();

        assertEquals(1, downstreamCallCount.get());
        assertEquals("/holidays", recordedRequests.getFirst().path());
    }

    @Test
    void validTokenRoutesV1AttendanceCheckInStrippingV1PrefixAndSanitizingHeaders() throws Exception {
        String validToken = createToken(Instant.now().minusSeconds(10), Instant.now().plusSeconds(900));

        webClient.post().uri("/api/v1/attendance/check-in")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .header("X-Tenant-Id", "forged-tenant")
                .header("X-Employee-Id", "forged-employee")
                .exchange()
                .expectStatus().isOk();

        assertEquals(1, downstreamCallCount.get());
        RecordedRequest recorded = recordedRequests.getFirst();
        assertEquals("/attendance/check-in", recorded.path());
        assertNull(recorded.headers().getFirst("X-Tenant-Id"));
        assertNull(recorded.headers().getFirst("X-Employee-Id"));
    }

    @Test
    void managerTokenRoutesV1ReportsStrippingV1PrefixAndSanitizingHeaders() throws Exception {
        String validToken = createToken(
                Instant.now().minusSeconds(10), Instant.now().plusSeconds(900), "MANAGER");

        webClient.get().uri("/api/v1/reports/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .header("X-Tenant-Id", "forged-tenant")
                .header("X-Employee-Id", "forged-employee")
                .exchange()
                .expectStatus().isOk();

        assertEquals(1, downstreamCallCount.get());
        RecordedRequest recorded = recordedRequests.getFirst();
        assertEquals("/reports/dashboard", recorded.path());
        assertNull(recorded.headers().getFirst("X-Tenant-Id"));
        assertNull(recorded.headers().getFirst("X-Employee-Id"));
    }

    @Test
    void employeeTokenCannotReachReports() throws Exception {
        String validToken = createToken(
                Instant.now().minusSeconds(10), Instant.now().plusSeconds(900), "EMPLOYEE");

        webClient.get().uri("/api/v1/reports/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .exchange()
                .expectStatus().isForbidden();

        assertEquals(0, downstreamCallCount.get());
    }

    private String createToken(Instant issuedAt, Instant expiresAt) throws Exception {
        return createToken(issuedAt, expiresAt, "OWNER");
    }

    @Test
    void auditRoutesToIdentityOnlyForOwner() throws Exception {
        String owner = createToken(Instant.now().minusSeconds(10), Instant.now().plusSeconds(900), "OWNER");
        webClient.get().uri("/api/v1/audit-logs?page=1&size=25")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner)
                .exchange().expectStatus().isOk();
        assertEquals("/audit-logs", recordedRequests.getFirst().path());
        for (String role : List.of("EMPLOYEE", "MANAGER", "HR_ADMIN")) {
            String token = createToken(Instant.now().minusSeconds(10), Instant.now().plusSeconds(900), role);
            webClient.get().uri("/api/v1/audit-logs")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange().expectStatus().isForbidden();
        }
        assertEquals(1, downstreamCallCount.get());
    }

    private String createToken(Instant issuedAt, Instant expiresAt, String role) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                        .issuer("hrflow-local")
                        .audience("hrflow-clients")
                        .subject("00000000-0000-0000-0000-000000000001")
                        .claim("tenantId", "00000000-0000-0000-0000-000000000001")
                        .claim("roles", List.of(role))
                        .issueTime(Date.from(issuedAt))
                        .expirationTime(Date.from(expiresAt))
                        .build());
        jwt.sign(new RSASSASigner(rsaKey));
        return jwt.serialize();
    }
}
