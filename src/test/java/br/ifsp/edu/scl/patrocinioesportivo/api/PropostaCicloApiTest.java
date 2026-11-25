package br.ifsp.edu.scl.patrocinioesportivo.api;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("Testes de API - Ciclo da Proposta (aprovação, recusa e cancelamento)")
class PropostaCicloApiTest extends BaseApiIntegrationTest {

    @Test
    @DisplayName("POST /api/v1/propostas/{id}/aprovacao deve retornar 200 para ADMIN com status ATIVO e parcelas geradas")
    void shouldApproveProposalAsAdminAndGenerateInstallments() {
        String userToken = authenticateAs(Role.USER);
        String adminToken = authenticateAs(Role.ADMIN);
        long propostaId = criarPropostaPendente(userToken, new BigDecimal("1000.01"), 2, new BigDecimal("150.00"));

        Response response = givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/aprovacao", propostaId);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        var json = response.jsonPath();
        List<BigDecimal> valoresParcelas = json.getList("parcelas.valor", BigDecimal.class);
        assertThat(valoresParcelas).hasSize(2);
        BigDecimal somaParcelas = valoresParcelas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        var persistido = contratoRepository.buscarPorId(propostaId).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(json.getLong("id")).isEqualTo(propostaId);
            softly.assertThat(json.getLong("clubeId")).isEqualTo(ContratoTestFixtures.CLUBE_DEMO_ID);
            softly.assertThat(json.getLong("patrocinadorId")).isEqualTo(ContratoTestFixtures.PATROCINADOR_DEMO_ID);
            softly.assertThat(json.getString("status")).isEqualTo("ATIVO");
            softly.assertThat(json.getObject("valorTotal", BigDecimal.class)).isEqualByComparingTo("1000.01");
            softly.assertThat(json.getObject("meta", BigDecimal.class)).isEqualByComparingTo("150.00");
            softly.assertThat(json.getObject("exposicaoAcumulada", BigDecimal.class)).isEqualByComparingTo("0");
            softly.assertThat(json.getObject("multaRescisoria", BigDecimal.class)).isEqualByComparingTo("0");
            softly.assertThat(json.getList("parcelas.numero", Integer.class)).containsExactly(1, 2);
            softly.assertThat(somaParcelas).isEqualByComparingTo("1000.01");
            softly.assertThat(json.getList("parcelas.paga", Boolean.class)).containsExactly(false, false);
            softly.assertThat(json.getList("parcelas.emAtraso", Boolean.class)).containsExactly(false, false);
            softly.assertThat(json.getList("historico")).isEmpty();

            softly.assertThat(persistido.getStatus()).isEqualTo(StatusContrato.ATIVO);
            softly.assertThat(persistido.getParcelas()).hasSize(2);
        });
    }

    @Test
    @DisplayName("POST /api/v1/propostas/{id}/aprovacao deve retornar 403 para USER e 401 sem token valido mantendo proposta pendente")
    void shouldRejectApprovalForUserAndUnauthenticatedRequests() {
        String userToken = authenticateAs(Role.USER);
        long propostaId = criarPropostaPendente(userToken);

        Response semToken = givenAnonymous()
                .post("/api/v1/propostas/{id}/aprovacao", propostaId);

        Response tokenInvalido = givenAuth("token-jwt-invalido")
                .post("/api/v1/propostas/{id}/aprovacao", propostaId);

        Response usuarioComum = givenAuth(userToken)
                .post("/api/v1/propostas/{id}/aprovacao", propostaId);

        assertApiError(
                usuarioComum,
                HttpStatus.FORBIDDEN,
                "PermissaoNegadaError",
                "diretor financeiro"
        );

        var persistido = contratoRepository.buscarPorId(propostaId).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(semToken.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            softly.assertThat(tokenInvalido.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            softly.assertThat(persistido.getStatus()).isEqualTo(StatusContrato.PENDENTE);
            softly.assertThat(persistido.getParcelas()).isEmpty();
        });
    }
}
