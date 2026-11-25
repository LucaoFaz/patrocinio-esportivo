package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ContratoPageObject extends BasePageObject 
{

    public ContratoPageObject(WebDriver driver)
    {
        super(driver);
        aguardaElemento(By.className("status"));
    }

    public String status()
    {
        return driver.findElement(By.className("status")).getText();
    }
}
