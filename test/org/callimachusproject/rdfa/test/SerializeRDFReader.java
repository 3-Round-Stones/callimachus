package org.callimachusproject.rdfa.test;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.helpers.AbstractRDFEventReader;
import org.callimachusproject.engine.model.VarOrTerm;

public class SerializeRDFReader extends AbstractRDFEventReader {
	private RDFEventReader delegate;

	public SerializeRDFReader(RDFEventReader delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() throws RDFParseException {
		delegate.close();
	}

	@Override
	protected RDFEvent take() throws RDFParseException {
		RDFEvent next = delegate.next();
		if (next == null)
			return next;
		StringBuilder sb = new StringBuilder();
		sb.append("new ");
		sb.append(next.getClass().getSimpleName());
		sb.append("(");
		if (next.isStart()) {
			sb.append("true");
		} else if (next.isEnd()) {
			sb.append("false");
		}
		if (next.isBase()) {
			sb.append("\"");
			sb.append(next.asBase().getBase());
			sb.append("\"");
		} else if (next.isNamespace()) {
			sb.append("\"");
			sb.append(next.asNamespace().getPrefix());
			sb.append("\",\"");
			sb.append(next.asNamespace().getNamespaceURI());
			sb.append("\"");
		} else if (next.isStartSubject() || next.isEndSubject()) {
			sb.append(",");
			serializeTerm(sb, next.asSubject().getSubject());
		} else if (next.isTriplePattern()) {
			TriplePattern tp = next.asTriplePattern();
			serializeTerm(sb, tp.getSubject());
			sb.append(",");
			serializeTerm(sb, tp.getPredicate());
			sb.append(",");
			serializeTerm(sb, tp.getObject());
			sb.append(",");
			sb.append(tp.isInverse());
		} else if (next.isComment()) {
			sb.append("\"");
			sb.append(next.asComment().getComment());
			sb.append("\"");
		}
		sb.append("),");
		System.out.println(sb.toString());
		return next;
	}

	private void serializeTerm(StringBuilder sb, VarOrTerm subj) {
		sb.append("new ");
		sb.append(subj.getClass().getSimpleName());
		sb.append("(\"");
		sb.append(subj.stringValue());
		sb.append("\")");
	}

}
