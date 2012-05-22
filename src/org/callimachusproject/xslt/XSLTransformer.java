/*
 * Copyright (c) 2009, Zepheira All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.xslt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryResultUtil;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.sparqlxml.SPARQLBooleanXMLWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Applies XSL transformations with the ability to convert the input and output
 * to a variety of formats.
 */
public class XSLTransformer {
	private final TransformerFactory tfactory;
	private final Templates xslt;
	private final String systemId;
	private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
	private final DOMSourceFactory sourceFactory = DOMSourceFactory
			.newInstance();
	private final DocumentFactory builder = DocumentFactory.newInstance();
	private String systemSourceId;

	public XSLTransformer() {
		this(null);
	}

	public XSLTransformer(String url) {
		this.systemId = url;
		tfactory = new CachedTransformerFactory(url);
		xslt = null;
	}

	public XSLTransformer(Reader r, String systemId) {
		try {
			try {
				this.systemId = systemId;
				tfactory = new CachedTransformerFactory(systemId);
				ErrorCatcher error = new ErrorCatcher(systemId);
				tfactory.setErrorListener(error);
				Source source = sourceFactory.createSource(r, systemId);
				xslt = tfactory.newTemplates(source);
				if (error.isFatal())
					throw error.getFatalError();
			} finally {
				r.close();
			}
		} catch (TransformerConfigurationException e) {
			throw new ObjectCompositionException(e);
		} catch (TransformerException e) {
			throw new ObjectCompositionException(e);
		} catch (IOException e) {
			throw new ObjectCompositionException(e);
		}
	}

	public String getSystemId() {
		if (systemSourceId != null)
			return systemSourceId; // resolved systemId
		return systemId; // provided systemId
	}

	@Override
	public String toString() {
		return getSystemId();
	}

	public TransformBuilder transform() throws TransformerException {
		return transform(new DOMSource(newDocument()));
	}

	public TransformBuilder transform(Void nil) throws TransformerException {
		return transform();
	}

	public TransformBuilder transform(Void nil, String systemId)
			throws TransformerException {
		return transform(nil);
	}

	public TransformBuilder transform(File file) throws TransformerException {
		if (file == null)
			return transform();
		return transform(file, file.toURI().toASCIIString());
	}

	public TransformBuilder transform(File file, String systemId)
			throws TransformerException {
		if (file == null)
			return transform();
		try {
			FileInputStream in = new FileInputStream(file);
			return transform(in, systemId);
		} catch (IOException e) {
			throw new TransformerException(e);
		}
	}

	public TransformBuilder transform(RDFObject object)
			throws TransformerException {
		if (object == null)
			return transform();
		String uri = object.getResource().stringValue();
		return transform(tfactory.getURIResolver().resolve(uri, null));
	}

	public TransformBuilder transform(RDFObject object, String systemId)
			throws TransformerException {
		return transform(object);
	}

	public TransformBuilder transform(URL url) throws IOException,
			TransformerException {
		if (url == null)
			return transform();
		return transform(tfactory.getURIResolver().resolve(
				url.toExternalForm(), null));
	}

	public TransformBuilder transform(URL url, String systemId)
			throws IOException, TransformerException {
		return transform(url);
	}

	public TransformBuilder transform(String string)
			throws TransformerException {
		if (string == null)
			return transform();
		return transform(new StringReader(string));
	}

	public TransformBuilder transform(String string, String systemId)
			throws TransformerException {
		if (string == null)
			return transform();
		return transform(new StringReader(string), systemId);
	}

	public TransformBuilder transform(CharSequence string)
			throws TransformerException {
		if (string == null)
			return transform();
		return transform(new StringReader(string.toString()));
	}

	public TransformBuilder transform(CharSequence string, String systemId)
			throws TransformerException {
		if (string == null)
			return transform();
		return transform(new StringReader(string.toString()), systemId);
	}

	public TransformBuilder transform(Readable readable)
			throws TransformerException {
		if (readable == null)
			return transform();
		if (readable instanceof Reader)
			return transform((Reader) readable);
		return transform(new ReadableReader(readable));
	}

	public TransformBuilder transform(Readable readable, String systemId)
			throws TransformerException {
		if (readable == null)
			return transform();
		if (readable instanceof Reader)
			return transform((Reader) readable, systemId);
		return transform(new ReadableReader(readable), systemId);
	}

	public TransformBuilder transform(Reader reader)
			throws TransformerException {
		if (reader == null)
			return transform();
		if (isIdentityTransform())
			return new ReaderTransform(reader);
		return transform(sourceFactory.createSource(reader, null));
	}

