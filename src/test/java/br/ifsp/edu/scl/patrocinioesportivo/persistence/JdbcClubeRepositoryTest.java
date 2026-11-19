package br.ifsp.edu.scl.patrocinioesportivo.persistence;

import br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc.JdbcClubeRepository;
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
@DisplayName("Testes de persistência JdbcClubeRepository")
class JdbcClubeRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcClubeRepository clubeRepository;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(SqliteTestDatabase.create());
        new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"),
                new ClassPathResource("data-demo.sql")
        ).execute(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.clubeRepository = new JdbcClubeRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quand o clube existir no banco por ID")
    void shouldReturnTrueWhenClubeExistsById() {
        jdbcTemplate.update("INSERT INTO clube (id, nome) VALUES (?, ?)", 30L, "Clube Atletico Sao Carlos");

        assertThat(clubeRepository.existePorId(30L)).isTrue();
    }
}
