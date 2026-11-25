package br.ifsp.edu.scl.patrocinioesportivo.ui;

import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.ContratoPageObject;
import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.LoginPageObject;
import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.VisaoGeralPageObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
public class PropostaTest extends BaseSeleniumTest {
    @Override
    protected void setInitialPage()
    {
        driver.get(URL_FRONT + "#/entrar");
        new LoginPageObject(driver).entrar(USUARIO, SENHA);
    }

    @Test
    @DisplayName("Proposta validada ainda fica em pendente")
    void criarPropostaValida()
    {
        var visaoGeral = new VisaoGeralPageObject(driver);
        var novaProposta = visaoGeral.navegarNovaProposta();
        
        novaProposta.criar("10", "20", "6000", "100", "01012027", "01072027");

        var contrato = new ContratoPageObject(driver);
        assertThat(contrato.status()).isEqualTo("Pendente");
    }
}
