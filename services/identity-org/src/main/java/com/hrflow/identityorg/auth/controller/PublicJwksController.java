package com.hrflow.identityorg.auth.controller;

import com.hrflow.identityorg.auth.service.JwkProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PublicJwksController {
    private final JwkProviderService jwkProviderService;
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwkProviderService.jwks();
    }
}
