/*
 * Copyright (c) 2013-2014 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.behaviours;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.tools.FileObject;

import net.sf.saxon.s9api.SaxonApiException;

import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.xml.XQueryEngine;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

public abstract class XQuerySupport implements CalliObject, FileObject {

	public String[] getXQueryValidationErrors(InputStream queryStream)
			throws IOException, OpenRDFException {
		XQueryEngine validator = new XQueryEngine(this.toString(),
				this.getHttpClient());
		validator.validate(queryStream);
		return validator.getErrorMessages();
	}

	public InputStream GetResult(Map<String, String[]> parameters)
			throws OpenRDFException, IOException, SaxonApiException, SAXException {
		XQueryEngine engine = new XQueryEngine(this.toString(),
				this.getHttpClient());
		return engine.evaluateResult(this.openInputStream(), parameters);
	}

	public InputStream PostResult(Map<String, String[]> parameters, String docId, InputStream doc)
			throws OpenRDFException, IOException, SaxonApiException, SAXException {
		XQueryEngine engine = new XQueryEngine(this.toString(),
				this.getHttpClient());
		return engine.evaluateResult(this.openInputStream(), parameters, doc, docId);
	}
}
