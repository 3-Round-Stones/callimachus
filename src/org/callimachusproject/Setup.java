/*
 * Copyright (c) 2012 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.callimachusproject.cli.Command;
import org.callimachusproject.cli.CommandSet;
import org.callimachusproject.setup.CallimachusSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for setting up a new repository.
 * 
 * @author James Leigh
 * 
 */
public class Setup {
	public static final String NAME = Version.getInstance().getVersion();

	private static final CommandSet commands = new CommandSet(NAME);
	static {
		commands.option("d", "dir").arg("directory").desc("Directory used for data storage and retrieval");
		commands.require("c", "config").arg("file").desc(
				"A repository config url (relative file: or http:)");
		commands.option("f", "car").arg("file").desc(
				"Target and CAR URL pairs, separated by equals, that should be installed for each primary origin");
		commands.option("w", "webapp").arg("file").desc(
				"Callimachus webapp CAR URL, the relative URL of the callimachus webapp CAR, that should be installed for each origin");
		commands.require("o", "origin").arg("http").desc(
				"The scheme, hostname and port ( http://localhost:8080 ) that resolves to this server");
		commands.option("v", "virtual").arg("http").desc(
				"Additional scheme, hostname and port ( http://localhost:8080 ) that resolves to this server");
		commands.option("r", "realm").arg("http").desc(
				"The scheme, hostname, port, and path ( http://example.com:8080/ ) that does not resolve to this server");
		commands.option("u", "user").optional("name").desc(
				"Create the given user");
		commands.option("n", "name").arg("name").desc(
				"If creating a new user use this full name");
		commands.option("e", "email").arg("addr").desc(
				"If creating a new user use this email address");
		commands.option("s", "silent").desc(
				"If the repository is already setup exit successfully");
		commands.option("h", "help").desc(
				"Print help (this message) and exit");
		commands.option("V", "version").desc(
				"Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			Setup setup = new Setup();
			setup.init(args);
			setup.start();
			setup.stop();
			setup.destroy();
		} catch (ClassNotFoundException e) {
			System.err.print("Missing jar with: ");
			System.err.println(e.toString());
			System.exit(5);
		} catch (Exception e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(1);
		}
	}

	private static void println(Throwable e) {
		Throwable cause = e.getCause();
		if (cause == null && e.getMessage() == null) {
			e.printStackTrace(System.err);
		} else if (cause != null) {
			println(cause);
		}
		System.err.println(e.toString());
		e.printStackTrace();
	}

	private final Logger logger = LoggerFactory.getLogger(Setup.class);
	private boolean silent;
	private File dir;
	private URL config;
	private URL webappCar;
	private final Map<String, URL> cars = new HashMap<String, URL>();
	private final Set<String> origins = new HashSet<String>();
	private final Map<String, String> vhosts = new HashMap<String, String>();
	private final Map<String, String> realms = new HashMap<String, String>();
	private String name;
	private String email;
	private String username;
	private char[] password;

