package br.ifsp.edu.scl.patrocinioesportivo.persistence;

import br.ifsp.edu.scl.patrocinioesportivo.security.user.JdbcUserRepository;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.User;
import br.ifsp.edu.scl.patrocinioesportivo.support.SqliteTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Testes de persistência JdbcUserRepository")
class JdbcUserRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcUserRepository userRepository;

    @BeforeEach
    void setUp(){
        var dataSource = new DriverManagerDataSource(SqliteTestDatabase.create());
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.userRepository = new JdbcUserRepository(jdbcTemplate);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("Deve salvar usuário e recupera-lo por email preservando todos seus atributos e perfil")
    void
    shouldSaveAndFindUserByEmailForEachRole(Role role) {
        UUID id = UUID.randomUUID();
        User user = new User(id, "Seu",
                "Ze", "seuze@ifsp.edu.br",
                "senhaBoa123", role);
        userRepository.save(user);

        var userfound = userRepository.findByEmail("seuze@ifsp.edu.br");
        assertThat(userfound).isPresent();
        assertThat(userfound.get().getId()).isEqualTo(id);
        assertThat(userfound.get().getName()).isEqualTo("Seu");
        assertThat(userfound.get().getLastname()).isEqualTo("Ze");
        assertThat(userfound.get().getEmail()).isEqualTo("seuze@ifsp.edu.br");
        assertThat(userfound.get().getPassword()).isEqualTo("senhaBoa123");
        assertThat(userfound.get().getRole()).isEqualTo(role);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"naoexiste@ifsp.edu.br", "   ", "LUCAS@IFSP.EDU.BR"})
    @DisplayName("Deve retornar vazio ao buscar por email nao registrado, vazio ou nulo")
    void shouldReturnEmptyWhenEmailDoesNotExistOrIsInvalid(String emailBusca) {
        userRepository.save(new User(UUID.randomUUID(), "Lucas", "Santos", "lucas@ifsp.edu.br", "hash", Role.USER));
        var result = userRepository.findByEmail(emailBusca);
        assertThat(result).isEmpty();
    }

}