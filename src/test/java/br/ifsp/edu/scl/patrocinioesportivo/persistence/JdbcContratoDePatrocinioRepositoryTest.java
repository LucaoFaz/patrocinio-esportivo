package br.ifsp.edu.scl.patrocinioesportivo.persistence;

import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc.JdbcContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.support.ContratoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Testes de persistência JdbcContratoDePatrocinioRepository")
class JdbcContratoDePatrocinioRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcContratoDePatrocinioRepository repository;

    @BeforeEach
    void setUp() {
        var dataSource = ContratoTestFixtures.createInitializedSqliteDataSource();
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.repository = new JdbcContratoDePatrocinioRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("Deve salvar novo contrato e recupera-lo por ID preservando decimais exatos e atributos")
    void shouldSaveNewContratoAndFindByIdPreservingExactDecimalsAndAttributes() {
        var valorExato = new BigDecimal("98765432109876.123456789");
        var metaExata = new BigDecimal("1500.755");
        var contrato = ContratoTestFixtures.contratoPendente(valorExato, metaExata);

        var salvo = repository.salvar(contrato);

        assertThat(salvo.getId()).isNotNull().isPositive();
        var encontrado = repository.buscarPorId(salvo.getId());
        assertThat(encontrado).isPresent();
        var persistido = encontrado.get();
        assertThat(persistido.getId()).isEqualTo(salvo.getId());
        assertThat(persistido.getStatus()).isEqualTo(StatusContrato.PENDENTE);
        assertThat(persistido.getClubeId()).isEqualTo(ContratoTestFixtures.CLUBE_DEMO_ID);
        assertThat(persistido.getPatrocinadorId()).isEqualTo(ContratoTestFixtures.PATROCINADOR_DEMO_ID);
        assertThat(persistido.getPeriodoContratual().inicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(persistido.getPeriodoContratual().termino()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(persistido.getValorTotal()).isEqualTo(valorExato);
        assertThat(persistido.getMetaContratual().valor()).isEqualTo(metaExata);
        assertThat(persistido.getExposicaoAcumulada()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(persistido.getMultaRescisoria()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(persistido.getParcelas()).isEmpty();
        assertThat(persistido.getHistorico()).isEmpty();
    }
}
