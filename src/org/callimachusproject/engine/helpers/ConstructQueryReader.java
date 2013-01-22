package org.callimachusproject.engine.helpers;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.Construct;
import org.callimachusproject.engine.events.RDFEvent;

public class ConstructQueryReader extends AbstractRDFEventReader {
	private final RDFEventReader delegate;

	public ConstructQueryReader(RDFEventReader selectReader) throws RDFParseException {
		RDFEventList select = new RDFEventList(selectReader);
		RDFEventList construct = new RDFEventList();
		boolean selectClause = false;
		boolean constructClause = false;
		for (RDFEvent event : select) {
			if (event.isSelect()) {
				selectClause = event.isStart();
				if (!constructClause) {
					constructClause = true;
					construct.add(new Construct(true, event.getLocation()));
					for (RDFEvent e : select) {
						if (e.isTriplePattern()) {
							construct.add(e);
						}
					}
					construct.add(new Construct(false, event.getLocation()));
				}
			} else if (selectClause) {
				// ignore
			} else {
				construct.add(event);
			}
		}
		this.delegate = construct.iterator();
	}

	@Override
	public void close() throws RDFParseException {
		delegate.close();
	}

	@Override
	protected RDFEvent take() throws RDFParseException {
		return delegate.next();
	}

}
