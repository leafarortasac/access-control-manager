package application.service;

import br.com.access_control_manager.domain.entity.User;
import br.com.access_control_manager.domain.enums.UserRole; // Importe seu Enum aqui
import br.com.access_control_manager.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;
    private final String tenantId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", "M2I3YzA5OGU1YmFhN2U3Zjk0ZWY0ZGIyYmU5ZDBjZjkxZWRhYmFmNjA4MGY1ZDIzM2M4ZGM0ZTI5N2I0YmU3YQ==");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L);

        ReflectionTestUtils.invokeMethod(jwtService, "init");

        user = new User();
        user.setEmail("teste@empresa.com");
        user.setFirstAccess(true);
        user.setRole(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Deve gerar um token contendo a claim first_access")
    void shouldGenerateTokenWithFirstAccessClaim() {
        var token = jwtService.generateToken(user, tenantId, user.isFirstAccess());

        assertNotNull(token);
        var username = jwtService.extractUsername(token);
        var firstAccess = jwtService.extractFirstAccess(token);

        assertEquals(user.getEmail(), username);
        assertTrue(firstAccess);
    }

    @Test
    @DisplayName("Deve validar token corretamente")
    void shouldValidateToken() {
        var token = jwtService.generateToken(user, tenantId, false);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    @DisplayName("Deve extrair a claim tenant_id corretamente")
    void shouldExtractTenantIdClaim() {
        var token = jwtService.generateToken(user, tenantId, true);
        var extracted = jwtService.extractClaim(token, "tenant_id");

        assertEquals(tenantId, extracted);
    }
}