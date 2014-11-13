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

public class StyleIntegrationTest extends TemporaryServerIntegrationTestCase {
	private static final String CSS = "hr {color:sienna;} \n" +
		    "p {margin-left:20px;} \n" +
		    "body {background-color:blue}";

	@Test
	public void testLess() throws Exception {
		WebResource create = getHomeFolder().ref("?create="+ getCallimachusUrl("types/StyleSheet"));
		WebResource resource = create.create("test-style.css", "text/css", CSS.getBytes());
		WebResource less = resource.rel("alternate", "text/css");
		less.get("text/css");
	}

}
