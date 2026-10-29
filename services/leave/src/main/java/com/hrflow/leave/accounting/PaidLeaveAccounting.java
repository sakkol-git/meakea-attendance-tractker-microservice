package com.hrflow.leave.accounting;

import com.hrflow.leave.repository.LeaveBalanceRepository;
import com.hrflow.leave.service.TenantSqlScope;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PaidLeaveAccounting implements LeaveAccounting {
    private final LeaveBalanceRepository balances;
    private final TenantSqlScope scope;

    @Override public BigDecimal available(Account account) {
        scope.use(account.tenant());
        return balances.findByTenantIdAndEmployeeIdAndLeaveTypeCodeAndYear(account.tenant(), account.employee(), account.type(), account.year())
                .map(com.hrflow.leave.entity.LeaveBalance::getAvailable).orElse(BigDecimal.ZERO);
    }

    @Override public String apply(Account account, Action action, BigDecimal days) {
        scope.use(account.tenant());
        var balance = balances.findForUpdate(account.tenant(), account.employee(), account.type(), account.year())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No leave balance allocated for year " + account.year()));
        switch (action) {
            case RESERVE -> {
                if (balance.getAvailable().compareTo(days) < 0) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient leave balance");
                balance.setAvailable(balance.getAvailable().subtract(days));
                balance.setPending(balance.getPending().add(days));
            }
            case APPROVE -> {
                balance.setPending(balance.getPending().subtract(days));
                balance.setUsed(balance.getUsed().add(days));
            }
            case RELEASE -> {
                balance.setPending(balance.getPending().subtract(days));
                balance.setAvailable(balance.getAvailable().add(days));
            }
            case CANCEL_APPROVED -> {
                balance.setUsed(balance.getUsed().subtract(days));
                balance.setAvailable(balance.getAvailable().add(days));
            }
        }
        balances.saveAndFlush(balance);
        return action.entry();
    }
}
