package com.hrflow.leave.accounting;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Composition for the two approved accounting modes; no leave-type naming convention decides policy. */
@Component
public class LeaveAccountingModes {
    private final Map<Boolean, LeaveAccounting> modes;
    public LeaveAccountingModes(PaidLeaveAccounting paid, UnpaidLeaveAccounting unpaid) { modes = Map.of(true, paid, false, unpaid); }
    public LeaveAccounting forType(boolean paid) { return modes.get(paid); }
    public LeaveAccounting forRequest(Boolean capturedPaid) {
        if (capturedPaid == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Original leave accounting mode is unknown; explicit repair is required");
        return modes.get(capturedPaid);
    }
}
