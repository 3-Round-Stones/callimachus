/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some Rights Reserved
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

import static org.callimachusproject.stream.SPARQLWriter.toSPARQL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.FileObject;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.TransformerException;

import org.callimachusproject.concepts.Page;
import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFaReader;
import org.callimachusproject.rdfa.events.Base;
import org.callimachusproject.stream.BoundedRDFReader;
import org.callimachusproject.stream.OverrideBaseReader;
import org.callimachusproject.stream.RDFXMLEventReader;
import org.callimachusproject.stream.SPARQLProducer;
import org.callimachusproject.stream.XMLElementReader;
import org.callimachusproject.traits.SoundexTrait;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.InternalServerError;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.xslt.TransformBuilder;
import org.openrdf.repository.object.xslt.XMLEventReaderFactory;
import org.openrdf.repository.object.xslt.XSLTransformer;

/**
 * Implements a few {@link Page} methods to read an RDFa document. This class is
 * responsible for applying XSL transformations, extracting x-pointer elements,
 * and parsing RDFa into sparql.
 * 
 * @author James Leigh
 * @author Steve Battle
 * 
 */
public abstract class RDFaSupport implements Page, SoundexTrait, RDFObject,
		FileObject {
	private static final Pattern TYPE_XSLT = Pattern
			.compile("\\btype=[\"'](text/xsl|application/xslt+xml)[\"']");
	private static final Pattern HREF_XSLT = Pattern
			.compile("<?xml-stylesheet\\b[^>]*\\bhref=[\"']([^\"']*)[\"']");
	private static final Pattern START_ELEMENT = Pattern.compile("<[^\\?]");
	private static final XMLEventReaderFactory factory = XMLEventReaderFactory
			.newInstance();
	private static final int MAX_XSLT = 16;
	private static final Map<String, Reference<XSLTransformer>> transformers = new LinkedHashMap<String, Reference<XSLTransformer>>(
			16, 0.75f, true) {
		private static final long serialVersionUID = 1362917757653811798L;

		protected boolean removeEldestEntry(
				Map.Entry<String, Reference<XSLTransformer>> eldest) {
			return size() > MAX_XSLT;
		}
	};
	private static final XSLTransformer DATA_ATTR;
	static {
		String path = "org/callimachusproject/xsl/data-attributes.xsl";
		ClassLoader cl = RDFaSupport.class.getClassLoader();
		String url = cl.getResource(path).toExternalForm();
		InputStream input = cl.getResourceAsStream(path);
		InputStreamReader reader = new InputStreamReader(input);
		DATA_ATTR = new XSLTransformer(reader, url);
	}

	@query("xslt")
	@type("application/xml")
	public XMLEventReader xslt(@query("query") String query,
			@query("element") String element)
			throws XMLStreamException, IOException, TransformerException {
		XMLEventReader doc = applyXSLT(query);
		return extract(addDataAttributes(doc, query), element);
	}

	@query("triples")
	@type("application/rdf+xml")
	public XMLEventReader triples(@query("query") String query,
			@query("element") String element) throws Exception {
		String base = toUri().toASCIIString();
		XMLEventReader doc = xslt(query, element);
		return new RDFXMLEventReader(new RDFaReader(base, doc, toString()));
	}

	@query("sparql")
	@type("application/sparql-query")
	public byte[] sparql(@query("about") String about,
			@query("query") String query, @query("element") String element)
			throws Exception {
		RDFEventReader sparql = openPatternReader(about, query, element);
		return toSPARQL(sparql).getBytes(Charset.forName("UTF-8"));
	}

	public RDFEventReader openBoundedPatterns(String about, String query)
			throws XMLStreamException, IOException, TransformerException {
		return new BoundedRDFReader(openPatternReader(query, about, null));
	}

	public RDFEventReader openPatternReader(String about, String query,
			String element) throws XMLStreamException, IOException,
			TransformerException {
		XMLEventReader template = xslt(query, element);
		RDFEventReader reader = new RDFaReader(about, template, toString());
		
		/* generate UNION form of sparql query */
		// reader = new GraphPatternReader(reader);
		reader = new SPARQLProducer(reader);
		
		Base resolver = new Base(getResource().stringValue());
		if (about == null) {
			reader = new OverrideBaseReader(resolver, null, reader);
		} else {
			String uri = resolver.resolve(about);
			reader = new OverrideBaseReader(resolver, new Base(uri), reader);
		}
		return reader;
	}

	public RDFEventReader openPatternReader(XMLEventReader template, String about, String query,
			String element) throws XMLStreamException, IOException,
			TransformerException {
		RDFEventReader reader = new RDFaReader(about, template, toString());
		
		/* generate UNION form of sparql query */
		// reader = new GraphPatternReader(reader);
		reader = new SPARQLProducer(reader);
		
		Base resolver = new Base(getResource().stringValue());
		if (about == null) {
			reader = new OverrideBaseReader(resolver, null, reader);
		} else {
			String uri = resolver.resolve(about);
			reader = new OverrideBaseReader(resolver, new Base(uri), reader);
		}
		return reader;
	}

	private XMLEventReader applyXSLT(@query("query") String query)
			throws XMLStreamException, IOException, TransformerException {
		String href = readXSLTSource();
		if (href == null)
			return factory.createXMLEventReader(openInputStream());
		java.net.URI uri = toUri();
		String xsl = uri.resolve(href).toASCIIString();
		XSLTransformer xslt = newXSLTransformer(xsl);
		TransformBuilder transform = xslt.transform(openInputStream(), uri
				.toASCIIString());
		transform = transform.with("this", uri.toASCIIString());
		transform = transform.with("xslt", xsl);
		transform = transform.with("query", query);
		transform = transform.with("template", true);
		return transform.asXMLEventReader();
	}

	private XMLEventReader addDataAttributes(XMLEventReader doc, String query)
			throws TransformerException, IOException, XMLStreamException {
		TransformBuilder transform = DATA_ATTR.transform(doc, this.toString());
		transform = transform.with("this", this.toString());
		transform = transform.with("query", query);
		return transform.asXMLEventReader();
	}

	private XMLEventReader extract(XMLEventReader xhtml, String element)
			throws XMLStreamException, IOException, TransformerException {
		try {
			if (element == null || element.equals("/1"))
				return xhtml;
			if (!element.startsWith("/1/"))
				throw new BadRequest("Invalid element parameter: " + element);
			String parent = element.substring(0, element.lastIndexOf('/'));
			String child = "/1" + element.substring(element.lastIndexOf('/'));
			XMLElementReader pxptr = new XMLElementReader(xhtml, parent);
			boolean prel = isRelationshipElement(pxptr); // data-add(construct)
															// data-member(construct)
															// data-more@rel(template)
			XMLElementReader xptr = new XMLElementReader(pxptr, child);
			boolean crel = isRelationshipElement(xptr); // data-search(search)
														// data-more@property(template)
			xptr = new XMLElementReader(xptr, "/1");
			xptr.mark(1024);
			XMLElementReader nxptr = new XMLElementReader(xptr, "/1/1");
			boolean nrel = isRelationshipElement(nxptr); // data-options(options)
			xptr.reset();
			if (!prel && !crel && !nrel)
				throw new BadRequest("Invalid element parameter: " + element);
			return xptr;
		} catch (NumberFormatException e) {
			throw new BadRequest(e);
		}
	}
	
	private XSLTransformer newXSLTransformer(String xsl) {
		synchronized (transformers) {
			Reference<XSLTransformer> ref = transformers.get(xsl);
			if (ref != null) {
				XSLTransformer xslt = ref.get();
				if (xslt != null)
					return xslt;
			}
			XSLTransformer xslt = new XSLTransformer(xsl);
			transformers.put(xsl, new SoftReference<XSLTransformer>(xslt));
			return xslt;
		}
	}

	/**
	 * This method parses the XSLT processing instruction. JAXP 1.4 (JDK6)
	 * cannot parse processing instructions.
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6849942
	 */
	private String readXSLTSource() throws IOException {
		String href = null;
		InputStream in = openInputStream();
		if (in == null)
			throw new InternalServerError("Missing Template Body: " + this);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("<?xml-stylesheet ")) {
					if (TYPE_XSLT.matcher(line).find()) {
						Matcher matcher = HREF_XSLT.matcher(line);
						if (matcher.find()) {
							href = matcher.group(1);
							break;
						}
					}
				}
				if (START_ELEMENT.matcher(line).find())
					break;
			}
		} finally {
			reader.close();
		}
		return href;
	}

	private boolean isRelationshipElement(XMLElementReader doc)
			throws XMLStreamException {
		doc.mark(1024);
		try {
			while (doc.hasNext() && !doc.peek().isStartElement()) {
				doc.next();
			}
			if (doc.hasNext()) {
				StartElement start = doc.nextEvent().asStartElement();
				Iterator<Attribute> iter = start.getAttributes();
				while (iter.hasNext()) {
					String part = iter.next().getName().getLocalPart();
					if ("rel".equals(part) || "rev".equals(part)
							|| "property".equals(part)) {
						return true;
					}
				}
			}
			return false;
		} finally {
			doc.reset();
		}
	}
}
