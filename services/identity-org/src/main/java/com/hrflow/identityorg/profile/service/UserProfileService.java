package com.hrflow.identityorg.profile.service;

import com.hrflow.identityorg.profile.dto.ProfilePatchRequest;
import com.hrflow.identityorg.profile.dto.UserProfileResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserProfileService {
    UserProfileResponse me(Jwt jwt);
    UserProfileResponse update(Jwt jwt, ProfilePatchRequest request);
}