	public TransformBuilder transform(Reader reader, String systemId)
			throws TransformerException {
		if (reader == null)
			return transform();
		if (isIdentityTransform())
			return new ReaderTransform(reader, systemId);
		return transform(sourceFactory.createSource(reader, systemId));
	}

	public TransformBuilder transform(ByteArrayOutputStream buf)
			throws TransformerException {
		if (buf == null)
			return transform();
		return transform(buf.toByteArray());
	}

	public TransformBuilder transform(ByteArrayOutputStream buf, String systemId)
			throws TransformerException {
		if (buf == null)
			return transform();
		return transform(buf.toByteArray(), systemId);
	}

	public TransformBuilder transform(byte[] buf) throws TransformerException {
		if (buf == null)
			return transform();
		return transform(new ByteArrayInputStream(buf));
	}

	public TransformBuilder transform(byte[] buf, String systemId)
			throws TransformerException {
		if (buf == null)
			return transform();
		return transform(new ByteArrayInputStream(buf), systemId);
	}

	public TransformBuilder transform(ReadableByteChannel channel)
			throws TransformerException {
		if (channel == null)
			return transform();
		return transform(Channels.newInputStream(channel));
	}

	public TransformBuilder transform(ReadableByteChannel channel,
			String systemId) throws TransformerException {
		if (channel == null)
			return transform();
		return transform(Channels.newInputStream(channel), systemId);
	}

	public TransformBuilder transform(InputStream in)
			throws TransformerException {
		if (in == null)
			return transform();
		if (isIdentityTransform())
			return new StreamTransform(in);
		return transform(sourceFactory.createSource(in, null));
	}

	public TransformBuilder transform(InputStream in, String systemId)
			throws TransformerException {
		if (in == null)
			return transform();
		if (isIdentityTransform())
			return new StreamTransform(in, systemId);
		return transform(sourceFactory.createSource(in, systemId));
	}

	public TransformBuilder transform(XMLEventReader reader)
			throws TransformerException {
		if (reader == null)
			return transform();
		try {
			String systemId = null;
			XMLEvent peek = reader.peek();
			if (peek != null) {
				Location location = peek.getLocation();
				if (location != null) {
					systemId = location.getSystemId();
				}
			}
			return transform(reader, systemId);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		}
	}

	public TransformBuilder transform(XMLEventReader reader, String systemId)
			throws TransformerException {
		return transform(toByteArrayInputStream(reader), systemId);
	}

	public TransformBuilder transform(Document node)
			throws TransformerException {
		if (node == null)
			return transform();
		return transform(sourceFactory
				.createSource(node, node.getDocumentURI()));
	}

	public TransformBuilder transform(Document node, String systemId)
			throws TransformerException {
		if (node == null)
			return transform();
		return transform(sourceFactory.createSource(node, systemId));
	}

	public TransformBuilder transform(DocumentFragment node)
			throws TransformerException {
		if (node == null)
			return transform();
		NodeList nodes = node.getChildNodes();
		if (nodes.getLength() == 1 && node.getFirstChild().getNodeType() == 1)
			return transform(sourceFactory.createSource(node.getFirstChild(),
					null));
		Document doc = newDocument();
		Element root = doc.createElement("root");
		root.appendChild(doc.importNode(node, true));
		return transform(sourceFactory.createSource(root, null));
	}

	public TransformBuilder transform(DocumentFragment node, String systemId)
			throws TransformerException {
		if (node == null)
			return transform();
		NodeList nodes = node.getChildNodes();
		if (nodes.getLength() == 1 && node.getFirstChild().getNodeType() == 1)
			return transform(sourceFactory.createSource(node.getFirstChild(),
					systemId));
		Document doc = newDocument();
		Element root = doc.createElement("root");
		root.appendChild(doc.importNode(node, true));
		return transform(sourceFactory.createSource(root, systemId));
	}

	public TransformBuilder transform(Element node)
			throws TransformerException {
		if (node == null)
			return transform();
		return transform(sourceFactory.createSource(node, null));
	}

	public TransformBuilder transform(Element node, String systemId)
			throws TransformerException {
		if (node == null)
			return transform();
		return transform(sourceFactory.createSource(node, systemId));
	}

	public TransformBuilder transform(Node node) throws TransformerException {
		if (node instanceof Document)
			return transform((Document) node);
		if (node instanceof Element)
			return transform((Element) node);
		if (node == null)
			return transform();
		return transform(sourceFactory.createSource(node, null));
	}

	public TransformBuilder transform(Node node, String systemId)
			throws TransformerException {
		if (node instanceof Document)
			return transform((Document) node, systemId);
		if (node instanceof Element)
			return transform((Element) node, systemId);
		if (node == null)
			return transform();
		return transform(sourceFactory.createSource(node, systemId));
	}

