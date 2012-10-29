/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some
  Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved 
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
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.concepts.Page;
import org.callimachusproject.xml.XMLEventReaderFactory;
import org.callimachusproject.xslt.XSLTransformer;
import org.callimachusproject.xslt.XSLTransformerFactory;
import org.openrdf.repository.object.RDFObject;

/**
 * Implements the construct search method to lookup resources by label prefix
 * and options method to list all possible values.
 * 
 * @author James Leigh 
 * @author Steve Battle
 * 
 */
public abstract class FormSupport implements Page, RDFObject {
	static final XSLTransformer HTML_XSLT;


	static {
		String path = "org/callimachusproject/xsl/page-to-html.xsl";
		ClassLoader cl = ViewSupport.class.getClassLoader();
		String url = cl.getResource(path).toExternalForm();
		InputStream input = cl.getResourceAsStream(path);
		InputStreamReader reader = new InputStreamReader(input);
		HTML_XSLT = XSLTransformerFactory.getInstance().createTransformer(reader, url);
	}

	@Override
	public String calliConstructHTML(Object target) throws Exception {
		return asHtmlString(calliConstruct(target));
	}

	private String asHtmlString(XMLEventReader xhtml) throws Exception {
		return HTML_XSLT.transform(xhtml, this.toString()).asString();
	}

	protected XMLEventReader xslt(String element)
			throws IOException, XMLStreamException {
		XMLEventReaderFactory factory = XMLEventReaderFactory.newInstance();
		String url = url("xslt", element);
		InputStream in = openRequest(url);
		return factory.createXMLEventReader(url, in);
	}

	private String url(String operation, String element)
			throws UnsupportedEncodingException {
		String uri = getResource().stringValue();
		StringBuilder sb = new StringBuilder();
		sb.append(uri);
		sb.append("?");
		sb.append(URLEncoder.encode(operation, "UTF-8"));
		if (element != null) {
			sb.append("&element=");
			sb.append(URLEncoder.encode(element, "UTF-8"));
		}
		return sb.toString();
	}

	private InputStream openRequest(String url) throws IOException {
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		return client.get(url).getEntity().getContent();
	}

}
