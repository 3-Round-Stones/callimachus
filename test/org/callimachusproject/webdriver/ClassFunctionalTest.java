package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.ClassEdit;
import org.callimachusproject.webdriver.pages.ClassView;
import org.callimachusproject.webdriver.pages.SampleResourceCreate;
import org.callimachusproject.webdriver.pages.SampleResourceEdit;
import org.callimachusproject.webdriver.pages.TextEditor;

public class ClassFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ClassFunctionalTest.class);
	}

	public ClassFunctionalTest() {
		super();
	}

	public ClassFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		String username = getUsername();
		logger.info("Login {}", username);
		page.openLogin().with(username, getPassword()).login();
	}

	@Override
	public void tearDown() throws Exception {
		logger.info("Logout");
		page.logout();
		super.tearDown();
	}

	public void testCreateClass() {
		String name = "Test";
		String comment = "testing";
		logger.info("Create class {}", name);
		page.openCurrentFolder().openClassCreate().with(name, comment)
				.create().waitUntilTitle(name);
		logger.info("Delete class {}", name);
		page.open(name + "?view").waitUntilTitle(name)
				.openEdit(ClassEdit.class).delete(name);
	}

	public void testCreatableClass() {
		String name = "Test";
		String comment = "testing";
		logger.info("Create class templates for {}", name);
		ClassEdit create = page.openCurrentFolder().openClassCreate();
		create.openCreateTemplate().saveAs("test-create.xhtml");
		create.openViewTemplate().saveAs("test-view.xhtml");
		create.openEditTemplate().saveAs("test-edit.xhtml");
		logger.info("Create class {}", name);
		ClassView cls = create.with(name, comment).create();
		logger.info("Create resource {}", "resource");
		cls.waitUntilTitle(name).createANew(name, SampleResourceCreate.class)
				.with("resource", "A test resource").createAs().back();
		logger.info("Delete resource {}", "resource");
		cls.openIndex(name).openResource("resource")
				.openEdit(SampleResourceEdit.class).delete("resource");
		logger.info("Delete class {}", name);
		page.open(name + "?view").waitUntilTitle(name)
				.openEdit(ClassEdit.class).delete(name);
		logger.info("Delete class templates for {}", name);
		page.open("test-create.xhtml?view").openEdit(TextEditor.class).delete();
		page.open("test-view.xhtml?view").openEdit(TextEditor.class).delete();
		page.open("test-edit.xhtml?view").openEdit(TextEditor.class).delete();
	}

}
