package application.service;

import br.com.access_control_manager.application.mapper.UserMapper;
import br.com.access_control_manager.application.service.UserImportProcessor;
import br.com.access_control_manager.application.service.UserService;
import br.com.access_control_manager.domain.entity.User;
import br.com.access_control_manager.domain.repository.UserRepository;
import br.com.access_control_manager.infrastructure.exception.BusinessException;
import br.com.access_control_manager.infrastructure.exception.ResourceNotFoundException;
import br.com.access_control_manager.infrastructure.security.TenantContext;
import com.br.shared.contracts.model.AcmUserRequestRepresentation;
import com.br.shared.contracts.model.AcmUserRoleRepresentation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserImportProcessor userImportProcessor;

    // Sênior: O segredo para usar InjectMocks com MapStruct é o @Spy
    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Deve lançar erro ao tentar criar usuário com CPF que já existe no Tenant")
    void shouldThrowExceptionWhenCpfAlreadyExists() {
        UUID tenantId = UUID.randomUUID();

        try (MockedStatic<TenantContext> mockedTenantContext = mockStatic(TenantContext.class)) {
            mockedTenantContext.when(TenantContext::getCurrentTenant).thenReturn(tenantId.toString());

            var request = new AcmUserRequestRepresentation();
            request.setFullName("Rafael Melo");
            request.setEmail("novo@email.com");
            request.setPassword("senha123");
            request.setCpf("12345678900");
            request.setRole(AcmUserRoleRepresentation.ADMIN);

            request.setTenantId(tenantId);

            when(userRepository.existsByEmailAndTenantId(any(), any())).thenReturn(false);
            when(userRepository.existsByCpfAndTenantId(any(), any())).thenReturn(true);

            assertThrows(BusinessException.class, () -> {
                userService.createAcmUsers(List.of(request));
            });

            verify(userRepository, atLeastOnce()).existsByCpfAndTenantId(any(), any());
        }
    }

    @Test
    @DisplayName("Deve realizar soft delete apenas se o usuário pertencer ao Tenant logado")
    void shouldPerformSoftDeleteCorrectly() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setActive(true);

        try (MockedStatic<TenantContext> mockedContext = mockStatic(TenantContext.class)) {
            mockedContext.when(TenantContext::getCurrentTenant).thenReturn(tenantId.toString());
            when(userRepository.findByIdAndTenantId(any(), any())).thenReturn(Optional.of(user));

            userService.deleteAcmUser(userId);

            assertFalse(user.isActive());
        }
    }

    @Test
    @DisplayName("Deve lançar erro ao buscar usuário por ID que pertence a outro Tenant")
    void shouldFailWhenUserBelongsToDifferentTenant() {
        UUID myTenantId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        try (MockedStatic<TenantContext> mockedContext = mockStatic(TenantContext.class)) {
            mockedContext.when(TenantContext::getCurrentTenant).thenReturn(myTenantId.toString());
            when(userRepository.findByIdAndTenantId(any(), any())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> userService.getAcmUserById(targetUserId));
        }
    }
}