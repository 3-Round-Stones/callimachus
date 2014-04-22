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
package org.callimachusproject.behaviours;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

import org.callimachusproject.concepts.Composite;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.form.helpers.TripleInserter;
import org.callimachusproject.io.CarOutputStream;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.io.DescribeResult;
import org.callimachusproject.io.ProducerStream;
import org.callimachusproject.io.ProducerStream.OutputProducer;
import org.callimachusproject.io.TurtleStreamWriterFactory;
import org.callimachusproject.server.exceptions.BadRequest;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Sparql;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.RDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FolderSupport implements RDFObject {
	private static final String PHONE = "http://www.openrdf.org/rdf/2011/keyword#phone";
	private static final String GENERATED_BY = "http://www.w3.org/ns/prov#wasGeneratedBy";
	private static final String TURTLE = "text/turtle;charset=UTF-8";
	private static final String PREFIX = "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX prov:<http://www.w3.org/ns/prov#>\n"
			+ "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final String CALLI_FOLDER = "http://callimachusproject.org/rdf/2009/framework#Folder";
	private final Logger logger = LoggerFactory.getLogger(FolderSupport.class);

	/**
	 * Called from folder.ttl and origin.ttl
	 */
	public String resolve(String reference) {
		return TermFactory.newInstance(this.toString()).resolve(reference);
	}

	public InputStream exportFolder() throws IOException {
		final String baseURI = this.getResource().stringValue();
		final ObjectConnection con = this.getObjectConnection();
		return new ProducerStream(new OutputProducer() {
			public void produce(OutputStream outputStream) throws IOException {
				try {
					CarOutputStream carStream = new CarOutputStream(
							outputStream);
					exportComponents(baseURI, con, carStream);
					carStream.finish();
				} catch (OpenRDFException e) {
					logger.error(e.toString(), e);
					throw new IOException(e);
				} catch (URISyntaxException e) {
					logger.error(e.toString(), e);
					throw new IOException(e);
				} catch (RuntimeException e) {
					logger.error(e.toString(), e);
					throw e;
				} catch (Error e) {
					logger.error(e.toString(), e);
					throw e;
				}
			}
		});
	}

	public void readFrom(String type, InputStream entryStream, String uri,
			URI graph) throws OpenRDFException, IOException {
		TripleInserter inserter = new TripleInserter(this.getObjectConnection());
		inserter.setGraph(graph);
		RDFParserRegistry registry = RDFParserRegistry.getInstance();
		RDFParser parser = registry
				.get(registry.getFileFormatForMIMEType(type)).getParser();
		parser.setValueFactory(this.getObjectConnection().getValueFactory());
		parser.setRDFHandler(inserter);
		parser.parse(entryStream, uri);
		if (inserter.isEmpty())
			throw new BadRequest("Missing resource information for: " + uri);
		if (!inserter.isSingleton())
			throw new BadRequest("Multiple resources for: " + uri);
		if (!uri.equals(inserter.getSubject().stringValue()))
			throw new BadRequest("Wrong subject of " + inserter.getSubject() + " for: " + uri);
		if (inserter.isDisconnectedNodePresent())
			throw new BadRequest("Blank nodes must be connected in: " + uri);
	}

	public Set<String> getComponentsWithExternalDependent() {
		Set<URI> uris = findComponentsWithExternalDependent();
		HashSet<String> result = new HashSet<String>(uris.size());
		for (URI uri : uris) {
			result.add(uri.stringValue());
		}
		return result;
	}

	public <F extends Composite> Composite findContainer(String uri,
			Class<F> Folder) throws OpenRDFException, IOException {
		ObjectConnection con = this.getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		Composite container = this.findExistingContainer(vf.createURI(uri));
		if (container == null) {
			int idx = uri.lastIndexOf('/', uri.length() - 2) + 1;
			container = designateAsFolder(uri.substring(0, idx), Folder);
		}
		return container;
	}

	public <F extends Composite> Composite designateAsFolder(String uri,
			Class<F> Folder) throws OpenRDFException, IOException {
		ObjectConnection con = this.getObjectConnection();
		RDFObject nonFolder = (RDFObject) con.getObject(uri);
		if (nonFolder instanceof Composite)
			return (Composite) nonFolder;
		ValueFactory vf = con.getValueFactory();
		ObjectFactory of = con.getObjectFactory();
		Resource resource = nonFolder.getResource();
		Composite container = this.findExistingContainer(resource);
		int idx = uri.lastIndexOf('/', uri.length() - 2) + 1;
		if (container == null) {
			container = designateAsFolder(uri.substring(0, idx), Folder);
			container.getCalliHasComponent().add(of.createObject(resource));
		}
		if (!(container instanceof Composite))
			throw new BadRequest("Schema data about " + container
					+ " must be imported before " + uri);
		String label = URLDecoder.decode(uri.substring(idx, uri.length() - 1),
				"UTF-8").replace('-', ' ');
		con.add(resource, RDFS.LABEL, vf.createLiteral(label));
		inheritPermissions(resource, container);
		con.add(resource, RDF.TYPE, vf.createURI(CALLI_FOLDER));
		return con.addDesignation(of.createObject(resource), Folder);
	}

	@Sparql(PREFIX + "INSERT {" + "$resource calli:reader ?reader .\n"
			+ " $reosurce calli:subscriber ?subscriber .\n"
			+ " $resource calli:editor ?editor .\n"
			+ " $resource calli:administrator ?administrator \n" + "} WHERE {"
			+ "{$container calli:reader ?reader\n"
			+ "} UNION {$container calli:subscriber ?subscriber\n"
			+ "} UNION {$container calli:editor ?editor\n"
			+ "} UNION {$conatiner calli:administrator ?administrator}" + "}")
	protected abstract void inheritPermissions(
			@Bind("resource") Resource resource,
			@Bind("container") Composite conatiner);

	@Sparql(PREFIX
			+ "SELECT ?component {\n"
			+ "	$this calli:hasComponent+ ?component .\n"
			+ "	GRAPH ?graph { ?external ?dependent ?component }\n"
			+ "	FILTER (!strstarts(str(?graph), str($this)))\n"
			+ "	FILTER (!isIRI(?external) || !strstarts(str(?external), str($this)))\n"
			+ "	FILTER (?dependent != prov:specializationOf && ?dependent != prov:wasInfluencedBy)\n"
			+ "	FILTER (?dependent != rdf:subject && ?dependent != rdf:object)\n"
			+ "}")
	protected abstract Set<URI> findComponentsWithExternalDependent();

	@Sparql(PREFIX + "SELECT ?composite {"
			+ "    ?composite calli:hasComponent $component\n" + "}")
	protected abstract Composite findExistingContainer(
			@Bind("component") Resource component);

	@Sparql(PREFIX
			+ "ASK {\n"
			+ "$this calli:hasComponent+ ?component .\n"
			+ "?component a calli:Folder .\n"
			+ "{ ?component calli:alternate ?target }\n"
			+ "UNION { ?component calli:canonical ?target }\n"
			+ "UNION { ?component calli:copy ?target }\n"
			+ "UNION { ?component calli:delete ?target }\n"
			+ "UNION { ?component calli:describedby ?target }\n"
			+ "UNION { ?component calli:gone ?target }\n"
			+ "UNION { ?component calli:missing ?target }\n"
			+ "UNION { ?component calli:moved ?target }\n"
			+ "UNION { ?component calli:patch ?target }\n"
			+ "UNION { ?component calli:post ?target }\n"
			+ "UNION { ?component calli:put ?target }\n"
			+ "UNION { ?component calli:resides ?target }\n"
			+ "UNION { # reduced permissions\n"
			+ "$this calli:reader ?reader FILTER NOT EXISTS { ?component calli:reader ?reader }\n"
			+ "$this calli:subscriber ?subscriber FILTER NOT EXISTS { ?component calli:subscriber ?subscriber }\n"
			+ "$this calli:editor ?editor FILTER NOT EXISTS { ?component calli:editor ?editor }\n"
			+ "$this calli:administrator ?administrator FILTER NOT EXISTS { ?component calli:administrator ?administrator }\n"
			+ "} UNION { # additional permissions\n"
			+ "?component calli:reader ?reader FILTER NOT EXISTS { $this calli:reader ?reader }\n"
			+ "?component calli:subscriber ?subscriber FILTER NOT EXISTS { $this calli:subscriber ?subscriber }\n"
			+ "?component calli:editor ?editor FILTER NOT EXISTS { $this calli:editor ?editor }\n"
			+ "?component calli:administrator ?administrator FILTER NOT EXISTS { $this calli:administrator ?administrator }\n"
			+ "}}")
	protected abstract boolean isFolderMetadataPresent()
			throws OpenRDFException;

	@Sparql(PREFIX
			+ "SELECT REDUCED ?component ?lastmod ?fileType ?folder ?class {\n"
			+ "$this calli:hasComponent+ ?component .\n"
			+ "OPTIONAL { ?component prov:wasGeneratedBy/prov:endedAtTime ?lastmod }\n"
			+ "OPTIONAL { ?component a [calli:mediaType ?fileType] }\n"
			+ "OPTIONAL { ?component a calli:Folder BIND (true AS ?folder) }\n"
			+ "OPTIONAL { ?component a owl:Class BIND (true AS ?class)}" + "}")
	protected abstract TupleQueryResult loadComponents()
			throws OpenRDFException;

	private void exportComponents(String baseURI, ObjectConnection con,
			CarOutputStream carStream) throws IOException, OpenRDFException,
			URISyntaxException {
		boolean exportFolder = isFolderMetadataPresent();
		Set<String> writtenNames = new java.util.HashSet<String>();
		TupleQueryResult components = loadComponents();
		while (components.hasNext()) {
			BindingSet result = components.next();
			URI component = (URI) result.getValue("component");
			String entryId = component.stringValue();
			if (entryId.indexOf(baseURI) != 0)
				continue;
			String name = entryId.substring(baseURI.length());
			if (!writtenNames.add(name))
				continue;
			writeEntry(name, component, carStream, result, exportFolder, con);
		}
	}

	private void writeEntry(String name, URI component,
			CarOutputStream carStream, BindingSet result, boolean exportFolder,
			ObjectConnection con) throws IOException, OpenRDFException,
			URISyntaxException {
		Literal lastmod = (Literal) result.getValue("lastmod");
		Literal fileType = (Literal) result.getValue("fileType");
		boolean isFolder = result.hasBinding("folder");
		boolean isClass = result.hasBinding("class");
		String entryId = component.stringValue();
		InputStream content;
		long time;
		if (lastmod != null) {
			time = lastmod.calendarValue().toGregorianCalendar()
					.getTimeInMillis();
		} else {
			time = java.lang.System.currentTimeMillis();
		}
		if (name.lastIndexOf('/') == name.length() - 1 && isFolder) {
			// # Export Folder
			if (exportFolder) {
				// # Export Folder Triples
				OutputStream entry = carStream.writeResourceEntry(name, time,
						TURTLE);
				writeTriples(component, con, entry, entryId);
				exportFolder = true;
			} else {
				carStream.writeFolderEntry(name, time).close();
			}
		} else if (fileType != null
				&& (content = con.getBlobObject(component).openInputStream()) != null) {
			// # Export File
			try {
				OutputStream entryStream = carStream.writeFileEntry(name, time,
						fileType.stringValue());
				try {
					ChannelUtil.transfer(content, entryStream);
				} finally {
					entryStream.close();
				}
			} finally {
				content.close();
			}
		} else if (isClass) {
			// # Export Schema
			OutputStream entry = carStream.writeSchemaEntry(name, time, TURTLE);
			writeTriples(component, con, entry, entryId);
		} else {
			// # Export Triples
			OutputStream entry = carStream.writeResourceEntry(name, time,
					TURTLE);
			writeTriples(component, con, entry, entryId);
		}
	}

	private void writeTriples(URI component, ObjectConnection con,
			OutputStream resourceEntry, String entryId)
			throws OpenRDFException, URISyntaxException, IOException,
			QueryEvaluationException {
		try {
			GraphQueryResult triples = new DescribeResult(component, con);
			try {
				TurtleStreamWriterFactory xf = new TurtleStreamWriterFactory();
				RDFWriter writer = xf.createWriter(resourceEntry, entryId);
				writer.startRDF();
				RepositoryResult<Namespace> namespaces = con.getNamespaces();
				while (namespaces.hasNext()) {
					Namespace ns = namespaces.next();
					writer.handleNamespace(ns.getPrefix(), ns.getName());
				}
				while (triples.hasNext()) {
					Statement st = triples.next();
					String pred = st.getPredicate().stringValue();
					if (!GENERATED_BY.equals(pred) && !PHONE.equals(pred)) {
						writer.handleStatement(st);
					}
				}
				writer.endRDF();
			} finally {
				triples.close();
			}
		} finally {
			resourceEntry.close();
		}
	}

}
