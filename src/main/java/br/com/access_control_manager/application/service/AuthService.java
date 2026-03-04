package br.com.access_control_manager.application.service;

import br.com.access_control_manager.application.mapper.AuthMapper;
import br.com.access_control_manager.domain.entity.Tenant;
import br.com.access_control_manager.domain.entity.User;
import br.com.access_control_manager.domain.repository.TenantRepository;
import br.com.access_control_manager.domain.repository.UserRepository;
import br.com.access_control_manager.infrastructure.exception.BusinessException;
import br.com.access_control_manager.infrastructure.exception.ResourceNotFoundException;
import br.com.access_control_manager.infrastructure.exception.UnauthorizedException;
import br.com.access_control_manager.infrastructure.security.JwtService;
import br.com.access_control_manager.infrastructure.security.TenantContext; // Adicionado
import com.br.shared.contracts.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;

    @Transactional
    public AcmAuthResponseRepresentation login(final AcmLoginRequestRepresentation request) {
        log.info("[Auth] Tentativa de login: email={} | subdomain={}", request.getEmail(), request.getSubdomain());

        Tenant tenant = tenantRepository.findBySubdomain(request.getSubdomain())
                .orElseThrow(() -> new UnauthorizedException("Empresa não encontrada ou subdomínio inválido."));

        User user = userRepository.findByEmailAndTenantId(request.getEmail(), tenant.getId())
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas para esta empresa."));

        validateAccessPolicy(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("[Auth] Senha incorreta para o usuário: {}", request.getEmail());
            throw new UnauthorizedException("Credenciais inválidas.");
        }

        updateSecurityContext(user);
        user.setLastLoginAt(LocalDateTime.now());

        String token = jwtService.generateToken(user, user.getTenantId().toString(), user.isFirstAccess());
        String refreshToken = jwtService.generateRefreshToken(user, user.getTenantId().toString());

        return authMapper.toAuthResponse(user, token, refreshToken);
    }

    @Transactional
    public void changePassword(final AcmChangePasswordRequestRepresentation request) {

        User user = getAuthenticatedUser();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("A senha atual fornecida está incorreta.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstAccess(false);
        log.info("[Auth] Senha alterada e primeiro acesso finalizado para: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public AcmAuthResponseRepresentation refreshToken(final String refreshTokenRequest) {

        String email = jwtService.extractUsername(refreshTokenRequest);
        String tenantIdStr = jwtService.extractClaim(refreshTokenRequest, "tenant_id");

        if (tenantIdStr == null) throw new UnauthorizedException("Token inválido.");

        User user = userRepository.findByEmailAndTenantId(email, UUID.fromString(tenantIdStr))
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida."));

        if (!jwtService.isTokenValid(refreshTokenRequest, user)) {
            throw new UnauthorizedException("Token de renovação expirado.");
        }

        String newToken = jwtService.generateToken(user, user.getTenantId().toString(), user.isFirstAccess());
        String newRefreshToken = jwtService.generateRefreshToken(user, user.getTenantId().toString());

        return authMapper.toAuthResponse(user, newToken, newRefreshToken);
    }

    @Transactional
    public void resetPassword(final AcmResetPasswordRequestRepresentation request) {
        if (jwtService.isTokenExpired(request.getToken())) {
            throw new BusinessException("O token de recuperação expirou.");
        }

        String email = jwtService.extractUsername(request.getToken());
        String tenantIdStr = jwtService.extractClaim(request.getToken(), "tenant_id");

        User user = userRepository.findByEmailAndTenantId(email, UUID.fromString(tenantIdStr))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstAccess(false);
    }

    @Transactional(readOnly = true)
    public void forgotPassword(final AcmForgotPasswordRequestRepresentation request) {
        tenantRepository.findBySubdomain(request.getSubdomain()).ifPresent(tenant -> {
            userRepository.findByEmailAndTenantId(request.getEmail(), tenant.getId()).ifPresent(user -> {
                String resetToken = jwtService.generateToken(user, tenant.getId().toString(), true);
                log.info("[Auth] Token de recuperação gerado para {}: {}", user.getEmail(), resetToken);
            });
        });
    }

    private void updateSecurityContext(User user) {
        var authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void validateAccessPolicy(User user) {
        if (!user.isActive()) throw new BusinessException("Conta desativada.");
        if (user.getTenant() != null && !user.getTenant().isActive()) {
            throw new BusinessException("Empresa inativa.");
        }
    }

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Não autenticado.");
        }

        String email = authentication.getName();
        String currentTenantId = TenantContext.getCurrentTenant();

        if (currentTenantId == null) {
            throw new UnauthorizedException("Contexto de empresa não identificado.");
        }

        return userRepository.findByEmailAndTenantId(email, UUID.fromString(currentTenantId))
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado na empresa atual."));
    }
}