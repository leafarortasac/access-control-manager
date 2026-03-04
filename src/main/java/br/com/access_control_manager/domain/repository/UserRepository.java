package br.com.access_control_manager.domain.repository;

import br.com.access_control_manager.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByCpfAndTenantId(String cpf, UUID tenantId);

    boolean existsByTenantId(UUID tenantId);
}