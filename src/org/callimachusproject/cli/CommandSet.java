package org.callimachusproject.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandSet {
	private final String name;
	private final Options options = new Options();
	private String otherArgName;

	public CommandSet(String version) {
		this.name = version;
	}

	public CommandOption require(String opt) {
		Option option = new Option(opt, opt);
		option.setRequired(true);
		return new CommandOption(option);
	}

	public CommandOption require(String opt, String longOpt) {
		Option option = new Option(opt, longOpt);
		if (!opt.equals(longOpt)) {
			option.setLongOpt(longOpt);
		}
		option.setRequired(true);
		return new CommandOption(option);
	}

	public CommandOption option(String opt) {
		Option option = new Option(opt, opt);
		return new CommandOption(option);
	}

	public CommandOption option(String opt, String longOpt) {
		Option option = new Option(opt, longOpt);
		if (!opt.equals(longOpt)) {
			option.setLongOpt(longOpt);
		}
		return new CommandOption(option);
	}

	public void other(String argName) {
		this.otherArgName = argName;
	}

	public Command parse(String[] args) {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			return new Command(name, options, otherArgName, line);
		} catch (ParseException e) {
			return new Command(name, options, otherArgName, e);
		}
	}

	public class CommandOption {
		private final Option option;

		protected CommandOption(Option option) {
			this.option = option;
		}

		public CommandOption arg(String argName) {
			option.setArgName(argName);
			option.setArgs(1);
			option.setOptionalArg(false);
			return this;
		}

		public CommandOption optional(String argName) {
			option.setArgName(argName);
			option.setArgs(1);
			option.setOptionalArg(true);
			return this;
		}

		public void desc(String description) {
			option.setDescription(description);
			options.addOption(option);
		}
	}
}
