package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.junit.Assert;
import org.openqa.selenium.By;

public class FolderView extends CalliPage {

	public FolderView(WebBrowserDriver driver) {
		super(driver);
	}

	public DocEditor openArticleCreate() {
		driver.click(By.id("create-menu"));
		driver.click(By.linkText("Article"));
		return page(DocEditor.class);
	}

	public ConceptCreate openConceptCreate() {
		driver.click(By.id("create-menu"));
		driver.click(By.linkText("Concept"));
		return page(ConceptCreate.class);
	}

	public FolderCreate openFolderCreate() {
		driver.click(By.id("create-menu"));
		driver.click(By.linkText("Folder"));
		return page(FolderCreate.class);
	}

	public PurlCreate openPurlCreate() {
		driver.click(By.id("create-menu"));
		driver.click(By.linkText("PURL"));
		return page(PurlCreate.class);
	}

	public ClassEdit openClassCreate() {
		driver.click(By.id("create-menu"));
		driver.clickHiddenLink(".dropdown-menu a[href$=\"Class\"]");
		return page(ClassEdit.class);
	}

	public DomainCreate openDomainCreate() {
		driver.click(By.id("create-menu"));
		driver.clickHiddenLink(".dropdown-menu a[href$=\"Domain\"]");
		return page(DomainCreate.class);
	}

	public GroupCreate openGroupCreate() {
		driver.click(By.id("create-menu"));
		driver.clickHiddenLink(".dropdown-menu a[href$=\"Group\"]");
		return page(GroupCreate.class);
	}

	public TextEditor openTextCreate(String hrefEndsWith) {
		driver.click(By.id("create-menu"));
		driver.clickHiddenLink(".dropdown-menu a[href$=\""+ hrefEndsWith + "\"]");
		return page(TextEditor.class);
	}

	public FolderView waitUntilFolderOpen(String folderName) {
		driver.focusInTopWindow();
		driver.waitUntilTextPresent(By.cssSelector(".ui-widget-header"), folderName);
		return this;
	}

	public FolderEdit openEdit() {
		return openEdit(FolderEdit.class);
	}

	public void assertLayout() {
		int bottom = driver.getPositionBottom(By.linkText("Callimachus"));
		int cogTop = driver.getPositionTop(By.cssSelector("i.icon-cog"));
		Assert.assertTrue(bottom > cogTop);
		Assert.assertEquals(driver.getText(By.id("totalEntries")), driver.getText(By.id("totalResults")));
		for (String text : driver.getTextOfElements(By.cssSelector("table tbody tr td:last-child"))) {
			if (!"super".equals(text)) {
				Assert.assertEquals("admin", text);
			}
		}
	}

}
