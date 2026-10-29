package com.hrflow.identityorg.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.HolidaySnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.contracts.organization.ShiftSnapshot;
import com.hrflow.identityorg.organization.service.OrganizationFactMapper;
import com.hrflow.identityorg.organization.service.OrganizationFactMapper.OutboxEntry;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OrganizationFactMapperTest {
    private final OrganizationFactMapper mapper = new OrganizationFactMapper(JsonMapper.builder().build());

    @Test
    void allThreeSnapshotsRoundTripThroughTheCanonicalAvroSchema() throws Exception {
        UUID employee = UUID.randomUUID();
        UUID shift = UUID.randomUUID();
        UUID holiday = UUID.randomUUID();
        OrganizationFact employeeFact = roundTrip(row("org.employee.v1", "org.employee.created.v1", employee,
                """
                {"employeeId":"%s","userId":null,"employeeNo":"E-1","fullName":"Test Person",
                 "email":"person@example.test","departmentId":null,"departmentName":null,
                 "positionId":null,"position":null,"managerId":null,"hireDate":null,
                 "terminatedOn":null,"status":"ACTIVE","deviceId":null,"locale":null,
                 "assignments":[],"deletedAt":null}
                """.formatted(employee)));
        assertInstanceOf(EmployeeSnapshot.class, employeeFact.getPayload());
        assertEquals("E-1", ((EmployeeSnapshot) employeeFact.getPayload()).getEmployeeNo());

        OrganizationFact shiftFact = roundTrip(row("org.shift.v1", "org.shift.defined.v1", shift,
                """
                {"shiftId":"%s","name":"Day","startTime":"08:00","endTime":"17:00",
                 "timezone":"Asia/Phnom_Penh","graceMinutes":10,"workingDays":31,
                 "site":{"siteId":"%s","name":"Main","latitude":11.56,"longitude":104.92,
                         "radiusM":100,"timezone":"Asia/Phnom_Penh","wifiSsid":null,"wifiBssid":null},
                 "deletedAt":null}
                """.formatted(shift, UUID.randomUUID())));
        assertInstanceOf(ShiftSnapshot.class, shiftFact.getPayload());
        assertEquals("Main", ((ShiftSnapshot) shiftFact.getPayload()).getSite().getName());

        OrganizationFact holidayFact = roundTrip(row("org.holiday.v1", "org.holiday.added.v1", holiday,
                """
                {"holidayId":"%s","date":"2026-11-09","name":"Company Day",
                 "kind":"COMPANY","deletedAt":null}
                """.formatted(holiday)));
        assertInstanceOf(HolidaySnapshot.class, holidayFact.getPayload());
        assertEquals(1L, holidayFact.getAggregateVersion());
    }

    @Test
    void mismatchedAggregateAndUnknownTypeDoNotBecomePublishedFacts() {
        UUID id = UUID.randomUUID();
        String payload = """
                {"holidayId":"%s","date":"2026-11-09","name":"Company Day","kind":"COMPANY"}
                """.formatted(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class,
                () -> mapper.map(row("org.holiday.v1", "org.holiday.added.v1", id, payload)));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.map(row("org.holiday.v1", "org.employee.created.v1", id, payload)));
    }

    @Test
    void fractionalAndOverflowingShiftIntegersAreRejected() {
        UUID id = UUID.randomUUID();
        String payload = """
                {"shiftId":"%s","name":"Day","startTime":"08:00","endTime":"17:00",
                 "timezone":"Asia/Phnom_Penh","graceMinutes":%s,"workingDays":31,
                 "site":{"siteId":"%s","name":"Main","latitude":11.56,"longitude":104.92,
                         "radiusM":100,"timezone":"Asia/Phnom_Penh"}}
                """;
        for (String invalid : new String[] {"1.5", "2147483648"}) {
            assertThrows(IllegalArgumentException.class, () -> mapper.map(row(
                    "org.shift.v1", "org.shift.defined.v1", id,
                    payload.formatted(id, invalid, UUID.randomUUID()))));
        }
    }

    private OutboxEntry row(String topic, String type, UUID aggregate, String payload) {
        return new OutboxEntry(UUID.randomUUID(), UUID.randomUUID(), topic, aggregate, 1,
                type, UUID.randomUUID(), null, payload, Instant.parse("2026-10-27T00:00:00Z"));
    }

    private OrganizationFact roundTrip(OutboxEntry row) throws Exception {
        OrganizationFact fact = mapper.map(row);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        var writer = new SpecificDatumWriter<OrganizationFact>(OrganizationFact.getClassSchema());
        var encoder = EncoderFactory.get().binaryEncoder(bytes, null);
        writer.write(fact, encoder);
        encoder.flush();
        var reader = new SpecificDatumReader<OrganizationFact>(OrganizationFact.getClassSchema());
        return reader.read(null, DecoderFactory.get().binaryDecoder(bytes.toByteArray(), null));
    }
}
