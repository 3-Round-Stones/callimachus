/*
   Copyright 2009 Zepheira LLC

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
package org.callimachusproject.traits;

import java.io.IOException;
import java.io.Reader;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.stream.TriplePatternStore;
import org.openrdf.repository.object.RDFObject;

public interface Template {

	/**
	 * Populates the page with the properties of the target resource.
	 */
	Reader calliConstruct(String mode, RDFObject target) throws Exception;

	RDFEventReader openBoundedPatterns(String mode, String about)
			throws XMLStreamException, IOException, TransformerException;

	TriplePatternStore readPatternStore(String mode, String element,
			String about) throws XMLStreamException, IOException,
			TransformerException, RDFParseException;

	RDFEventReader constructPossibleTriples(TriplePatternStore patterns,
			TriplePattern pattern);
}
