/*
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.callimachusproject.stream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

/**
 * Produce XHTML+RDFa events from a streamed template and SPARQL result set 
 * 
 * @author Steve Battle
 */

public class RDFaProducer extends XMLEventReaderBase {
	
	final String[] RDFA_VAR_ATTRIBUTES = { "about", "resource", "href", "src", "typeof" };
	List<String> RDFaVarAttributes = Arrays.asList(RDFA_VAR_ATTRIBUTES);

	// reads the input template
	BufferedXMLEventReader reader;
	Map<String,String> origins;
	TupleQueryResult resultSet;
	BindingSet result;
	Set<String> branches;
	Stack<Context> stack = new Stack<Context>();
	XMLEventFactory eventFactory = XMLEventFactory.newFactory();
	Context context = new Context();
	String skipElement = null;
	URI self;
	
	class Context {
		int position=1, mark;
		Map<String,Value> assignments = new HashMap<String,Value>();
		String path;
		Value content;
		boolean isBranch=false, hasBody=false;
		protected Context() {}
		protected Context(Context context) {
			assignments.putAll(context.assignments);
		}
	}

	public RDFaProducer(XMLEventReader reader, TupleQueryResult resultSet, Map<String,String> origins) 
	throws Exception {
		super();
		this.reader = new BufferedXMLEventReader(reader);
		this.reader.mark();
		this.origins = origins;
		this.resultSet = resultSet;
		result = nextResult();
		
		branches = new HashSet<String>();
		for (String name: resultSet.getBindingNames()) {
			String origin = origins.get(name);
			//System.out.println(name+" "+origin);
			int n = origin.indexOf("/");
			if (n>0) origin = origin.substring(n);
			branches.add(origin);
		}
	}
	
	public RDFaProducer(XMLEventReader reader, TupleQueryResult resultSet, Map<String,String> origins, URI self) 
	throws Exception {
		this(reader,resultSet, origins);
		this.self = self;
	}

	@Override
	public void close() throws XMLStreamException {
		reader.close();
	}

	@Override
	protected boolean more() throws XMLStreamException {
		try {
			while (reader.hasNext()) {
				if (process(reader.nextEvent())) return true;
			}
			return false;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new XMLStreamException(e.getMessage());
		}
	}
	
	protected BindingSet nextResult() throws Exception {
		if (resultSet.hasNext())
			return resultSet.next();
		else return null;
	}
	
	public String path() {
		StringBuffer b = new StringBuffer();
		for(Iterator<Context> i=stack.iterator(); i.hasNext();) {
			Context c = i.next();
			b.append("/"+c.position);
		}
		return b.toString();
	}
	
	private boolean process(XMLEvent event) throws Exception {
		if (event.isStartDocument()) {
			if (self!=null) {
				context.assignments.put("this", self);
			}
			add(event);
		}
		else if (event.isStartElement()) {
			StartElement start = event.asStartElement();
			context.hasBody = true;
			stack.push(context);
			context = new Context(context);
			context.path = path();
			// record the start element position in the stream
			context.mark = reader.mark()-1;
			
			// skip element if current result is inconsistent with assignments
			if (!consistent() && skipElement==null)
				skipElement = context.path;
			
			if (skipElement==null) {
				context.isBranch = branchPoint(start);
				if (context.isBranch) {		
					context.content = assign(start);
				
					// following the assignment there may be no more result bindings
					// we may need the next result to complete the triple
					if (!moreBindings()) result = nextResult();
	
					// if there are no solutions then skip this branch
					// All variables must be bound and the triple must have an object
					if (!grounded(start) || incomplete(start))
						skipElement = context.path;
					else addStartElement(start);
				}
				else add(start);
			}			
			return skipElement==null;
		}
		else if (event.isEndElement()) {
			if (skipElement!=null) {
				if (context.path.equals(skipElement)) skipElement = null;
				context = stack.pop();
				context.position++;
				return false;
			}
			if (!context.hasBody && context.content!=null)
				add(eventFactory.createCharacters(context.content.stringValue()));

			add(event);

			if (context.isBranch && result!=null) {
				if (!moreBindings()) result = nextResult();

				if (!consistent()) {
					int mark = context.mark;
					context = stack.pop();
					context.position++;
					if (consistent()) {
						reader.reset(mark);
						context.position--;
					}
					return true;
				}
			}
			context = stack.pop();
			context.position++;
			return true;
		}
		else if (skipElement==null && event.getEventType()!=XMLEvent.SPACE) {
			add(event);
			context.hasBody=true;
			return true;
		}
		return false;
	}
	
