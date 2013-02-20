package org.callimachusproject.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.RDFParser.DatatypeHandling;
import org.openrdf.rio.helpers.StatementCollector;

public class ConfigTemplate {
	private static final Pattern PARAMETER_PATTERN = Pattern
			.compile("\\{%[\\p{Print}&&[^\\}]]+%\\}");
	private final Random random = new Random();
	private final String template;

	public ConfigTemplate(URL url) throws IOException {
		this.template = readString(url);
	}

	public Map<String, String> getDefaultParameters() {
		Map<String, String> parameters = new HashMap<String, String>();
		Matcher matcher = PARAMETER_PATTERN.matcher(template);
		while (matcher.find()) {
			String group = matcher.group();
			String[] split = group.substring(2, group.length() - 2).split(
					"\\s*\\|\\s*", 2);
			parameters.put(split[0], split.length < 2 ? null : split[1]);
		}
		return parameters;
	}

	public RepositoryConfig render(Map<String, String> parameters) throws OpenRDFException, IOException {
		Graph graph = renderGraph(parameters);
		if (graph == null)
			return null;
		Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE,
				RepositoryConfigSchema.REPOSITORY);
		return RepositoryConfig.create(graph, node);
	}

	public Map<String, String> getParameters(RepositoryConfig config)
			throws IOException, OpenRDFException {
		GraphImpl graph = new GraphImpl();
		config.export(graph);
		Map<String, String> map = getDefaultParameters();
		for (String key : map.keySet()) {
			map.put(key, new BigInteger(130, random).toString(32));
		}
		Graph wild = renderGraph(map);
		Map<String, String> parameters = new HashMap<String, String>();
		for (Statement st : wild) {
			setLikelyValue(st, map, graph, parameters);
		}
		Graph same = new GraphImpl();
		render(parameters).export(same);
		if (ModelUtil.equals(graph, same))
			return parameters;
		return null;
	}

	private Graph renderGraph(Map<String, String> parameters)
			throws IOException, RDFParseException, RDFHandlerException {
		StringBuffer result = new StringBuffer(template.length());
		Matcher matcher = PARAMETER_PATTERN.matcher(template);
		while (matcher.find()) {
			String group = matcher.group();
			String[] split = group.substring(2, group.length() - 2).split(
					"\\s*\\|\\s*", 2);
			String value = null;
			if (parameters != null) {
				value = parameters.get(split[0]);
			}
			if (value == null && split.length < 2)
				return null;
			if (value == null) {
				value = split[1];
			}
			matcher.appendReplacement(result, value);
		}
		matcher.appendTail(result);
		return parseTurtleGraph(result.toString());
	}

	private void setLikelyValue(Statement st, Map<String, String> map,
			GraphImpl graph, Map<String, String> parameters) {
		URI p = st.getPredicate();
		String w = st.getObject().stringValue();
		for (String key : map.keySet()) {
			String val = map.get(key);
			int idx = w.indexOf(val);
			if (idx >= 0) {
				Iterator<Statement> iter = graph.match(null, p, null);
				if (iter.hasNext()) {
					String o = iter.next().getObject().stringValue();
					if (iter.hasNext())
						continue;
					if (w.equals(val)) {
						parameters.put(key, o);
					} else {
						int end = o.length() - w.length() + val.length() + idx;
						parameters.put(key, o.substring(idx, end));
					}
				}
			}
		}
	}

	private String readString(URL url) throws IOException,
			UnsupportedEncodingException {
		StringWriter writer = new StringWriter();
		URLConnection con = url.openConnection();
		con.setRequestProperty("Accept", "text/turtle,text/plain");
		con.setRequestProperty("Accept-Charset", "UTF-8");
		InputStream in = con.getInputStream();
		InputStreamReader reader = new InputStreamReader(in, "UTF-8");
		try {
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
			}
		} finally {
			reader.close();
		}
		return writer.toString();
	}

	private Graph parseTurtleGraph(String configString) throws IOException,
			RDFParseException, RDFHandlerException {
		Graph graph = new GraphImpl();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setDatatypeHandling(DatatypeHandling.IGNORE);
		rdfParser.setVerifyData(false);
		rdfParser.setRDFHandler(new StatementCollector(graph));
		String base = new File(".").getAbsoluteFile().toURI().toASCIIString();
		rdfParser.parse(new StringReader(configString), base);
		return graph;
	}
}
