/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some Rights Reserved
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
package org.callimachusproject.behaviours;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.language.Soundex;
import org.callimachusproject.traits.SoundexTrait;
import org.openrdf.repository.object.annotations.triggeredBy;

/**
 * Inserts calli:soundex triples for every label that is inserted and provides
 * methods to help lookup the labels.
 * 
 * @author James Leigh
 * 
 */
public abstract class SoundexLabelSupport implements SoundexTrait {
	private static Soundex soundex = new Soundex();

	public String asSoundex(String label) {
		if (label == null || label.length() == 0)
			return "_000";
		String ex = soundex.encode(clean(label));
		if (ex == null || ex.length() < 3)
			return "_000";
		return ex.substring(0, 3) + "0";
	}

	public String regexStartsWith(String string) {
		if (string == null)
			return "^";
		return "^" + string.replaceAll("[^a-zA-Z0-9\\s]", ".");
	}

	@triggeredBy( // see SoundexTrait.LABELS
	{ "http://www.w3.org/2000/01/rdf-schema#label",
			"http://xmlns.com/foaf/0.1/name",
			"http://www.w3.org/2004/02/skos/core#prefLabel",
			"http://www.w3.org/2004/02/skos/core#altLabel",
			"http://www.w3.org/2004/02/skos/core#hiddenLabel",
			"http://www.w3.org/2008/05/skos-xl#literalForm" })
	public void addSoundexForLabel(String label) {
		String clean = clean(label);
		String full = soundex.encode(clean);
		if (full == null || full.length() < 3) {
			getSoundexes().add("_000"); // no soundex
			return;
		}
		full = full.substring(0, 3);
		Set<String> phones = new HashSet<String>();
		int same = 0;
		String previous = null;
		for (int i = 1, n = clean.length(); i <= n; i++) {
			String prefix = clean.substring(0, i);
			String phone = soundex.encode(prefix).substring(0, 3);
			if (phone.equals(previous)) {
				same++;
			} else {
				same = 0;
				previous = phone;
				phones.add(phone + '0');
			}
			if (same > 3 && previous.equals(full))
				break;
		}
		getSoundexes().addAll(phones);
	}

	/**
	 * This removes non-english letters.
	 */
	private String clean(String str) {
		if (str == null || str.length() == 0)
			return str;
		int len = str.length();
		char[] chars = new char[len];
		int count = 0;
		for (int i = 0; i < len; i++) {
			char ch = str.charAt(i);
			if ('A' <= ch && ch <= 'Z') {
				chars[count++] = ch;
			} else if ('a' <= ch && ch <= 'z') {
				chars[count++] = Character.toUpperCase(ch);
			}
		}
		if (count == len)
			return str.toUpperCase(java.util.Locale.ENGLISH);
		return new String(chars, 0, count);
	}
}
