package com.hrflow.leave.service;

import com.hrflow.leave.dto.CreateLeaveRequestDto;
import com.hrflow.leave.dto.LeaveCalculationResult;
import com.hrflow.leave.dto.LeavePreviewResponse;
import com.hrflow.leave.dto.LeaveRecordResponse;
import com.hrflow.leave.entity.LeaveDay;
import com.hrflow.leave.entity.LeaveLedger;
import com.hrflow.leave.entity.LeaveRequest;
import com.hrflow.leave.entity.LeaveType;
import com.hrflow.leave.mapper.LeaveRequestMapper;
import com.hrflow.leave.repository.LeaveDayRepository;
import com.hrflow.leave.repository.LeaveLedgerRepository;
import com.hrflow.leave.repository.LeaveRequestRepository;
import com.hrflow.leave.repository.LeaveTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@lombok.RequiredArgsConstructor
public class LeaveSubmissionService {
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveLedgerRepository leaveLedgerRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveDayRepository leaveDayRepository;
    private final LeaveCalculationService leaveCalculationService;
    private final LeaveOutboxWriter leaveOutboxWriter;
    private final TenantSqlScope tenantSqlScope;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeaveWorkCalendar calendar;
    private final LeaveReservations reservations;
    private final com.hrflow.leave.accounting.LeaveAccountingModes accounting;
    private final com.hrflow.leave.mapper.LeavePreviewMapper previewMapper;

    @Transactional(readOnly = true)
    public LeavePreviewResponse previewLeaveRequest(String tenantId, String employeeId, CreateLeaveRequestDto dto) {
        validateDates(dto);
        tenantSqlScope.use(tenantId);
        LeaveType type = leaveTypeRepository.findByTenantIdAndCode(tenantId, dto.type())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave type not found: " + dto.type()));

        int year = dto.startDate().getYear();
        var account = new com.hrflow.leave.accounting.LeaveAccounting.Account(tenantId, employeeId, dto.type(), year);
        BigDecimal available = accounting.forType(type.isPaid()).available(account);

        var workCalendar = calendar.resolve(tenantId, employeeId, dto.startDate(), dto.endDate());
        LeaveCalculationResult result = leaveCalculationService.calculate(
                dto.startDate(),
                dto.endDate(),
                dto.portion(),
                available,
                type.getMinimumNoticeDays(),
                type.isAllowHalfDay(),
                type.isEvidenceRequired(),
                workCalendar.workingDays().keySet(),
                workCalendar.today()
        );

        return previewMapper.toResponse(result);
    }

    @Transactional
    public LeaveRecordResponse submitLeaveRequest(String tenantId, String employeeId, String employeeName,
                                                  CreateLeaveRequestDto dto) {
        validateDates(dto);
        tenantSqlScope.use(tenantId);
        tenantSqlScope.lockEmployee(tenantId, employeeId);

        LeaveType type = leaveTypeRepository.findByTenantIdAndCode(tenantId, dto.type())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave type not found: " + dto.type()));

        if (type.isEvidenceRequired()) {
            var problem = org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                    "This leave type requires private supporting evidence; the evidence submission workflow is not available for this UAT slice");
            problem.setProperty("code", "LEAVE_EVIDENCE_WORKFLOW_UNAVAILABLE");
            throw new org.springframework.web.ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY, problem, null);
        }

        int year = dto.startDate().getYear();

        var workCalendar = calendar.resolve(tenantId, employeeId, dto.startDate(), dto.endDate());

        var account = new com.hrflow.leave.accounting.LeaveAccounting.Account(tenantId, employeeId, dto.type(), year);
        var policy = accounting.forType(type.isPaid());
        BigDecimal available = policy.available(account);

        // 2. Authoritative calculation
        LeaveCalculationResult calc = leaveCalculationService.calculate(
                dto.startDate(),
                dto.endDate(),
                dto.portion(),
                available,
                type.getMinimumNoticeDays(),
                type.isAllowHalfDay(),
                type.isEvidenceRequired(),
                workCalendar.workingDays().keySet(),
                workCalendar.today()
        );

        if (calc.chargeableDays().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No working days in requested period");
        }

        String portion = (dto.portion() == null || dto.portion().isBlank()) ? "FULL_DAY" : dto.portion().toUpperCase(java.util.Locale.ROOT);
        reservations.requireFree(tenantId, employeeId, calc.workingDates(), portion);

        String entry = policy.apply(account, com.hrflow.leave.accounting.LeaveAccounting.Action.RESERVE, calc.chargeableDays());

        // 4. Create leave request
        String requestId = UUID.randomUUID().toString();
        LeaveRequest request = new LeaveRequest(
                requestId,
                tenantId,
                employeeId,
                employeeName,
                dto.type(),
                dto.title(),
                dto.startDate(),
                dto.endDate(),
                dto.portion(),
                calc.chargeableDays(),
                dto.reason()
        );
        request.captureAccounting(type.isPaid());
        leaveRequestRepository.saveAndFlush(request);

        // 5. Expand days
        for (LocalDate date : calc.workingDates()) {
            LeaveDay day = new LeaveDay(
                    UUID.randomUUID().toString(),
                    tenantId,
                    requestId,
                    employeeId,
                    date,
                    portion,
                    workCalendar.workingDays().get(date).seconds(portion)
            );
            leaveDayRepository.save(day);
        }

        // 6. Record ledger
        LeaveLedger ledger = new LeaveLedger(
                UUID.randomUUID().toString(),
                tenantId,
                employeeId,
                dto.type(),
                year,
                entry,
                calc.chargeableDays(),
                requestId,
                dto.reason()
        );
        leaveLedgerRepository.save(ledger);

        // 7. Record outbox event
        leaveOutboxWriter.recordEvent(
                tenantId,
                "LEAVE_REQUEST",
                requestId,
                "org.leave.request.created.v1",
                request,
                request.getVersion()
        );

        return leaveRequestMapper.toResponse(request);
    }
    private void validateDates(CreateLeaveRequestDto request) {
        try { LeaveCalculationService.validateRange(request.startDate(), request.endDate()); }
        catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, invalid.getMessage());
        }
    }
}
