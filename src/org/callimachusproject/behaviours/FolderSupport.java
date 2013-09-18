package org.callimachusproject.behaviours;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Set;

import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.io.CarOutputStream;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.io.DescribeResult;
import org.callimachusproject.io.ProducerStream;
import org.callimachusproject.io.ProducerStream.OutputProducer;
import org.callimachusproject.io.TurtleStreamWriterFactory;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Sparql;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FolderSupport implements RDFObject {
	private static final String TURTLE = "text/turtle;charset=UTF-8";
	private static final String PREFIX = "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n" +
			"PREFIX prov:<http://www.w3.org/ns/prov#>\n" +
			"PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
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
		return new ProducerStream(new OutputProducer(){
			public void produce(OutputStream outputStream) throws IOException {
            try {
				CarOutputStream carStream = new CarOutputStream(outputStream);
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
        }});
	}

	private void exportComponents(String baseURI, ObjectConnection con,
			CarOutputStream carStream) throws IOException, OpenRDFException,
			URISyntaxException {
		boolean exportFolder = false;
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
			Literal lastmod = (Literal) result.getValue("lastmod");
			Literal fileType = (Literal) result.getValue("fileType");
			Literal folder = (Literal) result.getValue("folder");
			boolean isClass = result.hasBinding("class");
			InputStream content = con.getBlobObject(component).openInputStream();
			long time;
			if (lastmod != null) {
				time = lastmod.calendarValue().toGregorianCalendar().getTimeInMillis();
			} else {
				time = java.lang.System.currentTimeMillis();
			}
			if (name.lastIndexOf('/') == name.length() - 1 && folder != null) {
				//# Export Folder
				if (exportFolder || folder.booleanValue()) {
					//# Export Folder Triples
					OutputStream resourceEntry = carStream.writeResourceEntry(name, time, TURTLE);
					writeTriples(component, con, resourceEntry, entryId);
					exportFolder = true;
				} else {
					carStream.writeFolderEntry(name, time).close();
				}
			} else if (content != null && fileType != null) {
				//# Export File
				try {
					OutputStream entryStream = carStream.writeFileEntry(name, time, fileType.stringValue());
					try {
						ChannelUtil.transfer(content, entryStream);
					} finally {
						entryStream.close();
					}
				} finally {
					content.close();
				}
			} else if (isClass) {
				//# Export Schema
				OutputStream schemaEntry = carStream.writeSchemaEntry(name, time, TURTLE);
				writeTriples(component, con, schemaEntry, entryId);
			} else {
				//# Export Triples
				OutputStream resourceEntry = carStream.writeResourceEntry(name, time, TURTLE);
				writeTriples(component, con, resourceEntry, entryId);
			}
		}
	}

	void writeTriples(URI component, ObjectConnection con,
			OutputStream resourceEntry, String entryId)
			throws OpenRDFException, URISyntaxException, IOException,
			QueryEvaluationException {
		GraphQueryResult triples = new DescribeResult(component, con);
		try {
			writeTo(triples, resourceEntry, entryId);
		} finally {
			triples.close();
		}
	}

	@Sparql(PREFIX + "SELECT REDUCED ?component ?lastmod ?fileType ?folder ?class {\n" +
			"$this calli:hasComponent+ ?component .\n" +
			"OPTIONAL { ?component prov:wasGeneratedBy/prov:endedAtTime ?lastmod }\n" +
			"OPTIONAL { ?component a [calli:mediaType ?fileType] }\n" +
			"OPTIONAL { ?component a calli:Folder\n" +
			"OPTIONAL { BIND (true AS ?exportFolder)\n" +
			"FILTER EXISTS {\n" +
			"{ ?component calli:alternate ?target }\n" +
			"UNION { ?component calli:canonical ?target }\n" +
			"UNION { ?component calli:copy ?target }\n" +
			"UNION { ?component calli:delete ?target }\n" +
			"UNION { ?component calli:describedby ?target }\n" +
			"UNION { ?component calli:gone ?target }\n" +
			"UNION { ?component calli:missing ?target }\n" +
			"UNION { ?component calli:moved ?target }\n" +
			"UNION { ?component calli:patch ?target }\n" +
			"UNION { ?component calli:post ?target }\n" +
			"UNION { ?component calli:put ?target }\n" +
			"UNION { ?component calli:resides ?target }\n" +
			"UNION { # reduced permissions\n" +
			"$this calli:reader ?reader FILTER NOT EXISTS { ?component calli:reader ?reader }\n" +
			"$this calli:subscriber ?subscriber FILTER NOT EXISTS { ?component calli:subscriber ?subscriber }\n" +
			"$this calli:editor ?editor FILTER NOT EXISTS { ?component calli:editor ?editor }\n" +
			"$this calli:administrator ?administrator FILTER NOT EXISTS { ?component calli:administrator ?administrator }\n" +
			"} UNION { # additional permissions\n" +
			"?component calli:reader ?reader FILTER NOT EXISTS { $this calli:reader ?reader }\n" +
			"?component calli:subscriber ?subscriber FILTER NOT EXISTS { $this calli:subscriber ?subscriber }\n" +
			"?component calli:editor ?editor FILTER NOT EXISTS { $this calli:editor ?editor }\n" +
			"?component calli:administrator ?administrator FILTER NOT EXISTS { $this calli:administrator ?administrator }\n" +
			"}}}\n" +
			"BIND (bound(?exportFolder) AS ?folder) }\n" +
			"OPTIONAL { ?component a ?class FILTER (owl:Class = ?class)}" +
			"}")
	protected abstract TupleQueryResult loadComponents() throws OpenRDFException;

	private void writeTo(GraphQueryResult triples, OutputStream entryStream,
			String baseURI) throws URISyntaxException, OpenRDFException,
			IOException {
		try {
			TurtleStreamWriterFactory xf = new TurtleStreamWriterFactory();
			RDFWriter writer = xf.createWriter(entryStream, baseURI);
			writer.startRDF();
			RepositoryResult<Namespace> namespaces = this.getObjectConnection()
					.getNamespaces();
			while (namespaces.hasNext()) {
				Namespace ns = namespaces.next();
				writer.handleNamespace(ns.getPrefix(), ns.getName());
			}
			while (triples.hasNext()) {
				Statement st = triples.next();
				String pred = st.getPredicate().stringValue();
				if (!"http://www.w3.org/ns/prov#wasGeneratedBy".equals(pred)
						&& !"http://www.openrdf.org/rdf/2011/keyword#phone".equals(pred)) {
					writer.handleStatement(st);
				}
			}
			writer.endRDF();
		} finally {
			entryStream.close();
		}
	}

}
