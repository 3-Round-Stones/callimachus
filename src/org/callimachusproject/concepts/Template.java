/*
 * Copyright (c) 2009-2010, Zepheira LLC and James Leigh Some rights reserved.
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
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.stream.TriplePatternStore;
import org.openrdf.repository.object.annotations.iri;

/**
 * Interface for all RDFa Templates.
 * 
 * @author James Leigh
 *
 */
@iri("http://callimachusproject.org/rdf/2009/framework#Template")
public interface Template {

	/**
	 * Populates the page with the properties of the target resource.
	 */
	String calliConstructHTML(Object target) throws Exception;

	/**
	 * Populates the page with the properties of the target resource.
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#ConstructHTML")
	String calliConstructHTML(
			@iri("http://callimachusproject.org/rdf/2009/framework#ConstructHTML-object") Object target,
			@iri("http://callimachusproject.org/rdf/2009/framework#ConstructHTML-query") String query)
			throws Exception;

	/**
	 * Populates the page with the properties of the target resource.
	 */
	InputStream calliConstruct(Object target, String query) throws Exception;

	/**
	 * Returns only the primary patterns as for the given subject.
	 * @param about the subject to filter the pattern with.
	 */
	RDFEventReader openBoundedPatterns(String query, String about)
			throws XMLStreamException, IOException, TransformerException;

	/**
	 * Reads the template as a graph pattern.
	 */
	RDFEventReader openPatternReader(String query, String element,
			String about) throws XMLStreamException, IOException,
			TransformerException;

	/**
	 * Reads only the patterns for the object of the given pattern.
	 */
	RDFEventReader constructPossibleTriples(TriplePatternStore patterns,
			TriplePattern pattern);
}
