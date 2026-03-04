package br.com.access_control_manager.presentation.controller;

import br.com.access_control_manager.application.service.TenantService;
import com.br.shared.contracts.api.AcmTenantsApi;
import com.br.shared.contracts.model.AcmTenantRequestRepresentation;
import com.br.shared.contracts.model.AcmTenantResponseRepresentation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TenantController implements AcmTenantsApi {

    private final TenantService tenantService;

    @Override
    public ResponseEntity<AcmTenantResponseRepresentation> createAcmTenant(
            AcmTenantRequestRepresentation acmTenantRequestRepresentation) {

        log.info("[TenantController] Recebida requisição para criar tenant: {}",
                acmTenantRequestRepresentation.getName());

        var response = tenantService.createTenant(acmTenantRequestRepresentation);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AcmTenantResponseRepresentation>> listAllAcmTenants() {
        log.info("[Tenant] Listando todos os tenants (Acesso SUPER_ADMIN)");
        return ResponseEntity.ok(tenantService.findAll());
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and @securityService.isUserFromTenant(#uuid))")
    public ResponseEntity<AcmTenantResponseRepresentation> getAcmTenantById(UUID uuid) {
        log.info("[Tenant] Buscando tenant por ID: {}", uuid);
        return ResponseEntity.ok(tenantService.findById(uuid));
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AcmTenantResponseRepresentation> updateAcmTenant(
            UUID uuid,
            @Valid AcmTenantRequestRepresentation request) {
        log.info("[Tenant] Atualizando dados do tenant: {}", uuid);
        return ResponseEntity.ok(tenantService.updateTenant(uuid, request));
    }
}