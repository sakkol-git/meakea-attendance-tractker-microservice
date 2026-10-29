package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "employee_shift", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeShift {
    @Id private UUID id;
    private UUID tenantId;
    private UUID employeeId;
    private UUID shiftId;
    private LocalDate validFrom;
    private LocalDate validTo;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> shiftSnapshot;
    @Version private long version;

    public EmployeeShift(UUID tenantId, UUID employeeId, UUID shiftId, LocalDate validFrom,
            LocalDate validTo, Map<String, Object> shiftSnapshot) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.employeeId = employeeId;
        this.shiftId = shiftId;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.shiftSnapshot = Map.copyOf(shiftSnapshot);
    }

    public void close(LocalDate date) { this.validTo = date; }
}
