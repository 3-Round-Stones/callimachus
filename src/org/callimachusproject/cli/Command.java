/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Command {
	private final String name;
	private final Options options;
	private final String otherArgName;
	private final CommandLine parsed;
	private final ParseException exception;

	protected Command(String name, Options options, String otherArgName,
			CommandLine parsed) {
		this.name = name;
		this.options = options;
		this.otherArgName = otherArgName;
		this.parsed = parsed;
		this.exception = null;
	}

	public Command(String name, Options options, String otherArgName,
			CommandLine parsed, ParseException exception) {
		this.name = name;
		this.options = options;
		this.otherArgName = otherArgName;
		this.parsed = parsed;
		this.exception = exception;
	}

	public boolean isParseError() {
		if (exception != null)
			return true;
		if (this.otherArgName == null && parsed.getArgs().length > 0)
			return true;
		for (Object o : options.getRequiredOptions()) {
			String opt = (String) o;
			if (!options.getOption(opt).hasOptionalArg() && get(opt) == null)
				return true;
		}
		return false;
	}

	public void printParseError() {
		if (exception != null) {
			String msg = exception.getMessage();
			if (msg == null) {
				msg = exception.toString();
			}
			System.err.println(msg);
		} else if (parsed != null && this.otherArgName == null
				&& parsed.getArgs().length > 0) {
			System.err.println("Unrecognized option: "
					+ Arrays.toString(parsed.getArgs()));
		} else {
			for (Object o : options.getRequiredOptions()) {
				String opt = ((Option) o).getOpt();
				if (!((Option) o).hasOptionalArg() && get(opt) == null) {
					System.err.println("Missing required option: " + opt);
				}
			}
		}
		printHelp();
	}

	public void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("[options]", this.options);
	}

	public void printCommandName() {
		System.out.println(this.name);
	}

	public boolean has(String opt) {
		return parsed != null && parsed.hasOption(opt);
	}

	public String get(String opt) {
		String[] values = getAll(opt);
		if (values == null || values.length == 0)
			return null;
		return values[values.length - 1];
	}

	public String[] getAll(String opt) {
		if (parsed == null)
			return null;
		if (opt.equals(otherArgName))
			return removeBlanks(parsed.getArgs());
		return removeBlanks(parsed.getOptionValues(opt));
	}

	private String[] removeBlanks(String[] values) {
		if (values == null)
			return null;
		List<String> list = new ArrayList<String>(Arrays.asList(values));
		list.remove(null);
		list.remove("");
		return list.toArray(new String[list.size()]);
	}
}
