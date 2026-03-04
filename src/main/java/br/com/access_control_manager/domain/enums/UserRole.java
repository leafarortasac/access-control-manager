package br.com.access_control_manager.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    SUPER_ADMIN("Acesso total ao sistema e gestão de tenants"),
    ADMIN("Administrador da empresa/tenant"),
    AGENT("Agente que atende e resolve chamados"),
    MANAGER("Gestor responsável por aprovações"),
    CUSTOMER("Cliente que abre e acompanha chamados");

    private final String description;
}