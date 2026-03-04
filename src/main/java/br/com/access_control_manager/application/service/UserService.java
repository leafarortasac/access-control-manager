package br.com.access_control_manager.application.service;

import br.com.access_control_manager.application.dto.UserImportCsvDTO;
import br.com.access_control_manager.application.mapper.UserMapper;
import br.com.access_control_manager.domain.entity.User;
import br.com.access_control_manager.domain.enums.UserRole;
import br.com.access_control_manager.domain.repository.UserRepository;
import br.com.access_control_manager.domain.repository.specification.UserSpecification;
import br.com.access_control_manager.infrastructure.exception.BusinessException;
import br.com.access_control_manager.infrastructure.exception.ResourceNotFoundException;
import br.com.access_control_manager.infrastructure.security.TenantContext;
import com.br.shared.contracts.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserImportProcessor importProcessor;

    @Transactional(readOnly = true)
    public AcmUserDocumentResponseRepresentation listUsers(
            String nome, String email, String role, Boolean active, String searchTerm,
            int page, int limit, String sortField, String sortDir, boolean unPaged) {

        UUID tenantId = getCurrentTenantId();
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortField);
        Pageable pageable = unPaged ? Pageable.unpaged() : PageRequest.of(page, limit, sort);

        UserRole roleEnum = parseRole(role);
        Specification<User> spec = UserSpecification.filter(nome, email, roleEnum, active, searchTerm, tenantId);

        Page<User> userPage = userRepository.findAll(spec, pageable);
        return userMapper.toPagedResponse(userPage);
    }

    @Transactional
    public AcmUserDocumentResponseRepresentation createAcmUsers(List<AcmUserRequestRepresentation> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("A lista de usuários não pode estar vazia.");
        }

        final UUID targetTenantId = resolveTargetTenant(requests);

        List<User> entities = requests.stream()
                .map(req -> {
                    validateUniqueness(req.getEmail(), req.getCpf(), targetTenantId);

                    return buildNewUser(req, targetTenantId);
                }).toList();

        List<User> saved = userRepository.saveAll(entities);
        return userMapper.toPagedResponse(new PageImpl<>(saved, Pageable.unpaged(), saved.size()));
    }

    @Transactional
    public AcmUserDocumentResponseRepresentation updateAcmUsers(List<AcmUserResponseRepresentation> requests) {
        UUID tenantId = getCurrentTenantId();

        List<User> entitiesToUpdate = requests.stream()
                .map(req -> {
                    User user = userRepository.findByIdAndTenantId(req.getId(), tenantId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + req.getId()));

                    updateUserData(user, req, tenantId);
                    return user;
                }).toList();

        List<User> saved = userRepository.saveAll(entitiesToUpdate);
        return userMapper.toPagedResponse(new PageImpl<>(saved, Pageable.unpaged(), saved.size()));
    }

    @Transactional(readOnly = true)
    public AcmUserResponseRepresentation getAcmUserById(UUID id) {
        return userRepository.findByIdAndTenantId(id, getCurrentTenantId())
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    @Transactional
    public void deleteAcmUser(UUID id) {
        User user = userRepository.findByIdAndTenantId(id, getCurrentTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        user.setActive(false);
    }

    public void importUsers(MultipartFile file) {
        UUID tenantId = getCurrentTenantId();

        if (file.isEmpty() || !Objects.equals(file.getContentType(), "text/csv")) {
            throw new BusinessException("Apenas arquivos CSV são permitidos.");
        }

        try {
            byte[] csvBytes = file.getBytes();
            importProcessor.processCsvAsync(csvBytes, tenantId, row -> this.processImportRow(row, tenantId));
        } catch (IOException e) {
            throw new BusinessException("Falha ao ler arquivo.");
        }
    }

    @Transactional
    public void processImportRow(UserImportCsvDTO row, UUID tenantId) {
        try {
            AcmUserRequestRepresentation req = new AcmUserRequestRepresentation();
            req.setFullName(row.getFullName());
            req.setEmail(row.getEmail());
            req.setCpf(row.getCpf());
            req.setPhone(row.getPhone());
            req.setRole(AcmUserRoleRepresentation.fromValue(row.getRole().toUpperCase()));
            req.setPassword("Trocar123!");

            createSingleUser(req, tenantId);
        } catch (Exception e) {
            log.error("[Import] Erro na linha {}: {}", row.getEmail(), e.getMessage());
        }
    }

    private void createSingleUser(AcmUserRequestRepresentation req, UUID tenantId) {
        validateUniqueness(req.getEmail(), req.getCpf(), tenantId);
        User user = buildNewUser(req, tenantId);
        userRepository.save(user);
    }

    private User buildNewUser(AcmUserRequestRepresentation req, UUID tenantId) {
        User user = userMapper.toEntity(req);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setTenantId(tenantId);
        user.setFirstAccess(true);
        user.setActive(true);
        return user;
    }

    private void updateUserData(User user, AcmUserResponseRepresentation req, UUID tenantId) {
        if (!user.getEmail().equalsIgnoreCase(req.getEmail())) {
            checkEmailAvailability(req.getEmail(), tenantId);
            user.setEmail(req.getEmail());
        }
        if (req.getCpf() != null && !user.getCpf().equals(req.getCpf())) {
            checkCpfAvailability(req.getCpf(), tenantId);
            user.setCpf(req.getCpf());
        }
        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        user.setActive(req.getActive());
        if (req.getRole() != null) user.setRole(UserRole.valueOf(req.getRole().getValue()));
    }

    private UUID getCurrentTenantId() {
        return UUID.fromString(Objects.requireNonNull(TenantContext.getCurrentTenant(), "Tenant nulo"));
    }

    private UUID resolveTargetTenant(List<AcmUserRequestRepresentation> requests) {
        String contextTenant = TenantContext.getCurrentTenant();
        if (contextTenant != null) return UUID.fromString(contextTenant);
        return requests.stream().findFirst()
                .map(AcmUserRequestRepresentation::getTenantId)
                .orElseThrow(() -> new BusinessException("TenantId obrigatório"));
    }

    private void validateUniqueness(String email, String cpf, UUID tenantId) {
        if (userRepository.existsByEmailAndTenantId(email, tenantId)) throw new BusinessException("Email já existe.");
        if (userRepository.existsByCpfAndTenantId(cpf, tenantId)) throw new BusinessException("CPF já existe.");
    }

    private void checkEmailAvailability(String email, UUID tenantId) {
        if (userRepository.existsByEmailAndTenantId(email, tenantId)) throw new BusinessException("Email em uso.");
    }

    private void checkCpfAvailability(String cpf, UUID tenantId) {
        if (userRepository.existsByCpfAndTenantId(cpf, tenantId)) throw new BusinessException("CPF em uso.");
    }

    private UserRole parseRole(String role) {
        if (role == null) return null;
        return UserRole.valueOf(role.toUpperCase());
    }
}