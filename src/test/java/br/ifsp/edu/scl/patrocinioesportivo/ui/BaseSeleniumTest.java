package br.ifsp.edu.scl.patrocinioesportivo.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class BaseSeleniumTest 
{
    protected static final String URL_FRONT = "http://127.0.0.1:5173/";
    protected WebDriver driver;

    @BeforeEach
    public void setUp() 
    {
        driver = new ChromeDriver();
        setInitialPage();
    }
    @AfterEach
    public void tearDown() 
    {
        driver.quit();
    }

    protected void setInitialPage(){}
}
