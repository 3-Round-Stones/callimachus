/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

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

import java.util.Set;

import org.openrdf.repository.object.annotations.iri;

/**
 * Interface for resolving label into the calli:soundex triples.
 * 
 * @author James Leigh
 *
 */
public interface SoundexTrait {
	public static String SOUNDEX = "http://callimachusproject.org/rdf/2009/framework#soundex";
	public static final String[] LABELS = {
			"http://www.w3.org/2000/01/rdf-schema#label",
			"http://www.w3.org/2004/02/skos/core#prefLabel",
			"http://www.w3.org/2004/02/skos/core#altLabel",
			"http://www.w3.org/2004/02/skos/core#hiddenLabel",
			"http://www.w3.org/2008/05/skos-xl#literalForm"
	};

	@iri(SOUNDEX)
	Set<String> getSoundexes();

	@iri(SOUNDEX)
	void setSoundexes(Set<String> soundexes);

	@iri("http://callimachusproject.org/rdf/2009/framework#asSoundex")
	String asSoundex(String label);

	@iri("http://callimachusproject.org/rdf/2009/framework#regexStartsWith")
	String regexStartsWith(String string);
}
