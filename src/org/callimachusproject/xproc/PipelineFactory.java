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
package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.http.client.HttpClient;
import org.xml.sax.SAXException;

public class PipelineFactory {

	public static PipelineFactory newInstance() {
		return new PipelineFactory();
	}

	private PipelineFactory() {
		super();
	}

	public Pipeline createPipeline(String systemId, HttpClient client) {
		return new Pipeline(systemId, client);
	}

	public Pipeline createPipeline(InputStream in, String systemId, HttpClient client)
			throws SAXException, IOException {
		return new Pipeline(in, systemId, client);
	}

	public Pipeline createPipeline(Reader reader, String systemId, HttpClient client)
			throws SAXException, IOException {
		return new Pipeline(reader, systemId, client);
	}

}
