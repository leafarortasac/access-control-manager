package br.com.access_control_manager.application.service;

import br.com.access_control_manager.application.mapper.TenantMapper;
import br.com.access_control_manager.domain.entity.Tenant;
import br.com.access_control_manager.domain.repository.TenantRepository;
import br.com.access_control_manager.infrastructure.exception.BusinessException;
import br.com.access_control_manager.infrastructure.exception.ResourceNotFoundException;
import com.br.shared.contracts.model.AcmTenantRequestRepresentation;
import com.br.shared.contracts.model.AcmTenantResponseRepresentation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    @Transactional
    public AcmTenantResponseRepresentation createTenant(AcmTenantRequestRepresentation request) {
        log.info("[TenantService] Criando novo tenant: {}", request.getName());

        if (tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw new BusinessException("Já existe uma empresa cadastrada com este subdomínio.");
        }

        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setActive(request.getActive() != null ? request.getActive() : true);

        Tenant savedTenant = tenantRepository.save(tenant);

        log.info("[TenantService] Tenant criado com sucesso. ID: {}", savedTenant.getId());
        return tenantMapper.toResponse(savedTenant);
    }

    @Transactional(readOnly = true)
    public List<AcmTenantResponseRepresentation> findAll() {
        return tenantRepository.findAll().stream()
                .map(tenantMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcmTenantResponseRepresentation findById(UUID id) {
        return tenantRepository.findById(id)
                .map(tenantMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com o ID: " + id));
    }

    @Transactional
    public AcmTenantResponseRepresentation updateTenant(UUID id, AcmTenantRequestRepresentation request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado"));

        if (!tenant.getSubdomain().equals(request.getSubdomain()) &&
                tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw new BusinessException("Subdomínio já está em uso por outra empresa.");
        }

        tenantMapper.updateEntityFromDto(request, tenant);
        return tenantMapper.toResponse(tenantRepository.save(tenant));
    }
}