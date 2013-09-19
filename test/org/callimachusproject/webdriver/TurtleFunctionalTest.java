package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class TurtleFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] turtle = new String[] {
			"test.ttl",
			"<> a <Test> ." };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(TurtleFunctionalTest.class);
	}

	public TurtleFunctionalTest() {
		super();
	}

	public TurtleFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	@Override
	public void runBare() throws Throwable {
		locker.writeLock().lock();
		try {
			super.runBare();
		} finally {
			locker.writeLock().unlock();
		}
	}

	public void testCreateTurtle() {
		String name = turtle[0];
		logger.info("Create turtle {}", name);
		page.openCurrentFolder().openTextCreate("GraphDocument").clear()
				.type(turtle[1]).end().saveAs(name);
		logger.info("Delete turtle {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
