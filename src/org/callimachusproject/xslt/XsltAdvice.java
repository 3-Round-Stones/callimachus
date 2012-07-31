package org.callimachusproject.xslt;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.transform.TransformerException;

import org.callimachusproject.annotations.type;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
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
	private final Type inputClass;
	private final int inputIdx;
	private final String[][] bindingNames;

	public XsltAdvice(XSLTransformer xslt, Class<?> returnClass,
			Type inputClass, int inputIdx, String[][] bindingNames) {
		this.xslt = xslt;
		this.returnClass = returnClass;
		this.inputClass = inputClass;
		this.inputIdx = inputIdx;
		this.bindingNames = bindingNames;
	}

	@Override
	public String toString() {
		return xslt.toString();
	}

	public Object intercept(ObjectMessage message) throws Exception {
		Object target = message.getTarget();
		Resource self = ((RDFObject) target).getResource();
		Type[] ptypes = message.getMethod().getGenericParameterTypes();
		String[][] mediaTypes = getMediaTypes(message.getMethod().getParameterAnnotations());
		Object[] args = message.getParameters();
		assert args.length == ptypes.length;
		TransformBuilder tb = transform(inputIdx < 0 ? null : args[inputIdx],
				inputClass);
		tb = tb.with("this", self.stringValue());
		for (int i = 0; i < bindingNames.length; i++) {
			for (String name : bindingNames[i]) {
				tb = with(tb, name, args[i], ptypes[i], mediaTypes[i]);
			}
		}
		return as(tb, returnClass);
	}

	private String[][] getMediaTypes(Annotation[][] anns) {
		String[][] result = new String[anns.length][];
		for (int i=0; i<anns.length; i++) {
			for (int j=0; j<anns[i].length; j++) {
				if (anns[i][j] instanceof type) {
					result[i] = ((type) anns[i][j]).value();
					break;
				}
			}
		}
		return result;
	}

	private TransformBuilder transform(Object input, Type cls)
			throws TransformerException, IOException,
			ParserConfigurationException {
		if (cls == null || Object.class.equals(cls) && input == null)
			return xslt.transform();
		return xslt.transform(input, null, cls);
	}

	private TransformBuilder with(TransformBuilder tb, String name, Object arg, Type type, String... media)
			throws TransformerException, IOException {
		if (arg instanceof Value)
			return tb.with(name, ((Value) arg).stringValue());
		if (arg instanceof RDFObject)
			return tb.with(name, ((RDFObject) arg).getResource().stringValue());
		return tb.with(name, arg, type, media);
	}

	private Object as(TransformBuilder result, Class<?> rclass)
			throws TransformerException, IOException {

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
