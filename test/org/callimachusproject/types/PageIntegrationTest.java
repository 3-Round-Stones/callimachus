/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.types;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;
import org.junit.Test;

public class PageIntegrationTest extends TemporaryServerIntegrationTestCase {
	private static final String XHTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
			"<head> <title> Wikipedia </title> </head> \n" +
			"<body> <p> Wikipedia is a great website. </p> </body> </html>";

	@Test
	public void testLess() throws Exception {
		WebResource create = getHomeFolder().ref("?create="+ getCallimachusUrl("types/Page"));
		WebResource page = create.create("test-page.xhtml", "application/xhtml+xml", XHTML.getBytes());
		page.ref("?element=/1&realm=/").get("text/html");
		page.ref("?template&realm=/").get("application/xhtml+xml");
	}

}
