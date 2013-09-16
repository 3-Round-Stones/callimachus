package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.junit.Assert;
import org.openqa.selenium.By;

public class FolderView extends CalliPage {

	public FolderView(WebBrowserDriver driver) {
		super(driver);
	}

	public DocEditor openArticleCreate() {
		browser.click(By.id("create-menu"));
		browser.click(By.linkText("Article"));
		return page(DocEditor.class);
	}

	public ConceptCreate openConceptCreate() {
		browser.click(By.id("create-menu"));
		browser.click(By.linkText("Concept"));
		return page(ConceptCreate.class);
	}

	public FolderCreate openFolderCreate() {
		browser.click(By.id("create-menu"));
		browser.click(By.linkText("Folder"));
		return page(FolderCreate.class);
	}

	public PurlCreate openPurlCreate() {
		browser.click(By.id("create-menu"));
		browser.click(By.linkText("PURL"));
		return page(PurlCreate.class);
	}

	public ClassEdit openClassCreate() {
		browser.click(By.id("create-menu"));
		browser.clickHiddenLink(".dropdown-menu a[href$=\"Class\"]");
		return page(ClassEdit.class);
	}

	public DomainCreate openDomainCreate() {
		browser.click(By.id("create-menu"));
		browser.clickHiddenLink(".dropdown-menu a[href$=\"Domain\"]");
		return page(DomainCreate.class);
	}

	public GroupCreate openGroupCreate() {
		browser.click(By.id("create-menu"));
		browser.clickHiddenLink(".dropdown-menu a[href$=\"Group\"]");
		return page(GroupCreate.class);
	}

	public DatasourceCreate openDatasourceCreate() {
		browser.click(By.id("create-menu"));
		browser.clickHiddenLink(".dropdown-menu a[href$=\"Datasource\"]");
		return page(DatasourceCreate.class);
	}

	public TextEditor openTextCreate(String hrefEndsWith) {
		browser.click(By.id("create-menu"));
		browser.clickHiddenLink(".dropdown-menu a[href$=\""+ hrefEndsWith + "\"]");
		return page(TextEditor.class);
	}

	public FolderView waitUntilFolderOpen(String folderName) {
		browser.focusInTopWindow();
		browser.waitUntilTextPresent(By.cssSelector(".ui-widget-header"), folderName);
		return this;
	}

	public FolderEdit openEdit() {
		return openEdit(FolderEdit.class);
	}

	public ImportPage openImportPage() {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("i.icon-cog"));
		browser.click(By.linkText("Import folder contents"));
		return page(ImportPage.class);
	}

	public void assertLayout() {
		int bottom = browser.getPositionBottom(By.linkText("Callimachus"));
		int cogTop = browser.getPositionTop(By.cssSelector("i.icon-cog"));
		Assert.assertTrue(bottom > cogTop);
		Assert.assertEquals(browser.getText(By.id("totalEntries")), browser.getText(By.id("totalResults")));
		for (String text : browser.getTextOfElements(By.cssSelector("table tbody tr td:last-child"))) {
			if (!"super".equals(text)) {
				Assert.assertEquals("admin", text);
			}
		}
	}

}
