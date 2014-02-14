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
package org.callimachusproject.setup;

import java.io.IOException;
import java.io.InputStream;

public class FaviconProvider extends UpdateProvider {
	private static final String FAVICON = "META-INF/templates/callimachus-icon.ico";
	private static final String ICO_TYPE = "types/IconGraphic";

	@Override
	public Updater updateOrigin(String virtual) throws IOException {
		return new FileUpdater(virtual) {

			@Override
			protected String getFileUrl(String origin) {
				return origin + "/favicon.ico";
			}

			@Override
			protected String[] getFileType(String webapps) {
				return new String[] { webapps + ICO_TYPE,
						"http://xmlns.com/foaf/0.1/Image" };
			}

			@Override
			protected InputStream getFileResourceAsStream() {
				ClassLoader cl = getClass().getClassLoader();
				return cl.getResourceAsStream(FAVICON);
			}
		};
	}

}
