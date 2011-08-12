package org.callimachusproject.stream;

import java.util.ArrayList;
import java.util.List;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.OrderBy;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.model.Var;
import org.callimachusproject.rdfa.model.VarOrTerm;

public class OrderedSparqlReader extends RDFEventReader {
	private final RDFEventReader delegate;
	private RDFEvent next;
	private List<List<Var>> stack = new ArrayList<List<Var>>();
	private List<Var> vars = new ArrayList<Var>();

	public OrderedSparqlReader(RDFEventReader delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() throws RDFParseException {
		delegate.close();
	}

	@Override
	protected RDFEvent take() throws RDFParseException {
		try {
			if (next != null)
				return next;
		} finally {
			next = null;
		}
		RDFEvent taken = delegate.next();
		if (taken == null)
			return taken;
		process(taken);
		return taken;
	}

	private void process(RDFEvent taken) {
		if (taken.isStartGroup()) {
			stack.add(vars);
			vars = new ArrayList<Var>();
		} else if (taken.isEndGroup()) {
			vars.addAll(stack.remove(stack.size() - 1));
		} else if (taken.isTriplePattern()) {
			addIfVar(taken.asTriplePattern().getPartner());
		} else if (taken.isEndWhere() && !vars.isEmpty()) {
			next = new OrderBy(vars);
		}
	}

	private void addIfVar(VarOrTerm subj) {
		if (subj.isVar()) {
			vars.add(subj.asVar());
		}
	}

}
