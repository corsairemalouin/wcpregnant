package org.manu.learning.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyMain {
    private static final Logger log = LoggerFactory.getLogger(MyMain.class);

    public static void main(String[] args) throws InterruptedException {

        WebDriver driver = new FirefoxDriver();
        try {
            driver.get("https://quicklinks.cnt.fr/");
            driver.findElement(By.linkText("Domaine CA")).click();
            driver.findElement(By.linkText("COEUR1")).click();
            driver.findElement(By.linkText("pwdLDAP__")).sendKeys("Zn5WKqf*rx2025");

        } catch (NoSuchElementException e) {
            log.error(" not found");
        } finally {

            driver.quit();

        }
    }

}