	public void init(String[] args) {
		try {
			Command line = commands.parse(args);
			if (line.isParseError()) {
				line.printParseError();
				System.exit(2);
				return;
			} else if (line.has("help")) {
				line.printHelp();
				System.exit(0);
				return;
			} else if (line.has("version")) {
				line.printCommandName();
				System.exit(0);
				return;
			} else {
				silent = line.has("silent");
				if (line.has("dir")) {
					dir = new File(line.get("dir")).getCanonicalFile();
				} else {
					dir = new File("").getCanonicalFile();
				}
				if (line.has("config")) {
					config = resolve(line.get("config"));
				}
				if (line.has("origin")) {
					for (String o : line.getAll("origin")) {
						CallimachusSetup.validateOrigin(o);
						origins.add(o);
					}
					if (line.has("webapp")) {
						webappCar = resolve(line.get("webapp"));
					}
					if (line.has("car")) {
						for (String pair : line.getAll("car")) {
							int idx = pair.indexOf('=');
							String path = pair.substring(0, idx);
							URL url = resolve(pair.substring(idx + 1));
							for (String origin : origins) {
								java.net.URI uri = java.net.URI.create(origin + "/");
								cars.put(uri.resolve(path).toASCIIString(), url);
							}
						}
					}
					String origin = line.get("origin");
					if (line.has("virtual")) {
						for (String v : line.getAll("virtual")) {
							CallimachusSetup.validateOrigin(v);
							vhosts.put(v, origin);
						}
					}
					if (line.has("realm")) {
						for (String r : line.getAll("realm")) {
							CallimachusSetup.validateRealm(r);
							realms.put(r, origin);
						}
					}
					if (line.has("user") || line.has("email")) {
						this.name = line.get("name");
						this.email = line.get("email");
						String u = line.get("user");
						if (u != null && u.contains(":")) {
							username = u.substring(0, u.indexOf(':'));
							password = u.substring(u.indexOf(':') + 1).toCharArray();
							CallimachusSetup.validateName(username);
						}
						Console console = System.console();
						if (username == null || username.length() < 1) {
							if (u != null && u.length() > 0 && !u.contains(":")) {
								username = u;
							} else if (console == null) {
								Reader reader = new InputStreamReader(System.in);
								username = new BufferedReader(reader).readLine();
							} else {
								username = console.readLine("Enter a username: ");
							}
							CallimachusSetup.validateName(username);
						}
						if (email == null || email.length() < 1) {
							if (console == null) {
								Reader reader = new InputStreamReader(System.in);
								email = new BufferedReader(reader).readLine();
							} else {
								email = console.readLine("Enter an email: ");
							}
							CallimachusSetup.validateEmail(email);
						}
					}
				}
			}
		} catch (IllegalArgumentException e) {
			println(e);
			System.exit(1);
		} catch (Exception e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(1);
		}
	}

	public void start() throws Exception {
		boolean changed = false;
		String configString = readContent(config);
		CallimachusSetup setup = new CallimachusSetup(dir, configString);
		try {
			if (webappCar != null) {
				for (String origin : origins) {
					changed |= setup.clearCallimachusWebapp(origin);
				}
			}
			for (String origin : origins) {
				changed |= setup.createWebappOrigin(origin);
			}
			for (Map.Entry<String, String> e : vhosts.entrySet()) {
				changed |= setup.createOrigin(e.getKey(), e.getValue());
			}
			for (Map.Entry<String, String> e : realms.entrySet()) {
				changed |= setup.createRealm(e.getKey(), e.getValue());
			}
			if (webappCar != null) {
				for (String origin : origins) {
					changed |= setup.importCallimachusWebapp(webappCar, origin);
				}
			}
			for (Map.Entry<String, URL> e : cars.entrySet()) {
				String origin = origins.iterator().next();
				for (String o : origins) {
					if (e.getKey().startsWith(o + "/")) {
						origin = o;
					}
				}
				changed |= setup.importCar(e.getValue(), e.getKey(), origin);
			}
			if (email != null && email.length() > 0) {
				for (String origin : origins) {
					changed |= setup.createAdmin(name, email, username, origin);
					if (password == null || password.length < 1) {
						for (String url : setup.getUserRegistrationLinks(username, email, origin)) {
							System.err.println("Use this URL to assign a password");
							System.out.println(url);
						}
					} else {
						changed |= setup.changeUserPassword(email, username, password, origin);
					}
				}
			}
		} finally {
			setup.shutDown();
		}
		if (changed || silent) {
			System.exit(0);
		} else {
			logger.warn("Repository is already setup");
			System.exit(166); // already setup
		}
	}

	public void stop() throws Exception {
		// do nothing
	}

	public void destroy() throws Exception {
		// do nothing
	}

	private URL resolve(String file) throws MalformedURLException {
		try {
			return new File(".").toURI().resolve(file).toURL();
		} catch (IllegalArgumentException e) {
			return new File(file).toURI().toURL();
		}
	}

	private String readContent(URL config) throws IOException {
		InputStream in = config.openStream();
		try {
			return new Scanner(in).useDelimiter("\\Z").next();
		} finally {
			in.close();
		}
	}

}
