package infrastructure.security;

import br.com.access_control_manager.infrastructure.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = br.com.access_control_manager.AccessControlManagerApplication.class)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário com primeiro acesso tenta listar usuários")
    void shouldReturnForbiddenWhenFirstAccessIsTrue() throws Exception {

        var tenantId = UUID.randomUUID().toString();
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("novo.usuario@empresa.com")
                .password("123")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .build();

        var token = jwtService.generateToken(userDetails, tenantId, true);

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Necessário trocar a senha no primeiro acesso."))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Deve permitir acesso à troca de senha mesmo quando firstAccess é true")
    void shouldAllowPasswordChangeWhenFirstAccessIsTrue() throws Exception {

        var tenantId = UUID.randomUUID().toString();
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("novo.usuario@empresa.com")
                .password("123")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .build();

        var token = jwtService.generateToken(userDetails, tenantId, true);

        mockMvc.perform(get("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(405));
    }
}