	/* search for additional bindings to complete a "hanging rel"
	 * This may be a bnode associated with the element itself (or a nested element)
	 */
	
	private boolean incomplete(StartElement start) {
		boolean hasRel = false, hasSubject=false, hasProperty=false;
		// look for a rel or rev that may be potentially incomplete
		for (Iterator<?> i = start.getAttributes(); i.hasNext();) {
			Attribute attr = (Attribute) i.next();
			String lname = attr.getName().getLocalPart();
			// assume an explicit @resource is bound
			if (lname.equals("about")
			&& attr.getName().getNamespaceURI().isEmpty())
				hasSubject = true;
			if (lname.equals("property")
			&& attr.getName().getNamespaceURI().isEmpty())
				hasProperty = true;
			if ((lname.equals("resource") || lname.equals("href")) 
			&& attr.getName().getNamespaceURI().isEmpty())
				return false;
			if ((lname.equals("rel") || lname.equals("rev")) 
			&& attr.getName().getNamespaceURI().isEmpty() )
				hasRel = true;
		}
//		if (!hasRel && !(hasSubject && !hasProperty)) return false;
		if (!hasRel) return false;
		
		// a hanging rel can't be completed by an empty result
		if (result==null) return false;
		
//		if (!hasProperty || result==null) return false;
//		if (!(hasProperty || hasSubject) || result==null) return true;
		// look for binding that explicitly completes the triple
		for (String name: result.getBindingNames()) {
			String origin = origins.get(name);
			int n = origin.indexOf("/");
			if (n<0) n=0;
			// a longer path completes the triple
			if (origin.substring(n).startsWith(context.path+"/")) 
				return false;

		}
		return true;
	}

	private boolean branchPoint(StartElement start) {
		if (branches.contains(context.path)) return true;
		for (Iterator<?> i = start.getAttributes(); i.hasNext();) {
			Attribute attr = (Attribute) i.next();
			if (RDFaVarAttributes.contains(attr.getName().getLocalPart()) 
			&& attr.getName().getNamespaceURI().isEmpty()
			&& attr.getValue().startsWith("?")) return true;
		}
		return false;
	}

	private boolean moreBindings() {
		if (result==null) return false;
		for (Iterator<Binding> i=result.iterator(); i.hasNext();) {
			Binding b = i.next();
			if (!context.assignments.keySet().contains(b.getName()) 
			|| !context.assignments.get(b.getName()).equals(b.getValue())) 
				return true;
		}		
		return false;
	}
	
	private boolean grounded(StartElement start) {
		for (String name: resultSet.getBindingNames()) {
			String origin = origins.get(name);
			int n = origin.indexOf("/");
			if (n>0) origin = origin.substring(n);
			if (origin.equals(context.path)) {
				// name must be bound
				if (context.assignments.get(name)==null) return false;
			}
		}
		for (Iterator<?> i = start.getAttributes(); i.hasNext();) {
			Attribute attr = (Attribute) i.next();
			String lname = attr.getName().getLocalPart();
			// check this is an RDFa attribute about, resource, href, src, etc
			if (RDFaVarAttributes.contains(lname) 
			&& attr.getName().getNamespaceURI().isEmpty()
			&& attr.getValue().startsWith("?")) {
				String name = attr.getValue().substring(1);				
				// name must be bound
				if (context.assignments.get(name)==null) return false;
			}
		}
		return true;
	}

