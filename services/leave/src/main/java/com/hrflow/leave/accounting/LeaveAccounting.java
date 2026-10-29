package com.hrflow.leave.accounting;

import java.math.BigDecimal;

/** Actual paid/unpaid variation: entitlement settlement is independent of workflow/audit transport. */
public interface LeaveAccounting {
    BigDecimal available(Account account);
    String apply(Account account, Action action, BigDecimal days);

    record Account(String tenant, String employee, String type, int year) {}
    enum Action {
        RESERVE("RESERVATION"), APPROVE("USED"), RELEASE("RELEASE"), CANCEL_APPROVED("COMPENSATION");
        private final String entry;
        Action(String entry) { this.entry = entry; }
        public String entry() { return entry; }
    }
}
