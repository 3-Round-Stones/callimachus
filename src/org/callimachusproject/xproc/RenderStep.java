/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.xproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;

import javax.xml.stream.XMLEventReader;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;

import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.engine.TemplateException;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.openrdf.query.TupleQueryResult;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcStep;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;

public class RenderStep implements XProcStep {
	private static final FluidFactory FF = FluidFactory.getInstance();
	private final TemplateEngine engine;
	private final XProcRuntime runtime;
	private final XAtomicStep step;
	private ReadablePipe sourcePipe = null;
	private ReadablePipe templatePipe = null;
	private WritablePipe resultPipe = null;
	private String outputBase;
	private final FluidBuilder fb = FF.builder();

	public RenderStep(XProcRuntime runtime, XAtomicStep step) {
		this.runtime = runtime;
		this.step = step;
		URI docId = step.getNode().getDocumentURI();
		assert docId != null;
		engine = TemplateEngine.newInstance(runtime.getHttpClient());
	}

	@Override
	public void setParameter(QName name, RuntimeValue value) {
        throw new XProcException("No parameters allowed.");
	}

	@Override
	public void setParameter(String port, QName name, RuntimeValue value) {
		setParameter(name, value);
	}

	@Override
	public void setOption(QName name, RuntimeValue value) {
		if ("output-base-uri".equals(name.getClarkName())) {
			outputBase = value.getString();
		}
	}

	public void setInput(String port, ReadablePipe pipe) {
		if ("source".equals(port)) {
			sourcePipe = pipe;
		} else {
			templatePipe = pipe;
		}
	}

	public void setOutput(String port, WritablePipe pipe) {
		if ("result".equals(port)) {
			resultPipe = pipe;
		} else {
			throw new XProcException("No other outputs allowed.");
		}
	}

	public void reset() {
		sourcePipe.resetReader();
		templatePipe.resetReader();
		resultPipe.resetWriter();
	}

	public void run() throws SaxonApiException {
		if (templatePipe == null || !templatePipe.moreDocuments()) {
			throw XProcException.dynamicError(6, step.getNode(),
					"No template provided.");
		}
		if (sourcePipe == null) {
			throw XProcException.dynamicError(6, step.getNode(),
					"No source provided.");
		}
		try {
			XdmNode template = templatePipe.read();
			while (sourcePipe.moreDocuments()) {
				XdmNode xml = render(template, sourcePipe.read());
				if (resultPipe != null && xml != null) {
					resultPipe.write(xml);
				}
			}
		} catch (SaxonApiException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (FluidException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (IOException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (TemplateException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		}
	}

	public XdmNode render(XdmNode t, XdmNode s) throws SaxonApiException,
			IOException, FluidException, TemplateException {
		String tempId = t.getBaseURI().toASCIIString();
		Reader template = asReader(t);
		Template tem = engine.getTemplate(template, tempId);
		TupleQueryResult source = asTupleQueryResult(s);
		Reader result = asReader(tem.render(source), resolve(outputBase));

		DocumentBuilder xdmBuilder = newDocumentBuilder();
		XdmNode xformed = xdmBuilder.build(new StreamSource(result));

		if (xformed != null
				&& outputBase != null && outputBase.length() > 0
				&& (xformed.getBaseURI() == null || "".equals(xformed
						.getBaseURI().toASCIIString()))) {
			xformed.getUnderlyingNode().setSystemId(resolve(outputBase));
		}
		return xformed;
	}

	private String resolve(String href) {
		String base = step.getNode().getBaseURI().toASCIIString();
		if (href == null)
			return base;
		return TermFactory.newInstance(base).resolve(href);
	}

	private Reader asReader(XdmNode document) throws SaxonApiException,
			IOException, FluidException {
		String sysId = document.getBaseURI().toASCIIString();
		StringWriter sw = new StringWriter();
		Serializer serializer = new Serializer();
		serializer.setOutputWriter(sw);
		S9apiUtils.serialize(runtime, document, serializer);
		return fb
				.consume(sw.getBuffer(), sysId, CharSequence.class, "text/xml")
				.asReader();
	}

	private TupleQueryResult asTupleQueryResult(XdmNode document)
			throws SaxonApiException, IOException, FluidException {
		String sysId = document.getBaseURI().toASCIIString();
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		Serializer serializer = new Serializer();
		serializer.setOutputStream(s);
		S9apiUtils.serialize(runtime, document, serializer);
		String media = "application/sparql-results+xml";
		Fluid fluid = fb.consume(s, sysId, ByteArrayOutputStream.class, media);
		return (TupleQueryResult) fluid.as(TupleQueryResult.class, media);
	}

	private Reader asReader(XMLEventReader xml, String base)
			throws IOException, FluidException {
		Fluid fluid = fb.consume(xml, base, XMLEventReader.class,
				"application/xml");
		return fluid.asReader();
	}

	private DocumentBuilder newDocumentBuilder() {
		return runtime.getConfiguration().getProcessor().newDocumentBuilder();
	}
}
