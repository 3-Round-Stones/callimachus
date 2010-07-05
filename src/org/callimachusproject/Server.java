package org.callimachusproject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.contentobjects.jnotify.JNotify;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.callimachusproject.logging.LoggerBean;
import org.openrdf.http.object.HTTPObjectPolicy;
import org.openrdf.http.object.client.HTTPObjectClient;
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
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

/**
 * Command line tool for launching the server.
 * 
 * @author James Leigh
 * 
 */
public class Server {
	private static final String VERSION_PATH = "/META-INF/callimachusproject.properties";
	private static final String VERSION;
	static {
		Properties properties = new Properties();
		InputStream in = Server.class.getResourceAsStream(VERSION_PATH);
		if (in != null) {
			try {
				properties.load(in);
			} catch (IOException e) {
				// ignore
			}
		}
		String version = properties.getProperty("version");
		if (version == null) {
			version = "devel";
		}
		VERSION = version;
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			String name = Server.class.getPackage().getName() + ":type=logger";
			mbs.registerMBean(new LoggerBean(), new ObjectName(name));
		} catch (Exception e) {
			// ignore
		}
	}
	private static final String REPOSITORY_TEMPLATE = "META-INF/templates/callimachus-config.ttl";

	private static final Options options = new Options();
	static {
		options.addOption("n", "name", true, "Server name");
		options.addOption("a", "authority", true,
				"The hostname and port ( localhost:8080 )");
		options
				.addOption("p", "port", true,
						"Port the server should listen on");
		options.addOption("r", "repository", true,
				"The existing repository url (relative file: or http:)");
		options.addOption("d", "dir", true,
				"Directory used for data storage and retrieval");
		options.addOption("u", "update", false,
				"If the server should reload all web resources");
		options.addOption("trust", false,
				"Allow all server code to read, write, and execute all files and directories "
						+ "according to the file system's ACL");
		Option fromOpt = new Option("from", true,
				"Email address for the human user who controls this server");
		fromOpt.setOptionalArg(true);
		options.addOption(fromOpt);
		options.addOption("h", "help", false,
				"Print help (this message) and exit");
		options.addOption("v", "version", false,
				"Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("[options]", options);
				return;
			}
			if (line.hasOption('v')) {
				System.out.print("Callimachus Project Server/");
				System.out.println(VERSION);
				return;
			}
			File dir = new File("").getCanonicalFile();
			File webappsDir = new File(dir, "webapps");
			if (line.hasOption('d')) {
				dir = new File(line.getOptionValue('d')).getCanonicalFile();
				webappsDir = new File(dir, "webapps");
			}
			Repository repository = getRepository(line, dir);
			if (repository.getDataDir() != null) {
				dir = repository.getDataDir();
			}
			File cacheDir = new File(dir, "cache");
			File in = new File(cacheDir, "client");
			HTTPObjectClient.setInstance(in, 1024);
			if (line.hasOption("from")) {
				String from = line.getOptionValue("from");
				HTTPObjectClient.getInstance()
						.setFrom(from == null ? "" : from);
			}
			CallimachusServer server = new CallimachusServer(repository, dir,
					webappsDir);
			if (line.hasOption('p')) {
				server.setPort(Integer.parseInt(line.getOptionValue('p')));
			}
			if (line.hasOption('a')) {
				server.setAuthority(line.getOptionValue('a'));
			}
			if (line.hasOption('n')) {
				server.setServerName(line.getOptionValue('n'));
			}
			if (line.hasOption('u')) {
				server.setConditionalRequests(false);
			}
			try {
				JNotify.removeWatch(-1); // load library
			} catch (UnsatisfiedLinkError e) {
				System.err.println(e.getMessage());
			}
			webappsDir.mkdirs();
			if (!line.hasOption("trust")) {
				applyPolicy(line, repository, dir, webappsDir);
			}
			server.start();
			Thread.sleep(1000);
			if (server.isRunning()) {
				System.out.println(server.getClass().getSimpleName()
						+ " is listening on port " + server.getPort()
						+ " for http://" + server.getAuthority() + "/");
				System.out.println("Repository: " + server.getRepository());
				System.out.println("Webapps: " + server.getWebappsDir());
				System.out.println("Authority: " + server.getAuthority());
			}
		} catch (Exception e) {
			if (e.getMessage() != null) {
				System.err.println(e.getMessage());
			} else {
				e.printStackTrace(System.err);
			}
			System.exit(1);
		}
	}

	private static Repository getRepository(CommandLine line, File dir)
			throws RepositoryException, RepositoryConfigException,
			MalformedURLException, IOException, RDFParseException,
			RDFHandlerException, GraphUtilException {
		if (line.hasOption('r')) {
			String url = line.getOptionValue('r');
			return RepositoryProvider.getRepository(url);
		}
		RepositoryManager manager = RepositoryProvider.getRepositoryManager(dir
				.toURI().toASCIIString());
		ClassLoader cl = Server.class.getClassLoader();
		URL url = cl.getResource(REPOSITORY_TEMPLATE);
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
		String id = GraphUtil.getUniqueObjectLiteral(graph, node,
				RepositoryConfigSchema.REPOSITORYID).stringValue();
		if (manager.hasRepositoryConfig(id))
			return manager.getRepository(id);
		RepositoryConfig config = RepositoryConfig.create(graph, node);
		config.validate();
		manager.addRepositoryConfig(config);
		return manager.getRepository(id);
	}

	private static void applyPolicy(CommandLine line, Repository repository,
			File dir, File webappsDir) {
		if (!line.hasOption("trust")) {
			if (repository.getDataDir() == null) {
				HTTPObjectPolicy.apply(new String[0], dir, webappsDir);
			} else {
				File repositoriesDir = repository.getDataDir().getParentFile();
				HTTPObjectPolicy.apply(new String[0], repositoriesDir, dir,
						webappsDir);
			}
		}
	}

}
