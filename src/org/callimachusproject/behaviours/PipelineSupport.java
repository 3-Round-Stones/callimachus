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
