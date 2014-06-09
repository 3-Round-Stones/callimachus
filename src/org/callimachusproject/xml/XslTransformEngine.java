package org.callimachusproject.xml;

import java.io.InputStream;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;

import org.callimachusproject.client.HttpUriClient;

public class XslTransformEngine {
	private final String baseURI;
	private final HttpUriClient client;

	public XslTransformEngine(String baseURI, HttpUriClient client) {
		this.baseURI = baseURI;
		this.client = client;
	}

	public XslTransformer compile(InputStream queryStream) throws SaxonApiException {
		Processor processor = new Processor(false);
		XdmNodeFactory resolver = new XdmNodeFactory(processor, client);
		XsltCompiler compiler = processor.newXsltCompiler();
        compiler.setSchemaAware(false);
        compiler.setURIResolver(resolver);
        XsltExecutable exec = compiler.compile(new StreamSource(queryStream, baseURI));
        return new XslTransformer(exec, resolver);
	}

}
