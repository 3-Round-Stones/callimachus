package org.callimachusproject.stream;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.Var;
import org.callimachusproject.rdfa.model.VarOrTerm;

public class SPARQLPosteditor extends BufferedRDFEventReader {
	List<TriplePatternMatcher> triplePatternMatchers = new LinkedList<TriplePatternMatcher>();
	
	interface TriplePatternMatcher {
		boolean match(TriplePattern triple);
	}
	
	public static class OriginMatcher implements TriplePatternMatcher {
		Map<String,String> origins;
		Pattern subject, object;

		public OriginMatcher(Map<String, String> origins, String subject, String object) {
			this.origins = origins;
			if (subject!=null) this.subject = Pattern.compile(subject);
			if (object!=null) this.object = Pattern.compile(object);
		}

		@Override
		public boolean match(TriplePattern triplePattern) {
			VarOrTerm s = triplePattern.getSubject();
			VarOrTerm o = triplePattern.getObject();
			return match(subject,s) && match(object,o);
		}
		
		private boolean match(Pattern p, VarOrTerm vt) {
			if (p!=null && vt!=null && vt.isVar()) {
				Var v = vt.asVar();
				String origin = origins.get(v.stringValue());
				return p.matcher(origin).matches();
			}
			return true;
		}
		
	}
	
	public SPARQLPosteditor(RDFEventReader reader) {
		super(reader);
	}
	
	public void addMatcher(TriplePatternMatcher m) {
		triplePatternMatchers.add(m);
	}

	/* Any triple matcher has the power to veto inclusion of the triple */
	
	@Override
	protected void process(RDFEvent event) throws RDFParseException {
		if (event==null) return;
		else if (event.isTriplePattern()) {
			TriplePattern triplePattern = event.asTriplePattern();
			for (TriplePatternMatcher m: triplePatternMatchers) {
				if (!m.match(triplePattern)) return;
			}
			add(event);
		}
		else {
			add(event);
		}
	}

}
