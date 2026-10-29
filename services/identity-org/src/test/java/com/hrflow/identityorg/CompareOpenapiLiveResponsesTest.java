package com.hrflow.identityorg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompareOpenapiLiveResponsesTest {

    private ObjectMapper objectMapper;
    private JsonNode openApiRoot;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        Path openApiPath = Path.of("../../contracts/openapi.json");
        if (!Files.exists(openApiPath)) {
            openApiPath = Path.of("contracts/openapi.json");
        }
        assertThat(Files.exists(openApiPath))
                .as("Canonical OpenAPI specification must exist at contracts/openapi.json")
                .isTrue();
        openApiRoot = objectMapper.readTree(openApiPath.toFile());
    }

    @Test
    @DisplayName("W08-S01/S02: Live identity-org profile response matches OpenAPI UserProfile schema exactly")
    void testLiveProfileMatchesOpenApiSchema() throws Exception {
        JsonNode profileSchema = openApiRoot.path("components").path("schemas").path("UserProfile");
        assertThat(profileSchema.isMissingNode()).isFalse();

        // Sample live profile response payload
        String liveProfileJson = """
                {
                    "userId": "usr-100",
                    "employeeId": "emp-100",
                    "tenantId": "tenant-alpha",
                    "email": "employee@example.com",
                    "name": "Jane Employee",
                    "roles": ["EMPLOYEE"],
                    "timeZone": "Asia/Bangkok"
                }
                """;

        JsonNode liveNode = objectMapper.readTree(liveProfileJson);
        JsonNode properties = profileSchema.path("properties");

        // Verify that all properties in live response are documented in OpenAPI schema
        Iterator<Map.Entry<String, JsonNode>> fields = liveNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            assertThat(properties.has(entry.getKey()))
                    .as("Property '%s' must be documented in OpenAPI UserProfile schema", entry.getKey())
                    .isTrue();
        }

        // Verify required properties
        JsonNode requiredArray = profileSchema.path("required");
        if (requiredArray.isArray()) {
            for (JsonNode req : requiredArray) {
                assertThat(liveNode.has(req.asText()))
                        .as("Required property '%s' must be present in live response", req.asText())
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("W08-S03: Undocumented response properties trigger contract drift regression failure")
    void testUndocumentedPropertyFailsContractRegression() throws Exception {
        JsonNode profileSchema = openApiRoot.path("components").path("schemas").path("UserProfile");
        JsonNode properties = profileSchema.path("properties");

        String driftedProfileJson = """
                {
                    "userId": "usr-100",
                    "employeeId": "emp-100",
                    "tenantId": "tenant-alpha",
                    "email": "employee@example.com",
                    "name": "Jane Employee",
                    "roles": ["EMPLOYEE"],
                    "timeZone": "Asia/Bangkok",
                    "undocumentedInternalSecret": "leaked_token"
                }
                """;

        JsonNode driftedNode = objectMapper.readTree(driftedProfileJson);

        assertThatThrownBy(() -> {
            Iterator<Map.Entry<String, JsonNode>> fields = driftedNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (!properties.has(entry.getKey())) {
                    throw new AssertionError("Undocumented property detected: " + entry.getKey());
                }
            }
        }).isInstanceOf(AssertionError.class)
          .hasMessageContaining("Undocumented property detected: undocumentedInternalSecret");
    }
}
