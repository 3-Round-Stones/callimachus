package org.callimachusproject.engine;

import static org.callimachusproject.engine.helpers.SPARQLWriter.toSPARQL;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.engine.helpers.OrderedSparqlReader;
import org.callimachusproject.engine.helpers.RDFaProducer;
import org.callimachusproject.engine.helpers.SPARQLProducer;
import org.callimachusproject.engine.helpers.XMLElementReader;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.xslt.XMLEventReaderFactory;
import org.openrdf.repository.object.xslt.XSLTransformer;

public class Template {
	static final XSLTransformer HTML_XSLT;
	static {
		String path = "org/callimachusproject/xsl/xhtml-to-html.xsl";
		ClassLoader cl = Template.class.getClassLoader();
		String url = cl.getResource(path).toExternalForm();
		InputStream input = cl.getResourceAsStream(path);
		InputStreamReader reader = new InputStreamReader(input);
		HTML_XSLT = new XSLTransformer(reader, url);
	}
	private final RepositoryConnection con;
	private final String source;
	private final String systemId;
	private final XMLEventReaderFactory factory;

	protected Template(String source, String systemId, RepositoryConnection con) {
		this.con = con;
		this.systemId = systemId;
		factory = XMLEventReaderFactory.newInstance();
		this.source = source;
	}

	public String getSystemId() {
		return systemId;
	}

	public String getSource() {
		return source;
	}

	public void construct(BindingSet bindings, OutputStream out)
			throws TemplateException {
		XMLEventReader xhtml = openResultReader(getQuery(), bindings);
		try {
			HTML_XSLT.transform(xhtml, systemId).toOutputStream(out);
		} catch (IOException e) {
			throw new TemplateException(e);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		} catch (TransformerException e) {
			throw new TemplateException(e);
		}
	}

	public String getQuery() throws TemplateException {
		try {
			return toSPARQL(openQueryReader());
		} catch (RDFParseException e) {
			throw new TemplateException(e);
		} catch (IOException e) {
			throw new TemplateException(e);
		}
	}

	public XMLEventReader openSourceReader() throws TemplateException {
		StringReader reader = new StringReader(getSource());
		try {
			return factory.createXMLEventReader(systemId, reader);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		}
	}

	public RDFEventReader openQueryReader() throws TemplateException {
		XMLEventReader xml = openSourceReader();
		RDFEventReader reader = new RDFaReader(systemId, xml, systemId);
		RDFEventReader sparql = new SPARQLProducer(reader);
		return new OrderedSparqlReader(sparql);
	}

	public XMLEventReader openResultReader(String sparql, BindingSet bindings)
			throws TemplateException {
		// evaluate SPARQL derived from the template
		try {
			TupleQuery q = con.prepareTupleQuery(SPARQL, sparql, systemId);
			for (Binding bind : bindings) {
				q.setBinding(bind.getName(), bind.getValue());
			}
			TupleQueryResult results = q.evaluate();
			Map<String, String> origins = SPARQLProducer.getOrigins(sparql);
			XMLEventReader xml = openSourceReader();
			return new RDFaProducer(xml, results, origins, bindings, con);
		} catch (MalformedQueryException e) {
			throw new TemplateException(e);
		} catch (RepositoryException e) {
			throw new TemplateException(e);
		} catch (QueryEvaluationException e) {
			throw new TemplateException(e);
		}
	}

	public Template getElement(String xptr) throws TemplateException,
			IllegalArgumentException {
		if (xptr == null || xptr.equals("/1"))
			return this;
		XMLEventReader xml = openSourceReader();
		try {
			xml = new XMLElementReader(xml, xptr);
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			StringWriter writer = new StringWriter(8192);
			XMLEventWriter xmlWriter = factory.createXMLEventWriter(writer);
			try {
				xmlWriter.add(xml);
			} finally {
				xmlWriter.close();
			}
			String source = writer.toString();
			return new Template(source, systemId, con);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		}
	}

}
