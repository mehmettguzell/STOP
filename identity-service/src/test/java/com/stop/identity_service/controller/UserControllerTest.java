package com.stop.identity_service.controller;

import com.stop.identity_service.user.dto.response.UserPublicResponse;
import com.stop.identity_service.user.dto.response.UserSelfResponse;
import com.stop.identity_service.user.controller.UserController;
import com.stop.identity_service.user.entity.user.Role;
import com.stop.identity_service.user.entity.user.Status;
import com.stop.identity_service.user.service.UserService;
import com.stop.identity_service.userProfile.dto.response.SliceResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private UserService userService;

    private final UUID targetUserId = UUID.randomUUID();

    private UserController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new UserController(userService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUser_admin_returnsSelfResponse() {
        UserSelfResponse self = new UserSelfResponse(
                targetUserId,
                "admin@example.com",
                null,
                "Admin",
                Role.ADMIN,
                Status.ACTIVE,
                BigDecimal.valueOf(7.0),
                BigDecimal.valueOf(7.0),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z"),
                false,
                false,
                null
        );

        when(userService.getSelfUserResponseById(eq(targetUserId))).thenReturn(self);

        setAuthentication(jwtWithRole("ADMIN", "admin@example.com", targetUserId));

        ResponseEntity<?> response = controller.getUser(targetUserId);
        Object body = response.getBody();

        assertNotNull(body);
        assertInstanceOf(UserSelfResponse.class, body);

        UserSelfResponse data = (UserSelfResponse) body;
        assertEquals(targetUserId, data.id());
        assertEquals("Admin", data.displayName());
        assertEquals("ADMIN", data.role().name());

        verify(userService, times(1)).getSelfUserResponseById(eq(targetUserId));
        verify(userService, never()).getPublicUserResponseById(eq(targetUserId));
    }

    @Test
    void getUser_nonAdmin_returnsPublicResponse() {
        UserPublicResponse publicResponse = new UserPublicResponse(targetUserId, "User", BigDecimal.valueOf(5.0), BigDecimal.valueOf(6.0), null);

        when(userService.getPublicUserResponseById(eq(targetUserId))).thenReturn(publicResponse);

        setAuthentication(jwtWithRole("USER", "user@example.com", targetUserId));

        ResponseEntity<?> response = controller.getUser(targetUserId);
        Object body = response.getBody();

        assertNotNull(body);
        assertInstanceOf(UserPublicResponse.class, body);

        UserPublicResponse data = (UserPublicResponse) body;
        assertEquals("User", data.displayName());
        assertEquals(BigDecimal.valueOf(5.0), data.trustScore());
        assertEquals(BigDecimal.valueOf(6.0), data.rankScore());

        verify(userService, times(1)).getPublicUserResponseById(eq(targetUserId));
        verify(userService, never()).getSelfUserResponseById(eq(targetUserId));
    }

    @Test
    void getMe_authenticated_returnsSelfResponse() {
        UUID currentUserId = UUID.randomUUID();
        UserSelfResponse self = new UserSelfResponse(
                currentUserId,
                "me@example.com",
                null,
                "Me",
                Role.USER,
                Status.ACTIVE,
                BigDecimal.valueOf(7.0),
                BigDecimal.valueOf(7.0),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z"),
                false,
                false,
                null
        );

        when(userService.getSelfUserResponseById(eq(currentUserId))).thenReturn(self);

        setAuthentication(jwtWithRole("USER", "me@example.com", currentUserId));

        ResponseEntity<UserSelfResponse> response = controller.getMe();
        UserSelfResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(currentUserId, body.id());
        assertEquals("Me", body.displayName());

        verify(userService, times(1)).getSelfUserResponseById(eq(currentUserId));
    }

    @Test
    void getAllUsers_admin_returnsPagedSelfResponses() {
        UUID uid = UUID.randomUUID();
        UserSelfResponse row = new UserSelfResponse(
                uid,
                "u@example.com",
                null,
                "User",
                Role.USER,
                Status.ACTIVE,
                BigDecimal.valueOf(7.0),
                BigDecimal.valueOf(7.0),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z"),
                false,
                false,
                null
        );

        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        var page = new PageImpl<>(List.of(row), expectedPageable, 1);
        when(userService.findAllUsers(any(Pageable.class))).thenReturn(page);

        setAuthentication(jwtWithRole("ADMIN", "admin@example.com", UUID.randomUUID()));

        ResponseEntity<SliceResponse<UserSelfResponse>> response = controller.getAllUsers(0, 20, null);
        SliceResponse<UserSelfResponse> body = response.getBody();

        assertNotNull(body);
        assertEquals(1, body.content().size());
        assertEquals(uid, body.content().getFirst().id());

        verify(userService, times(1)).findAllUsers(any(Pageable.class));
    }

    private Jwt jwtWithRole(String role, String email, UUID subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("role", role)
                .claim("email", email)
                .subject(subject.toString())
                .build();
    }

    private void setAuthentication(Jwt jwt) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("role"))
        );
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
