package org.callimachusproject.stream;

import java.util.HashMap;
import java.util.Map;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Namespace;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Triple;
import org.callimachusproject.rdfa.model.CURIE;

public class PrependTriple extends PipedRDFEventReader {
	private Triple triple;
	private boolean begining = true;
	private Map<String, String> namespaces = new HashMap<String, String>();

	public PrependTriple(Triple triple, RDFEventReader reader) {
		super(reader);
		this.triple = triple;
		if (triple.getSubject().isCURIE()) {
			CURIE subj = triple.getSubject().asCURIE();
			namespaces.put(subj.getPrefix(), subj.getNamespaceURI());
		}
		if (triple.getPredicate().isCURIE()) {
			CURIE pred = triple.getPredicate().asCURIE();
			namespaces.put(pred.getPrefix(), pred.getNamespaceURI());
		}
		if (triple.getObject().isCURIE()) {
			CURIE obj = triple.getObject().asCURIE();
			namespaces.put(obj.getPrefix(), obj.getNamespaceURI());
		}
	}

	@Override
	protected void process(RDFEvent next) throws RDFParseException {
		if (begining && triple != null && !next.isStartDocument()
				&& !next.isBase()) {
			if (next.isNamespace()) {
				namespaces.remove(next.asNamespace().getPrefix());
			} else if (!namespaces.isEmpty()) {
				for (Map.Entry<String, String> e : namespaces.entrySet()) {
					add(new Namespace(e.getKey(), e.getValue()));
				}
				namespaces.clear();
			}
			if (next.isEndDocument()) {
				begining = false;
				add(triple);
			} else if (next.isEndSubject()) {
				begining = false;
				add(triple);
			} else if (next.isTriplePattern()) {
				begining = false;
				add(triple);
			}
		}
		add(next);
	}

}
