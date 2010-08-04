/*
   Copyright 2009 Zepheira LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.behaviours;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.FileObject;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.concepts.Template;
import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.RDFaReader;
import org.callimachusproject.rdfa.events.Base;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.stream.BoundedRDFReader;
import org.callimachusproject.stream.GraphPatternReader;
import org.callimachusproject.stream.OverrideBaseReader;
import org.callimachusproject.stream.PipedRDFEventReader;
import org.callimachusproject.stream.RDFXMLEventReader;
import org.callimachusproject.stream.TriplePatternStore;
import org.callimachusproject.stream.XMLElementReader;
import org.callimachusproject.traits.SoundexTrait;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.InternalServerError;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.xslt.TransformBuilder;
import org.openrdf.repository.object.xslt.XMLEventReaderFactory;
import org.openrdf.repository.object.xslt.XSLTransformer;

public abstract class RDFaSupport implements Template, SoundexTrait, RDFObject,
		FileObject {
	private static final Pattern TYPE_XSLT = Pattern
			.compile("\\btype=[\"'](text/xsl|application/xslt+xml)[\"']");
	private static final Pattern HREF_XSLT = Pattern
			.compile("<?xml-stylesheet\\b[^>]*\\bhref=[\"']([^\"']*)[\"']");
	private static final Pattern START_ELEMENT = Pattern.compile("<[^\\?]");
	private static XMLEventReaderFactory factory = XMLEventReaderFactory.newInstance();

	@operation("xslt")
	public XMLEventReader xslt(@parameter("mode") String mode,
			@parameter("element") String element) throws XMLStreamException,
			IOException, TransformerException {
		try {
			XMLEventReader doc = applyXSLT(mode);
			if (element == null)
				return doc;
			return new XMLElementReader(doc, element);
		} catch (NumberFormatException e) {
			throw new BadRequest(e);
		}
	}

	@operation("rdfa-triples")
	@type("application/rdf+xml")
	public XMLEventReader parseRDFa() throws XMLStreamException, IOException,
			TransformerException, RDFParseException, RepositoryException,
			MalformedQueryException, QueryEvaluationException {
		String base = toUri().toASCIIString();
		return new RDFXMLEventReader(new RDFaReader(base, xslt("view", null),
				toString()));
	}

	public RDFEventReader openBoundedPatterns(String mode, String about)
			throws XMLStreamException, IOException, TransformerException {
		return new BoundedRDFReader(openPatternReader(mode, null, about));
	}

	public TriplePatternStore readPatternStore(String mode, String element,
			String about) throws XMLStreamException, IOException,
			TransformerException, RDFParseException {
		String base = toUri().toASCIIString();
		TriplePatternStore query = new TriplePatternStore(base);
		RDFEventReader reader = openPatternReader(mode, element, about);
		try {
			query.consume(reader);
		} finally {
			reader.close();
		}
		return query;
	}

	public RDFEventReader constructPossibleTriples(TriplePatternStore patterns,
			TriplePattern pattern) {
		final TriplePattern project = patterns.getProjectedPattern(pattern);
		RDFEventReader query = patterns.openQueryBySubject(pattern.getObject());
		if (query == null)
			return null;
		query = new PipedRDFEventReader(query) {
			private boolean sent;

			protected void process(RDFEvent next) throws RDFParseException {
				if (!sent && next.isTriplePattern()) {
					add(project);
					sent = true;
				}
				add(next);
			}
		};
		return query;
	}

	private RDFEventReader openPatternReader(String mode, String element,
			String about) throws XMLStreamException, IOException,
			TransformerException {
		RDFEventReader reader = new RDFaReader(about, xslt(mode, element),
				toString());
		reader = new GraphPatternReader(reader);
		Base resolver = new Base(getResource().stringValue());
		if (about == null) {
			reader = new OverrideBaseReader(resolver, null, reader);
		} else {
			String uri = resolver.resolve(about);
			reader = new OverrideBaseReader(resolver, new Base(uri), reader);
		}
		return reader;
	}

	private XMLEventReader applyXSLT(@parameter("mode") String mode)
			throws XMLStreamException, IOException, TransformerException {
		String href = readXSLTSource();
		if (href == null)
			return factory.createXMLEventReader(openInputStream());
		java.net.URI uri = toUri();
		String xsl = uri.resolve(href).toASCIIString();
		XSLTransformer xslt = new XSLTransformer(xsl);
		TransformBuilder transform = xslt.transform(openInputStream(), uri
				.toASCIIString());
		transform = transform.with("this", uri.toASCIIString());
		transform = transform.with("xslt", xsl);
		transform = transform.with("mode", mode);
		return transform.asXMLEventReader();
	}

	/**
	 * This method parses the XSLT processing instruction. JAXP 1.4 (JDK6)
	 * cannot parse processing instructions.
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6849942
	 */
	private String readXSLTSource() throws IOException {
		String href = null;
		Reader source = openReader(true);
		if (source == null)
			throw new InternalServerError("Missing Template Body: " + this);
		BufferedReader reader = new BufferedReader(source);
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
}
