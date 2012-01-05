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
import java.util.Set;
import java.util.TreeSet;

/**
 * A persistent (file-base) map to store filenames.
 * 
 * @author James Leigh
 * 
 */
public class MultiValuedFileMap {
	private final boolean relative;
	private final File entriesDir;
	private final File entriesFile;

	public MultiValuedFileMap(File entriesDir) throws IOException {
		this(entriesDir, "entries.list", true);
	}

	public MultiValuedFileMap(File entriesDir, String list, boolean relative)
			throws IOException {
		this.relative = relative;
		this.entriesDir = entriesDir.getCanonicalFile();
		this.entriesDir.mkdirs();
		this.entriesFile = new File(this.entriesDir, list);
	}

	public synchronized Set<String> get(File file) throws IOException {
		Set<String> set = new TreeSet<String>();
		File valuesFile = getValuesFile(file);
		if (!valuesFile.exists())
			return set;
		String line;
		BufferedReader reader;
		reader = new BufferedReader(new FileReader(valuesFile));
		try {
			while ((line = reader.readLine()) != null) {
				set.add(line);
			}
		} finally {
			reader.close();
		}
		return set;
	}

	public synchronized void add(File file, String value) throws IOException {
		Set<String> entries = get(file);
		entries.add(value);
		put(file, entries);
	}

	public synchronized void addAll(File file, Set<String> values)
			throws IOException {
		Set<String> entries = get(file);
		entries.addAll(values);
		put(file, entries);
	}

	public synchronized void addAll(MultiValuedFileMap map) throws IOException {
		for (File key : map.keySet()) {
			addAll(key, map.get(key));
		}
	}

	public synchronized void put(File file, Set<String> values)
			throws IOException {
		PrintWriter writer;
		File valuesFile = getValuesFile(file);
		if (!valuesFile.exists()) {
			Set<String> set = getEntries();
			set.add(getEntryPath(file));
			writeEntries(set);
		}
		writer = new PrintWriter(new FileWriter(valuesFile));
		try {
			for (String line : values) {
				writer.println(line);
			}
		} finally {
			writer.close();
		}
	}

	public synchronized void remove(File file) throws IOException {
		Set<String> set = getEntries();
		set.remove(getEntryPath(file));
		writeEntries(set);
		getValuesFile(file).delete();
	}

	public Set<File> keySet() throws IOException {
		Set<File> set = new TreeSet<File>();
		if (entriesFile.exists()) {
			for (String path : getEntries()) {
				File abs = new File(path);
				if (abs.isAbsolute()) {
					set.add(abs.getCanonicalFile());
				} else {
					set.add(new File(entriesDir, path).getCanonicalFile());
				}
			}
		}
		return set;
	}

	private Set<String> getEntries() throws IOException {
		Set<String> set = new TreeSet<String>();
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

	private void writeEntries(Set<String> entries) throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(entriesFile));
		try {
			for (String path : entries) {
				writer.println(path);
			}
		} finally {
			writer.close();
		}
	}

	private File getValuesFile(File file) throws IOException {
		int code = getEntryPath(file).hashCode();
		String name = file.getName() + toHexString(code) + ".list";
		return new File(entriesDir, name);
	}

	private String getEntryPath(File file) throws IOException {
		return relative(file, entriesDir, relative);
	}

	private String relative(File dir, File base, boolean relative)
			throws IOException {
		String path = dir.getCanonicalPath();
		if (relative) {
			String working = base.getCanonicalPath() + File.separatorChar;
			if (path.startsWith(working))
				return path.substring(working.length());
			if (base.getParentFile() == null)
				return path;
			String up = relative(dir, base.getParentFile(), true);
			if (path.equals(up))
				return path;
			return ".." + File.separatorChar + up;
		}
		return path;
	}
}
