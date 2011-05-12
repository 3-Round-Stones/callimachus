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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

/**
 * Produce XHTML+RDFa events from a streamed template and SPARQL result set 
 * 
 * @author Steve Battle
 */

public class RDFaProducer extends XMLEventReaderBase {
	
	final static String[] RDFA_OBJECT_ATTRIBUTES = { "about", "resource", "typeof", "content" };
	final static List<String> RDFA_OBJECTS = Arrays.asList(RDFA_OBJECT_ATTRIBUTES);

	final String[] RDFA_VAR_ATTRIBUTES = { "about", "resource", "href", "src", "typeof", "content" };
	List<String> RDFaVarAttributes = Arrays.asList(RDFA_VAR_ATTRIBUTES);

	// reads the input template
	BufferedXMLEventReader reader;
	Map<String,String> origins;
	TupleQueryResult resultSet;
	BindingSet result;
	Set<String> branches;
	Stack<Context> stack = new Stack<Context>();
	XMLEventFactory eventFactory = XMLEventFactory.newFactory();
	ValueFactory valueFactory = new ValueFactoryImpl();
	Context context = new Context();
	String skipElement = null;
	URI self;
	RepositoryConnection con;
	
	class Context {
		int position=1, mark;
		Map<String,Value> assignments = new HashMap<String,Value>();
		String path;
		Value content;
		boolean isBranch=false;
		protected Context() {}
		protected Context(Context context) {
			assignments.putAll(context.assignments);
		}
	}
	
	/* @Param con	required to resolve (datatype) namespace prefixes */
	
	public RDFaProducer(BufferedXMLEventReader reader, TupleQueryResult resultSet, Map<String,String> origins, URI self, RepositoryConnection con) 
	throws Exception {
		super();
		this.reader = reader;
		this.origins = origins;
		this.resultSet = resultSet;
		result = nextResult();
		this.self = self;
		this.con = con;
		
		branches = new HashSet<String>();
		for (String name: resultSet.getBindingNames()) {
			String origin = origins.get(name);
			//System.out.println(name+" "+origin);
			int n = origin.indexOf("/");
			branches.add(origin.substring(n<0?0:n));
		}
	}

	public RDFaProducer(XMLEventReader reader, TupleQueryResult resultSet, Map<String,String> origins, URI self, RepositoryConnection con) 
	throws Exception {
		this(new BufferedXMLEventReader(reader), resultSet, origins, self, con);
		this.reader.mark();
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
				// even if this is not a branch-point it may still contain substitutable attributes
				else addStartElement(start);
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
		else if (event.isCharacters()) {
			if (event.toString().trim().equals(""))
				return false;
			if (skipElement!=null) return false;
			String text = substitute(event.toString());
			if (text!=null) add(eventFactory.createCharacters(text));
			else add(event);
			return true;
		}
		else if (event.getEventType()==XMLEvent.COMMENT) {
			if (skipElement!=null) return false;
			add(event);
			return true;			
		}
		else if (skipElement==null) {
			add(event);
			return true;
		}
		return false;
	}
	
	/* search for additional bindings to complete a "hanging rel"
	 * This may be a bnode associated with the element itself (or a nested element)
	 */
	
