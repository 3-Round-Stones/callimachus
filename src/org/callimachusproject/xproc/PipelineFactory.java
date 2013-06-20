package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.http.client.HttpClient;
import org.xml.sax.SAXException;

public class PipelineFactory {

	public static PipelineFactory newInstance() {
		return new PipelineFactory();
	}

	private PipelineFactory() {
		super();
	}

	public Pipeline createPipeline(String systemId, HttpClient client) {
		return new Pipeline(systemId, client);
	}

	public Pipeline createPipeline(InputStream in, String systemId, HttpClient client)
			throws SAXException, IOException {
		return new Pipeline(in, systemId, client);
	}

	public Pipeline createPipeline(Reader reader, String systemId, HttpClient client)
			throws SAXException, IOException {
		return new Pipeline(reader, systemId, client);
	}

}
