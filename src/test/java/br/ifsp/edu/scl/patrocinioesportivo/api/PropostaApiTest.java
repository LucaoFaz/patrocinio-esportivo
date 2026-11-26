package br.ifsp.edu.scl.patrocinioesportivo.api;

import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.support.BaseApiIntegrationTest;
import br.ifsp.edu.scl.patrocinioesportivo.support.ContratoTestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("Testes de API - Propostas de Patrocínio (submissão e edição)")
class PropostaApiTest extends BaseApiIntegrationTest {

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("POST /api/v1/propostas deve retornar 201 com header Location e persistir proposta PENDENTE")
    void shouldSubmitProposalReturning201WithLocationHeaderAndPersistingPendingContract(Role role) {
        String token = authenticateAs(role);
        LocalDate inicio = LocalDate.now();
        LocalDate termino = inicio.plusMonths(4);

        Response response = givenAuth(token)
                .body(Map.of(
                        "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                        "patrocinadorId", ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                        "valor", new BigDecimal("2500.75"),
                        "inicio", inicio.toString(),
                        "termino", termino.toString(),
                        "meta", new BigDecimal("180.50")
                ))
                .post("/api/v1/propostas");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        var json = response.jsonPath();
        long propostaId = json.getLong("id");
        var persistida = contratoRepository.buscarPorId(propostaId).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(response.getHeader("Location")).isEqualTo("/api/v1/contratos/" + propostaId);
            softly.assertThat(propostaId).isPositive();
            softly.assertThat(json.getLong("clubeId")).isEqualTo(ContratoTestFixtures.CLUBE_DEMO_ID);
            softly.assertThat(json.getLong("patrocinadorId")).isEqualTo(ContratoTestFixtures.PATROCINADOR_DEMO_ID);
            softly.assertThat(json.getString("status")).isEqualTo("PENDENTE");
            softly.assertThat(json.getString("inicio")).isEqualTo(inicio.toString());
            softly.assertThat(json.getString("termino")).isEqualTo(termino.toString());
            softly.assertThat(json.getObject("valorTotal", BigDecimal.class)).isEqualByComparingTo("2500.75");
            softly.assertThat(json.getObject("meta", BigDecimal.class)).isEqualByComparingTo("180.50");
            softly.assertThat(json.getObject("exposicaoAcumulada", BigDecimal.class)).isEqualByComparingTo("0");
            softly.assertThat(json.getObject("multaRescisoria", BigDecimal.class)).isEqualByComparingTo("0");
            softly.assertThat(json.getList("parcelas")).isEmpty();
            softly.assertThat(json.getList("historico")).isEmpty();

            softly.assertThat(persistida.getStatus()).isEqualTo(StatusContrato.PENDENTE);
            softly.assertThat(persistida.getValorTotal()).isEqualByComparingTo("2500.75");
            softly.assertThat(persistida.getMetaContratual().valor()).isEqualByComparingTo("180.50");
            softly.assertThat(persistida.getPeriodoContratual().inicio()).isEqualTo(inicio);
            softly.assertThat(persistida.getPeriodoContratual().termino()).isEqualTo(termino);
        });
    }
}