	private boolean incomplete(StartElement start) {
		boolean hasRel = false; //, hasSubject=false, hasProperty=false;
		// look for a rel or rev that may be potentially incomplete
		for (Iterator<?> i = start.getAttributes(); i.hasNext();) {
			Attribute attr = (Attribute) i.next();
			String lname = attr.getName().getLocalPart();
			// assume an explicit @resource is bound
//			if (lname.equals("about")
//			&& attr.getName().getNamespaceURI().isEmpty())
//				hasSubject = true;
//			if (lname.equals("property")
//			&& attr.getName().getNamespaceURI().isEmpty())
//				hasProperty = true;
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
			// a longer path completes the triple
			if (origin.substring(n<0?0:n).startsWith(context.path+"/")) 
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
			&& attr.getValue().startsWith("?")) 
				return true;
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
		// all (implicit and explicit) variables with this element at their origin must be bound
		// These origins are the first use of a variable - no need to check subsequent use in descendents
		// This avoids forced grounding of @href with relative URL "?..."
		for (String name: resultSet.getBindingNames()) {
			String origin = origins.get(name);
			int n = origin.indexOf("/");
			if (origin.substring(n<0?0:n).equals(context.path)) {
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

	/* An attribute is substitutable if it is an RDFa assignable attribute: "about", "resource", "typeof"
	 * or if it is the subject or object of a triple (e.g. "href", "src"), with a variable value "?VAR".
	 * ANY other attribute value with the variable expression syntax {?VAR} is substitutable.
	 * RDFa @content to be added later.
	 * Returns the variable name or null. 
	 */
	
	Value substitute(Attribute attr, String path) {
		String namespace = attr.getName().getNamespaceURI();
		String localPart = attr.getName().getLocalPart();
		String value = attr.getValue();
		// primary RDFa object attribute, excluding 'content' and 'property'
		if (namespace.isEmpty() && RDFA_OBJECTS.contains(localPart)) {
			if (value.startsWith("?")) {
				String var = value.substring(1);
				return context.assignments.get(var);
			}
			else return null;
		}
		// enumerate variables in triples with ?VAR syntax
		for (String name: resultSet.getBindingNames()) {
			String origin = origins.get(name);
			int n = origin.indexOf("/");
			if (origin.substring(n<0?0:n).equals(path) && value.startsWith("?")) {
				String var = value.substring(1);
				return context.assignments.get(var);
			}
		}
		// look for variable expressions in the attribute value
		value = substitute(value);
		return value!=null?valueFactory.createLiteral(value):null;
	}
	
	/* Substitute variable expressions in attributes and text nodes */
	
	private static final String VAR_EXP_REGEX = "\\{\\?([a-zA-Z]\\w*)\\}";
	private static final Pattern VAR_EXP_PATTERN = Pattern.compile(VAR_EXP_REGEX);
		
	String substitute(String text) {
		// look for variable expressions in the attribute value
		Matcher m = VAR_EXP_PATTERN.matcher(text);
		boolean found = false;
		while (m.find()) {
			String var = m.group(1);
			String val = context.assignments.get(var).stringValue();
			text = text.replace(m.group(), val);
			found = true;
		}
		return found?text:null;		
	}
	
	/* Use result bindings to make assignments in this context */

	private Value assign(StartElement start) {
		if (result==null) return null;
		Value content = null;
		// identify implicit variables for this element, not found among the attributes
		for (Iterator<Binding> i=result.iterator(); i.hasNext();) {
			Binding b = i.next();
			String origin = origins.get(b.getName());
			int n = origin.indexOf("/");
			if (origin.substring(n<0?0:n).equals(context.path)) {
				if (origin.equals("CONTENT"+context.path))
					content = b.getValue();
				if (b.getName().startsWith("_")) 
					context.assignments.put(b.getName(), b.getValue());
			}
		}
		// identify attributes that MAY contain RDFa variables,
		// if they contain a bound variable, add the assignment
		for (Iterator<?> i = start.getAttributes(); i.hasNext();) {
			Attribute attr = (Attribute) i.next();
			if (RDFaVarAttributes.contains(attr.getName().getLocalPart())
			&& attr.getName().getNamespaceURI().isEmpty()
			&& attr.getValue().startsWith("?")) {
				String name = attr.getValue().substring(1);
				Value v = result.getValue(name);
				if (v!=null) context.assignments.put(name, v);
			}
		}
		return content;
	}
	
	String getPrefix(String namespace, NamespaceContext ctx) {
		if (namespace==null) return null;
		String prefix = ctx.getPrefix(namespace);
		// deal with problematic namespaces without trailing symbol
		if (prefix==null && namespace.endsWith("#")) 
			prefix = ctx.getPrefix(namespace.substring(0,namespace.length()-1));
		return prefix;
	}
	
	String getPrefix(String namespace, RepositoryConnection con) {
		if (namespace==null) return null;
		String prefix = null;
		try {
			List<org.openrdf.model.Namespace> l = con.getNamespaces().asList();
			for (int i=0; i<l.size(); i++) {
				if (namespace.equals(l.get(i).getName())) {
					prefix = l.get(i).getPrefix();
					break;
				}
				if (namespace.endsWith("#") 
				&& l.get(i).getName().equals(namespace.substring(0, namespace.length()-1))) {
					prefix = l.get(i).getPrefix();
					break;					
				}
			}
		} 
		catch (Exception e) {}
		return prefix;
	}
		
	// ^"[^\"]*"\^\^<(.*)>$
	private static final String DATATYPE_REGEX = "^\"[^\\\"]*\"\\^\\^<(.*)>$";
	private static final Pattern DATATYPE_REGEX_PATTERN = Pattern.compile(DATATYPE_REGEX);
	
	private String getDatatype(Value content) {
		if (content!=null) {
			// use toString() that includes the datatype
			Matcher m = DATATYPE_REGEX_PATTERN.matcher(content.toString());
			if (m.matches()) return m.group(1);
		}
		return null;
	}

	private String getDatatypeCurie(Value content, NamespaceContext ctx) {
		String namespaceURI = getDatatype(content);
		String datatype = null;
		if (namespaceURI!=null) {
			// convert datatype URI into a curie
			try {
				URI uri = new URIImpl(namespaceURI);
				String prefix = getPrefix(uri.getNamespace(), ctx);
				// otherwise use the repository connection to resolve the prefix
				if (prefix==null) prefix = getPrefix(uri.getNamespace(), con);
				datatype = prefix+":"+uri.getLocalName();
			}
			catch (Exception e) {}
		}
		return datatype;
	}

	
	/* an iterator over attributes - substituting variable bindings 
	 * Only assigns content to @content if the attribute is present
	 * May return more attributes than in the input, adding e.g. datatype and xml:lang
	 */
	
	class AttributeIterator implements Iterator<Object> {
		Iterator<?> attributes;
		Value content;
		String datatype = null;
		String path;
		Attribute nextAttribute;
		boolean hasBody;
		
		public AttributeIterator
		(Iterator<?> attributes, Value content, String path, boolean hasBody, NamespaceContext ctx) {
			this.attributes = attributes;
			this.content = content;
			this.path = path;
			this.hasBody = hasBody;
			datatype = getDatatypeCurie(content,ctx);
			while (nextAttribute==null && attributes.hasNext()) 
			nextAttribute = more();
		}
		@Override
		public boolean hasNext() {
			return nextAttribute!=null;
		}
		@Override
		public Object next() {
			Attribute a = nextAttribute;
			nextAttribute = more();
			return a;
		}
		private Attribute more() {
			if (attributes.hasNext()) {
				Attribute attr = (Attribute) attributes.next();
				Value newValue = substituteValue(attr);
				if (newValue!=null) 
					return eventFactory.createAttribute(attr.getName(), newValue.stringValue());
				else return attr;
			}
			// opportunity to add additional attributes
			else if (hasBody && content!=null) {
				// add @content if no existing @content and a non-empty element body
				Attribute a = eventFactory.createAttribute("content", content.stringValue());				
				// prevent adding content again
				content = null;
				return a;
			}
			else if (datatype!=null) {
				Attribute a = eventFactory.createAttribute("datatype", datatype);
				datatype = null;
				return a;
			}
			return null;
		}
		/* If there is a content attribute there is no need to add text content */
		private Value substituteValue(Attribute attr) {
			String namespace = attr.getName().getNamespaceURI();
			String localPart = attr.getName().getLocalPart();
			String value = attr.getValue();
			Value v = substitute(attr,context.path);
			// content variables are currently represented as empty strings
			if (v==null && localPart.equals("content") && namespace.isEmpty() && value.isEmpty()) {
				// remove content from the context to prevent addition as text content
				context.content = null;
				return content;	
			}
			return v;
		}
		@Override
		public void remove() {
		}	
	}
	
	/* NamespaceIterator is able to add additional namespace declaration for content */
	
	class NamespaceIterator implements Iterator<Namespace> {
		Iterator<?> namespaces;
		String namespaceURI, prefix;
		Namespace nextNamespace;
		public NamespaceIterator(Iterator<?> namespaces, Value content, NamespaceContext ctx) {
			this.namespaces = namespaces;
			String datatype = getDatatype(content);
			if (datatype!=null) {
				namespaceURI = new URIImpl(datatype).getNamespace();
				String p = getPrefix(namespaceURI,ctx);
				// if the namespace is already defined clear it
				if (p!=null) namespaceURI = null;
				else prefix = getPrefix(namespaceURI,con);
			}
			nextNamespace = more();
		}
		public boolean hasNext() {
			return nextNamespace!=null;
		}
		public Namespace next() {
			Namespace ns = nextNamespace;
			nextNamespace = more();
			return ns;
		}
		private Namespace more() {
			if (namespaces.hasNext()) return (Namespace) namespaces.next();
			// if there is an undefined (null) prefix add xmlns:null
			if (namespaceURI!=null) {
				String ns = namespaceURI;
				// prevent adding this again
				namespaceURI = null;
				return eventFactory.createNamespace(prefix!=null?prefix:"null", ns);
			}
			return null;
		}
		public void remove() {
		}		
	}
	
	/* Add the start element, and content if the next event is not end element */
	
	private void addStartElement(StartElement start) throws XMLStreamException {
		QName name = start.getName();
		// only add content if the body is empty or ignorable whitespace
		boolean hasBody = false;
		for (int i=0; !hasBody; i++) {
			XMLEvent e = reader.peek(i);
			if (e.isEndElement()) break;
			if (e.isCharacters() && e.toString().trim().isEmpty()) continue;
			hasBody = true;
		}
		NamespaceContext ctx = start.getNamespaceContext();
		Iterator<?> namespaces = new NamespaceIterator(start.getNamespaces(), context.content, ctx);
		// AttributeIterator may clear context.content on construction so do this last
		Iterator<?> attributes = new AttributeIterator(start.getAttributes(), context.content, context.path, hasBody, ctx);
		XMLEvent e = eventFactory.createStartElement(name.getPrefix(), name.getNamespaceURI(), name.getLocalPart(), attributes, namespaces, ctx);
		add(e);
		
		// The AttributeIterator (above) clears context.content if it adds the content attribute
		if (!hasBody && context.content!=null)
			add(eventFactory.createCharacters(context.content.stringValue()));
	}

}
