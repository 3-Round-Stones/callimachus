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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.xml.AggressiveCachedURIResolver;
import org.callimachusproject.xml.DOMSourceFactory;
import org.callimachusproject.xml.DocumentFactory;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Applies XSL transformations with the ability to convert the input and output
 * to a variety of formats.
 */
public class XSLTransformer {
	private final TransformerFactory tfactory;
	private final Templates xslt;
	private final String systemId;
	private final DOMSourceFactory sourceFactory = DOMSourceFactory
			.newInstance();
	private final DocumentFactory builder = DocumentFactory.newInstance();
	private String systemSourceId;

	XSLTransformer(TransformerFactory factory) {
		this(null, factory);
	}

	XSLTransformer(String url, TransformerFactory factory) {
		this.systemId = url;
		tfactory = factory;
		xslt = null;
	}

	XSLTransformer(InputStream in, String systemId, TransformerFactory factory) {
		try {
			try {
				this.systemId = systemId;
				tfactory = factory;
				ErrorCatcher error = new ErrorCatcher(systemId);
				tfactory.setErrorListener(error);
				Source source = sourceFactory.createSource(in, systemId);
				xslt = tfactory.newTemplates(source);
				if (error.isFatal())
					throw error.getFatalError();
			} finally {
				in.close();
			}
		} catch (TransformerConfigurationException e) {
			throw new ObjectCompositionException(e);
		} catch (TransformerException e) {
			throw new ObjectCompositionException(e);
		} catch (IOException e) {
			throw new ObjectCompositionException(e);
		} catch (SAXException e) {
			throw new ObjectCompositionException(e);
		} catch (ParserConfigurationException e) {
			throw new ObjectCompositionException(e);
		}
	}

	XSLTransformer(Reader r, String systemId, TransformerFactory factory) {
		try {
			try {
				this.systemId = systemId;
				tfactory = factory;
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
		} catch (SAXException e) {
			throw new ObjectCompositionException(e);
		} catch (ParserConfigurationException e) {
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

	public TransformBuilder transform() throws TransformerException,
			IOException {
		return transform(new DOMSource(newDocument()));
	}

	public TransformBuilder transform(InputStream source, String systemId) throws TransformerException, IOException {
		return transform(source, systemId, InputStream.class);
	}

	public TransformBuilder transform(Reader source, String systemId) throws TransformerException, IOException {
		return transform(source, systemId, Reader.class);
	}

	public TransformBuilder transform(XMLEventReader source, String systemId) throws TransformerException, IOException {
		return transform(source, systemId, XMLEventReader.class);
	}

	public TransformBuilder transform(Object source, String systemId, Type sourceType, String... media) throws TransformerException,
			IOException {
		Document doc = document(source, systemId, sourceType, media);
		if (doc == null)
			return transform();
		if (systemId == null) {
			systemId = doc.getDocumentURI();
			if (systemId == null) {
				systemId = doc.getBaseURI();
			}
		}
		return transform(new DOMSource(doc, systemId));
	}

	private Document document(Object source, String systemId, Type type, String... media) throws IOException, TransformerException {
		try {
			FluidBuilder fb = FluidFactory.getInstance().builder();
			FluidType xml = new FluidType(type, media).asXML();
			return fb.consume(source, systemId, xml).asDocument();
		} catch (TransformerConfigurationException e) {
			throw new TransformerException(e);
		} catch (OpenRDFException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} catch (SAXException e) {
			throw new TransformerException(e);
		} catch (TransformerException e) {
			throw new TransformerException(e);
		}
	}

	private TransformBuilder transform(final DOMSource source)
			throws TransformerException, IOException {
		String xsltId = getSystemId();
		TransformBuilder tb = new XSLTransformBuilder(newTransformer(), source,
				new AggressiveCachedURIResolver(xsltId, tfactory.getURIResolver()));
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
