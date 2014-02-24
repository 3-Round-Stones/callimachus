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
package org.callimachusproject.xproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIUtils;
import org.callimachusproject.engine.ParameterizedQuery;
import org.callimachusproject.engine.ParameterizedQueryParser;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.repository.CalliRepository.HttpRepositoryClient;
import org.callimachusproject.xml.XdmNodeFactory;
import org.openrdf.OpenRDFException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcStep;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;

public class SparqlStep implements XProcStep {
	private static final String RESULTS_XML = "application/sparql-results+xml";
	private static final QName _content_type = new QName("content-type");
	public static final QName _encoding = new QName("", "encoding");
	private static final FluidFactory FF = FluidFactory.getInstance();
	private final Logger logger = LoggerFactory.getLogger(SparqlStep.class);
	private final FluidBuilder fb = FF.builder();
	private final Map<String, String> parameters = new LinkedHashMap<String, String>();
	private final XProcRuntime runtime;
	private final XAtomicStep step;
	private ReadablePipe sourcePipe = null;
	private ReadablePipe queryPipe = null;
	private WritablePipe resultPipe = null;
	private String endpoint;
	private String outputBase;

	public SparqlStep(XProcRuntime runtime, XAtomicStep step) {
		this.runtime = runtime;
		this.step = step;
	}

	@Override
	public void setParameter(QName name, RuntimeValue value) {
		parameters.put(name.getLocalName(), value.getString());
	}

	@Override
	public void setParameter(String port, QName name, RuntimeValue value) {
		setParameter(name, value);
	}

	@Override
	public void setOption(QName name, RuntimeValue value) {
		if ("output-base-uri".equals(name.getClarkName())) {
			outputBase = value.getString();
		} else if ("endpoint".equals(name.getClarkName())) {
			endpoint = value.getString();
		}
	}

	public void setInput(String port, ReadablePipe pipe) {
		if ("source".equals(port)) {
			sourcePipe = pipe;
		} else if ("query".equals(port)) {
			queryPipe = pipe;
		}
	}

	public void setOutput(String port, WritablePipe pipe) {
		if ("result".equals(port)) {
			resultPipe = pipe;
		} else {
			throw new XProcException("No other outputs allowed.");
		}
	}

	public void reset() {
		sourcePipe.resetReader();
		queryPipe.resetReader();
		resultPipe.resetWriter();
	}

	public void run() throws SaxonApiException {
		if (queryPipe == null || !queryPipe.moreDocuments()) {
			throw XProcException.dynamicError(6, step.getNode(),
					"No query provided.");
		}
		try {

			RepositoryConnection con;
			if (endpoint == null || endpoint.length() == 0) {
				con = createConnection();
			} else {
				con = createConnection(resolve(endpoint));
			}
			try {
				while (sourcePipe != null && sourcePipe.moreDocuments()) {
					importData(sourcePipe.read(), con);
				}
				while (queryPipe.moreDocuments()) {
					XdmNode query = queryPipe.read();
					String queryBaseURI = resolve(query.getBaseURI().toASCIIString());
					String queryString = getQueryString(query);
					XdmNode factory = evaluate(queryString, queryBaseURI, con);
					resultPipe.write(factory);
				}
			} finally {
				Repository repo = con.getRepository();
				con.close();
				if (endpoint == null || endpoint.length() == 0) {
					repo.shutDown();
				}
			}
		} catch (SAXException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (SaxonApiException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (FluidException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (IOException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (OpenRDFException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		}
	}

	private RepositoryConnection createConnection() throws RepositoryException {
		Repository repository = new SailRepository(new MemoryStore());
		repository.initialize();
		RepositoryConnection con = repository.getConnection();
		return con;
	}

	private RepositoryConnection createConnection(String endpoint)
			throws OpenRDFException, IOException {
		final SPARQLRepository repository = new SPARQLRepository(endpoint);
		HttpClient client = runtime.getHttpClient();
		if (client instanceof HttpRepositoryClient) {
			HttpRepositoryClient rclient = (HttpRepositoryClient) client;
			CredentialsProvider provider = rclient.getCredentialsProvider();
			HttpHost authority = URIUtils.extractHost(URI.create(endpoint));
			AuthScope scope = new AuthScope(authority);
			if (provider != null && provider.getCredentials(scope) != null) {
				Credentials cred = provider.getCredentials(scope);
				String username = cred.getUserPrincipal().getName();
				String password = cred.getPassword();
				repository.setUsernameAndPassword(username, password);
			}
		} else {
			logger.warn("Repository credentials could not be read");
		}
		repository.initialize();
		RepositoryConnection con = repository.getConnection();
		return con;
	}

	private void importData(XdmNode document, RepositoryConnection con)
			throws SaxonApiException, OpenRDFException, IOException {
		String sysId = document.getBaseURI().toASCIIString();
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		Serializer serializer = new Serializer();
		serializer.setOutputStream(s);
		S9apiUtils.serialize(runtime, document, serializer);
		con.add(new ByteArrayInputStream(s.toByteArray()), sysId,
				RDFFormat.RDFXML);
	}

	private String getQueryString(XdmNode document) {
		XdmNode root = S9apiUtils.getDocumentElement(document);

		if ((XProcConstants.c_data.equals(root.getNodeName()) && "application/octet-stream"
				.equals(root.getAttributeValue(_content_type)))
				|| "base64".equals(root.getAttributeValue(_encoding))) {
			byte[] decoded = Base64.decode(root.getStringValue());
			return new String(decoded);
		}
		return root.getStringValue();
	}

	private XdmNode evaluate(String queryString, String queryBaseURI,
			RepositoryConnection con) throws OpenRDFException, IOException,
			FluidException, SAXException {
		ParameterizedQueryParser parser = ParameterizedQueryParser.newInstance();
		ParameterizedQuery qry = parser.parseQuery(queryString, queryBaseURI);
		XdmNodeFactory factory = new XdmNodeFactory(runtime.getProcessor(), runtime.getHttpClient());
		String baseURI = getBaseURI(queryBaseURI);
		TupleQueryResult results = qry.evaluate(parameters, con);
		try {
			Fluid f = fb.consume(results, baseURI, TupleQueryResult.class, RESULTS_XML);
			return factory.parse(baseURI, f.asStream());
		} finally {
			results.close();
		}
	}

	private String getBaseURI(String queryBaseURI) {
		if (outputBase == null || outputBase.length() == 0)
			return queryBaseURI;
		return resolve(outputBase);
	}

	private String resolve(String href) {
		String base = step.getNode().getBaseURI().toASCIIString();
		if (href == null)
			return base;
		return TermFactory.newInstance(base).resolve(href);
	}

}
