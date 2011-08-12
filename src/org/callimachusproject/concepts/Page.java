/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some
   Rights Reserved 
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved 
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
package org.callimachusproject.concepts;

import java.io.IOException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.rdfa.RDFEventReader;
import org.openrdf.repository.object.annotations.iri;

/**
 * Interface for all RDFa page templates.
 * 
 * @author James Leigh
 *
 */
@iri("http://callimachusproject.org/rdf/2009/framework#Page")
public interface Page {

	/**
	 * Populates the page with the properties of the target resource.
	 */
	String calliConstructHTML(Object target) throws Exception;

	/**
	 * Populates the page with the properties of the target resource.
	 */
	String calliConstructHTML(Object target, String query) throws Exception;

	/**
	 * Populates the page with the properties of the target resource.
	 */
	XMLEventReader calliConstruct(Object target, String query) throws Exception;

	/**
	 * Reads the template as a graph pattern.
	 */
	RDFEventReader openPatternReader(String about, String query,
			String element) throws XMLStreamException, IOException,
			TransformerException;
		
}
