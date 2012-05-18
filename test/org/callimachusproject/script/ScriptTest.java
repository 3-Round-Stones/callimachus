package org.callimachusproject.script;

import junit.framework.TestSuite;

public class ScriptTest extends ScriptTestCase {

	public static TestSuite suite() throws Exception {
		return ScriptTestCase.suite(ScriptTest.class, "script.ttl", "#test");
	}

}
