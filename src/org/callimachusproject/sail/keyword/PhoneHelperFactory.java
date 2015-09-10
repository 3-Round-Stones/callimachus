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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the {@link PhoneHelper} configuration from Java resources.
 * 
 * @author James Leigh
 *
 */
public class PhoneHelperFactory {
	private final Logger logger = LoggerFactory.getLogger(PhoneHelperFactory.class);
	private final ClassLoader cl;

	public static PhoneHelperFactory newInstance() {
		return new PhoneHelperFactory(PhoneHelperFactory.class.getClassLoader());
	}

	protected PhoneHelperFactory(ClassLoader cl) {
		this.cl = cl;
	}

	public PhoneHelper createPhoneHelper() {
		Set<String> linking = readSet("META-INF/org.callimachusproject.sail.keyword.linking");
		Set<String> suffix = readSet("META-INF/org.callimachusproject.sail.keyword.suffix");
		Set<Character> punctuation = readCharacter("META-INF/org.callimachusproject.sail.keyword.punctuation");
		Map<Character, Character> substitutes = readInverseMap("META-INF/org.callimachusproject.sail.keyword.substitutions");
		Map<Character, Set<Character>> substitutable = readMap("META-INF/org.callimachusproject.sail.keyword.substitutions");
		return new PhoneHelper(linking, suffix, punctuation, substitutes, substitutable);
	}

	private Map<Character, Character> readInverseMap(String name) {
		Set<String> lines = readSet(name);
		Map<Character, Character> map = new HashMap<Character, Character>();
		for (String line : lines) {
			String[] split = line.split("\\s+=\\s+", 2);
			for (char chr : split[1].toCharArray()) {
				map.put(chr, split[0].charAt(0));
			}
		}
		return map;
	}

	private Map<Character, Set<Character>> readMap(String name) {
		Set<String> lines = readSet(name);
		Map<Character, Set<Character>> map = new HashMap<Character, Set<Character>>();
		for (String line : lines) {
			String[] split = line.split("\\s+=\\s+", 2);
			char key = split[0].charAt(0);
			HashSet<Character> value = new HashSet<Character>();
			for (char chr : split[1].toCharArray()) {
				value.add(chr);
			}
			map.put(key, value);
		}
		return map;
	}

	private Set<Character> readCharacter(String name) {
		Set<String> strings = readSet(name);
		HashSet<Character> set = new HashSet<Character>();
		for (String str : strings) {
			set.add(str.charAt(0));
		}
		return set;
	}

	private Set<String> readSet(String name) {
		HashSet<String> set = new HashSet<String>();
		Enumeration<URL> resources = getResources(name);
		while (resources.hasMoreElements()) {
			try {
				InputStream in = resources.nextElement().openStream();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in, "UTF-8"));
				try {
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.length() > 0) {
							set.add(line);
						}
					}
				} finally {
					reader.close();
				}
			} catch (IOException e) {
				logger.error(e.toString(), e);
			}
		}
		return set;
	}

	private Enumeration<URL> getResources(String name) {
		try {
			return cl.getResources(name);
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return new Vector<URL>().elements();
		}
	}
}
