package br.ifsp.edu.scl.patrocinioesportivo.api;

import br.ifsp.edu.scl.patrocinioesportivo.security.auth.RegisterUserRequest;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.UserRepository;
import br.ifsp.edu.scl.patrocinioesportivo.support.SqliteTestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("Testes de integração da API de Autenticação e Usuario")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")


public class AuthApiTest {
    private static final String DATABASE_URL = SqliteTestDatabase.create();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", ()-> DATABASE_URL);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Deve cadastrar usuário válido retornando 201 com ID no payload e persistir no banco")
    void shouldRegisterUserAndReturn201WithGeneratedId() throws Exception{
        String email = UUID.randomUUID() + "@ifsp.edu.br";
        var request = new RegisterUserRequest("Lucas", "Santos", email, "SenhaForte123!");
        String responseBody = mvc.perform(post("/api/v1/register")
                .contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID returnedId = UUID.fromString(json.readTree(responseBody).get("id").asText());
        var persistedUser = userRepository.findByEmail(email);

        assertThat(persistedUser).isPresent();
        assertThat(persistedUser.get().getId()).isEqualTo(returnedId);
        assertThat(persistedUser.get().getName()).isEqualTo("Lucas");
        assertThat(persistedUser.get().getLastname()).isEqualTo("Santos");
        assertThat(persistedUser.get().getRole()).isEqualTo(Role.USER);
        assertThat(persistedUser.get().getPassword()).isNotEqualTo("SenhaForte123!");
        assertThat(passwordEncoder.matches("SenhaForte123!", persistedUser.get().getPassword())).isTrue();
    }
}
