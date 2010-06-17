package org.callimachusproject.stream;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Subject;
import org.callimachusproject.rdfa.model.VarOrTerm;

public class About extends PipedRDFEventReader {
	private VarOrTerm about;
	private boolean begining = true;

	public About(VarOrTerm about, RDFEventReader reader) {
		super(reader);
		this.about = about;
	}

	@Override
	protected void process(RDFEvent next) throws RDFParseException {
		if (begining && about != null) {
			if (next.isEndDocument()) {
				begining = false;
				add(new Subject(true, about));
				add(new Subject(false, about));
			} else if (next.isStartSubject()) {
				begining = false;
				if (!next.asSubject().getSubject().equals(about)) {
					add(new Subject(true, about));
					add(new Subject(false, about));
				}
			} else if (next.isTriplePattern()) {
				begining = false;
				if (!next.asTriplePattern().getSubject().equals(about)) {
					add(new Subject(true, about));
					add(new Subject(false, about));
				}
			}
		}
		add(next);
	}

}
