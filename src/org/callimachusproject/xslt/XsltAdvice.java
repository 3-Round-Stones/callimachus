package org.callimachusproject.xslt;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.annotations.type;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;

public class XsltAdvice implements Advice {
	private final XSLTransformer xslt;
	private final Type returnClass;
	private final Type inputClass;
	private final int inputIdx;
	private final String[][] bindingNames;

	public XsltAdvice(XSLTransformer xslt, Type returnClass,
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
		String[] returnMedia = new String[0];
		type returnType = message.getMethod().getAnnotation(type.class);
		if (returnType != null) {
			returnMedia = returnType.value();
		}
		Object[] args = message.getParameters();
		assert args.length == ptypes.length;
		TransformBuilder tb = transform(inputIdx < 0 ? null : args[inputIdx],
				inputClass, mediaTypes[inputIdx]);
		tb = tb.with("this", self.stringValue());
		for (int i = 0; i < bindingNames.length; i++) {
			for (String name : bindingNames[i]) {
				tb = with(tb, name, args[i], ptypes[i], mediaTypes[i]);
			}
		}
		return as(tb, returnClass, returnMedia);
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

	private TransformBuilder transform(Object input, Type cls, String... media)
			throws TransformerException, IOException,
			ParserConfigurationException {
		if (cls == null || Object.class.equals(cls) && input == null)
			return xslt.transform();
		return xslt.transform(input, null, cls, media);
	}

	private TransformBuilder with(TransformBuilder tb, String name, Object arg, Type type, String... media)
			throws TransformerException, IOException {
		if (arg instanceof Value)
			return tb.with(name, ((Value) arg).stringValue());
		if (arg instanceof RDFObject)
			return tb.with(name, ((RDFObject) arg).getResource().stringValue());
		return tb.with(name, arg, type, media);
	}

	private Object as(TransformBuilder result, Type rclass, String... media)
			throws TransformerException, IOException {
		return result.as(rclass, media);
	}

}
