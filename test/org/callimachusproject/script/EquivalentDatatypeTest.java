package org.callimachusproject.script;

import junit.framework.TestSuite;

public class EquivalentDatatypeTest extends ScriptTestCase {

	public static TestSuite suite() throws Exception {
		return ScriptTestCase.suite(EquivalentDatatypeTest.class, "equivalent.ttl", "#test");
	}

}
