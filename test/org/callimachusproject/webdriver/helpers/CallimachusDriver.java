package org.callimachusproject.webdriver.helpers;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallimachusDriver {
	private final Logger logger = LoggerFactory.getLogger(CallimachusDriver.class);
	private WebBrowserDriver driver;

	public CallimachusDriver(RemoteWebDriver driver, String startUrl) {
		this.driver = new WebBrowserDriver(driver);
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		driver.manage().timeouts().setScriptTimeout(30, TimeUnit.SECONDS);
		driver.navigate().to(startUrl);
	}

	public RemoteWebDriver getRemoteWebDriver() {
		return driver.getRemoteWebDriver();
	}

	public void quit() {
		driver.quit();
	}

	public void register(String username, String password, String fullname, String email) {
		logger.info("Register {}", username);
	    driver.type(By.id("fullname"), fullname);
	    driver.type(By.id("email"), email);
	    driver.type(By.id("username"), username);
	    driver.type(By.id("password"), password);
	    driver.click(By.id("signup"));
	    driver.waitForCursor();
	}

	public void login(String username, char[] password) {
		logger.info("Login {}", username);
	    driver.click(By.id("login-link"));
	    driver.type(By.id("username"), username);
	    driver.type(By.id("password"), new String(password));
	    driver.click(By.cssSelector("button.btn.btn-primary"));
	    driver.waitForCursor();
	}

	public void logout() {
		logger.info("Logout");
	    driver.click(By.cssSelector("i.icon-cog"));
	    driver.click(By.id("logout-link"));
	    driver.waitForCursor();
	}

	public void createArticle(String articleName, String articleTitle,
			String articleText) {
		logger.info("Create article {}", articleName);
	    driver.navigateTo("./?view");
	    driver.click(By.id("create-menu"));
	    driver.click(By.linkText("Article"));
	    driver.waitForCursor();
	    driver.focusInFrame(0, 0);
		driver.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE, Keys.BACK_SPACE, Keys.BACK_SPACE);
	    driver.sendKeys(articleTitle);
		driver.focusInFrame(0);
		driver.click(By.cssSelector(".cke_combo__format a.cke_combo_button"));
		driver.focusInFrame(0, 1);
		driver.click(By.partialLinkText("Heading 1"));
	    driver.focusInFrame(0, 0);
	    driver.sendKeys(Keys.ENTER, articleText);
	    driver.focusInTopWindow();
	    driver.click(By.id("create-article"));
	    driver.waitForCursor();
	    driver.focusInFrame("save-as___");
	    driver.waitForCursor();
	    driver.type(By.id("label"), articleName);
	    driver.focusInTopWindow();
	    driver.click(By.xpath("(//button[@type='button'])[2]"));
	    driver.waitUntilTextPresent(articleTitle);
	}

	public void deleteArticle(String articleName, String articleTitle) {
		logger.info("Delete article {}", articleName);
	    driver.navigateTo(articleName + "?view");
	    driver.waitUntilTextPresent(articleTitle);
	    driver.click(By.linkText("Edit"));
	    driver.click(By.cssSelector("button.btn.btn-danger"));
	    driver.confirm("Are you sure you want to delete");
	}

	public void createBook(String bookName, String bookTitle, String bookXml) {
		logger.info("Create book {}", bookName);
	    driver.navigateTo("./?view");
	    driver.click(By.id("create-menu"));
	    driver.mouseOver(By.linkText("More options"));
	    driver.click(By.linkText("Book"));
	    driver.waitForCursor();
		String bookElement = "<book xmlns=\"http://docbook.org/ns/docbook\" xmlns:xl=\"http://www.w3.org/1999/xlink\" xmlns:xi=\"http://www.w3.org/2001/XInclude\" version=\"5.0\">\n";
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ bookElement + "<title>" + bookTitle + "</title>\n" + bookXml
				+ "\n</book>";
		driver.focusInFrame(0);
		driver.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
		driver.sendKeys(markup);
		driver.sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_DOWN,
				Keys.ARROW_DOWN, Keys.ARROW_DOWN, Keys.ARROW_DOWN,
				Keys.ARROW_DOWN, Keys.ARROW_DOWN), Keys.DELETE);
	    driver.focusInTopWindow();
		driver.click(By.id("create-book"));
	    driver.waitForCursor();
	    driver.focusInFrame("save-as___");
	    driver.waitForCursor();
	    driver.type(By.id("label"), bookName);
	    driver.focusInTopWindow();
	    driver.click(By.xpath("(//button[@type='button'])[2]"));
	    driver.waitUntilTextPresent(bookTitle);
	}

	public void deleteBook(String bookName, String bookTitle) {
		logger.info("Delete book {}", bookName);
	    driver.navigateTo(bookName + "?view");
	    driver.waitUntilTextPresent(bookTitle);
	    driver.click(By.linkText("Edit"));
	    driver.click(By.cssSelector("button.btn.btn-danger"));
	    driver.confirm("Are you sure you want to delete");
	}

	public void createConcept(String conceptName, String conceptLabel,
			String conceptDefinition, String conceptExample) {
		logger.info("Create concept {}", conceptName);
	    driver.navigateTo("./?view");
	    driver.click(By.id("create-menu"));
	    driver.click(By.linkText("Concept"));
	    driver.type(By.id("label"), conceptLabel);
	    driver.type(By.id("definition"), conceptDefinition);
	    driver.type(By.id("example"), conceptExample);
	    driver.click(By.id("create"));
	    driver.waitUntilTextPresent(conceptLabel);
	}

	public void deleteConcept(String conceptName, String conceptLabel) {
		logger.info("Delete concept {}", conceptName);
	    driver.navigateTo(conceptName + "?view");
	    driver.waitUntilTextPresent(conceptLabel);
	    driver.click(By.linkText("Edit"));
	    driver.waitUntilTextPresent(conceptLabel);
	    driver.click(By.id("delete"));
	    driver.confirm("Are you sure you want to delete " + conceptLabel);
	}

	public void createFolder(String folderName) {
		logger.info("Create folder {}", folderName);
	    driver.navigateTo("./?view");
	    driver.click(By.id("create-menu"));
	    driver.click(By.linkText("Folder"));
	    driver.type(By.id("label"), folderName);
	    driver.click(By.cssSelector("button.btn.btn-success"));
	    driver.waitUntilTextPresent(folderName);
	}

	public void deleteFolder(String folderName) {
		logger.info("Delete folder {}", folderName);
	    driver.navigateTo("./?view");
	    driver.waitUntilTextPresent(folderName);
	    driver.click(By.linkText("Edit"));
	    driver.waitUntilTextPresent(folderName);
	    driver.click(By.id("delete"));
	    driver.confirm("Are you sure you want to delete this folder and all the contents of this folder");
	}

	public void createPurlAlt(String purlName, String purlComment,
			String purlTarget) {
		logger.info("Create purl {}", purlName);
	    driver.navigateTo("./?view");
	    driver.click(By.id("create-menu"));
	    driver.click(By.linkText("PURL"));
	    driver.type(By.id("local"), purlName);
	    driver.type(By.id("comment"), purlComment);
	    driver.type(By.id("pattern"), purlTarget);
	    driver.click(By.id("create"));
	    driver.waitUntilTextPresent(purlName);
	}

	public void deletePurl(String purlName) {
		logger.info("Delete purl {}", purlName);
	    driver.navigateTo(purlName + "?view");
	    driver.waitUntilTextPresent(purlName);
	    driver.click(By.linkText("Edit"));
	    driver.waitUntilTextPresent(purlName);
	    driver.click(By.cssSelector("button.btn.btn-danger"));
	    driver.confirm("Are you sure you want to delete " + purlName);
	}

}
