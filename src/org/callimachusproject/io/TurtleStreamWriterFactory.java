/*
   Copyright (c) 2013 3 Round Stones Inc, Some Rights Reserved

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
package org.callimachusproject.io;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import javax.xml.stream.XMLStreamException;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;

public class TurtleStreamWriterFactory {

	public RDFFormat getRDFFormat() {
		return RDFFormat.TURTLE;
	}

	public RDFWriter createWriter(OutputStream out, String systemId) throws XMLStreamException, URISyntaxException {
		OutputStreamWriter writer = new OutputStreamWriter(out, Charset.forName("UTF-8"));
		return createWriter(writer, systemId);
	}

	public RDFWriter createWriter(Writer writer, String systemId) throws XMLStreamException, URISyntaxException {
		return new ArrangedWriter(new TurtleStreamWriter(writer, systemId));
	}

}
