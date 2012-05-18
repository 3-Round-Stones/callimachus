package org.callimachusproject.script;

import junit.framework.TestSuite;

public class RestrictionTest extends ScriptTestCase {

	public static TestSuite suite() throws Exception {
		return ScriptTestCase.suite(RestrictionTest.class, "restriction.ttl", "#test");
	}

}
