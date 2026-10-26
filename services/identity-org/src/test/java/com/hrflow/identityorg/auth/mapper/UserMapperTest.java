package com.hrflow.identityorg.auth.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.hrflow.identityorg.profile.dto.UserProfileResponse;
import com.hrflow.identityorg.profile.entity.Tenant;
import com.hrflow.identityorg.profile.entity.User;
import java.util.List;

import com.hrflow.identityorg.profile.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserMapperTest {
    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void mapsAuthUserToProfileResponseAccurately() {
        Tenant tenant = new Tenant("demo");
        User user = new User(tenant, "employee@example.com", "hash", "Test Employee");

        UserProfileResponse response = mapper.toProfileResponse(user);

        assertNotNull(response);
        assertEquals(user.getId().toString(), response.userId());
        assertEquals(user.getEmployeeId().toString(), response.employeeId());
        assertEquals(tenant.getId().toString(), response.tenantId());
        assertEquals("Test Employee", response.name());
        assertEquals("employee@example.com", response.email());
        assertEquals("Asia/Phnom_Penh", response.timeZone());
        assertEquals("en", response.locale());
        assertEquals(List.of("EMPLOYEE", "OWNER"), response.roles());
    }

    @Test
    void handlesNullOrBlankRolesGracefully() {
        assertEquals(List.of(), mapper.splitRoles(null));
        assertEquals(List.of(), mapper.splitRoles("   "));
        assertEquals(List.of("HR_ADMIN", "MANAGER"), mapper.splitRoles("HR_ADMIN,MANAGER"));
        assertThrows(IllegalStateException.class, () -> mapper.splitRoles("SUPERUSER"));
    }
}
