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

        var persistido = contratoRepository.buscarPorId(propostaId).orElseThrow();
        assertSoftly(softly -> {
            assertApiError(
                    softly,
                    usuarioComum,
                    HttpStatus.FORBIDDEN,
                    "PermissaoNegadaError",
                    "diretor financeiro"
            );
            softly.assertThat(semToken.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            softly.assertThat(tokenInvalido.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            softly.assertThat(persistido.getStatus()).isEqualTo(StatusContrato.PENDENTE);
            softly.assertThat(persistido.getParcelas()).isEmpty();
        });
    }

    @Test
    @DisplayName("POST /api/v1/propostas/{id}/aprovacao deve retornar 404 inexistente, 400 ID invalido e 409 ja avaliada")
    void shouldRejectApprovalForNonExistentMalformedOrAlreadyEvaluatedProposal() {
        String adminToken = authenticateAs(Role.ADMIN);

        Response inexistente = givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/aprovacao", 999999L);
        Response idInvalido = givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/aprovacao", "invalido");

        long aprovadaId = criarPropostaPendente(adminToken);
        givenAuth(adminToken).post("/api/v1/propostas/{id}/aprovacao", aprovadaId)
                .then().statusCode(HttpStatus.OK.value());
        Response reAprovacao = givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/aprovacao", aprovadaId);

        long recusadaId = criarPropostaPendente(adminToken);
        givenAuth(adminToken).post("/api/v1/propostas/{id}/recusa", recusadaId)
                .then().statusCode(HttpStatus.OK.value());
        Response aprovarRecusada = givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/aprovacao", recusadaId);

        assertSoftly(softly -> {
            assertApiError(softly, inexistente, HttpStatus.NOT_FOUND, "ContratoInexistenteError", "Proposta não encontrada");
            assertApiError(softly, idInvalido, HttpStatus.BAD_REQUEST, "MethodArgumentTypeMismatchException", "Requisição inválida");
            assertApiError(softly, reAprovacao, HttpStatus.CONFLICT, "TransicaoDeStatusInvalidaError", "propostas pendentes");
            assertApiError(softly, aprovarRecusada, HttpStatus.CONFLICT, "TransicaoDeStatusInvalidaError", "propostas pendentes");
            softly.assertThat(contratoRepository.buscarPorId(aprovadaId).orElseThrow().getStatus())
                    .isEqualTo(StatusContrato.ATIVO);
            softly.assertThat(contratoRepository.buscarPorId(recusadaId).orElseThrow().getStatus())
                    .isEqualTo(StatusContrato.RECUSADO);
        });
    }

    @Test
    @DisplayName("POST /api/v1/propostas/{id}/recusa deve retornar 200 para ADMIN com status RECUSADO e sem parcelas")
    void shouldRefuseProposalAsAdminAndPersistRecusadoStatus() {
        String userToken = authenticateAs(Role.USER);
        String adminToken = authenticateAs(Role.ADMIN);
        long propostaId = criarPropostaPendente(userToken, new BigDecimal("2500.00"), 3, new BigDecimal("200.00"));

        Response response = givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/recusa", propostaId);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        var json = response.jsonPath();
        var persistido = contratoRepository.buscarPorId(propostaId).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(json.getLong("id")).isEqualTo(propostaId);
            softly.assertThat(json.getLong("clubeId")).isEqualTo(ContratoTestFixtures.CLUBE_DEMO_ID);
            softly.assertThat(json.getLong("patrocinadorId")).isEqualTo(ContratoTestFixtures.PATROCINADOR_DEMO_ID);
            softly.assertThat(json.getString("status")).isEqualTo("RECUSADO");
            softly.assertThat(json.getObject("valorTotal", BigDecimal.class)).isEqualByComparingTo("2500.00");
            softly.assertThat(json.getObject("meta", BigDecimal.class)).isEqualByComparingTo("200.00");
            softly.assertThat(json.getObject("exposicaoAcumulada", BigDecimal.class)).isEqualByComparingTo("0");
            softly.assertThat(json.getObject("multaRescisoria", BigDecimal.class)).isEqualByComparingTo("0");
            softly.assertThat(json.getList("parcelas")).isEmpty();
            softly.assertThat(json.getList("historico")).isEmpty();

            softly.assertThat(persistido.getStatus()).isEqualTo(StatusContrato.RECUSADO);
            softly.assertThat(persistido.getParcelas()).isEmpty();
        });
    }

    @Test
    @DisplayName("POST /api/v1/propostas/{id}/recusa deve retornar 401 sem token, 403 para USER, 404 inexistente e 409 ja avaliada")
    void shouldRejectRefusalForUnauthorizedForbiddenNotFoundAndConflictCases() {
        String userToken = authenticateAs(Role.USER);
        String adminToken = authenticateAs(Role.ADMIN);

        long pendenteId = criarPropostaPendente(userToken);
        Response semToken = givenAnonymous()
                .post("/api/v1/propostas/{id}/recusa", pendenteId);
        Response usuarioComum = givenAuth(userToken)
                .post("/api/v1/propostas/{id}/recusa", pendenteId);

        Response inexistente = givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/recusa", 999999L);

        long recusadaId = criarPropostaPendente(adminToken);
        givenAuth(adminToken).post("/api/v1/propostas/{id}/recusa", recusadaId)
                .then().statusCode(HttpStatus.OK.value());
        Response reRecusa = givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/recusa", recusadaId);

        long aprovadaId = criarPropostaPendente(adminToken);
        givenAuth(adminToken).post("/api/v1/propostas/{id}/aprovacao", aprovadaId)
                .then().statusCode(HttpStatus.OK.value());
        Response recusarAprovada = givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/recusa", aprovadaId);

        assertSoftly(softly -> {
            softly.assertThat(semToken.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertApiError(softly, usuarioComum, HttpStatus.FORBIDDEN, "PermissaoNegadaError", "diretor financeiro");
            assertApiError(softly, inexistente, HttpStatus.NOT_FOUND, "ContratoInexistenteError", "Proposta não encontrada");
            assertApiError(softly, reRecusa, HttpStatus.CONFLICT, "TransicaoDeStatusInvalidaError", "propostas pendentes");
            assertApiError(softly, recusarAprovada, HttpStatus.CONFLICT, "TransicaoDeStatusInvalidaError", "propostas pendentes");

            softly.assertThat(contratoRepository.buscarPorId(pendenteId).orElseThrow().getStatus())
                    .isEqualTo(StatusContrato.PENDENTE);
            softly.assertThat(contratoRepository.buscarPorId(recusadaId).orElseThrow().getStatus())
                    .isEqualTo(StatusContrato.RECUSADO);
            softly.assertThat(contratoRepository.buscarPorId(aprovadaId).orElseThrow().getStatus())
                    .isEqualTo(StatusContrato.ATIVO);
        });
    }
}
