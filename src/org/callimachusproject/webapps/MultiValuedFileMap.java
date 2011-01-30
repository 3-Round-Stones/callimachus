/*
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
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
package org.callimachusproject.webapps;

import static java.lang.Integer.toHexString;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * A persistent (file-base) map to store filenames.
 * 
 * @author James Leigh
 *
 */
public class MultiValuedFileMap {
	private File entriesDir;
	private File keys;

	public MultiValuedFileMap(File entriesDir) {
		this.entriesDir = entriesDir;
		entriesDir.mkdirs();
		this.keys = new File(entriesDir, "keys.list");
	}

	public synchronized Set<String> get(File file) throws IOException {
		File entriesFile = getEntriesFile(file);
		Set<String> set = new HashSet<String>();
		if (entriesFile.exists()) {
			String line;
			BufferedReader reader;
			reader = new BufferedReader(new FileReader(entriesFile));
			try {
				while ((line = reader.readLine()) != null) {
					set.add(line);
				}
			} finally {
				reader.close();
			}
		}
		return set;
	}

	public synchronized void add(File file, String value)
			throws IOException {
		Set<String> entries = get(file);
		entries.add(value);
		put(file, entries);
	}

	public synchronized void put(File file, Set<String> values)
			throws IOException {
		PrintWriter writer;
		File entriesFile = getEntriesFile(file);
		if (!entriesFile.exists()) {
			addKey(file);
		}
		writer = new PrintWriter(new FileWriter(entriesFile));
		try {
			for (String line : values) {
				writer.println(line);
			}
		} finally {
			writer.close();
		}
	}

	public synchronized void remove(File file) throws IOException {
		removeKey(file);
		getEntriesFile(file).delete();
	}

	public Set<File> keySet() throws IOException {
		Set<File> set = new HashSet<File>();
		if (keys.exists()) {
			String line;
			BufferedReader reader;
			reader = new BufferedReader(new FileReader(keys));
			try {
				while ((line = reader.readLine()) != null) {
					set.add(new File(line));
				}
			} finally {
				reader.close();
			}
		}
		return set;
	}

	private void addKey(File file) throws IOException {
		Set<File> set = keySet();
		set.add(file);
		PrintWriter writer = new PrintWriter(new FileWriter(keys));
		try {
			for (File key : set) {
				writer.println(key.getPath());
			}
		} finally {
			writer.close();
		}
	}

	private void removeKey(File file) throws IOException {
		Set<File> set = keySet();
		set.remove(file);
		PrintWriter writer = new PrintWriter(new FileWriter(keys));
		try {
			for (File key : set) {
				writer.println(key.getPath());
			}
		} finally {
			writer.close();
		}
	}

	private File getEntriesFile(File file) {
		int code = file.getPath().hashCode();
		String name = file.getName() + toHexString(code) + ".list";
		File entriesFile = new File(entriesDir, name);
		return entriesFile;
	}
}
