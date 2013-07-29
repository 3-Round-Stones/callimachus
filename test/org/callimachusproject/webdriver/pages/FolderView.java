package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
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
		driver.mouseOverAndClick(By.linkText("More options"), By.linkText("Class"));
		return page(ClassEdit.class);
	}

	public DomainCreate openDomainCreate() {
		driver.click(By.id("create-menu"));
		driver.mouseOverAndClick(By.linkText("More options"), By.linkText("Domain"));
		return page(DomainCreate.class);
	}

	public GroupCreate openGroupCreate() {
		driver.click(By.id("create-menu"));
		driver.mouseOverAndClick(By.linkText("More options"), By.linkText("Group"));
		return page(GroupCreate.class);
	}

	public TextEditor openTextCreate(String link) {
		driver.click(By.id("create-menu"));
		driver.mouseOverAndClick(By.linkText("More options"), By.linkText(link));
		return page(TextEditor.class);
	}

	public FolderView waitUntilFolderOpen(String folderName) {
		driver.waitUntilTextPresent(By.cssSelector(".ui-widget-header"), folderName);
		return this;
	}

	public FolderEdit openEdit() {
		return openEdit(FolderEdit.class);
	}

}
