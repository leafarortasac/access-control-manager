package br.com.access_control_manager.application.mapper;

import br.com.access_control_manager.domain.entity.User;
import com.br.shared.contracts.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "firstAccess", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(AcmUserRequestRepresentation request);

    @Mapping(target = "role", source = "role", qualifiedByName = "mapRoleToAcm")
    AcmUserResponseRepresentation toResponse(User entity);

    default AcmUserDocumentResponseRepresentation toPagedResponse(Page<User> userPage) {
        if (userPage == null) return null;

        AcmUserDocumentResponseRepresentation response = new AcmUserDocumentResponseRepresentation();

        List<AcmUserDocumentRepresentation> records = userPage.getContent().stream()
                .map(user -> {
                    AcmUserDocumentRepresentation doc = new AcmUserDocumentRepresentation();
                    doc.setUser(this.toResponse(user));
                    return doc;
                }).toList();

        response.setRecords(records);

        PaginaRepresentation pagina = new PaginaRepresentation();
        pagina.setTotalPaginas(userPage.getTotalPages());
        pagina.setTotalElementos(userPage.getTotalElements());
        pagina.setPaginaAtual(userPage.getNumber());
        pagina.setQuantidadeRegistros(userPage.getSize());

        response.setPage(pagina);

        return response;
    }

    @Named("mapRoleToAcm")
    default AcmUserRoleRepresentation mapRoleToAcm(br.com.access_control_manager.domain.enums.UserRole role) {
        if (role == null) return null;
        try {
            return AcmUserRoleRepresentation.fromValue(role.name());
        } catch (Exception e) {
            return null;
        }
    }
}