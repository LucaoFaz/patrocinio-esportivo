package br.ifsp.edu.scl.patrocinioesportivo.api;

import br.ifsp.edu.scl.patrocinioesportivo.model.HistoricoDePeriodo;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.support.BaseApiIntegrationTest;
import br.ifsp.edu.scl.patrocinioesportivo.support.ContratoTestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("Testes de API - Contratos de Patrocínio")
class ContratoApiTest extends BaseApiIntegrationTest {

    @Test
    @DisplayName("GET /api/v1/contratos/{id} deve retornar 200 com payload completo, atraso nas parcelas e historico")
    void shouldConsultContratoReturningCompletePayloadWithOverdueAndHistory() {
        String userToken = authenticateAs(Role.USER);
        LocalDate hoje = LocalDate.now();
        LocalDate inicioAtual = hoje.minusMonths(2);
        LocalDate terminoAtual = hoje.plusMonths(2);

        var parcelaAtrasada = ParcelaDePagamento.reconstituir(
                2, new BigDecimal("500.00"), hoje.minusDays(1), false, null
        );
        var parcelaVenceHoje = ParcelaDePagamento.reconstituir(
                3, new BigDecimal("500.00"), hoje, false, null
        );
        var parcelaPagaAtrasada = ParcelaDePagamento.reconstituir(
                4, new BigDecimal("500.00"), hoje.minusDays(5), true, hoje.minusDays(2)
        );
        var parcelaFutura = ParcelaDePagamento.reconstituir(
                5, new BigDecimal("500.00"), hoje.plusDays(15), false, null
        );

        var periodoAnterior = new PeriodoContratual(hoje.minusMonths(6), hoje.minusMonths(2).minusDays(1));
        var parcelaHistorico = ParcelaDePagamento.reconstituir(
                1, new BigDecimal("1000.00"), periodoAnterior.termino(), true, periodoAnterior.termino()
        );
        var historico = new HistoricoDePeriodo(
                periodoAnterior,
                new MetaContratual(new BigDecimal("120.00")),
                new BigDecimal("150.00"),
                List.of(parcelaHistorico)
        );

        long contratoId = salvarContratoReconstituido(
                StatusContrato.ATIVO,
                inicioAtual,
                terminoAtual,
                new BigDecimal("300.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("75.50"),
                List.of(parcelaAtrasada, parcelaVenceHoje, parcelaPagaAtrasada, parcelaFutura),
                List.of(historico)
        );

        Response response = givenAuth(userToken)
                .get("/api/v1/contratos/{id}", contratoId);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        var json = response.jsonPath();

        assertSoftly(softly -> {
            softly.assertThat(json.getLong("id")).isEqualTo(contratoId);
            softly.assertThat(json.getLong("clubeId")).isEqualTo(ContratoTestFixtures.CLUBE_DEMO_ID);
            softly.assertThat(json.getLong("patrocinadorId")).isEqualTo(ContratoTestFixtures.PATROCINADOR_DEMO_ID);
            softly.assertThat(json.getString("status")).isEqualTo("ATIVO");
            softly.assertThat(json.getString("inicio")).isEqualTo(inicioAtual.toString());
            softly.assertThat(json.getString("termino")).isEqualTo(terminoAtual.toString());
            softly.assertThat(json.getObject("meta", BigDecimal.class)).isEqualByComparingTo("300.00");
            softly.assertThat(json.getObject("valorTotal", BigDecimal.class)).isEqualByComparingTo("2000.00");
            softly.assertThat(json.getObject("exposicaoAcumulada", BigDecimal.class)).isEqualByComparingTo("75.50");
            softly.assertThat(json.getObject("multaRescisoria", BigDecimal.class)).isEqualByComparingTo("0");

            softly.assertThat(json.getList("parcelas.numero", Integer.class)).containsExactly(2, 3, 4, 5);
            softly.assertThat(json.getList("parcelas.paga", Boolean.class)).containsExactly(false, false, true, false);
            softly.assertThat(json.getList("parcelas.emAtraso", Boolean.class)).containsExactly(true, false, false, false);

            softly.assertThat(json.getList("historico")).hasSize(1);
            softly.assertThat(json.getString("historico[0].inicio")).isEqualTo(periodoAnterior.inicio().toString());
            softly.assertThat(json.getString("historico[0].termino")).isEqualTo(periodoAnterior.termino().toString());
            softly.assertThat(json.getObject("historico[0].meta", BigDecimal.class)).isEqualByComparingTo("120.00");
            softly.assertThat(json.getObject("historico[0].exposicaoAcumulada", BigDecimal.class))
                    .isEqualByComparingTo("150.00");
            softly.assertThat(json.getList("historico[0].parcelas.numero", Integer.class)).containsExactly(1);
        });
    }
}
