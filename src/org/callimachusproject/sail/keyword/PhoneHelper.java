/*
 * Copyright (c) 2011, 3 Round Stones Inc. Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.sail.keyword;

import static java.lang.Character.isWhitespace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.language.Soundex;

/**
 * Cleans up input string before generating soundex and regex for matching
 * keywords.
 * 
 * @author James Leigh
 * 
 */
public class PhoneHelper {
	private static final String NO_SOUNDEX = "_000";
	private final Set<String> linking;
	private final Set<String> suffix;
	private final Set<Character> punctuation;
	private final Map<Character, Character> substitutes;
	private final Map<Character, Set<Character>> substitutable;
	private Soundex soundex = new Soundex();

	protected PhoneHelper(Set<String> linking, Set<String> suffix,
			Set<Character> punctuation, Map<Character, Character> substitutes,
			Map<Character, Set<Character>> substitutable) {
		assert linking != null;
		assert suffix != null;
		assert punctuation != null;
		assert substitutes != null;
		assert substitutable != null;
		this.linking = linking;
		this.suffix = suffix;
		this.punctuation = punctuation;
		this.substitutes = substitutes;
		this.substitutable = substitutable;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + linking.hashCode();
		result = prime * result + punctuation.hashCode();
		result = prime * result + substitutes.hashCode();
		result = prime * result + suffix.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PhoneHelper other = (PhoneHelper) obj;
		if (!linking.equals(other.linking))
			return false;
		if (!punctuation.equals(other.punctuation))
			return false;
		if (!substitutable.equals(other.substitutable))
			return false;
		if (!substitutes.equals(other.substitutes))
			return false;
		if (!suffix.equals(other.suffix))
			return false;
		return true;
	}

	/**
	 * The soundex of every word in the input together with the soundex of the
	 * input from the begining of each word.
	 * 
	 * @param input
	 * @return soundex of each keyword of the input
	 */
	public Set<String> phones(String input) {
		String clean = clean(input);
		Set<String> phones = new HashSet<String>();
		int start = 0;
		do {
			int end = clean.indexOf(' ', start + 1);
			if (end > start + 1) {
				// not one letter word
				String word = clean.substring(start, end);
				if (start > 0 && linking.contains(word))
					continue; // skip linking words
				phones.add(encode(word));
			}
			String word = clean.substring(start);
			if (start > 0 && linking.contains(word))
				continue; // skip linking words
			phones.add(encode(word));
			start = end;
		} while (start > 0 && (start == 0 || start < clean.length() - 2));
		if (phones.isEmpty()) {
			phones.add(NO_SOUNDEX);
		}
		return phones;
	}

	/**
	 * Soundex of the entire input.
	 * 
	 * @param input
	 * @return a letter or '_' followed by three numbers.
	 */
	public String soundex(String input) {
		String word = clean(input);
		String encode = encode(word);
		if (encode != null && encode.length() > 0) {
			return encode;
		} else {
			return NO_SOUNDEX;
		}
	}

	/**
	 * Converts the input into a regex. Punctuation and word separators are wild
	 * and accents are tolerated.
	 * 
	 * @param word
	 *            input string to generate a regex from
	 * @return a regular expression
	 */
	public String regex(String word) {
		String str = clean(word);
		if (str == null || str.length() == 0)
			return "";
		StringBuilder sb = new StringBuilder(str.length());
		for (int i = 0, n = str.length(); i < n; i++) {
			char ch = str.charAt(i);
			char l = Character.toLowerCase(ch);
			if (substitutable.containsKey(ch)) {
				sb.append('[').append(ch).append(l);
				for (Character a : substitutable.get(ch)) {
					sb.append(a);
				}
				sb.append("]\\p{M}*");
			} else if ('A' <= ch && ch <= 'Z') {
				sb.append('[').append(ch).append(l).append("]\\p{M}*");
			} else if ('0' <= ch && ch <= '9') {
				sb.append(ch);
			} else if (' ' == ch) {
				sb.append(".*");
			} else {
				sb.append(".").append("\\p{M}*");
			}
		}
		return sb.toString();

	}

	/**
	 * This removes non-english letters.
	 */
	private String clean(String str) {
		if (str == null || str.length() == 0)
			return "";
		List<String> list = new ArrayList<String>();
		StringBuilder sb = new StringBuilder(str.length());
		for (int i = 0, n = str.length(); i < n; i++) {
			char ch = Character.toUpperCase(str.charAt(i));
			if ('A' <= ch && ch <= 'Z' || '0' <= ch && ch <= '9') {
				sb.append(ch);
			} else if (substitutes.containsKey(ch)) {
				sb.append(substitutes.get(ch));
			} else if (isWhitespace(ch) || punctuation.contains(ch)) {
				trimSuffix(sb);
				if (sb.length() > 0) {
					list.add(sb.toString());
				}
				sb.setLength(0);
			} else {
				sb.append('_');
			}
		}
		trimSuffix(sb);
		if (sb.length() > 0) {
			list.add(sb.toString());
		}
		if (list.isEmpty())
			return "";
		if (list.size() == 1)
			return list.get(0);
		sb.setLength(0);
		for (String word : list) {
			sb.append(word);
			sb.append(' ');
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	private boolean trimSuffix(StringBuilder sb) {
		if (sb.length() > 3) {
			String last4 = sb.substring(sb.length() - 4, sb.length());
			String last3 = last4.substring(1);
			String last2 = last3.substring(1);
			String last1 = last2.substring(1);
			if (sb.length() > 4
					&& last4.indexOf(sb.charAt(sb.length() - 5)) < 0
					&& suffix.contains(last4)) {
				sb.setLength(sb.length() - 4);
				return true;
			}
			if (last3.indexOf(sb.charAt(sb.length() - 4)) < 0
					&& suffix.contains(last3)) {
				sb.setLength(sb.length() - 3);
				return true;
			}
			if (last2.indexOf(sb.charAt(sb.length() - 3)) < 0
					&& suffix.contains(last2)) {
				sb.setLength(sb.length() - 2);
				return true;
			}
			if (last1.indexOf(sb.charAt(sb.length() - 2)) < 0
					&& suffix.contains(last1)) {
				sb.setLength(sb.length() - 1);
				return true;
			}
		}
		return false;
	}

	private String encode(String word) {
		String encoded = soundex.encode(word);
		if (encoded != null && encoded.length() > 0)
			return encoded;
		return NO_SOUNDEX;
	}

}
