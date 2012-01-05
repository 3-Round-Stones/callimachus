package org.callimachusproject.engine.expressions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.Triple;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.PlainLiteral;
import org.callimachusproject.engine.model.TermFactory;
import org.openrdf.model.Value;

public class PropertyExpression implements Expression {
	private TermFactory tf;
	private IRI property;
	private String origin;

	public PropertyExpression(String curie, Map<String, String> namespaces,
			TermFactory tf) throws RDFParseException {
		int idx = curie.indexOf(":");
		assert idx >= 0;
		// this may not be a curie
		if (curie.contains("://")) {
			property = tf.iri(curie);
		} else {
			String prefix = curie.substring(0, idx);
			String namespaceURI = namespaces.get(prefix);
			if (namespaceURI == null)
				throw new RDFParseException("Undefined Prefix: " + prefix);
			String reference = curie.substring(idx + 1);
			property = tf.curie(namespaceURI, reference, prefix);
		}
	}

	@Override
	public String getTemplate() {
		return "";
	}

	@Override
	public String bind(Map<String, Value> variables) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<RDFEvent> pattern(Node subject) {
		PlainLiteral lit = tf.literal("");
		lit.setOrigin(origin+" "+property);
		RDFEvent triple = new Triple(subject, property, lit);
		return Collections.singletonList(triple);
	}

}
