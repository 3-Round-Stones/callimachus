/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved

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
package org.callimachusproject.util;

import java.io.OutputStream;
import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;

public class RDFXMLStreamWriterFactory {

	public RDFFormat getRDFFormat() {
		return RDFFormat.RDFXML;
	}

	public RDFWriter createWriter(OutputStream out) throws XMLStreamException {
		XMLOutputFactory xf = XMLOutputFactory.newFactory();
		XMLStreamWriter writer = xf.createXMLStreamWriter(out);
		return new SortBySubjectWriter(new RDFXMLStreamWriter(writer));
	}

	public RDFWriter createWriter(Writer out) throws XMLStreamException {
		XMLOutputFactory xf = XMLOutputFactory.newFactory();
		XMLStreamWriter writer = xf.createXMLStreamWriter(out);
		return new SortBySubjectWriter(new RDFXMLStreamWriter(writer));
	}

	public RDFWriter createWriter(XMLStreamWriter writer) throws XMLStreamException {
		return new SortBySubjectWriter(new RDFXMLStreamWriter(writer));
	}

}
