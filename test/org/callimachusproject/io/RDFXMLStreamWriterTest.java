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
package org.callimachusproject.io;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URISyntaxException;

import javax.xml.stream.XMLStreamException;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.rdfxml.RDFXMLParserFactory;

public class RDFXMLStreamWriterTest extends RDFWriterTestCase {

	public RDFXMLStreamWriterTest() {
		super(new RDFWriterFactory() {
			public RDFFormat getRDFFormat() {
				return RDFFormat.RDFXML;
			}

			public RDFWriter getWriter(OutputStream out) {
				try {
					return new RDFXMLStreamWriterFactory().createWriter(out,
							"http://example.com/");
				} catch (XMLStreamException e) {
					throw new UndeclaredThrowableException(e);
				} catch (URISyntaxException e) {
					throw new UndeclaredThrowableException(e);
				}
			}

			public RDFWriter getWriter(Writer writer) {
				try {
					return new RDFXMLStreamWriterFactory().createWriter(writer,
							"http://example.com/");
				} catch (XMLStreamException e) {
					throw new UndeclaredThrowableException(e);
				} catch (URISyntaxException e) {
					throw new UndeclaredThrowableException(e);
				}
			}
		}, new RDFXMLParserFactory());
	}

}
