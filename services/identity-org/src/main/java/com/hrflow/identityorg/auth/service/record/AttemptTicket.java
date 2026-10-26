package com.hrflow.identityorg.auth.service.record;

import com.hrflow.identityorg.auth.entity.AuthAttempt;

public record AttemptTicket(AuthAttempt account, AuthAttempt source) {}
