package org.callimachusproject.stream;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.BuiltInCall;
import org.callimachusproject.rdfa.events.Expression;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.PlainLiteral;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.Var;
import org.callimachusproject.rdfa.model.VarOrTerm;


public class SPARQLPosteditor extends BufferedRDFEventReader {
	private static TermFactory tf = TermFactory.newInstance();

	List<Editor> editors = new LinkedList<Editor>();
	Map<String, String> origins;
	
	interface Editor {
		boolean edit(RDFEvent event);
	}
	
	public class CutTriplePattern implements Editor {
		Map<String,String> origins;
		Pattern subject, object;
		public CutTriplePattern(String subject, String object) {
			if (subject!=null) this.subject = Pattern.compile(subject);
			if (object!=null) this.object = Pattern.compile(object);
		}
		@Override
		public boolean edit(RDFEvent event) {
			if (!event.isTriplePattern()) return false;
			TriplePattern t = event.asTriplePattern();
			return !(match(subject,t.getSubject()) && match(object,t.getObject()));
		}
	}
	
	public class AddCondition implements Editor {
		Map<String,String> origins;
		IRI pred;
		PlainLiteral lit;
		Pattern subjectPattern;
		public AddCondition(String subjectRegex, IRI pred, PlainLiteral lit) {
			super();
			this.subjectPattern = Pattern.compile(subjectRegex);
			this.pred = pred;
			this.lit = lit;
		}
		@Override
		public boolean edit(RDFEvent event) {
			if (!event.isEndSubject()) return false;
			VarOrTerm vt = event.asSubject().getSubject();
			if (vt.isVar() 
			&& !vt.asVar().stringValue().startsWith("_") 
			&& match(subjectPattern,vt)) {
				add(new TriplePattern(vt,pred,lit));
			}
			return false;
		}
	}
	
	public class AddFilter implements Editor {
		Map<String,String> origins;
		List<String> props;
		Pattern subjectPattern;
		String regex;
		public AddFilter(String subjectRegex, String[] props, String regex) {
			super();
			this.props = Arrays.asList(props);
			this.subjectPattern = Pattern.compile(subjectRegex);
			this.regex = regex;
		}
		@Override
		public boolean edit(RDFEvent event) {
			if (!event.isTriplePattern()) return false;
			TriplePattern t = event.asTriplePattern();
			
			if (match(subjectPattern,t.getSubject()) 
			&& props.contains(t.getPredicate().stringValue())) {
				add(event);
				// eg. FILTER regex( str(<object>),<regex>,i)
				add(new BuiltInCall(true, "regex"));
				add(new BuiltInCall(true, "str"));
				add(new Expression(t.getObject()));
				add(new BuiltInCall(false, "str"));
				add(new Expression(tf.literal(regex)));
				add(new Expression(tf.literal("i")));
				add(new BuiltInCall(false, "regex"));
				return true;
			}
			return false;
		}
	}
	
	private boolean match(Pattern p, VarOrTerm vt) {
		if (p!=null && vt!=null && vt.isVar()) {
			Var v = vt.asVar();
			String origin = origins.get(v.stringValue());
			return p.matcher(origin).matches();
		}
		return true;
	}	

	public SPARQLPosteditor(RDFEventReader reader, Map<String, String> origins) {
		super(reader);
		this.origins = origins;
	}
	
	public SPARQLPosteditor(SPARQLProducer producer) {
		this(producer,producer.getOrigins());
	}
	
	public void addEditor(Editor m) {
		editors.add(m);
	}

	/* Any triple matcher has the power to veto inclusion of the triple */
	
	@Override
	protected void process(RDFEvent event) throws RDFParseException {
		if (event==null) return;
		for (Editor ed: editors) {
			if (ed.edit(event)) return;
		}
		add(event);
	}

}
