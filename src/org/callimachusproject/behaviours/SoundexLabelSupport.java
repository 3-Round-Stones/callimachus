/*
   Copyright (c) 2009-2010 Zepheira LLC, Some rights reserved

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
package org.callimachusproject.behaviours;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.language.Soundex;
import org.callimachusproject.traits.SoundexTrait;
import org.openrdf.repository.object.annotations.triggeredBy;

public abstract class SoundexLabelSupport implements SoundexTrait {
	private static Soundex soundex = new Soundex();

	public String asSoundex(String label) {
		if (label == null || label.length() == 0)
			return "";
		String ex = soundex.encode(label);
		if (ex == null || ex.length() < 3)
			return "";
		return ex.substring(0, 3) + "0";
	}

	public String regexStartsWith(String string) {
		if (string == null)
			return "^";
		return "^" + string.replaceAll("[^a-zA-Z0-9\\s]", ".");
	}

	@triggeredBy( { "http://www.w3.org/2000/01/rdf-schema#label",
			"http://www.w3.org/2004/02/skos/core#prefLabel",
			"http://www.w3.org/2004/02/skos/core#altLabel",
			"http://www.w3.org/2004/02/skos/core#hiddenLabel" })
	public void addSoundexForLabel(String label) {
		Set<String> phones = new HashSet<String>();
		String previous = null;
		int same = 0;
		for (int i = 1; i <= label.length(); i++) {
			String prefix = label.substring(0, i);
			String phone = asSoundex(prefix);
			if (phone.equals(previous)) {
				same++;
			} else {
				same = 0;
			}
			if (same > 3)
				break;
			previous = phone;
			phones.add(phone);
		}
		getSoundexes().addAll(phones);
	}
}
