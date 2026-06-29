package com.stop.identity_service.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.user.dto.response.UserPublicResponse;
import com.stop.identity_service.user.dto.response.UserSelfResponse;
import com.stop.identity_service.user.entity.user.Role;
import com.stop.identity_service.user.entity.user.Status;
import com.stop.identity_service.user.entity.user.User;
import com.stop.identity_service.user.repository.UserRepository;
import com.stop.identity_service.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void getSelfUserResponseById_mapsAllFields() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .displayName("User")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .trustScore(BigDecimal.valueOf(7.0))
                .rankScore(BigDecimal.valueOf(7.0))
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .passwordHash("hash")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserSelfResponse res = userService.getSelfUserResponseById(id);

        assertNotNull(res);
        assertEquals(id, res.id());
        assertEquals("user@example.com", res.email());
        assertEquals("User", res.displayName());
        assertEquals(Role.USER, res.role());
        assertEquals(Status.ACTIVE, res.status());
        assertEquals(BigDecimal.valueOf(7.0), res.trustScore());
        assertEquals(BigDecimal.valueOf(7.0), res.rankScore());
    }

    @Test
    void getPublicUserResponseById_mapsOnlyPublicFields() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .displayName("User")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .trustScore(BigDecimal.valueOf(5.0))
                .rankScore(BigDecimal.valueOf(6.0))
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .passwordHash("hash")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserPublicResponse res = userService.getPublicUserResponseById(id);

        assertNotNull(res);
        assertEquals("User", res.displayName());
        assertEquals(BigDecimal.valueOf(5.0), res.trustScore());
        assertEquals(BigDecimal.valueOf(6.0), res.rankScore());
    }

    @Test
    void getSelfUserResponseById_notFound_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userService.getSelfUserResponseById(id)
        );
        assertEquals(
                IdentityErrorCode.USER_NOT_FOUND.getCode(),
                ex.getErrorCode().getCode()
        );
    }
}

