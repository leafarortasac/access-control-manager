package br.com.access_control_manager.domain.repository.specification;

import br.com.access_control_manager.domain.entity.User;
import br.com.access_control_manager.domain.enums.UserRole;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserSpecification {

    public static Specification<User> filter(
            String nome, String email, UserRole role, Boolean active,
            String searchTerm, UUID tenantId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (StringUtils.hasText(nome)) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), "%" + nome.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(email)) {
                predicates.add(cb.equal(cb.lower(root.get("email")), email.toLowerCase()));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (StringUtils.hasText(searchTerm)) {
                String pattern = "%" + searchTerm.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}