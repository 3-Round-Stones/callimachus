package org.callimachusproject.xslt;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.transform.TransformerException;

import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XsltAdvice implements Advice {
	private final XSLTransformer xslt;
	private final Class<?> returnClass;
	private final Class<?> inputClass;
	private final int inputIdx;
	private final String[][] bindingNames;

	public XsltAdvice(XSLTransformer xslt, Class<?> returnClass,
			Class<?> inputClass, int inputIdx, String[][] bindingNames) {
		this.xslt = xslt;
		this.returnClass = returnClass;
		this.inputClass = inputClass;
		this.inputIdx = inputIdx;
		this.bindingNames = bindingNames;
	}

	public Object intercept(ObjectMessage message) throws Exception {
		Object target = message.getTarget();
		Resource self = ((RDFObject) target).getResource();
		Object[] args = message.getParameters();
		TransformBuilder tb = transform(inputIdx < 0 ? null : args[inputIdx],
				inputClass);
		tb = tb.with("this", self.stringValue());
		for (int i = 0; i < bindingNames.length; i++) {
			for (String name : bindingNames[i]) {
				tb = with(tb, name, args[i]);
			}
		}
		return as(tb, returnClass);
	}

	private TransformBuilder transform(Object input, Class<?> cls)
			throws TransformerException, IOException,
			ParserConfigurationException {
		if (File.class.equals(cls))
			return xslt.transform((File) input);
		if (URL.class.equals(cls))
			return xslt.transform((URL) input);

		if (String.class.equals(cls))
			return xslt.transform((String) input);
		if (CharSequence.class.equals(cls))
			return xslt.transform((CharSequence) input);
		if (Readable.class.equals(cls))
			return xslt.transform((Readable) input);
		if (Reader.class.equals(cls))
			return xslt.transform((Reader) input);
		if (ByteArrayOutputStream.class.equals(cls))
			return xslt.transform((ByteArrayOutputStream) input);
		if (byte[].class.equals(cls))
			return xslt.transform((byte[]) input);
		if (ReadableByteChannel.class.equals(cls))
			return xslt.transform((ReadableByteChannel) input);
		if (InputStream.class.equals(cls))
			return xslt.transform((InputStream) input);
		if (XMLEventReader.class.equals(cls))
			return xslt.transform((XMLEventReader) input);

		if (Document.class.equals(cls))
			return xslt.transform((Document) input);
		if (DocumentFragment.class.equals(cls))
			return xslt.transform((DocumentFragment) input);
		if (Element.class.equals(cls))
			return xslt.transform((Element) input);
		if (Node.class.equals(cls))
			return xslt.transform((Node) input);

		if (GraphQueryResult.class.equals(cls))
			return xslt.transform((GraphQueryResult) input);
		if (TupleQueryResult.class.equals(cls))
			return xslt.transform((TupleQueryResult) input);
		if (Boolean.class.equals(cls))
			return xslt.transform((Boolean) input);
		if (input == null)
			return xslt.transform();
		if (input instanceof RDFObject)
			return xslt.transform((RDFObject) input);
		throw new IllegalArgumentException("Unknown input type: "
				+ cls.getName());
	}

	private TransformBuilder with(TransformBuilder tb, String name, Object arg)
			throws TransformerException {
		if (arg instanceof Value)
			return tb.with(name, ((Value) arg).stringValue());
		if (arg instanceof RDFObject)
			return tb.with(name, ((RDFObject) arg).getResource().stringValue());
		return tb.with(name, arg);
	}

	private Object as(TransformBuilder result, Class<?> rclass)
			throws TransformerException {

		if (Document.class.equals(rclass)) {
			return result.asDocument();
		} else if (DocumentFragment.class.equals(rclass)) {
			return result.asDocumentFragment();
		} else if (Element.class.equals(rclass)) {
			return result.asElement();
		} else if (Node.class.equals(rclass)) {
			return result.asNode();

		} else if (byte[].class.equals(rclass)) {
			return result.asByteArray();
		} else if (CharSequence.class.equals(rclass)) {
			return result.asCharSequence();
		} else if (Readable.class.equals(rclass)) {
			return result.asReadable();
		} else if (String.class.equals(rclass)) {
			return result.asString();

		} else if (Reader.class.equals(rclass)) {
			return result.asReader();
		} else if (CharArrayWriter.class.equals(rclass)) {
			return result.asCharArrayWriter();
		} else if (ByteArrayOutputStream.class.equals(rclass)) {
			return result.asByteArrayOutputStream();
		} else if (ReadableByteChannel.class.equals(rclass)) {
			return result.asReadableByteChannel();
		} else if (InputStream.class.equals(rclass)) {
			return result.asInputStream();
		} else if (XMLEventReader.class.equals(rclass)) {
			return result.asXMLEventReader();

		} else {
			return result.asObject();
		}
	}

}
