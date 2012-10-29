package org.callimachusproject.engine;

import static org.callimachusproject.engine.helpers.SPARQLWriter.toSPARQL;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.callimachusproject.engine.events.Base;
import org.callimachusproject.engine.events.Namespace;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.helpers.ClusterCounter;
import org.callimachusproject.engine.helpers.OrderedSparqlReader;
import org.callimachusproject.engine.helpers.RDFaProducer;
import org.callimachusproject.engine.helpers.SPARQLPosteditor;
import org.callimachusproject.engine.helpers.SPARQLProducer;
import org.callimachusproject.engine.helpers.SPARQLWriter;
import org.callimachusproject.engine.helpers.XMLElementReader;
import org.callimachusproject.engine.helpers.XMLEventList;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.engine.model.TermOrigin;
import org.callimachusproject.engine.model.VarOrTerm;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.openrdf.model.Resource;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class Template {
	private static final Pattern PROLOGUE = Pattern.compile("#.*$|BASE\\s*<([^>\\s]*)>|PREFIX\\s+([^:\\s]*)\\s*:\\s*<([^>\\s]*)>", Pattern.CASE_INSENSITIVE);
	private static final Pattern SELECT = Pattern.compile("\\s*(?:#.*(?:$|\n|\r)\\s*)*SELECT\\s+\\?([^\\{\\s]*)\\s*(?:WHERE\\s*)?\\{", Pattern.CASE_INSENSITIVE);
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

	public String getQueryString() throws TemplateException {
		try {
			return toSafeSparql(openQuery());
		} catch (RDFParseException e) {
			throw new TemplateException(e);
		} catch (IOException e) {
			throw new TemplateException(e);
		}
	}

	public String getQueryString(String subQuery) throws TemplateException {
		try {
			int end = 0;
			String projectedVariable = null;
			ClusterCounter reader = new ClusterCounter(openQuery());
			StringWriter str = new StringWriter();
			SPARQLWriter writer = new SPARQLWriter(str);
			Map<String, String> spaces = new HashMap<String, String>();
			while (reader.hasNext()) {
				RDFEvent next = reader.next();
				if (next.isNamespace()) {
					Namespace ns = next.asNamespace();
					spaces.put(ns.getPrefix(), ns.getNamespaceURI());
				} else if (next.isSelect() && end == 0) {
					Matcher m = PROLOGUE.matcher(subQuery);
					while (m.find()) {
						String base = m.group(1);
						String prefix = m.group(2);
						String space = m.group(3);
						if (base != null) {
							writer.write(new Base(base));
							end = m.end();
						} else if (space != null) {
							if (spaces.containsKey(prefix)) {
								if (!space.equals(spaces.get(prefix)))
									throw new IllegalArgumentException("Conflicting prefix: " + prefix);
							} else {
								writer.write(new Namespace(prefix, space));
							}
							end = m.end();
						}
					}
				}
				writer.write(next);
				if (next.isStartWhere() && !reader.peek().isEndWhere() && end < subQuery.length()) {
					String select = subQuery.substring(end);
					Matcher m = SELECT.matcher(select);
					if (m.find()) {
						projectedVariable = m.group(1);
					} else {
						throw new InternalServerError("Query must have exactly one projected variable");
					}
					end = subQuery.length();
					writer.flush();
					str.write("{");
					str.write(select);
					str.write("}");
				}
			}
			for (Set<String> cluster : reader.getClusters()) {
				boolean found = false;
				for (String variable : cluster) {
					if (variable.equals(projectedVariable)) {
						found = true;
						break;
					}
				}
				if (!found)
					throw new InternalServerError("Variables not bound: " + cluster);
			}
			return str.toString();
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
		return evaluate(EmptyBindingSet.getInstance(), con);
	}

	public TupleQueryResult evaluate(BindingSet bindings, RepositoryConnection con)
			throws TemplateException {
		// evaluate SPARQL derived from the template
		try {
			RDFEventReader reader = new RDFaReader(getSystemId(), openSource(), getSystemId());
			SPARQLProducer producer = new SPARQLProducer(reader);
			String sparql = toSafeSparql(new OrderedSparqlReader(producer), bindings);
			TupleQuery q = con.prepareTupleQuery(SPARQL, sparql, getSystemId());
			for (Binding bind : bindings) {
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

	/**
	 * Remove top triple and evaluate.
	 * 
	 * @param partner bound to the object variable of the removed top triple
	 * @param con
	 * @return
	 * @throws TemplateException
	 */
	public TupleQueryResult evaluatePartner(Resource partner,
			RepositoryConnection con) throws TemplateException {
		try {
			RDFEventReader reader = new RDFaReader(getSystemId(), openSource(),
					getSystemId());
			SPARQLProducer rq = new SPARQLProducer(reader);
			SPARQLPosteditor ed = new SPARQLPosteditor(rq);

			// only pass object vars (excluding prop-exps and content) beyond a
			// certain depth:
			// ^(/\d+){3,}$|^(/\d+)*\s.*$
			ed.addEditor(ed.new TriplePatternCutter());

			// find top-level new subjects to bind
			SPARQLPosteditor.TriplePatternRecorder rec;
			ed.addEditor(rec = ed.new TriplePatternRecorder());

			String sparql = toSafeSparql(new OrderedSparqlReader(ed)) + "\nLIMIT 1000";
			TupleQuery qry = con.prepareTupleQuery(SPARQL, sparql,
					getSystemId());
			if (partner != null) {
				for (TriplePattern t : rec.getTriplePatterns()) {
					VarOrTerm vt = t.getSubject();
					if (vt.isVar())
						qry.setBinding(vt.asVar().stringValue(), partner);
				}
			}
			return qry.evaluate();
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
			XMLEventReader xml = openSource();
			return new RDFaProducer(xml, results, origins);
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
			String sparql = toSafeSparql(new OrderedSparqlReader(producer), bindings);
			TupleQuery q = con.prepareTupleQuery(SPARQL, sparql, getSystemId());
			for (Binding bind : bindings) {
				q.setBinding(bind.getName(), bind.getValue());
			}
			TupleQueryResult results = q.evaluate();
			Map<String, TermOrigin> origins = producer.getOrigins();
			XMLEventReader xml = openSource();
			return new RDFaProducer(xml, results, origins);
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

	private String toSafeSparql(OrderedSparqlReader reader, BindingSet bindings) throws RDFParseException, IOException {
		String[] bindingNames = bindings.getBindingNames().toArray(new String[bindings.size()]);
		return toSafeSparql(reader, bindingNames);
	}

	private String toSafeSparql(RDFEventReader reader, String[] bindingNames)
			throws RDFParseException, IOException {
		ClusterCounter counter = new ClusterCounter(reader);
		String sparql = toSPARQL(counter);
		if (counter.getNumberOfVariableClusters(bindingNames) > 0)
			throw new InternalServerError("Variables not connected: " + counter.getSmallestCluster(bindingNames));
		return sparql;
	}

	private String toSafeSparql(RDFEventReader reader)
			throws RDFParseException, IOException {
		ClusterCounter counter = new ClusterCounter(reader);
		String sparql = toSPARQL(counter);
		if (counter.getNumberOfVariableClusters() > 1)
			throw new InternalServerError("Variables not connected: " + counter.getSmallestCluster());
		return sparql;
	}

}
