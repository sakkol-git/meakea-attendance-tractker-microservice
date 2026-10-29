package com.hrflow.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.SchemaCompatibility.SchemaCompatibilityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AvroCompatibilityTest {

    @Test
    @DisplayName("Compatible evolution: adding an optional defaulted field is backward compatible")
    void optionalFieldIsBackwardCompatible() throws Exception {
        Path schemaPath = Path.of("events/attendance-checked-in.v1.avsc");
        String originalJson = Files.readString(schemaPath);
        Schema originalWriter = new Schema.Parser().parse(originalJson);

        // Reader schema adds an optional field with default null
        String evolvedJson = """
                {
                  "type": "record",
                  "name": "AttendanceCheckedIn",
                  "namespace": "com.hrflow.contracts.attendance",
                  "fields": [
                    {"name": "eventId", "type": {"type": "string", "logicalType": "uuid"}},
                    {"name": "tenantId", "type": "string"},
                    {"name": "employeeId", "type": "string"},
                    {"name": "occurredAt", "type": {"type": "long", "logicalType": "timestamp-millis"}},
                    {"name": "source", "type": {"type": "enum", "name": "Source", "symbols": ["MANUAL","NUDGE","INFERRED"]}},
                    {"name": "reason", "type": ["null","string"], "default": null},
                    {"name": "note", "type": ["null","string"], "default": null}
                  ]
                }
                """;
        Schema evolvedReader = new Schema.Parser().parse(evolvedJson);

        var result = SchemaCompatibility.checkReaderWriterCompatibility(evolvedReader, originalWriter);
        assertThat(result.getType()).isEqualTo(SchemaCompatibilityType.COMPATIBLE);
    }

    @Test
    @DisplayName("Incompatible mutation: adding a required field without default fails compatibility")
    void requiredFieldWithoutDefaultIsIncompatible() throws Exception {
        Path schemaPath = Path.of("events/attendance-checked-in.v1.avsc");
        String originalJson = Files.readString(schemaPath);
        Schema originalWriter = new Schema.Parser().parse(originalJson);

        // Reader schema adds a required field with no default value
        String incompatibleJson = """
                {
                  "type": "record",
                  "name": "AttendanceCheckedIn",
                  "namespace": "com.hrflow.contracts.attendance",
                  "fields": [
                    {"name": "eventId", "type": {"type": "string", "logicalType": "uuid"}},
                    {"name": "tenantId", "type": "string"},
                    {"name": "employeeId", "type": "string"},
                    {"name": "occurredAt", "type": {"type": "long", "logicalType": "timestamp-millis"}},
                    {"name": "source", "type": {"type": "enum", "name": "Source", "symbols": ["MANUAL","NUDGE","INFERRED"]}},
                    {"name": "reason", "type": ["null","string"], "default": null},
                    {"name": "requiredSecurityToken", "type": "string"}
                  ]
                }
                """;
        Schema incompatibleReader = new Schema.Parser().parse(incompatibleJson);

        var result = SchemaCompatibility.checkReaderWriterCompatibility(incompatibleReader, originalWriter);
        assertThat(result.getType()).isEqualTo(SchemaCompatibilityType.INCOMPATIBLE);
    }

    @Test
    @DisplayName("OrganizationFact schema parses and validates backward compatibility")
    void organizationFactSchemaValidates() throws Exception {
        Path schemaPath = Path.of("events/organization-fact.v1.avsc");
        String factJson = Files.readString(schemaPath);
        Schema factSchema = new Schema.Parser().parse(factJson);

        assertThat(factSchema).isNotNull();
        assertThat(factSchema.getField("eventId")).isNotNull();
        assertThat(factSchema.getField("tenantId")).isNotNull();
        assertThat(factSchema.getField("payload")).isNotNull();

        var selfResult = SchemaCompatibility.checkReaderWriterCompatibility(factSchema, factSchema);
        assertThat(selfResult.getType()).isEqualTo(SchemaCompatibilityType.COMPATIBLE);
    }
    @Test
    void actualAccountRoleSnapshotEvolutionPreservesOldWritersAndReaders() throws Exception {
        Schema original = new Schema.Parser().parse(Path.of("src/test/resources/organization-fact.original-v1.avsc").toFile());
        Schema current = new Schema.Parser().parse(Path.of("events/organization-fact.v1.avsc").toFile());
        Schema employee = current.getField("payload").schema().getTypes().stream()
                .filter(value -> value.getName().equals("EmployeeSnapshot")).findFirst().orElseThrow();
        assertThat(employee.getField("accountRoles")).isNotNull();
        assertThat(SchemaCompatibility.checkReaderWriterCompatibility(current, original).getType()).isEqualTo(SchemaCompatibilityType.COMPATIBLE);
        assertThat(SchemaCompatibility.checkReaderWriterCompatibility(original, current).getType()).isEqualTo(SchemaCompatibilityType.COMPATIBLE);
    }
    @Test
    void perLeaveConflictEvolutionPreservesOldDailyWritersAndReaders() throws Exception {
        Schema original = new Schema.Parser().parse(Path.of("src/test/resources/attendance-daily-fact.original-v1.avsc").toFile());
        Schema current = new Schema.Parser().parse(Path.of("events/attendance-daily-fact.v1.avsc").toFile());
        assertThat(current.getField("leaveReferences").schema().getElementType().getField("workRecorded")).isNotNull();
        assertThat(SchemaCompatibility.checkReaderWriterCompatibility(current, original).getType()).isEqualTo(SchemaCompatibilityType.COMPATIBLE);
        assertThat(SchemaCompatibility.checkReaderWriterCompatibility(original, current).getType()).isEqualTo(SchemaCompatibilityType.COMPATIBLE);
    }
}
