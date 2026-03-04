package application.service;

import br.com.access_control_manager.application.mapper.AuthMapper;
import br.com.access_control_manager.application.service.AuthService;
import br.com.access_control_manager.domain.entity.Tenant;
import br.com.access_control_manager.domain.entity.User;
import br.com.access_control_manager.domain.enums.UserRole;
import br.com.access_control_manager.domain.repository.TenantRepository;
import br.com.access_control_manager.domain.repository.UserRepository;
import br.com.access_control_manager.infrastructure.security.JwtService;
import com.br.shared.contracts.model.AcmAuthResponseRepresentation;
import com.br.shared.contracts.model.AcmLoginRequestRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthMapper authMapper;

    @InjectMocks private AuthService authService;

    private User user;
    private Tenant tenant;
    private AcmLoginRequestRepresentation loginRequest;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setActive(true);
        tenant.setId(UUID.randomUUID());
        tenant.setSubdomain("skopia");

        user = new User();
        user.setEmail("admin@skopia.com.br");
        user.setPassword("encoded_password");
        user.setActive(true);
        user.setFirstAccess(true);
        user.setTenant(tenant);
        user.setTenantId(tenant.getId());
        user.setRole(UserRole.ADMIN);

        loginRequest = new AcmLoginRequestRepresentation();
        loginRequest.setEmail("admin@skopia.com.br");
        loginRequest.setPassword("password123");
        loginRequest.setSubdomain("skopia");
    }

    @Test
    @DisplayName("Deve logar com sucesso buscando por Subdomain e Email")
    void loginSuccess() {

        when(tenantRepository.findBySubdomain("skopia")).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmailAndTenantId(loginRequest.getEmail(), tenant.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);

        when(jwtService.generateToken(any(), anyString(), anyBoolean())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any(), anyString())).thenReturn("refresh_token");
        when(authMapper.toAuthResponse(any(), anyString(), anyString())).thenReturn(new AcmAuthResponseRepresentation());

        var response = authService.login(loginRequest);

        assertNotNull(response);
        verify(tenantRepository).findBySubdomain("skopia");
        verify(userRepository).findByEmailAndTenantId(loginRequest.getEmail(), tenant.getId());
    }

    @Test
    @DisplayName("Deve renovar o token usando as claims de email e tenantId")
    void refreshTokenSuccess() {

        var oldRefreshToken = "old_refresh_token";
        when(jwtService.extractUsername(oldRefreshToken)).thenReturn(user.getEmail());
        when(jwtService.extractClaim(oldRefreshToken, "tenant_id")).thenReturn(tenant.getId().toString());

        when(userRepository.findByEmailAndTenantId(user.getEmail(), tenant.getId())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(oldRefreshToken, user)).thenReturn(true);

        when(jwtService.generateToken(any(), anyString(), anyBoolean())).thenReturn("new_token");
        when(jwtService.generateRefreshToken(any(), anyString())).thenReturn("new_refresh");
        when(authMapper.toAuthResponse(any(), anyString(), anyString())).thenReturn(new AcmAuthResponseRepresentation());

        authService.refreshToken(oldRefreshToken);

        verify(userRepository).findByEmailAndTenantId(user.getEmail(), tenant.getId());
    }
}