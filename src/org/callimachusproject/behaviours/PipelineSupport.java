package org.callimachusproject.behaviours;

import java.io.IOException;
import java.io.InputStream;

import javax.tools.FileObject;
import javax.xml.parsers.ParserConfigurationException;

import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.xproc.Pipe;
import org.callimachusproject.xproc.Pipeline;
import org.callimachusproject.xproc.PipelineFactory;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcException;

public abstract class PipelineSupport implements CalliObject, FileObject {

	public Pipe pipe() throws SAXException, IOException, XProcException,
			ParserConfigurationException, OpenRDFException {
		PipelineFactory factory = PipelineFactory.newInstance();
		Pipeline pipeline = factory.createPipeline(this.openInputStream(),
				this.toString(), this.getHttpClient());
		return pipeline.pipe();
	}

	public Pipe pipeStreamOf(InputStream source, String systemId, String media)
			throws SAXException, IOException, XProcException,
			ParserConfigurationException, OpenRDFException {
		PipelineFactory factory = PipelineFactory.newInstance();
		Pipeline pipeline = factory.createPipeline(this.openInputStream(),
				this.toString(), this.getHttpClient());
		return pipeline.pipeStreamOf(source, systemId, media);
	}
}
