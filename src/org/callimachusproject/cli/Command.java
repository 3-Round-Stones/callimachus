package org.callimachusproject.cli;

import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
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
			ParseException exception) {
		this.name = name;
		this.options = options;
		this.otherArgName = otherArgName;
		this.parsed = null;
		this.exception = exception;
	}

	public boolean isParseError() {
		return exception != null || this.otherArgName == null
				&& parsed.getArgs().length > 0;
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
			return parsed.getArgs();
		return parsed.getOptionValues(opt);
	}
}