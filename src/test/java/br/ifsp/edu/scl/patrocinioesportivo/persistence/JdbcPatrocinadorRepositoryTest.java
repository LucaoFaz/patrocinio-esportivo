package br.ifsp.edu.scl.patrocinioesportivo.persistence;

import br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc.JdbcPatrocinadorRepository;
import br.ifsp.edu.scl.patrocinioesportivo.support.SqliteTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Testes de persistência JdbcPatrocinadorRepository")
class JdbcPatrocinadorRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcPatrocinadorRepository patrocinadorRepository;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(SqliteTestDatabase.create());
        new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"),
                new ClassPathResource("data-demo.sql")
        ).execute(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.patrocinadorRepository = new JdbcPatrocinadorRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o patrocinador existir no banco pelo ID")
    void shouldReturnTrueWhenPatrocinadorExistsById() {
        jdbcTemplate.update("INSERT INTO patrocinador (id, nome) VALUES (?, ?)", 50L, "Patrocinador Esporte Max");

        assertThat(patrocinadorRepository.existePorId(20L)).isTrue();
        assertThat(patrocinadorRepository.existePorId(50L)).isTrue();
    }
}

