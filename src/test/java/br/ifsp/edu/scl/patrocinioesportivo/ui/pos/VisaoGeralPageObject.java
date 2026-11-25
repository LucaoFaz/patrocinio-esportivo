package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;

import org.openqa.selenium.WebDriver;

public class VisaoGeralPageObject extends BasePageObject {
    private static final String TITULO = "Parcerias em movimento.";

    public VisaoGeralPageObject(WebDriver driver)
    {
        super(driver);
        aguardaTitulo(TITULO);
    }
}
