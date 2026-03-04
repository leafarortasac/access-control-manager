package br.com.access_control_manager.application.mapper;

import br.com.access_control_manager.domain.entity.User;
import br.com.access_control_manager.domain.enums.UserRole;
import com.br.shared.contracts.model.AcmAuthResponseRepresentation;
import com.br.shared.contracts.model.AcmUserRoleRepresentation;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true)
)
public interface AuthMapper {

    @Mapping(target = "accessToken", source = "token")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "firstAccess", source = "user.firstAccess")
    @Mapping(target = "role", source = "user.role", qualifiedByName = "mapAcmRole")
    AcmAuthResponseRepresentation toAuthResponse(User user, String token, String refreshToken);

    @Named("mapAcmRole")
    default AcmUserRoleRepresentation mapAcmRole(UserRole role) {
        if (role == null) return null;
        try {
            return AcmUserRoleRepresentation.fromValue(role.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}