package br.ifsp.edu.scl.patrocinioesportivo.persistence;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.HistoricoDePeriodo;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc.JdbcContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.support.ContratoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

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
        var persistido = repository.buscarPorId(salvo.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(persistido.getId()).isEqualTo(salvo.getId());
            softly.assertThat(persistido.getStatus()).isEqualTo(StatusContrato.PENDENTE);
            softly.assertThat(persistido.getClubeId()).isEqualTo(ContratoTestFixtures.CLUBE_DEMO_ID);
            softly.assertThat(persistido.getPatrocinadorId()).isEqualTo(ContratoTestFixtures.PATROCINADOR_DEMO_ID);
            softly.assertThat(persistido.getPeriodoContratual().inicio()).isEqualTo(LocalDate.of(2026, 1, 1));
            softly.assertThat(persistido.getPeriodoContratual().termino()).isEqualTo(LocalDate.of(2026, 4, 1));
            softly.assertThat(persistido.getValorTotal()).isEqualTo(valorExato);
            softly.assertThat(persistido.getMetaContratual().valor()).isEqualTo(metaExata);
            softly.assertThat(persistido.getExposicaoAcumulada()).isEqualByComparingTo(BigDecimal.ZERO);
            softly.assertThat(persistido.getMultaRescisoria()).isEqualByComparingTo(BigDecimal.ZERO);
            softly.assertThat(persistido.getParcelas()).isEmpty();
            softly.assertThat(persistido.getHistorico()).isEmpty();
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1L, 0L, 999999L})
    @DisplayName("Deve retornar vazio ao buscar contrato por ID inexistente ou nulo")
    void shouldReturnEmptyWhenSearchingNonExistentContratoId(Long id) {
        assertThat(repository.buscarPorId(id)).isEmpty();
    }

    @Test
    @DisplayName("Deve atualizar contrato existente substituindo atributos, parcelas e historico sem duplicacao")
    void shouldUpdateExistingContratoReplacingParcelasAndHistorico() {
        var inicial = repository.salvar(ContratoTestFixtures.contratoPendente(
                new BigDecimal("1000.00"), new BigDecimal("100.00")
        ));
        Long id = inicial.getId();

        var parcelaHistorico = ParcelaDePagamento.reconstituir(
                1, new BigDecimal("1000.00"), LocalDate.of(2026, 2, 1), true, LocalDate.of(2026, 1, 30)
        );
        var historico = new HistoricoDePeriodo(
                new PeriodoContratual(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1)),
                new MetaContratual(new BigDecimal("100.00")),
                new BigDecimal("120.00"),
                List.of(parcelaHistorico)
        );
        var novaParcela = ParcelaDePagamento.reconstituir(
                2, new BigDecimal("2500.50"), LocalDate.of(2026, 6, 1), false, null
        );
        var atualizado = ContratoDePatrocinio.reconstituir(
                id,
                StatusContrato.ENCERRADO,
                new PeriodoContratual(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 7, 2)),
                new MetaContratual(new BigDecimal("250.00")),
                new BigDecimal("2500.50"),
                ContratoTestFixtures.CLUBE_DEMO_ID,
                ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                new BigDecimal("80.25"),
                new BigDecimal("500.10"),
                List.of(novaParcela),
                List.of(historico)
        );

        repository.salvar(atualizado);
        repository.salvar(atualizado);

        var persistido = repository.buscarPorId(id).orElseThrow();
        assertThat(persistido.getParcelas()).hasSize(1);
        assertThat(persistido.getHistorico()).hasSize(1);
        var cicloSalvo = persistido.getHistorico().getFirst();
        assertThat(cicloSalvo.parcelas()).hasSize(1);

        assertSoftly(softly -> {
            softly.assertThat(persistido.getStatus()).isEqualTo(StatusContrato.ENCERRADO);
            softly.assertThat(persistido.getPeriodoContratual().inicio()).isEqualTo(LocalDate.of(2026, 4, 2));
            softly.assertThat(persistido.getPeriodoContratual().termino()).isEqualTo(LocalDate.of(2026, 7, 2));
            softly.assertThat(persistido.getMetaContratual().valor()).isEqualByComparingTo("250.00");
            softly.assertThat(persistido.getValorTotal()).isEqualByComparingTo("2500.50");
            softly.assertThat(persistido.getExposicaoAcumulada()).isEqualByComparingTo("80.25");
            softly.assertThat(persistido.getMultaRescisoria()).isEqualByComparingTo("500.10");

            softly.assertThat(persistido.getParcelas().getFirst().getNumero()).isEqualTo(2);
            softly.assertThat(persistido.getParcelas().getFirst().isPaga()).isFalse();
            softly.assertThat(persistido.getParcelas().getFirst().getDataPagamento()).isNull();

            softly.assertThat(cicloSalvo.exposicaoAcumulada()).isEqualByComparingTo("120.00");
            softly.assertThat(cicloSalvo.parcelas().getFirst().getNumero()).isEqualTo(1);
            softly.assertThat(cicloSalvo.parcelas().getFirst().isPaga()).isTrue();
            softly.assertThat(cicloSalvo.parcelas().getFirst().getDataPagamento()).isEqualTo(LocalDate.of(2026, 1, 30));
        });
    }
}
