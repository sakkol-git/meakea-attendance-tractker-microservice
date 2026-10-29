package com.hrflow.identityorg.organization.service;

import com.hrflow.contracts.organization.AssignmentSnapshot;
import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.HolidaySnapshot;
import com.hrflow.contracts.organization.ShiftSnapshot;
import com.hrflow.contracts.organization.SiteSnapshot;
import java.math.BigDecimal;
import java.util.List;

/** Typed JSON persisted in the owner outbox; Avro is created at publication. */
public final class OrganizationSnapshots {
    private OrganizationSnapshots() {}

    public sealed interface Snapshot permits Employee, Shift, Holiday {}

    public record Assignment(String assignmentId, String shiftId, String validFrom, String validTo) {
        AssignmentSnapshot avro() {
            return new AssignmentSnapshot(required(assignmentId), required(shiftId),
                    required(validFrom), validTo);
        }
    }

    public record Employee(String employeeId, String userId, String employeeNo, String fullName,
            String email, String departmentId, String departmentName, String positionId,
            String position, String managerId, String hireDate, String terminatedOn,
            String status, String deviceId, String locale, List<Assignment> assignments,
            String deletedAt, List<String> accountRoles) implements Snapshot {
        EmployeeSnapshot avro() {
            if (assignments == null) throw new IllegalArgumentException("Assignments required");
            return new EmployeeSnapshot(required(employeeId), userId, required(employeeNo),
                    required(fullName), email, departmentId, departmentName, positionId,
                    position, managerId, hireDate, terminatedOn, required(status), deviceId,
                    locale, assignments.stream().map(Assignment::avro).toList(), deletedAt, accountRoles);
        }
    }

    public record Site(String siteId, String name, BigDecimal latitude, BigDecimal longitude,
            BigDecimal radiusM, String timezone, String wifiSsid, String wifiBssid) {
        SiteSnapshot avro() {
            return new SiteSnapshot(required(siteId), required(name), finite(latitude),
                    finite(longitude), finite(radiusM), required(timezone), wifiSsid, wifiBssid);
        }
    }

    public record Shift(String shiftId, String name, String startTime, String endTime,
            String timezone, BigDecimal graceMinutes, BigDecimal workingDays,
            Site site, String deletedAt) implements Snapshot {
        ShiftSnapshot avro() {
            if (site == null) throw new IllegalArgumentException("Site required");
            return new ShiftSnapshot(required(shiftId), required(name), required(startTime),
                    required(endTime), required(timezone), integer(graceMinutes),
                    integer(workingDays), site.avro(), deletedAt);
        }
    }

    public record Holiday(String holidayId, String date, String name, String kind,
            String deletedAt) implements Snapshot {
        HolidaySnapshot avro() {
            return new HolidaySnapshot(required(holidayId), required(date), required(name),
                    required(kind), deletedAt);
        }
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Required organization field missing");
        return value;
    }

    private static int integer(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Required organization integer missing");
        try {
            return value.intValueExact();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("Invalid organization integer", error);
        }
    }

    private static double finite(BigDecimal value) {
        if (value == null || !Double.isFinite(value.doubleValue())) {
            throw new IllegalArgumentException("Invalid organization number");
        }
        return value.doubleValue();
    }
}
