package org.callimachusproject.engine;

import static org.callimachusproject.engine.helpers.SPARQLWriter.toSPARQL;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.callimachusproject.engine.helpers.OrderedSparqlReader;
import org.callimachusproject.engine.helpers.RDFaProducer;
import org.callimachusproject.engine.helpers.SPARQLProducer;
import org.callimachusproject.engine.helpers.XMLElementReader;
import org.callimachusproject.engine.helpers.XMLEventList;
import org.callimachusproject.engine.model.Term;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.engine.model.TermOrigin;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class Template {
	private final TermFactory systemId;
	private final XMLEventList source;

	protected Template(XMLEventReader source, String systemId) throws XMLStreamException {
		this.systemId = TermFactory.newInstance(systemId);
		this.source = new XMLEventList(source);
	}

	public String toString() {
		return getSystemId();
	}

	public String getSystemId() {
		return systemId.getSystemId();
	}

	public String getQuery() throws TemplateException {
		try {
			return toSPARQL(openQuery());
		} catch (RDFParseException e) {
			throw new TemplateException(e);
		} catch (IOException e) {
			throw new TemplateException(e);
		}
	}

	public XMLEventReader openSource() throws TemplateException {
		return source.iterator();
	}

	public RDFEventReader openQuery() throws TemplateException {
		XMLEventReader xml = openSource();
		RDFEventReader reader = new RDFaReader(getSystemId(), xml, getSystemId());
		try {
			RDFEventReader sparql = new SPARQLProducer(reader);
			return new OrderedSparqlReader(sparql);
		} catch (RDFParseException e) {
			throw new TemplateException(e);
		}
	}

	public TupleQueryResult evaluate(RepositoryConnection con)
			throws TemplateException {
		Map<String, String> parameters = Collections.emptyMap();
		return evaluate(parameters, null, con);
	}

	public TupleQueryResult evaluate(Map<String, ?> parameters, String base, RepositoryConnection con)
			throws TemplateException {
		// evaluate SPARQL derived from the template
		try {
			RDFEventReader reader = new RDFaReader(getSystemId(), openSource(), getSystemId());
			SPARQLProducer producer = new SPARQLProducer(reader);
			String sparql = toSPARQL(new OrderedSparqlReader(producer));
			ValueFactory vf = con.getValueFactory();
			TupleQuery q = con.prepareTupleQuery(SPARQL, sparql, getSystemId());
			for (Binding bind : getBindings(parameters, base, producer, vf)) {
				q.setBinding(bind.getName(), bind.getValue());
			}
			return q.evaluate();
		} catch (MalformedQueryException e) {
			throw new TemplateException(e);
		} catch (RepositoryException e) {
			throw new TemplateException(e);
		} catch (QueryEvaluationException e) {
			throw new TemplateException(e);
		} catch (RDFParseException e) {
			throw new TemplateException(e);
		} catch (IOException e) {
			throw new TemplateException(e);
		}
	}

	public XMLEventReader render(TupleQueryResult results)
			throws TemplateException {
		Map<String, String> parameters = Collections.emptyMap();
		return render(parameters, null, results);
	}

	public XMLEventReader render(Map<String, ?> parameters, String base, TupleQueryResult results)
			throws TemplateException {
		try {
			RDFEventReader reader = new RDFaReader(getSystemId(), openSource(), getSystemId());
			SPARQLProducer producer = new SPARQLProducer(reader);
			try {
				while (producer.hasNext()) {
					producer.next();
				}
			} finally {
				producer.close();
			}
			Map<String, TermOrigin> origins = producer.getOrigins();
			ValueFactoryImpl vf = ValueFactoryImpl.getInstance();
			BindingSet bindings = getBindings(parameters, base, producer, vf);
			XMLEventReader xml = openSource();
			return new RDFaProducer(xml, results, origins, bindings);
		} catch (QueryEvaluationException e) {
			throw new TemplateException(e);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		} catch (RDFParseException e) {
			throw new TemplateException(e);
		}
	}

	public XMLEventReader openResult(BindingSet bindings, RepositoryConnection con)
			throws TemplateException {
		// evaluate SPARQL derived from the template
		try {
			RDFEventReader reader = new RDFaReader(getSystemId(), openSource(), getSystemId());
			SPARQLProducer producer = new SPARQLProducer(reader);
			String sparql = toSPARQL(new OrderedSparqlReader(producer));
			TupleQuery q = con.prepareTupleQuery(SPARQL, sparql, getSystemId());
			for (Binding bind : bindings) {
				q.setBinding(bind.getName(), bind.getValue());
			}
			TupleQueryResult results = q.evaluate();
			Map<String, TermOrigin> origins = producer.getOrigins();
			XMLEventReader xml = openSource();
			return new RDFaProducer(xml, results, origins, bindings);
		} catch (MalformedQueryException e) {
			throw new TemplateException(e);
		} catch (RepositoryException e) {
			throw new TemplateException(e);
		} catch (QueryEvaluationException e) {
			throw new TemplateException(e);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		} catch (RDFParseException e) {
			throw new TemplateException(e);
		} catch (IOException e) {
			throw new TemplateException(e);
		}
	}

	public Template getElement(String xptr) throws TemplateException,
			IllegalArgumentException {
		if (xptr == null || xptr.equals("/1"))
			return this;
		XMLEventReader xml = openSource();
		try {
			xml = new XMLElementReader(xml, xptr);
			return new Template(xml, getSystemId());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		}
	}

	private BindingSet getBindings(Map<String, ?> parameters, String base,
			SPARQLProducer producer, ValueFactory vf) {
		TermFactory tf = systemId;
		if (base != null) {
			TermFactory.newInstance(systemId.reference(base).stringValue());
		}
		MapBindingSet bs = new MapBindingSet();
		for (Map.Entry<String, Term> e : producer.getParameterTerms().entrySet()) {
			String name = e.getKey();
			Term term = e.getValue();
			String value = getString(parameters, name);
			if (value == null) {
				value = term.stringValue();
			}
			if (term.isIRI()) {
				String uri = tf.reference(value).stringValue();
				bs.addBinding(name, vf.createURI(uri));
			} else if (term.isPlainLiteral()) {
				String lang = term.asPlainLiteral().getLang();
				if (lang == null) {
					bs.addBinding(name, vf.createLiteral(value));
				} else {
					bs.addBinding(name, vf.createLiteral(value, lang));
				}
			} else {
				String dt = term.asLiteral().getDatatype().stringValue();
				bs.addBinding(name, vf.createLiteral(value, vf.createURI(dt)));
			}
		}
		return bs;
	}

	private String getString(Map<String, ?> parameters, String name) {
		if (parameters == null)
			return null;
		Object value = parameters.get(name);
		if (value == null)
			return null;
		if (!value.getClass().isArray())
			return value.toString();
		for (int i = 0, n = Array.getLength(value); i < n; i++) {
			Object element = Array.get(value, i);
			if (element != null)
				return element.toString();
		}
		return null;
	}

}
