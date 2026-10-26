package com.hrflow.identityorg.auth.service;

import com.hrflow.identityorg.auth.service.record.AttemptTicket;

public interface AttemptThrottlerService {
    AttemptTicket acquire(String sourceAddress, String tenant, String identifier);
    void failed(AttemptTicket ticket);
    void succeeded(AttemptTicket ticket);
}
