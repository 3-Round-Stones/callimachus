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
package org.callimachusproject.repository;

import info.aduna.net.ParsedURI;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.callimachusproject.concurrent.ManagedExecutors;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CalliActivityFactory implements ActivityFactory {
	private static final String PROV_SUFFIX = "#provenance";
	private static final Executor executor = ManagedExecutors.getInstance()
			.newSingleScheduler(
					CalliActivityFactory.class.getSimpleName());
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	private static final String ACTIVITY = "http://www.w3.org/ns/prov#Activity";
	private static final String PREFIX = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX prov:<http://www.w3.org/ns/prov#>\n"
			+ "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final String END_ACTIVITY = PREFIX
			+ "DELETE {\n"
			+ "    GRAPH $graph {\n"
			+ "          $activity prov:endedAtTime ?earlier\n"
			+ "      }\n"
			+ "} INSERT {\n"
			+ "    GRAPH $graph {\n"
			+ "          $activity prov:endedAtTime ?endedAtTime . $graph calli:reader ?reader\n"
			+ "      }\n"
			+ "} WHERE {{ BIND ( $now AS ?endedAtTime) } UNION { GRAPH $graph { $activity prov:endedAtTime ?earlier } } UNION {\n"
			+ "        {SELECT DISTINCT ?reader {\n"
			+ "            ?entity prov:wasGeneratedBy $activity\n"
			+ "            {\n"
			+ "                ?entity calli:subscriber ?reader\n"
			+ "            } UNION {\n"
			+ "                ?entity calli:editor ?reader\n"
			+ "            } UNION {\n"
			+ "                ?entity calli:administrator ?reader\n"
			+ "        }}  }\n"
			+ "    } UNION {\n"
			+ "        {SELECT DISTINCT ?type {\n"
			+ "            [rdf:type ?type] prov:wasGeneratedBy $activity\n"
			+ "        }}\n"
			+ "        {\n"
			+ "            ?type (calli:subscriber|calli:editor|calli:administrator) ?reader\n"
			+ "        } UNION {\n"
			+ "            ?type ^owl:equivalentClass/(calli:subscriber|calli:editor|calli:administrator) ?reader\n"
			+ "        }\n"
			+ "}   }";
	private static final String INSERT_FOLDER = PREFIX + "INSERT {\n"
			+ "$parent calli:hasComponent $folder .\n"
			+ "$folder a calli:Folder, $folderType;\n" + "rdfs:label $label;\n"
			+ "calli:administrator ?administrator;\n"
			+ "calli:editor ?editor;\n" + "calli:subscriber ?subscriber;\n"
			+ "calli:reader ?reader\n" + "} WHERE {\n"
			+ "OPTIONAL {$parent calli:administrator ?administrator}\n"
			+ "OPTIONAL {$parent calli:editor ?editor}\n"
			+ "OPTIONAL {$parent calli:subscriber ?subscriber}\n"
			+ "OPTIONAL {$parent calli:reader ?reader}\n" + "}";
	private static final DatatypeFactory df;
	static {
		try {
			df = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new AssertionError(e);
		}
	}
	private static final String CALLI_HASCOMPONENT = "http://callimachusproject.org/rdf/2009/framework#hasComponent";
	private final Logger logger = LoggerFactory.getLogger(CalliActivityFactory.class);
	private final String uid = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private final AtomicLong seq = new AtomicLong(0);
	private final Repository repository;
	private final String uriSpace;
	private final URI changeType;
	private final URI folderType;
	private String namespace;
	private GregorianCalendar date;
	private String folder;

	public CalliActivityFactory(Repository repository, String uriSpace) {
		this(repository, uriSpace, null, null);
	}

	public CalliActivityFactory(Repository repository, String uriSpace, URI changeType,
			URI folderType) {
		assert repository != null;
		assert uriSpace != null;
		this.repository = repository;
		this.uriSpace = uriSpace;
		this.changeType = changeType;
		this.folderType = folderType;
		assert uriSpace.endsWith("/");
	}

	@Override
	public String toString() {
		return getNamespace();
	}

	public URI createActivityURI(URI bundle, ValueFactory vf) {
		if (bundle != null)
			return vf.createURI(bundle.stringValue() + PROV_SUFFIX);
		String local = uid + seq.getAndIncrement();
		return vf.createURI(getNamespace() + local + PROV_SUFFIX);
	}

	public void activityStarted(URI activity, URI graph, RepositoryConnection con)
			throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI(graph.getNamespace()),
				vf.createURI(CALLI_HASCOMPONENT), graph, graph);
		if (changeType != null) {
			con.add(graph, RDF.TYPE, changeType, graph);
		}
		con.add(activity, RDF.TYPE, vf.createURI(ACTIVITY), graph);
	}

	public void activityEnded(URI act, URI graph, RepositoryConnection con)
			throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		XMLGregorianCalendar now = df
				.newXMLGregorianCalendar(new GregorianCalendar(UTC));
		try {
			Update up = con.prepareUpdate(QueryLanguage.SPARQL, END_ACTIVITY);
			up.setBinding("activity", act);
			up.setBinding("graph", graph);
			up.setBinding("now", vf.createLiteral(now));
			up.execute();
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		}
		createFolder(graph);
	}

	private synchronized void createFolder(URI bundle) throws RepositoryException {
		final String space = bundle.getNamespace();
		if (!space.equals(folder) && space.endsWith("/")
				&& new ParsedURI(space).isHierarchical()) {
			folder = space;
			executor.execute(new Runnable() {
				public void run() {
					try {
						RepositoryConnection con = repository.getConnection();
						try {
							ValueFactory vf = con.getValueFactory();
							createFolder(vf.createURI(space), con);
						} finally {
							con.close();
						}
					} catch (Throwable e) {
						logger.error(e.toString(), e);
					}
				}
			});
		}
	}

	boolean createFolder(URI folder, RepositoryConnection con)
			throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		if (uriSpace.equals(folder.stringValue()))
			return false;
		if (con.hasStatement(folder, RDF.TYPE, folderType, true))
			return false;
		String str = folder.stringValue();
		assert str.endsWith("/");
		int split = str.lastIndexOf('/', str.length() - 2);
		if (split < 0)
			return false;
		String label = str.substring(split + 1, str.length() - 1).replace('-',
				' ');
		URI parent = vf.createURI(str.substring(0, split + 1));
		createFolder(parent, con);
		try {
			Update update = con.prepareUpdate(QueryLanguage.SPARQL,
					INSERT_FOLDER);
			update.setBinding("folder", folder);
			update.setBinding("parent", parent);
			if (folderType != null) {
				update.setBinding("folderType", folderType);
			}
			update.setBinding("label", vf.createLiteral(label));
			update.execute();
			return true;
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		}
	}

	private synchronized String getNamespace() {
		GregorianCalendar cal = new GregorianCalendar();
		if (date == null || date.get(Calendar.DATE) != cal.get(Calendar.DATE)
				|| date.get(Calendar.MONTH) != cal.get(Calendar.MONTH)
				|| date.get(Calendar.YEAR) != cal.get(Calendar.YEAR)) {
			date = cal;
			return namespace = uriSpace + date.get(Calendar.YEAR) + "/"
					+ zero(date.get(Calendar.MONTH) + 1) + "/"
					+ zero(date.get(Calendar.DATE)) + "/";
		}
		return namespace;
	}

	private String zero(int number) {
		if (number < 10)
			return "0" + number;
		return String.valueOf(number);
	}
}
