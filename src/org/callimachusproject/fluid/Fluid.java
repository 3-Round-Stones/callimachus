/*
   Copyright (c) 2012 3 Round Stones Inc, Some Rights Reserved

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
package org.callimachusproject.fluid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.openrdf.OpenRDFException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.SAXException;

/**
 * Intermediate converting format.
 * 
 * @author James Leigh
 * 
 */
public interface Fluid {

	FluidType getFluidType();

	String getSystemId();

	void asVoid() throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException;

	/**
	 * {@link ReadableByteChannel}
	 */
	String toChannelMedia(String... media);

	ReadableByteChannel asChannel(String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException;

	void transferTo(WritableByteChannel out, String... media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException;

	/**
	 * {@link InputStream}
	 */
	String toStreamMedia(String... media);

	InputStream asStream(String... media) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException;

	void streamTo(OutputStream out, String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException;

	/**
	 * {@link Reader}
	 */
	String toReaderMedia(String... media);

	Reader asReader(String... media) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException;

	void writeTo(Writer writer, String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException;

	/**
	 * {@link String}
	 */
	String toStringMedia(String... media);

	String asString(String... media) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException;

	/**
	 * {@link HttpEntity}
	 */
	String toHttpEntityMedia(String... media);

	HttpEntity asHttpEntity(String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException;

	/**
	 * {@link HttpResponse}
	 */
	String toHttpResponseMedia(String... media);

	HttpResponse asHttpResponse(String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException;

	/**
	 * {@link XMLEventReader}
	 */
	String toXMLEventReaderMedia(String... media);

	XMLEventReader asXMLEventReader(String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException;

	/**
	 * {@link Document}
	 */
	String toDocumentMedia(String... media);

	Document asDocument(String... media) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException;

	/**
	 * {@link DocumentFragment}
	 */
	String toDocumentFragmentMedia(String... media);

	DocumentFragment asDocumentFragment(String... media) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException;

	/**
	 * {@link Type}
	 */
	String toMedia(Type gtype, String... media);

	Object as(Type gtype, String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException;

	/**
	 * {@link FluidType}
	 */
	String toMedia(FluidType ftype);

	Object as(FluidType ftype) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException;
}