	public TransformBuilder transform(GraphQueryResult result)
			throws TransformerException {
		return transform(toByteArrayInputStream(result));
	}

	public TransformBuilder transform(GraphQueryResult result, String systemId)
			throws TransformerException {
		return transform(toByteArrayInputStream(result), systemId);
	}

	public TransformBuilder transform(TupleQueryResult result)
			throws TransformerException {
		return transform(toByteArrayInputStream(result));
	}

	public TransformBuilder transform(TupleQueryResult result, String systemId)
			throws TransformerException {
		return transform(toByteArrayInputStream(result), systemId);
	}

	public TransformBuilder transform(Boolean result)
			throws TransformerException {
		return transform(toByteArrayInputStream(result));
	}

	public TransformBuilder transform(Boolean result, String systemId)
			throws TransformerException {
		return transform(toByteArrayInputStream(result), systemId);
	}

	private ByteArrayInputStream toByteArrayInputStream(XMLEventReader reader)
			throws TransformerException {
		if (reader == null)
			return null;
		ByteArrayOutputStream output = new ByteArrayOutputStream(8192);
		try {
			XMLEventWriter writer = outFactory.createXMLEventWriter(output);
			try {
				writer.add(reader);
			} finally {
				reader.close();
				writer.close();
				output.close();
			}
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (IOException e) {
			throw new TransformerException(e);
		}
		ByteArrayInputStream input = new ByteArrayInputStream(
				output.toByteArray());
		return input;
	}

	private ByteArrayInputStream toByteArrayInputStream(GraphQueryResult result)
			throws TransformerException {
		if (result == null)
			return null;
		ByteArrayOutputStream output = new ByteArrayOutputStream(8192);
		try {
			RDFXMLWriter writer = new RDFXMLWriter(output);
			try {
				QueryResultUtil.report(result, writer);
			} catch (RDFHandlerException e) {
				throw new TransformerException(e);
			} catch (QueryEvaluationException e) {
				throw new TransformerException(e);
			} finally {
				output.close();
			}
		} catch (IOException e) {
			throw new TransformerException(e);
		}
		ByteArrayInputStream input = new ByteArrayInputStream(
				output.toByteArray());
		return input;
	}

	private ByteArrayInputStream toByteArrayInputStream(TupleQueryResult result)
			throws TransformerException {
		if (result == null)
			return null;
		ByteArrayOutputStream output = new ByteArrayOutputStream(8192);
		try {
			SPARQLResultsXMLWriter writer;
			writer = new SPARQLResultsXMLWriter(output);
			try {
				QueryResultUtil.report(result, writer);
			} catch (TupleQueryResultHandlerException e) {
				throw new TransformerException(e);
			} catch (QueryEvaluationException e) {
				throw new TransformerException(e);
			} finally {
				output.close();
			}
		} catch (IOException e) {
			throw new TransformerException(e);
		}
		ByteArrayInputStream input = new ByteArrayInputStream(
				output.toByteArray());
		return input;
	}

	private ByteArrayInputStream toByteArrayInputStream(Boolean result)
			throws TransformerException {
		if (result == null)
			return null;
		ByteArrayOutputStream output = new ByteArrayOutputStream(8192);
		SPARQLBooleanXMLWriter writer = new SPARQLBooleanXMLWriter(output);
		try {
			try {
				writer.write(result);
			} finally {
				output.close();
			}
		} catch (IOException e) {
			throw new TransformerException(e);
		}
		return new ByteArrayInputStream(output.toByteArray());
	}

	private boolean isIdentityTransform() {
		return xslt == null && systemId == null;
	}

	private TransformBuilder transform(final Source source)
			throws TransformerException {
		String xsltId = getSystemId();
		TransformBuilder tb = new XSLTransformBuilder(newTransformer(), source,
				new CachedURIResolver(xsltId, tfactory.getURIResolver()));
		if (xsltId != null) {
			tb = tb.with("xsltId", xsltId);
		}
		String systemId = source.getSystemId();
		if (systemId != null) {
			tb = tb.with("systemId", systemId);
		}
		return tb;
	}

	private Transformer newTransformer() throws TransformerException {
		if (xslt != null)
			return xslt.newTransformer();
		if (systemId == null)
			return tfactory.newTransformer();
		Source xsl = tfactory.getURIResolver().resolve(systemId, null);
		systemSourceId = xsl.getSystemId();
		Templates templates = tfactory.newTemplates(xsl);
		if (templates == null)
			return tfactory.newTransformer();
		return templates.newTransformer();
	}

	private Document newDocument() throws TransformerException {
		try {
			return builder.newDocument();
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		}
	}

}