	/* is the result set consistent with assignments to this point */
	
	private boolean consistent() {
		if (result==null) return true;
		for (Iterator<Binding> i=result.iterator(); i.hasNext();) {
			Binding b = i.next();
			Value v = context.assignments.get(b.getName());
			if (v!=null && !b.getValue().equals(v)) return false;
		}
		return true;
	}

	private Value assign(StartElement start) {
		if (result==null) return null;
		Value content = null;
		for (Iterator<Binding> i=result.iterator(); i.hasNext();) {
			Binding b = i.next();
			String origin = origins.get(b.getName());
			int n = origin.indexOf("/");
			if (n<0) n=0;

			if (origin.substring(n).equals(context.path)) {
				if (origin.equals("CONTENT"+context.path))
					content = b.getValue();

				if (b.getName().startsWith("_")) 
					context.assignments.put(b.getName(), b.getValue());
			}
		}
		for (Iterator<?> i = start.getAttributes(); i.hasNext();) {
			Attribute attr = (Attribute) i.next();
			if (RDFaVarAttributes.contains(attr.getName().getLocalPart()) 
			&& attr.getName().getNamespaceURI().isEmpty()
			&& attr.getValue().startsWith("?")) {
				String name = attr.getValue().substring(1);
				Value v = result.getValue(name);
				if (v!=null)
					context.assignments.put(name, v);				
			}
		}
		return content;
	}
		
	class XMLNamespaceContext implements NamespaceContext {
		StartElement start;
		public XMLNamespaceContext(StartElement start) {
			this.start = start;
		}
		public String getNamespaceURI(String prefix) {
			for (Iterator<?> i=start.getNamespaces(); i.hasNext();) {
				Namespace ns=(Namespace) i.next();
				if (ns.getPrefix().equals(prefix)) return ns.getNamespaceURI();
			}
			return null;
		}
		public String getPrefix(String arg0) {
			return null;
		}
		public Iterator<?> getPrefixes(String arg0) {
			return null;
		}
	}
	
	/* an iterator over attributes - substituting variable bindings 
	 * Only add content if @content is present */
	
	class AttributeIterator implements Iterator<Object> {
		Iterator<?> attributes;
		Value content;
		Attribute nextAttribute;
		public AttributeIterator(Iterator<?> attributes, Value content) {
			this.attributes = attributes;
			this.content = content;
			while (nextAttribute==null && attributes.hasNext()) 
			nextAttribute = more();
		}
		@Override
		public boolean hasNext() {
			//return attributes.hasNext();
			return nextAttribute!=null;
		}
		@Override
		public Object next() {			
			Attribute attr = attributes.hasNext()?more():null;
			while (attributes.hasNext() && attr==null)
				attr = more();
			
			Attribute a = nextAttribute;
			nextAttribute = attr;
			return a;
		}
		
		private Attribute more() {
			Attribute attr = (Attribute) attributes.next();
			String value = attr.getValue();
			if (RDFaVarAttributes.contains(attr.getName().getLocalPart()) 
			&& attr.getName().getNamespaceURI().isEmpty() 
			&& value.startsWith("?")) {
				Value v = context.assignments.get(value.substring(1));
				if (v!=null)
					attr = eventFactory.createAttribute(attr.getName(), v.stringValue());
			}
			else if (attr.getName().equals("content") && value.isEmpty()) {
				attr = eventFactory.createAttribute("content", content.stringValue());	
				content = null;
			}
			return attr;			
		}
		@Override
		public void remove() {
		}	
	}
	
	private void addStartElement(StartElement start) {
		QName name = start.getName();
		Iterator<?> attributes = new AttributeIterator(start.getAttributes(), context.content);
		Iterator<?> namespaces = start.getNamespaces();
		NamespaceContext context = new XMLNamespaceContext(start);
		XMLEvent e = eventFactory.createStartElement(name.getPrefix(), name.getNamespaceURI(), name.getLocalPart(), attributes, namespaces, context);
		add(e);
	}

}
