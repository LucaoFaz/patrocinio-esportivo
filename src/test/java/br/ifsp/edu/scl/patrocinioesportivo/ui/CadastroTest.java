package br.ifsp.edu.scl.patrocinioesportivo.ui;

import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.CadastroPageObject;
import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.VisaoGeralPageObject;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
public class CadastroTest extends BaseSeleniumTest 
{
    private final Faker faker = new Faker();

    @Override
    protected void setInitialPage()
    {
        driver.get(URL_FRONT + "#/cadastro");
    }

    @Test
    @DisplayName("Cadastro com sucesso com dados validadoss")
    void deveCadastrarComDadosValidos()
    {
        var emailAleatorio = faker.internet().emailAddress();

        var cadastro = new CadastroPageObject(driver);
        cadastro.cadastrar("Ana", "Souza", emailAleatorio, "Senha123!", "Senha123!");

        var visaoGeral = new VisaoGeralPageObject(driver);
        assertThat(visaoGeral.titulo()).isEqualTo("Parcerias em movimento.");
    }
}
