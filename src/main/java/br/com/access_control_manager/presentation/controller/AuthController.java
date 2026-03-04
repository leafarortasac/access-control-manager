package br.com.access_control_manager.presentation.controller;

import br.com.access_control_manager.application.service.AuthService;
import com.br.shared.contracts.api.AcmAuthenticationApi;
import com.br.shared.contracts.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements AcmAuthenticationApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<AcmAuthResponseRepresentation> acmLogin(
            AcmLoginRequestRepresentation loginRequest) {

        log.info("[AuthController] Iniciando autenticação para o usuário: {}", loginRequest.getEmail());
        var response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AcmAuthResponseRepresentation> acmRefreshToken(
            AcmRefreshTokenRequestRepresentation acmRefreshTokenRequestRepresentation) {

        log.info("[AuthController] Solicitando renovação de token.");

        String token = acmRefreshTokenRequestRepresentation.getRefreshToken();

        var response = authService.refreshToken(token);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> acmChangePassword(
            AcmChangePasswordRequestRepresentation changePasswordRequest) {

        log.info("[AuthController] Processando alteração de senha.");
        authService.changePassword(changePasswordRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> acmForgotPassword(
            AcmForgotPasswordRequestRepresentation forgotPasswordRequest) {

        log.info("[AuthController] Solicitação de recuperação de senha para: {}", forgotPasswordRequest.getEmail());
        authService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> acmResetPassword(
            AcmResetPasswordRequestRepresentation resetPasswordRequest) {

        log.info("[AuthController] Executando reset de senha via token.");
        authService.resetPassword(resetPasswordRequest);
        return ResponseEntity.noContent().build();
    }
}