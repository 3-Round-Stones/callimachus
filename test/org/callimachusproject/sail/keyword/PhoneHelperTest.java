package org.callimachusproject.sail.keyword;

import junit.framework.TestCase;

public class PhoneHelperTest extends TestCase {
	private PhoneHelper helper = PhoneHelperFactory.newInstance().createPhoneHelper();

	public void testBaseBall() throws Exception {
		assertTrue(helper.phones("base ball").contains(helper.soundex("base")));
		assertTrue(helper.phones("base ball").contains(helper.soundex("ball")));
		assertTrue(helper.phones("base ball").contains(helper.soundex("base ball")));
	}

}
