/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimeTypeParseException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.util.FileUtil;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.store.blob.file.FileBlobStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for launching the server.
 * 
 * @author James Leigh
 * 
 */
public class Server {
	private static final String REPOSITORY_TEMPLATE = "META-INF/templates/object.ttl";
	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	private static final Options options = new Options();
	static {
		options.addOption("n", "name", true, "Server name");
		options
				.addOption("identityprefix", true,
						"URI prefix used when absolute-URI request-target is percent encoded in path");
		options
				.addOption(
						"envelope",
						true,
						"Content-Type that is used for envelope responses that should be openned for the client");
		options.addOption("p", "port", true,
						"Port the server should listen on");
		options.addOption("s", "sslport", true,
						"Secure port the server should listen on");
		options.addOption("m", "manager", true,
				"The repository manager or server url");
		options.addOption("i", "id", true,
				"The existing repository id in the local manager");
		options.addOption("r", "repository", true,
				"The existing repository url (relative file: or http:)");
		options.addOption("t", "template", true,
				"A repository configuration template url "
						+ "(relative file: or http:)");
		options
				.addOption("user", true,
						"The secret username:password file used to bootstrap the system");
		options.addOption("trust", false,
				"Allow all server code to read, write, and execute all files and directories "
						+ "according to the file system's ACL");
		Option fromOpt = new Option("from", true,
				"Email address for the human user who controls this server");
		fromOpt.setOptionalArg(true);
		options.addOption(fromOpt);
		options.addOption("dynamic", false,
				"Read behaviour operations from the RDF store");
		options.addOption("w", "www", true,
				"Directory used for data storage and retrieval (replaced by -b)");
		options.addOption("b", "blob", true,
				"Directory used for blob storage and retrieval");
		options.addOption("c", "cache", true,
				"Directory used for transient storage");
		options.addOption("h", "help", false,
				"Print help (this message) and exit");
		options.addOption("v", "version", false,
				"Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			Server server = new Server();
			server.init(args);
			server.start();
			Thread.sleep(1000);
			if (server.isRunning()) {
				String string = Arrays.toString(server.getPorts());
				String ports = string.substring(1, string.length() - 1);
				System.out.println(server.getClass().getSimpleName()
						+ " listening on port " + ports);
				System.out.println("repository: " + server.getRepository());
				System.out.println("blob dir: " + server.getBlobDir());
				System.out.println("cache dir: " + server.getCacheDir());
			}
		} catch (Exception e) {
			println(e);
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
		if (e.getMessage() == null) {
			System.err.println(e.toString());
		} else {
			System.err.println(e.getMessage());
		}
	}

	private HTTPObjectServer server;
	private File wwwDir;
	private File blobDir;
	private File cacheDir;
	private int[] ports = new int[0];
	private int[] sslports = new int[0];

	public int[] getPorts() {
		return ports;
	}

	public int[] getSslPorts() {
		return sslports;
	}

	public Repository getRepository() {
		if (server == null)
			return null;
		return server.getRepository();
	}

	public File getBlobDir() {
		return blobDir;
	}

	public File getCacheDir() {
		return cacheDir;
	}

	public void init(String[] args) {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				String cmdLineSyntax = " [-r repository | -m manager [-i id [-t config]]] [-w webdir] [options] ontology...";
				String header = "ontology... a list of RDF or JAR urls that should be compiled and loaded into the server.";
				formatter.printHelp(cmdLineSyntax, header, options, "");
				System.exit(0);
				return;
			} else if (line.hasOption('v')) {
				System.out.println(HTTPObjectServer.DEFAULT_NAME);
				System.exit(0);
				return;
			}
			init(line);
		} catch (Exception e) {
			if (e.getMessage() != null) {
				System.err.println(e.getMessage());
			} else {
				e.printStackTrace(System.err);
			}
			System.exit(2);
		}
	}

	public void start() throws Exception {
		server.start();
	}

	public boolean isRunning() {
		if (server == null)
			return false;
		return server.isRunning();
	}

	public void stop() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

	public void destroy() throws Exception {
		if (server != null) {
			Repository repository = getRepository();
			server.destroy();
			if (repository != null) {
				repository.shutDown();
			}
		}
	}

	private void init(CommandLine line) throws RepositoryException,
			RepositoryConfigException, MalformedURLException, IOException,
			RDFParseException, RDFHandlerException, GraphUtilException,
			URISyntaxException, FileNotFoundException, Exception,
			MimeTypeParseException {
		RepositoryManager manager = null;
		Repository repository = null;
		List<URL> imports = new ArrayList<URL>();
		if (line.hasOption('r')) {
			String url = line.getOptionValue('r');
			repository = RepositoryProvider.getRepository(url);
		} else {
			if (line.hasOption('m')) {
				String dir = line.getOptionValue('m');
				manager = RepositoryProvider.getRepositoryManager(dir);
			} else {
				manager = RepositoryProvider.getRepositoryManager(".");
			}
			if (line.hasOption('i')) {
				String id = line.getOptionValue('i');
				if (manager.hasRepositoryConfig(id)) {
					repository = manager.getRepository(id);
				} else {
					URL url = getRepositoryConfigURL(line);
					manager.addRepositoryConfig(createConfig(url));
					repository = manager.getRepository(id);
					if (repository == null)
						throw new RepositoryConfigException(
								"Repository id and config id don't match: "
										+ id);
				}
			} else if (manager.hasRepositoryConfig("object")) {
				repository = manager.getRepository("object");
			} else {
				URL url = getRepositoryConfigURL(line);
				manager.addRepositoryConfig(createConfig(url));
				repository = manager.getRepository("object");
			}
		}
		if (line.hasOption('w')) {
			wwwDir = new File(line.getOptionValue('w')).getCanonicalFile();
		} else if (line.hasOption('r') && repository.getDataDir() != null) {
			wwwDir = new File(repository.getDataDir(), "www")
					.getCanonicalFile();
		} else if (line.hasOption('m') && isDirectory(manager.getLocation())) {
			File base = new File(manager.getLocation().toURI())
					.getCanonicalFile();
			wwwDir = new File(base, "www").getCanonicalFile();
		} else {
			wwwDir = new File("www").getCanonicalFile();
		}
		if (line.hasOption('b')) {
			blobDir = new File(line.getOptionValue('b')).getCanonicalFile();
		} else if (line.hasOption('r') && repository.getDataDir() != null) {
			blobDir = new File(repository.getDataDir(), "blob")
					.getCanonicalFile();
		} else if (line.hasOption('m') && isDirectory(manager.getLocation())) {
			File base = new File(manager.getLocation().toURI())
					.getCanonicalFile();
			blobDir = new File(base, "blob").getCanonicalFile();
		} else {
			blobDir = new File("blob").getCanonicalFile();
		}
		cacheDir = getCacheDir(line, manager, repository);
		String basic = null;
		if (line.hasOption("user")) {
			String file = line.getOptionValue("user");
			BufferedReader reader = new BufferedReader(new FileReader(file));
			try {
				basic = reader.readLine();
				if (basic.length() == 0) {
					basic = null;
				}
			} finally {
				reader.close();
			}
		}
		if (!line.hasOption("trust")) {
			if (repository.getDataDir() == null) {
				HTTPObjectPolicy.apply(line.getArgs(), wwwDir, blobDir, cacheDir);
			} else {
				File repositoriesDir = repository.getDataDir().getParentFile()
						.getCanonicalFile();
				HTTPObjectPolicy.apply(line.getArgs(), repositoriesDir, wwwDir, blobDir,
						cacheDir);
			}
		}
		for (String owl : line.getArgs()) {
			imports.add(getURL(owl));
		}
		ObjectRepository or;
		if (imports.isEmpty() && repository instanceof ObjectRepository) {
			or = (ObjectRepository) repository;
		} else {
			ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
			ObjectRepositoryConfig config = factory.getConfig();
			if (line.hasOption("dynamic")) {
				config.setCompileRepository(true);
			}
			if (!imports.isEmpty()) {
				for (URL url : imports) {
					if (url.toExternalForm().toLowerCase().endsWith(".jar")
							|| isDirectory(url)) {
						config.addConceptJar(url);
					} else {
						config.addImports(url);
					}
				}
			}
			if (wwwDir.isDirectory() && !blobDir.isDirectory()) {
				// 2.0-beta13 compatibility
				config.setBlobStore(wwwDir.toURI().toString());
				Map<String, String> map = new HashMap<String, String>();
				map.put("provider", FileBlobStoreProvider.class.getName());
				config.setBlobStoreParameters(map);
			} else {
				config.setBlobStore(blobDir.toURI().toString());
			}
			or = factory.createRepository(config, repository);
		}
		File in = new File(cacheDir, "client");
		File out = new File(cacheDir, "server");
		HTTPObjectClient.setInstance(in, 1024);
		if (line.hasOption("from")) {
			String from = line.getOptionValue("from");
			HTTPObjectClient.getInstance().setFrom(from == null ? "" : from);
		}
		server = new HTTPObjectServer(or, out, basic);
		if (line.hasOption('n')) {
			server.setName(line.getOptionValue('n'));
		}
		if (line.hasOption("identityprefix")) {
			String[] identitypath = line.getOptionValues("identityprefix");
			server.setIdentityPrefix(identitypath);
		}
		if (line.hasOption("envelope")) {
			String envelopeType = line.getOptionValue("envelope");
			server.setEnvelopeType(envelopeType);
			HTTPObjectClient.getInstance().setEnvelopeType(envelopeType);
		}
		if (line.hasOption('p')) {
			String[] values = line.getOptionValues('p');
			ports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				ports[i] = Integer.parseInt(values[i]);
			}
		}
		if (line.hasOption('s')) {
			String[] values = line.getOptionValues('s');
			sslports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				sslports[i] = Integer.parseInt(values[i]);
			}
		}
		if (!line.hasOption('p') && !line.hasOption('s')) {
			ports = new int[] { 8080 };
		}
		server.listen(ports, sslports);
	}

	private File getCacheDir(CommandLine line, RepositoryManager manager,
			Repository repository) throws URISyntaxException,
			MalformedURLException {
		File cacheDir;
		if (line.hasOption('c')) {
			cacheDir = new File(line.getOptionValue('c'));
		} else if (line.hasOption('r') && repository.getDataDir() != null) {
			cacheDir = new File(repository.getDataDir(), "cache");
		} else if (line.hasOption('m') && isDirectory(manager.getLocation())) {
			File base = new File(manager.getLocation().toURI());
			cacheDir = new File(base, "cache");
		} else {
			cacheDir = new File("cache");
		}
		FileUtil.deleteOnExit(cacheDir);
		return cacheDir.getAbsoluteFile();
	}

	private boolean isDirectory(URL url) throws URISyntaxException {
		return url.getProtocol().equalsIgnoreCase("file")
				&& new File(url.toURI()).isDirectory();
	}

	private URL getRepositoryConfigURL(CommandLine line)
			throws MalformedURLException {
		if (line.hasOption('t')) {
			String relative = line.getOptionValue('t');
			URL url = new File(".").toURI().resolve(relative).toURL();
			logger.info("Using repository configuration template: {}", url);
			return url;
		} else {
			ClassLoader cl = Server.class.getClassLoader();
			return cl.getResource(REPOSITORY_TEMPLATE);
		}
	}

	private RepositoryConfig createConfig(URL url) throws IOException,
			RDFParseException, RDFHandlerException, GraphUtilException,
			RepositoryConfigException {
		Graph graph = new GraphImpl();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));

		String base = new File(".").getAbsoluteFile().toURI().toASCIIString();
		URLConnection con = url.openConnection();
		StringBuilder sb = new StringBuilder();
		for (String mimeType : RDFFormat.TURTLE.getMIMETypes()) {
			if (sb.length() < 1) {
				sb.append(", ");
			}
			sb.append(mimeType);
		}
		con.setRequestProperty("Accept", sb.toString());
		InputStream in = con.getInputStream();
		try {
			rdfParser.parse(in, base);
		} finally {
			in.close();
		}

		Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE,
				RepositoryConfigSchema.REPOSITORY);
		RepositoryConfig config = RepositoryConfig.create(graph, node);
		config.validate();
		return config;
	}

	private URL getURL(String path) throws MalformedURLException {
		return new File(".").toURI().resolve(path).toURL();
	}

}
