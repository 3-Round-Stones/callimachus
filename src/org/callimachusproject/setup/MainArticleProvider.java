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

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

public class MainArticleProvider extends UpdateProvider {
	private static final String MAIN_ARTICLE = "META-INF/templates/main-article.docbook";
	private static final String ARTICLE_TYPE = "types/Article";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_DESCRIBEDBY = CALLI + "describedby";

	@Override
	public Updater updateCallimachusWebapp(String origin) throws IOException {
		return new FileUpdater(origin) {

			@Override
			protected String getFileUrl(String origin) {
				return origin + "/main-article.docbook";
			}

			@Override
			protected String[] getFileType(String webapps) {
				return new String[] { webapps + ARTICLE_TYPE,
						"http://xmlns.com/foaf/0.1/Document" };
			}

			@Override
			protected InputStream getFileResourceAsStream() {
				ClassLoader cl = getClass().getClassLoader();
				return cl.getResourceAsStream(MAIN_ARTICLE);
			}

			@Override
			protected void additionalMetadata(URI file, ObjectConnection con)
					throws RepositoryException {
				ValueFactory vf = con.getValueFactory();
				URI folder = vf.createURI(file.getNamespace());
				URI describedby = vf.createURI(CALLI_DESCRIBEDBY);
				if (!con.hasStatement(folder, describedby, null)) {
					con.add(folder, describedby,
							vf.createLiteral("main-article.docbook?view"));
				}
			}
		};
	}

}
