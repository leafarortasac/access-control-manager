package application.service;

import br.com.access_control_manager.application.mapper.TenantMapper;
import br.com.access_control_manager.application.service.TenantService;
import br.com.access_control_manager.domain.entity.Tenant;
import br.com.access_control_manager.domain.repository.TenantRepository;
import br.com.access_control_manager.infrastructure.exception.BusinessException;
import br.com.access_control_manager.infrastructure.exception.ResourceNotFoundException;
import com.br.shared.contracts.model.AcmTenantRequestRepresentation;
import com.br.shared.contracts.model.AcmTenantResponseRepresentation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantMapper tenantMapper;

    @InjectMocks private TenantService tenantService;

    @Test
    @DisplayName("Deve lançar erro ao criar tenant com subdomínio já existente")
    void shouldFailWhenSubdomainExists() {

        var request = new AcmTenantRequestRepresentation();
        request.setSubdomain("skopia");

        when(tenantRepository.existsBySubdomain("skopia")).thenReturn(true);

        assertThrows(BusinessException.class, () -> tenantService.createTenant(request));
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar tenant com sucesso quando dados estiverem corretos")
    void shouldCreateTenantSuccessfully() {

        var request = new AcmTenantRequestRepresentation();
        request.setName("Nova Empresa");
        request.setSubdomain("nova");

        var tenant = new Tenant();
        when(tenantRepository.existsBySubdomain("nova")).thenReturn(false);
        when(tenantMapper.toEntity(request)).thenReturn(tenant);
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        assertDoesNotThrow(() -> tenantService.createTenant(request));
        verify(tenantRepository).save(tenant);
    }

    @Test
    @DisplayName("Deve buscar todos os tenants e retornar lista convertida")
    void shouldFindAllTenants() {

        var t1 = new Tenant();
        var t2 = new Tenant();

        when(tenantRepository.findAll()).thenReturn(List.of(t1, t2));
        when(tenantMapper.toResponse(any(Tenant.class))).thenReturn(new AcmTenantResponseRepresentation());

        var result = tenantService.findAll();

        assertEquals(2, result.size());
        verify(tenantRepository, times(1)).findAll();
        verify(tenantMapper, times(2)).toResponse(any());
    }

    @Test
    @DisplayName("Deve buscar por ID com sucesso")
    void shouldFindByIdSuccessfully() {

        var id = UUID.randomUUID();
        var tenant = new Tenant();

        when(tenantRepository.findById(id)).thenReturn(java.util.Optional.of(tenant));
        when(tenantMapper.toResponse(tenant)).thenReturn(new AcmTenantResponseRepresentation());

        var result = tenantService.findById(id);

        assertNotNull(result);
        verify(tenantRepository).findById(id);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando ID não existir")
    void shouldThrowExceptionWhenTenantNotFound() {
        var id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tenantService.findById(id));
    }

    @Test
    @DisplayName("Deve atualizar tenant com sucesso mudando subdomínio disponível")
    void shouldUpdateTenantSuccessfully() {

        var id = UUID.randomUUID();
        var request = new AcmTenantRequestRepresentation();

        request.setSubdomain("novo-sub");

        var tenantExistente = new Tenant();
        tenantExistente.setSubdomain("velho-sub");

        when(tenantRepository.findById(id)).thenReturn(java.util.Optional.of(tenantExistente));
        when(tenantRepository.existsBySubdomain("novo-sub")).thenReturn(false);
        when(tenantRepository.save(tenantExistente)).thenReturn(tenantExistente);
        when(tenantMapper.toResponse(tenantExistente)).thenReturn(new AcmTenantResponseRepresentation());

        tenantService.updateTenant(id, request);

        verify(tenantMapper).updateEntityFromDto(request, tenantExistente);
        verify(tenantRepository).save(tenantExistente);
    }

    @Test
    @DisplayName("Deve falhar ao atualizar subdomínio se ele já pertencer a outra empresa")
    void shouldFailWhenUpdatingToExistingSubdomain() {
        var id = UUID.randomUUID();
        var request = new AcmTenantRequestRepresentation();
        request.setSubdomain("empresa-b");

        var tenantA = new Tenant();
        tenantA.setId(id);
        tenantA.setSubdomain("empresa-a");

        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenantA));
        when(tenantRepository.existsBySubdomain("empresa-b")).thenReturn(true);

        assertThrows(BusinessException.class, () -> tenantService.updateTenant(id, request));
    }

    @Test
    @DisplayName("Deve permitir atualização se o subdomínio for o mesmo da própria empresa")
    void shouldAllowUpdateWhenSubdomainIsUnchanged() {

        var id = UUID.randomUUID();
        var request = new AcmTenantRequestRepresentation();

        request.setSubdomain("mesmo-sub");

        var tenant = new Tenant();
        tenant.setSubdomain("mesmo-sub");

        when(tenantRepository.findById(id)).thenReturn(java.util.Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        assertDoesNotThrow(() -> tenantService.updateTenant(id, request));

        verify(tenantRepository, never()).existsBySubdomain(anyString());
    }
}