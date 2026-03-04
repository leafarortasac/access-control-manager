package br.com.access_control_manager.application.mapper;

import br.com.access_control_manager.domain.entity.Tenant;
import com.br.shared.contracts.model.AcmTenantRequestRepresentation;
import com.br.shared.contracts.model.AcmTenantResponseRepresentation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    ObjectMapper objectMapper = new ObjectMapper();

    @Mapping(target = "settings", source = "settings", qualifiedByName = "objectToJson")
    Tenant toEntity(AcmTenantRequestRepresentation request);

    @Mapping(target = "settings", source = "settings", qualifiedByName = "jsonToObject")
    AcmTenantResponseRepresentation toResponse(Tenant tenant);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "settings", source = "settings", qualifiedByName = "objectToJson")
    void updateEntityFromDto(AcmTenantRequestRepresentation dto, @MappingTarget Tenant tenant);

    @Named("objectToJson")
    default String objectToJson(Object settings) {
        try {
            return settings != null ? objectMapper.writeValueAsString(settings) : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("jsonToObject")
    default Object jsonToObject(String settings) {
        try {
            return settings != null ? objectMapper.readValue(settings, Object.class) : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}