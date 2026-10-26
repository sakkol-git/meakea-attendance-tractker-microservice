package com.hrflow.identityorg.auth.service;

public interface CookieHandlerService {
    String csrfCookie();
    String refreshCookie(String token);
    String clearRefreshCookie();
    void verify(String origin, String csrfCookie, String csrfHeader);
}
