/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.setup;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.clerezza.rdf.core.BNode;
import org.callimachusproject.engine.helpers.SparqlUpdateFactory;
import org.callimachusproject.form.helpers.EntityUpdater;
import org.callimachusproject.form.helpers.TripleInserter;
import org.callimachusproject.io.CarInputStream;
import org.callimachusproject.io.DescribeResult;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.Conflict;
import org.openrdf.http.object.exceptions.ServiceUnavailable;
import org.openrdf.http.object.fluid.MediaType;
import org.openrdf.http.object.io.XMLEventReaderFactory;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.util.Models;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.store.blob.BlobObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebappArchiveImporter {
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	private static final String REALM_TYPE = "types/Realm";
	private static final String ORIGIN_TYPE = "types/Origin";
	private static final String FOLDER_TYPE = "types/Folder";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_FOLDER = CALLI + "Folder";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private static final String PREFIX = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX prov:<http://www.w3.org/ns/prov#>\n"
			+ "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final String EXTERNAL_DEPENDENTS = PREFIX
			+ "SELECT ?component {\n"
			+ "	<> calli:hasComponent+ ?component .\n"
			+ "	GRAPH ?graph { ?external ?dependent ?component }\n"
			+ "	FILTER (!strstarts(str(?graph), str(<>)))\n"
			+ "	FILTER (!isIRI(?external) || !strstarts(str(?external), str(<>)))\n"
			+ "	FILTER (?dependent != prov:specializationOf && ?dependent != prov:wasInfluencedBy)\n"
			+ "	FILTER (?dependent != rdf:subject && ?dependent != rdf:object)\n"
			+ "}";
	private static final String COMPONENTS = PREFIX
			+ "SELECT ?container ?component {\n"
			+ "	<> calli:hasComponent+ ?component .\n"
			+ "	?container calli:hasComponent ?component .\n" + "}";
	private static final String CONSTRUCT_PERMISSIONS = PREFIX + "CONSTRUCT {"
			+ "$resource calli:reader ?reader .\n"
			+ " $reosurce calli:subscriber ?subscriber .\n"
			+ " $resource calli:editor ?editor .\n"
			+ " $resource calli:administrator ?administrator \n" + "} WHERE {"
			+ "{$resource calli:reader ?reader\n"
			+ "} UNION {$resource calli:subscriber ?subscriber\n"
			+ "} UNION {$resource calli:editor ?editor\n"
			+ "} UNION {$resource calli:administrator ?administrator}" + "}";
	private static final String LOOKUP_CONSTRUCTOR = PREFIX
			+ "SELECT REDUCED ?class {\n"
			+ "    {\n"
			+ "        ?class rdfs:subClassOf* <types/File> .\n"
			+ "        FILTER isIRI(?class)\n"
			+ "        FILTER (\n"
			+ "            bound($documentTag) && EXISTS { ?class calli:documentTag $documentTag } ||\n"
			+ "            bound($mediaType) && NOT EXISTS { ?class calli:documentTag ?tag } && EXISTS { ?class calli:mediaType $mediaType }\n"
			+ "        )\n"
			+ "        BIND (1 AS ?preference)\n"
			+ "    } UNION {\n"
			+ "        # boot strap graphs before calli:mediaType triples are loaded\n"
			+ "        BIND (<types/RdfTurtle> AS ?class)\n"
			+ "        FILTER sameTerm($mediaType,\"text/turtle\")\n"
			+ "        BIND (3 AS ?preference)\n"
			+ "    } UNION {\n"
			+ "        BIND (<types/File> AS ?class)\n"
			+ "        BIND (4 AS ?preference)\n" + "    }\n"
			+ "} ORDER BY ?preference LIMIT 1";
	private static final Map<String, String> MEDIA_ALIAS = new HashMap<String, String>() {
		private static final long serialVersionUID = 2360302068339109277L;

		{
			this.put("application/javascript", "text/javascript");
			this.put("text/xsl", "application/xslt+xml");
			this.put("application/java-archive", "application/zip");
			this.put("image/x-png", "image/png");
			this.put("image/pjpeg", "image/jpeg");
		}
	};

	private final Logger logger = LoggerFactory.getLogger(WebappArchiveImporter.class);
	private final String webapp;
	private final CalliRepository repository;
	private final ValueFactory vf;
	private final URI primaryTopic;
	private final URI foafDocument;
	private final URI rdfType;
	private final URI owlClass;
	private final URI rdfsLabel;
	private final URI rdfSource;
	private final URI hasComponent;
	private final URI rdfSchemaGraph;
	private final DatatypeFactory df;

	public WebappArchiveImporter(String webapp, CalliRepository repository)
			throws DatatypeConfigurationException {
		this.webapp = webapp;
		this.repository = repository;
		this.vf = repository.getValueFactory();
		primaryTopic = vf.createURI("http://xmlns.com/foaf/0.1/primaryTopic");
		foafDocument = vf.createURI("http://xmlns.com/foaf/0.1/Document");
		rdfType = vf.createURI(RDF.TYPE.stringValue());
		owlClass = vf.createURI(OWL.CLASS.stringValue());
		rdfsLabel = vf.createURI(RDFS.LABEL.stringValue());
		rdfSource = vf.createURI("http://www.w3.org/ns/ldp#RDFSource");
		hasComponent = vf.createURI(CALLI_HASCOMPONENT);
		rdfSchemaGraph = vf.createURI(webapp + "types/RdfSchemaGraph");
		df = DatatypeFactory.newInstance();
	}

	public void importArchive(InputStream carStream, String folder)
			throws IOException, OpenRDFException, ReflectiveOperationException,
			XMLStreamException {
		ObjectConnection con = repository.getConnection();
		try {
			con.begin();
			createFolder(folder, webapp, con);
			con = importArchive(carStream, folder, webapp, con);
			con.commit();
		} finally {
			con.close();
		}
	}

	public void removeFolder(String folder) throws OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			con.begin();
			deleteComponents(Collections.singleton(vf.createURI(folder)), con);
			con.commit();
		} finally {
			con.close();
		}
	}

	private ObjectConnection importArchive(InputStream carStream, String folderUri,
			String webapp, ObjectConnection con) throws IOException,
			OpenRDFException, ReflectiveOperationException, XMLStreamException {
		RepositoryConnection schema = null;
		try {
			Set<URI> missingDependees = getDependees(folderUri, con);
			Set<URI> existingSources = getExistingSources(folderUri, con);
			int capacity = Math.max(existingSources.size(), 256);
			Set<URI> includedSources = new LinkedHashSet<URI>(capacity);
			Set<URI> updatedSources = new LinkedHashSet<URI>(capacity);
			Map<URI, MediaType> updatedNonSources = new LinkedHashMap<URI, MediaType>(capacity);
			CarInputStream car = new CarInputStream(carStream, folderUri);
			try {
				while (car.readEntryName() != null) {
					boolean error = true;
					try {
						URI uri = vf.createURI(car.getEntryIRI());
						if (car.isFolderEntry()) {
							URI entity = createFolder(car.getEntryIRI(), webapp, con);
							includedSources.add(entity);
						} else if (car.isSourceEntry()) {
							Model insertData = car.getEntryModel(vf);
							URI entity = insertData.filter(uri, primaryTopic, null).objectURI();
							includedSources.add(entity);
							if (!existingSources.contains(entity)) {
								URI container = findContainer(entity, con);
								insert(insertData, uri, entity, container, con);
								updatedSources.add(entity);
							} else if (update(insertData, uri, entity, con)) {
								updatedSources.add(entity);
							}
							if (insertData.contains(entity, rdfType, owlClass)) {
								if (schema == null) {
									schema = openSchemaConnection();
									schema.begin();
								}
								schema.clear(entity);
								schema.add(insertData, entity);
							}
						} else if (car.isFileEntry()) {
							MediaType type = MediaType.valueOf(car.getEntryType());
							String charset = type.getParameter("charset");
							if (charset == null) {
								updatedNonSources.put(uri, type);
								store(car.getEntryStream(), uri, con);
							} else {
								updatedNonSources.put(uri, type);
								store(car.getEntryStream(), charset, uri, con);
							}
							String media = type.getBaseType();
							schema = insertRdfGraph(uri, media, con, schema);
						}
						error = false;
					} finally {
						if (error) {
							logger.error("Could not import {}", car.getEntryName());
						}
						car.getEntryStream().close();
					}
				}
			} finally {
				car.close();
			}
			updatedNonSources.keySet().removeAll(existingSources);
			updatedNonSources.keySet().removeAll(includedSources);
			existingSources.removeAll(includedSources);
			existingSources.removeAll(updatedNonSources.keySet());
			deleteComponents(existingSources, con);
			missingDependees.removeAll(includedSources);
			missingDependees.removeAll(updatedNonSources.keySet());
			checkForMissingDependees(missingDependees, con);
			// FIXME LOOKUP_CONSTRUCTOR query is too complex for OptimisticSail
			con.commit();
			con.begin();
			Model mediaSources = new LinkedHashModel();
			if (!updatedNonSources.isEmpty()) {
				XMLGregorianCalendar now = df
						.newXMLGregorianCalendar(new GregorianCalendar(UTC));
				Map<String, URI> constructors = new HashMap<String, URI>();
				for (URI file : updatedNonSources.keySet()) {
					MediaType type = updatedNonSources.get(file);
					try {
						mediaSources.addAll(addMediaSource(file, type, constructors, con, now));
					} catch (XMLStreamException e) {
						logger.error("Could not parse {} of type {}", file, type);
						throw e;
					}
				}
			}
			con.add(mediaSources);
			updatedSources.addAll(updatedNonSources.keySet());
			Set<URI> notValidated = validateSources(updatedSources, con);
			if (schema != null) {
				con.commit();
				con.close();
				schema.commit();
				schema.close(); // recompiling
				schema = null;
				con = repository.getConnection();
				con.begin();
				Set<URI> couldNotValidate = validateSources(notValidated, con);
				if (!couldNotValidate.isEmpty()) {
					logger.warn("Could not validate: {}", couldNotValidate);
				}
				return con;
			} else if (!notValidated.isEmpty()) {
				logger.warn("Could not validate: {}", notValidated);
				return con;
			} else {
				return con;
			}
		} finally {
			if (schema != null) {
				schema.rollback();
				schema.close();
			}
		}
	}

	private Model addMediaSource(URI file, MediaType type,
			Map<String, URI> constructors, ObjectConnection con,
			XMLGregorianCalendar now) throws RepositoryException, IOException,
			XMLStreamException, OpenRDFException {
		Model model = new LinkedHashModel();
		String media = type.getBaseType();
		String filename = file.getLocalName().replaceAll("(.*)\\.[a-zA-Z]+$",
				"$1");
		String label = PercentCodec.decode(filename).replaceAll(
				"[_\\-\\+\\s]+", " ");
		model.add(file, rdfsLabel, vf.createLiteral(label));
		model.add(file, DCTERMS.CREATED, vf.createLiteral(now));
		URI documentTag = getDocumentTag(con.getBlobObject(file), media);
		URI construct = lookupConstructor(documentTag, media, con, constructors);
		model.add(file, rdfType, foafDocument);
		model.add(file, rdfType, construct);
		URI container = findContainer(file, con);
		assert container != null;
		model.add(container, hasComponent, file);
		for (Statement st : constructPermissions(container, con)) {
			model.add(file, st.getPredicate(), st.getObject());
		}
		return model;
	}

	private RepositoryConnection insertRdfGraph(URI file, String media, ObjectConnection con, RepositoryConnection schema)
			throws RepositoryException, IOException, RDFParseException {
		if (RDFFormat.TURTLE.hasDefaultMIMEType(media) || RDFFormat.RDFXML.hasDefaultMIMEType(media)) {
			con.clear(file);
			insertGraph(con.getBlobObject(file), file, media, con);
			if (con.hasStatement(file, rdfType, rdfSchemaGraph, file)) {
				if (schema == null) {
					schema = openSchemaConnection();
					schema.begin();
				}
				schema.clear(file);
				insertGraph(con.getBlobObject(file), file, media, schema);
				return schema;
			}
		}
		return schema;
	}

	private void insertGraph(BlobObject blob, URI file, String media, RepositoryConnection con)
			throws IOException, RepositoryException, RDFParseException {
		if (RDFFormat.TURTLE.hasDefaultMIMEType(media)) {
			Reader reader = blob.openReader(false);
			try {
				con.add(reader, file.stringValue(), Rio.getParserFormatForMIMEType(media), file);
			} finally {
				reader.close();
			}
		} else if (RDFFormat.RDFXML.hasDefaultMIMEType(media)) {
			InputStream in = blob.openInputStream();
			try {
				con.add(in, file.stringValue(), Rio.getParserFormatForMIMEType(media), file);
			} finally {
				in.close();
			}
		}
	}

	private RepositoryConnection openSchemaConnection()
			throws RepositoryException {
		return repository.openSchemaConnection();
	}

	private URI getDocumentTag(BlobObject file, String type)
			throws IOException, XMLStreamException {
		if (type.indexOf("text/xml") != 0
				&& type.indexOf("application/xml") != 0
				&& type.indexOf("+xml") < 0)
			return null;
		InputStream in = file.openInputStream();
		try {
			XMLEventReaderFactory xmlFactory = XMLEventReaderFactory
					.newInstance();
			String base = file.toUri().toASCIIString();
			XMLEventReader xml = xmlFactory.createXMLEventReader(base, in);
			if (xml == null)
				throw new BadRequest("Document cannot be empty");
			while (xml.hasNext()) {
				XMLEvent tag = xml.nextEvent();
				if (tag.isStartElement()) {
					QName qname = tag.asStartElement().getName();
					String ns = qname.getNamespaceURI();
					if (ns.indexOf('#') < 0
							&& ns.lastIndexOf('/') < ns.length() - 1
							&& ns.lastIndexOf(':') < ns.length() - 1) {
						ns = ns + '#';
					}
					return vf.createURI(ns, qname.getLocalPart());
				}
			}
			return null;
		} finally {
			in.close();
		}
	}

	private URI lookupConstructor(URI documentTag, String media,
			ObjectConnection con, Map<String, URI> constructors) throws OpenRDFException {
		String key = media + (documentTag == null ? "" : " " + documentTag.stringValue());
		if (constructors.containsKey(key))
			return constructors.get(key);
		TupleQuery rq = con.prepareTupleQuery(QueryLanguage.SPARQL,
				LOOKUP_CONSTRUCTOR, webapp);
		if (documentTag != null) {
			rq.setBinding("documentTag", documentTag);
		}
		if (MEDIA_ALIAS.containsKey(media)) {
			rq.setBinding("mediaType", vf.createLiteral(MEDIA_ALIAS.get(media)));
		} else if (media != null) {
			rq.setBinding("mediaType", vf.createLiteral(media));
		}
		TupleQueryResult result = rq.evaluate();
		try {
			URI value = (URI) result.next().getValue("class");
			constructors.put(key, value);
			return value;
		} finally {
			result.close();
		}
	}

	private Set<URI> getDependees(String folderUri, ObjectConnection con)
			throws OpenRDFException {
		TupleQueryResult rs = con.prepareTupleQuery(QueryLanguage.SPARQL,
				EXTERNAL_DEPENDENTS, folderUri).evaluate();
		try {
			Set<URI> result = new LinkedHashSet<URI>();
			while (rs.hasNext()) {
				result.add((URI) rs.next().getValue("component"));
			}
			return result;
		} finally {
			rs.close();
		}
	}

	private void checkForMissingDependees(Set<URI> dependees, ObjectConnection con) throws OpenRDFException {
		for (URI obj : dependees) {
			RepositoryResult<Statement> stmts = con.getStatements(null, null, obj, true);
			while (stmts.hasNext()) {
				Statement st = stmts.next();
				String ns = st.getPredicate().getNamespace();
                if ("http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(ns))
                    continue;
                if ("http://www.w3.org/ns/prov#".equals(ns))
                    continue;
                if ("http://www.openrdf.org/rdf/2009/auditing#".equals(ns))
                    continue;
                throw new BadRequest("Required resource(s) not present, including: " + st.getObject());
			}
		}
	}

	private Set<URI> getExistingSources(String folderUri, ObjectConnection con)
			throws OpenRDFException {
		TupleQueryResult rs = con.prepareTupleQuery(QueryLanguage.SPARQL,
				COMPONENTS, folderUri).evaluate();
		try {
			Set<URI> result = new LinkedHashSet<URI>();
			while (rs.hasNext()) {
				result.add((URI) rs.next().getValue("component"));
			}
			return result;
		} finally {
			rs.close();
		}
	}

	private Model getRDFSource(URI entity, URI doc, ObjectConnection con) throws OpenRDFException {
		Model model = new LinkedHashModel();
        model.add(doc, rdfType, rdfSource);
        model.add(doc, primaryTopic, entity);
        DescribeResult result = new DescribeResult(entity, con);
        try {
        	while (result.hasNext()) {
        		model.add(result.next());
        	}
        } finally {
        	result.close();
        }
        return model;
	}

	private URI findContainer(URI uri, ObjectConnection con)
			throws OpenRDFException, IOException {
		URI container = getExistingContainer(uri, con);
		if (container == null) {
			String folder = getParentFolder(uri.stringValue());
			if (folder == null)
				return null;
			return createFolder(folder, webapp, con);
		}
		return container;
	}

	private URI getExistingContainer(URI uri, ObjectConnection con)
			throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		RepositoryResult<Statement> stmts = con.getStatements(null,
				vf.createURI(CALLI_HASCOMPONENT), uri);
		try {
			try {
				if (stmts.hasNext())
					return (URI) stmts.next().getSubject();
			} finally {
				if (stmts.hasNext())
					return null;
			}
		} finally {
			stmts.close();
		}
		return null;
	}

	private boolean update(Model insertData, URI doc, URI entity,
			ObjectConnection con)
			throws OpenRDFException, QueryEvaluationException, IOException {
		Model deleteData = getRDFSource(entity, doc, con);
		if (Models.isomorphic(insertData, deleteData))
			return false;
		SparqlUpdateFactory update = new SparqlUpdateFactory(doc.stringValue());
		String ru = update.replacement(deleteData, insertData);
		EntityUpdater updater = new EntityUpdater(entity, doc.stringValue());
		updater.executeUpdate(ru, con);
		return true;
	}

	private void insert(Model insertData, URI doc, URI entity, URI container,
			ObjectConnection con) throws RepositoryException, OpenRDFException {
		TripleInserter inserter = new TripleInserter(con);
		inserter.insert(insertData, doc.stringValue());
		if (inserter.isEmpty())
			throw new BadRequest("Missing resource information for: " + doc);
		if (!inserter.isSingleton())
			throw new BadRequest("Multiple resources for: " + doc);
		if (!entity.equals(inserter.getPrimaryTopic()))
			throw new BadRequest("Wrong topic of " + inserter.getPrimaryTopic() + " for: " + doc);
		if (inserter.isDisconnectedNodePresent())
			throw new BadRequest("Blank nodes must be connected in: " + doc);
		if (inserter.isContainmentTriplePresent())
			throw new Conflict("ldp:contains is prohibited");
		con.add(container, hasComponent, entity);
	}

	private void store(InputStream in, URI uri, ObjectConnection con)
			throws RepositoryException, IOException {
		try {
			BlobObject blob = con.getBlobObject(uri);
			OutputStream out = blob.openOutputStream();
			try {
				int read;
				byte[] buf = new byte[1024];
				while ((read = in.read(buf)) >= 0) {
					out.write(buf, 0, read);
				}
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
	}

	private void store(InputStream in, String charset, URI uri,
			ObjectConnection con) throws UnsupportedEncodingException,
			RepositoryException, IOException {
		Reader reader = new InputStreamReader(in, charset);
		try {
			BlobObject blob = con.getBlobObject(uri);
			Writer writer = blob.openWriter();
			try {
				int read;
				char[] cbuf = new char[1024];
				while ((read = reader.read(cbuf)) >= 0) {
					writer.write(cbuf, 0, read);
				}
			} finally {
				writer.close();
			}
		} finally {
			reader.close();
		}
	}

	private void deleteComponents(Set<URI> deletedSources, ObjectConnection con) throws OpenRDFException {
		for (Resource resource : followResources(deletedSources, con)) {
			if (resource instanceof URI) {
				con.clear(resource);
				con.getBlobObject((URI) resource).delete();
				con.remove((Resource) null, hasComponent, resource);
			}
			con.remove(resource, null, null);
		}
	}

	private Set<Resource> followResources(Set<URI> resources,
			ObjectConnection con) throws RepositoryException {
		Set<Resource> result = new LinkedHashSet<Resource>(resources.size());
		Queue<Resource> queue = new ArrayDeque<Resource>(resources);
		Resource res;
		while ((res = queue.poll()) != null) {
			String hash = res.stringValue() + "#";
			String qry = res.stringValue() + "?";
			RepositoryResult<Statement> stmts = con.getStatements(res, null,
					null);
			try {
				while (stmts.hasNext()) {
					Statement st = stmts.next();
					Value obj = st.getObject();
					if (res.equals(obj) || obj instanceof Literal)
						continue;
					if (obj instanceof BNode
							|| st.getPredicate().equals(hasComponent)
							|| obj.stringValue().startsWith(hash)
							|| obj.stringValue().startsWith(qry)) {
						queue.add((Resource) obj);
					}
				}
			} finally {
				stmts.close();
			}
			result.add(res);
		}
		return result;
	}

	private Set<URI> validateSources(Set<URI> updatedSources, ObjectConnection con)
			throws RepositoryException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		Set<URI> notValidated = new LinkedHashSet<URI>(updatedSources.size());
		if (!updatedSources.isEmpty()) {
			for (URI entity : updatedSources) {
				Object object = con.getObject(entity);
				try {
					object.getClass().getMethod("Validate").invoke(object);
				} catch (NoSuchMethodException e) {
					notValidated.add(entity);
				} catch (InvocationTargetException e) {
					try {
						throw e.getCause();
					} catch (ServiceUnavailable cause) {
						logger.info("Storing {}", entity);
					} catch (RepositoryException cause) {
						throw cause;
					} catch (Error cause) {
						throw cause;
					} catch (RuntimeException cause) {
						throw cause;
					} catch (Throwable cause) {
						throw e;
					}
				}
			}
		}
		return notValidated;
	}

	private URI createFolder(String folder, String webapp,
			ObjectConnection con) throws OpenRDFException {
		String parent = getParentFolder(folder);
		if (parent != null) {
			createFolder(parent, webapp, con);
		}
		ValueFactory vf = con.getValueFactory();
		URI uri = vf.createURI(folder);
		if (parent == null) {
			RepositoryResult<Statement> stmts = con.getStatements(uri,
					RDF.TYPE, null);
			try {
				while (stmts.hasNext()) {
					Statement st = stmts.next();
					String type = st.getObject().stringValue();
					if (type.endsWith(ORIGIN_TYPE)
							|| type.endsWith(REALM_TYPE)
							|| type.endsWith(FOLDER_TYPE)) {
						return uri;
					}
				}
			} finally {
				stmts.close();
			}
			throw new IllegalStateException(
					"Can only import a CAR within a previously defined origin or realm");
		} else {
			if (con.hasStatement(uri, RDF.TYPE,
					vf.createURI(webapp + ORIGIN_TYPE)))
				return uri;
			if (con.hasStatement(uri, RDF.TYPE,
					vf.createURI(webapp + REALM_TYPE)))
				return uri;
			if (con.hasStatement(uri, RDF.TYPE,
					vf.createURI(webapp + FOLDER_TYPE)))
				return uri;
			URI container = vf.createURI(parent);
			if (con.hasStatement(container, hasComponent, uri))
				return uri;
			logger.info("Creating {}", uri);
			con.add(container, hasComponent, uri);
			String label = folder.substring(parent.length())
					.replace("/", "").replace('-', ' ');
			con.add(uri, RDF.TYPE, vf.createURI(CALLI_FOLDER));
			con.add(uri, RDF.TYPE, vf.createURI(webapp + FOLDER_TYPE));
			con.add(uri, RDFS.LABEL, vf.createLiteral(label));
			for (Statement st : constructPermissions(container, con)) {
				con.add(uri, st.getPredicate(), st.getObject());
			}
			return uri;
		}
	}

	private Collection<Statement> constructPermissions(URI resource,
			ObjectConnection con) throws OpenRDFException {
		GraphQuery rq = con.prepareGraphQuery(QueryLanguage.SPARQL,
				CONSTRUCT_PERMISSIONS, resource.stringValue());
		rq.setBinding("resource", resource);
		StatementCollector sc = new StatementCollector();
		rq.evaluate(sc);
		return sc.getStatements();
	}

	private String getParentFolder(String folder) {
		int idx = folder.lastIndexOf('/', folder.length() - 2);
		if (idx < 0)
			return null;
		String parent = folder.substring(0, idx + 1);
		if (parent.endsWith("://"))
			return null;
		return parent;
	}

}
