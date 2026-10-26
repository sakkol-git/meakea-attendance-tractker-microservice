package com.hrflow.identityorg.profile.controller;

import com.hrflow.identityorg.profile.dto.ProfilePatchRequest;
import com.hrflow.identityorg.profile.dto.UserProfileResponse;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.profile.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw AuthFailedException.invalid("Invalid JWT");
        return userProfileService.me(jwt);
    }

    @PatchMapping("/profile")
    public UserProfileResponse update(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody ProfilePatchRequest request) {
        if (jwt == null) throw AuthFailedException.invalid("Invalid JWT");
        return userProfileService.update(jwt, request);
    }
}
