package br.com.access_control_manager.presentation.controller;

import br.com.access_control_manager.application.service.UserService;
import com.br.shared.contracts.api.AcmUsersApi;
import com.br.shared.contracts.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController implements AcmUsersApi {

    private final UserService userService;

    @Override
    public ResponseEntity<AcmUserDocumentResponseRepresentation> listAcmUsers(
            String nome,
            String email,
            AcmUserRoleRepresentation role,
            Boolean active,
            String searchTerm,
            Integer limit,
            Integer page,
            String sortField,
            String sortDir,
            Boolean unPaged) {

        log.info("[Controller] Requisição de listagem de usuários recebida.");

        String roleStr = role != null ? role.getValue() : null;

        var response = userService.listUsers(
                nome, email, roleStr, active, searchTerm,
                page, limit, sortField, sortDir, unPaged
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AcmUserDocumentResponseRepresentation> createAcmUsers(
            List<AcmUserRequestRepresentation> acmUserRequestRepresentation) {

        log.info("[Controller] Requisição de criação em massa para {} usuários.", acmUserRequestRepresentation.size());

        var response = userService.createAcmUsers(acmUserRequestRepresentation);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<AcmUserDocumentResponseRepresentation> updateAcmUsers(
            List<AcmUserResponseRepresentation> acmUserResponseRepresentation) {
        log.info("[Controller] Iniciando atualização em massa. Total de registros: {}.", acmUserResponseRepresentation.size());

        var response = userService.updateAcmUsers(acmUserResponseRepresentation);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AcmUserResponseRepresentation> getAcmUserById(UUID id) {
        log.info("[Controller] Buscando usuário por ID: {}", id);
        return ResponseEntity.ok(userService.getAcmUserById(id));
    }

    @Override
    public ResponseEntity<Void> importAcmUsers(MultipartFile file) {
        log.info("[Controller] Recebido arquivo para importação: {}", file.getOriginalFilename());
        userService.importUsers(file);
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> deleteAcmUser(UUID id) {
        log.info("[Controller] Solicitação de exclusão para ID: {}", id);
        userService.deleteAcmUser(id);
        return ResponseEntity.noContent().build();
    }
}