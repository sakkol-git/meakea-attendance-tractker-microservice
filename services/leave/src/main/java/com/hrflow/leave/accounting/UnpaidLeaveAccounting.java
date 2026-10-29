package com.hrflow.leave.accounting;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Untracked entitlement is null, never a fake zero/unlimited allocation. Workflow still records its history. */
@Component
public class UnpaidLeaveAccounting implements LeaveAccounting {
    @Override public BigDecimal available(Account account) { return null; }
    @Override public String apply(Account account, Action action, BigDecimal days) { return "UNPAID_" + action.entry(); }
}
