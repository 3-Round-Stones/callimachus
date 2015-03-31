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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.util.SystemProperties;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.client.HttpClientFactory;
import org.openrdf.http.object.client.UnavailableRequestDirector;

public class CallimachusWebappImportProvider extends UpdateProvider {

	@Override
	public Updater updateCallimachusWebapp(final String origin)
			throws IOException {
		if (SystemProperties.getWebappCarFile().canRead()) {
			return new Updater() {
				public boolean update(String webapp, CalliRepository repository)
						throws IOException, OpenRDFException {
					HttpHost host = URIUtils.extractHost(java.net.URI.create(webapp));
					UnavailableRequestDirector unavailable = new UnavailableRequestDirector();
					try {
						HttpClientFactory.getInstance().putProxyIfAbsent(host, unavailable);
						importArchive(webapp, repository);
						return true;
					} catch (ReflectiveOperationException e) {
						throw new UndeclaredThrowableException(e);
					} catch (XMLStreamException e) {
						throw new UndeclaredThrowableException(e);
					} catch (DatatypeConfigurationException e) {
						throw new UndeclaredThrowableException(e);
					} finally {
						HttpClientFactory.getInstance().removeProxy(host, unavailable);
					}
				}
			};
		}
		return null;
	}

	void importArchive(String webapp, CalliRepository repo) throws IOException,
			OpenRDFException, ReflectiveOperationException, XMLStreamException,
			DatatypeConfigurationException {
		WebappArchiveImporter importer = new WebappArchiveImporter(webapp, repo);
		InputStream in = new FileInputStream(
				SystemProperties.getWebappCarFile());
		try {
			importer.importArchive(in, webapp);
		} finally {
			in.close();
		}
	}

}
