package org.callimachusproject.xproc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.callimachusproject.annotations.type;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcException;

public class XprocAdvice implements Advice {
	private final FluidBuilder fb;
	private final Pipeline pipeline;
	private final Method method;
	private final String[] bindingNames;
	private final int source;

	public XprocAdvice(Pipeline pipeline, Method method, String[] bindingNames,
			int source) {
		assert source < method.getParameterTypes().length;
		assert bindingNames.length == method.getParameterTypes().length;
		this.fb = FluidFactory.getInstance().builder();
		this.pipeline = pipeline;
		this.method = method;
		this.bindingNames = bindingNames;
		this.source = source;
	}

	@Override
	public Object intercept(ObjectMessage message) throws IOException, SAXException, XProcException {
		PipelineBuilder pb = createPipeline(message.getParameters());
		setParameters(pb, message.getParameters());
		Type gtype = method.getGenericReturnType();
		String[] media = getMediaTypes(method.getAnnotations());
		return pb.as(gtype, media);
	}

	private PipelineBuilder createPipeline(Object[] parameters) throws XProcException, SAXException, IOException {
		if (source < 0)
			return pipeline.pipe();
		assert parameters.length > source;
		Type type = method.getGenericParameterTypes()[source];
		String[] media = getMediaTypes(method.getParameterAnnotations()[source]);
		return pipeline.pipe(parameters[source], type, media);
	}

	private void setParameters(PipelineBuilder pb, Object[] parameters) throws IOException, XProcException {
		assert bindingNames.length <= parameters.length;
		for (int i=0; i<bindingNames.length; i++) {
			Type gtype = method.getGenericParameterTypes()[i];
			String[] media = getMediaTypes(method.getParameterAnnotations()[i]);
			String value = asString(parameters[i], gtype, media);
			pb.passOption(bindingNames[i], value);
		}
	}

	private String asString(Object value, Type gtype,
			String[] media) throws IOException, XProcException {
		try {
			return fb.consume(value, null, gtype, media).asString();
		} catch (FluidException e) {
			throw new XProcException(e);
		}
	}

	private String[] getMediaTypes(Annotation[] annotations) {
		for (Annotation ann : annotations) {
			if (ann instanceof type)
				return ((type) ann).value();
		}
		return null;
	}

}
