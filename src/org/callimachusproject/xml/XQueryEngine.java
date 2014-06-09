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
package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import net.sf.saxon.Configuration;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;

import org.callimachusproject.client.HttpUriClient;
import org.callimachusproject.io.ProducerStream;
import org.callimachusproject.io.ProducerStream.OutputProducer;
import org.xml.sax.SAXException;

public class XQueryEngine {
	private static final String XQUERY_MEDIA = "application/xquery, application/xml, application/xslt+xml, text/xml, text/xsl";
	private final String baseURI;
	private final HttpUriClient client;
	private final ErrorListListener messages = new ErrorListListener();

	public XQueryEngine(String baseURI, HttpUriClient client) {
		this.baseURI = baseURI;
		this.client = client;
	}

	public void validate(InputStream queryStream) throws IOException {
		Configuration config = Configuration.newConfiguration();
        config.setHostLanguage(Configuration.XQUERY);
        StaticQueryContext staticEnv = config.newStaticQueryContext();
        staticEnv.setBaseURI(baseURI);
        staticEnv.setErrorListener(messages);
        staticEnv.setModuleURIResolver(new InputSourceResolver(XQUERY_MEDIA, client));
        try {
			staticEnv.compileQuery(queryStream, null);
		} catch (XPathException e) {
			messages.fatalError(e);
		}
	}

	public String[] getErrorMessages() {
		return messages.getErrorMessages();
	}

	public InputStream evaluateResult(InputStream queryStream,
			Map<String, String[]> parameters) throws IOException,
			SaxonApiException, SAXException {
		return evaluateResult(queryStream, parameters, null, null);
	}

	public InputStream evaluateResult(InputStream queryStream,
			Map<String, String[]> parameters, InputStream documentStream,
			String documentId) throws IOException, SaxonApiException, SAXException {
		Processor qtproc = new Processor(false);
		XdmNodeFactory resolver = new XdmNodeFactory(qtproc, client);
		XQueryCompiler xqcomp = qtproc.newXQueryCompiler();
		xqcomp.setBaseURI(URI.create(baseURI));
		xqcomp.setModuleURIResolver(resolver);
		XQueryExecutable xqexec = xqcomp.compile(queryStream);
		final XQueryEvaluator xqeval = xqexec.load();
		if (parameters != null) {
			for (Map.Entry<String, String[]> e : parameters.entrySet()) {
				QName name = new QName(e.getKey());
				ArrayList<XdmItem> values = new ArrayList<>(e.getValue().length);
				for (String value : e.getValue()) {
					values.add(new XdmAtomicValue(value, ItemType.STRING));
				}
				xqeval.setExternalVariable(name, new XdmValue(values));
			}
		}
		if (documentStream != null) {
			xqeval.setContextItem(resolver.parse(documentId, documentStream));
		}
		return new ProducerStream(new OutputProducer() {
			public void produce(OutputStream out) throws IOException {
				xqeval.setDestination(new Serializer(out));
				try {
					xqeval.run();
				} catch (SaxonApiException e) {
					throw new IOException(e);
				}
			}
		});
	}
}
