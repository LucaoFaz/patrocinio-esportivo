package br.ifsp.edu.scl.patrocinioesportivo.ui;

import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.ContratoPageObject;
import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.LoginPageObject;
import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.VisaoGeralPageObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
public class VisaoGeralTest extends BaseSeleniumTest {


    @Override
    protected void setInitialPage()
    {
        driver.get(URL_FRONT + "#/entrar");
        new LoginPageObject(driver).entrar(USUARIO, SENHA);
    }

    @Test
    @DisplayName("Consulta de contrato existente")
    void deveAbrirContrato()
    {
        var visaoGeral = new VisaoGeralPageObject(driver);
        var contrato = new ContratoPageObject(driver);

        visaoGeral.consultar("1");

        assertThat(contrato.titulo()).isEqualTo("Contrato #1");
        assertThat(contrato.status()).isEqualTo("Ativo");
    }
}
