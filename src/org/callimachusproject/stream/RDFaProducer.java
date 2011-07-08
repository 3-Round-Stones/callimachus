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

import org.callimachusproject.rdfa.RDFaReader;
import org.callimachusproject.rdfa.model.Node;
import org.callimachusproject.rdfa.model.TermFactory;
import org.openrdf.http.object.exceptions.InternalServerError;
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
	private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
	private static final String BASE_TAG = "http://www.w3.org/1999/xhtmlbase";

	final static String[] RDFA_OBJECT_ATTRIBUTES = { "about", "resource", "typeof", "content" };
	final static List<String> RDFA_OBJECTS = Arrays.asList(RDFA_OBJECT_ATTRIBUTES);

	final String[] RDFA_VAR_ATTRIBUTES = { "about", "resource", "href", "src", "typeof", "content" };
	List<String> RDFaVarAttributes = Arrays.asList(RDFA_VAR_ATTRIBUTES);

	// reads the input template
	BufferedXMLEventReader reader;
	Map<String,String> origins;
	TupleQueryResult resultSet;
	BindingSet result;
	Set<Binding> consumed;
	// variables that don't originate directly from the document (ignored here)
	Set<String> extraneous = new HashSet<String>();
	Set<String> branches = new HashSet<String>();
	Stack<Context> stack = new Stack<Context>();
	XMLEventFactory eventFactory = XMLEventFactory.newFactory();
	ValueFactory valueFactory = new ValueFactoryImpl();
	TermFactory termFactory = TermFactory.newInstance();
	Context context = new Context();
	String skipElement = null;
	URI self;
	RepositoryConnection con;
	XMLEvent previous;
	
	class Context {
		int position=1, mark;
		Map<String,Value> assignments = new HashMap<String,Value>();
		String path;
		Value content;
		boolean isBranch=false, isHanging=false;
		StartElement start;
		XMLEvent previousWhitespace=null;
		BindingSet resultOnEntry;

		protected Context() {}
		protected Context(Context context, StartElement start) {
			this.start = start;
			assignments.putAll(context.assignments);
			// all sub-contexts of a hanging context are hanging
			/*isHanging = context.isHanging;*/
			resultOnEntry = result;
		}
	}
	
	/* @Param con	required to resolve (datatype) namespace prefixes */
	
	public RDFaProducer
	(BufferedXMLEventReader reader, TupleQueryResult resultSet, Map<String,String> origins, URI self, RepositoryConnection con) 
	throws Exception {
		super();
		this.reader = reader;
		this.origins = origins;
		this.resultSet = resultSet;
		result = nextResult();
		this.self = self;
		this.con = con;
		
		for (String name: resultSet.getBindingNames()) {
			String origin = origins.get(name);
			if (origin!=null)
				branches.add(origin.split(" ")[0]);
			else extraneous.add(name);
		}
	}
	
	public RDFaProducer
	(XMLEventReader reader, TupleQueryResult resultSet, Map<String,String> origins, URI self, RepositoryConnection con) 
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
		catch (RuntimeException e) {
			throw e;
		}
		catch (Error e) {
			throw e;
		}
		catch (Exception e) {
			throw new XMLStreamException(e);
		}
	}
	
	protected BindingSet nextResult() throws Exception {
		// clear the record of consumed bindings
		consumed = new HashSet<Binding>();
		if (resultSet.hasNext())
			return resultSet.next();
		resultSet.close();
		return null;
	}
	
	public String path() {
		StringBuffer b = new StringBuffer();
		for (Iterator<Context> i=stack.iterator(); i.hasNext();)
			b.append("/"+i.next().position);
		return b.toString();
	}
	
	private boolean process(XMLEvent event) throws Exception {
		// add previous whitespace if this is not a start event
		if (isWhitespace(previous)) {
			add(previous);
			context.previousWhitespace = previous;
		}
		
		if (event.isStartDocument()) {
			processStartDocument(event);
		}
		else if (event.isStartElement()) {
			return processStartElement(event);
		}
		else if (event.isEndElement()) {
			return processEndElement(event);
		}
		else if (event.isCharacters()) {
			return processCharacters(event);
		}
		else if (event.getEventType()==XMLEvent.COMMENT) {
			return processComment(event);			
		}
		else if (skipElement==null) {
			add(event);
			previous = event;
			return true;
		}
		previous = event;
		return false;
	}

	private boolean processComment(XMLEvent event) {
		previous = event;
		if (skipElement!=null) return false;
		add(event);
		return true;
	}

	private void processStartDocument(XMLEvent event) {
		if (self!=null)
			context.assignments.put("this", self);
		add(event);
	}

	private boolean processStartElement(XMLEvent event) throws Exception,
			XMLStreamException {
		StartElement start = event.asStartElement();
		stack.push(context);
		context = new Context(context, start);
		context.path = path();
		// record the start element position in the stream
		context.mark = reader.mark()-1;
		
		// skip element if current result is inconsistent with assignments
		if (skipElement==null && !consistent() && hasRDFaMarkup(context.path))
			skipElement = context.path;
		
		if (skipElement==null) {
			context.isBranch = branchPoint(start);
			if (context.isBranch) {					
				// optional properties (in the next result) required deeper in the tree
				// collapse multiple consistent solutions into a single element
				// required for property expressions
				
				// consume results that are consistent with the current assignments
				while (consistent() && result!=null) {
					Value c = assign(start);
					if (context.content==null) context.content = c;
					// if there are no more bindings to consume in the current result then step to the next result
					if (!moreBindings()) result = nextResult();
					else break;
				}
				// if there are no solutions then skip this branch
				// All variables must be bound
				if (!context.isHanging && grounded(start)) {
					if (!complete(start)) context.isHanging = true;
					addStartElement(start);
				}
				else skipElement = context.path;
			} else addStartElement(start);
		}
		previous = event;
		return skipElement==null;
	}

	private boolean processEndElement(XMLEvent event) throws Exception {
		if (skipElement!=null) {
			if (context.path.equals(skipElement)) skipElement = null;
			context = stack.pop();
			context.position++;
			previous = event;
			return false;
		}
		add(event);
		previous = event;
		
		// has the current result been entirely consumed?
		if (result!=null && consumed()) result = nextResult(); 

		// don't repeat if we haven't consumed any results
		if (context.isBranch && result!=null && complete() && context.resultOnEntry!=result) {
			int mark = context.mark;
			XMLEvent ws = context.previousWhitespace;
			context = stack.pop();
			context.position++;
			if (consistent()) {
				reader.reset(mark);
				context.position--;
				if (ws!=null) previous = ws;
			}
		}
		else {
			context = stack.pop();
			context.position++;
		}
		return true;
	}

	private boolean processCharacters(XMLEvent event) {
		previous = event;
		if (skipElement!=null) return false;
		// postpone whitespace, add/repeat in advance of the next event
		if (isWhitespace(event)) return false;
		String text = substitute(event.toString());
		if (text!=null) add(eventFactory.createCharacters(text));
		else add(event);
		return true;
	}
	
	/* true for elements that participate in RDFa relationships */
	
	private boolean hasRDFaMarkup(String path) {
		for (String var: origins.keySet()) {
			String[] origin = origins.get(var).split(" ");
			// the RDFa may originate in this node or a descendant
			if (origin[0].startsWith(path)) return true;
		}
		return false;
	}
	
	/* Is the next result consumable in this context  */
	
	private boolean complete() {
		for (Iterator<?> i=result.iterator(); i.hasNext();) {
			Binding b = (Binding) i.next();
			if (context.assignments.containsKey(b.getName())) continue;
			String value = origins.get(b.getName());
			if (value == null) continue;
			String[] origin = value.split(" ");
			if (!origin[0].startsWith(context.path)) return false;
		}
		return true;
	}
	
	/* has every result binding been consumed.
	 * Not necessarily all in the current context, 
	 * previous siblings may have consumed bindings */

	private boolean consumed() {
		for (Iterator<?> i=result.iterator(); i.hasNext();) {
			Binding b = (Binding) i.next();
			if (!extraneous.contains(b.getName()) && !consumed.contains(b)) return false;
		}
		return true;
	}
	
	private boolean branchPoint(StartElement start) {
		if (branches.contains(context.path)) return true;
		// RDFa may not identify variable first-use as origin
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
			&& !extraneous.contains(b.getName()))
				return true;
		}		
		return false;
	}
	
	private boolean grounded(StartElement start) {
		// all explicit variables or content originating from this element must be bound
		// These origins are the first use of a variable - no need to check subsequent use in descendants
		for (String name: resultSet.getBindingNames()) {
			String origin = origins.get(name);
			if (origin==null) continue;
			List<String> split = Arrays.asList(origin.split(" "));
			// implicit vars (apart from CONTENT) need not be grounded
			if (name.startsWith("_") 
			&& !(split.contains(RDFaReader.CONTENT) || split.contains("_"))) 
				continue;
			if (split.get(0).equals(context.path) && context.assignments.get(name)==null) 
				return false;
		}
		return true;
	}
	
	private boolean complete(StartElement start) {
		// all implicit variables originating from this element must be bound
		// excluding property expressions
		for (String name: resultSet.getBindingNames()) {
			if (!name.startsWith("_") || extraneous.contains(name)) continue;
			String value = origins.get(name);
			if (value == null) continue;
			String[] origin = value.split(" ");
			if (origin[0].equals(context.path) && context.assignments.get(name)==null) 
				return false;
		}
		return true;
	}

	/* is the result set consistent with assignments to this point 
	 * variables tied to property expressions are not considered */
	
	private boolean consistent() {
		if (result==null) return true;
		for (Iterator<Binding> i=result.iterator(); i.hasNext();) {
			Binding b = i.next();
			String value = origins.get(b.getName());
			if (value == null) continue;
			String[] origin = value.split(" ");
			// is this a property expression with a curie
			if (origin.length>1 && origin[1].contains(":")) continue;
			Value v = context.assignments.get(b.getName());
			if (v!=null && !b.getValue().equals(v)) return false;
		}
		return true;
	}

	/* An attribute is substitutable if it is an RDFa assignable attribute: "about", "resource", "typeof"
	 * or if it is the subject or object of a triple in this OR ANY SHALLOWER element
	 * (e.g. "href", "src"), with a variable value "?VAR".
	 * ANY other attribute value with the variable expression syntax {?VAR} is substitutable.
	 * RDFa @content to be added later.
	 * Returns the variable name or null. 
	 */
	
	Value substitute(String tag, Attribute attr, String path) {
		String namespace = attr.getName().getNamespaceURI();
		String localPart = attr.getName().getLocalPart();
		String value = attr.getValue();
		// primary RDFa object attribute, excluding 'content' and 'property'
		if ((namespace.isEmpty() && RDFA_OBJECTS.contains(localPart)) || tag.equals(BASE_TAG)) {
			if (value.startsWith("?")) {
				String var = value.substring(1);
				return context.assignments.get(var);
			}
			else return null;
		}
		// enumerate variables in triples with ?VAR syntax
		for (String name: resultSet.getBindingNames()) {
			String origin = origins.get(name);
			if (origin!=null && context.path.startsWith(origin.split(" ")[0]) 
			&& value.startsWith("?") && value.substring(1).equals(name)
			&& namespace.isEmpty() && RDFaVarAttributes.contains(localPart)) 
				return context.assignments.get(name);
		}
		// look for variable expressions in the attribute value
		value = substitute(value);
		return value!=null?valueFactory.createLiteral(value):null;
	}
	
	// whitespace
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
	
	boolean isWhitespace(XMLEvent event) {
		if (event!=null && event.isCharacters()) {
			String text = event.asCharacters().getData();
			return WHITESPACE_PATTERN.matcher(text).matches();
		}
		return false;
	}
	
	/* Substitute variable and literal expressions in attributes and text nodes */
	
	// Variable expression
	// \{\?([a-zA-Z]\w*)\}
	private static final String VAR_EXP_REGEX = "\\{\\?([a-zA-Z]\\w*)\\}";
	private static final Pattern VAR_EXP_PATTERN = Pattern.compile(VAR_EXP_REGEX);
	
	// Literal expression
	// A string with " or ' delimiters, allowing escaped characters \" and \'
	// \{(\"|\')(([^\"\n]|\\['"])*?)\1\}
	private static final String LITERAL_EXP_REGEX1 = "\\{\"(([^\"\\n]|\\\\\")*?)\"\\}";
	private static final Pattern LITERAL_EXP_PATTERN1 = Pattern.compile(LITERAL_EXP_REGEX1);
	private static final String LITERAL_EXP_REGEX2 = "\\{'(([^'\\n]|\\\\')*?)'\\}";
	private static final Pattern LITERAL_EXP_PATTERN2 = Pattern.compile(LITERAL_EXP_REGEX2);
	private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u(\\w\\w\\w\\w)");
	
	// Property expression
	private final Pattern PROPERTY_EXP_PATTERN = Pattern.compile(RDFaReader.PROPERTY_EXP_REGEX);
	
	String substitute(String text) {
		// look for variable expressions in the attribute value
		Matcher m = VAR_EXP_PATTERN.matcher(text);
		Matcher m1 = LITERAL_EXP_PATTERN1.matcher(text);
		Matcher m2 = LITERAL_EXP_PATTERN2.matcher(text);
		Matcher m3 = PROPERTY_EXP_PATTERN.matcher(text);
		boolean found = false;
		boolean b=false, b1=false, b2=false, b3=false;
		int start = 0;
		while ((b=m.find(start)) || (b1=m1.find(start)) || (b2=m2.find(start)) || (b3=m3.find(start))) {
			// variable expression
			if (b) {
				String var = m.group(1);
				Value assignment = context.assignments.get(var);
				if (assignment != null) {
					String val = assignment.stringValue();
					text = text.replace(m.group(), val);
				} else {
					text = text.replace(m.group(), "");
				}
				found = true;
				start = m.end();
			}
			// literal expression 1
			else if (b1) {
				String val = m1.group(1);
				// substitute escaped characters
				val = backslash(val);
				text = text.replace(m1.group(), val);
				found = true;
				start = m1.end();
			}
			// literal expression 2
			else if (b2) {
				String val = m2.group(1);
				// substitute escaped characters
				val = backslash(val);
				text = text.replace(m2.group(), val);
				found = true;
				start = m2.end();
			}
			// property expression
			else if (b3) {
				String prefix = m3.group(1), localPart = m3.group(2);
				String var = getVar(prefix+":"+localPart,context.path);
				Value val = context.assignments.get(var);
				text = text.replace(m3.group(), val!=null?val.stringValue():"");
				found = true;
				start = m3.end();
			}
		}
		return found?text:null;		
	}

	private String backslash(String val) {
		Matcher m = UNICODE_ESCAPE.matcher(val);
		loop: while (m.find()) {
			int unicode = 0;
			for (char c : m.group(1).toCharArray()) {
				if ((c >= '0') && (c <= '9')) {
				    unicode = (unicode << 4) + c - '0';
				}
				else if ((c >= 'a') && (c <= 'f')) {
				    unicode = (unicode << 4) + 10 + c - 'a';
				}
				else if ((c >= 'A') && (c <= 'F')) {
				    unicode = (unicode << 4) + 10 + c - 'A';
				}
				else {
				    break loop;
				}
			}
			val.replace(m.group(), Character.toString((char) unicode));
		}
		val = val.replace("\\b", "\b");
		val = val.replace("\\f", "\f");
		val = val.replace("\\n", "\n");
		val = val.replace("\\r", "\r");
		val = val.replace("\\t", "\t");
		val = val.replace("\\'", "\'");
		val = val.replace("\\\"", "\"");
		val = val.replaceAll("\\\\(.)", "$1");
		return val;
	}
	
	Node curie(String curie) {
		if (curie==null) return null;
		String[] parts = curie.split(":");
		if (parts.length!=2) return null;
		return curie(parts[0],parts[1]);
	}
	
	Node curie(String prefix, String localPart) {
		NamespaceContext ctx = context.start.getNamespaceContext();
		String ns = ctx.getNamespaceURI(prefix);
		if (ns==null) ns = getNamespaceURI(prefix);
		return termFactory.curie(ns, localPart, prefix);
	}
	
	private String getVar(String property, String path) {
		for (String name: origins.keySet()) {
			if (!name.startsWith("_")) continue;
			List<String> origin = Arrays.asList(origins.get(name).split(" "));
			if (origin.get(0).startsWith(path) && origin.contains(property)) 
				return name;
		}
		return null;
	}
	
	/* Use result bindings to make assignments in this context */

	private Value assign(StartElement start) {
		if (result==null) return null;
		Value content = null;
		// identify implicit variables for this element, not found among the attributes
		for (Iterator<Binding> i=result.iterator(); i.hasNext();) {
			Binding b = i.next();
			String value = origins.get(b.getName());
			if (value == null) continue;
			List<String> origin = Arrays.asList(value.split(" "));
			if (origin.get(0).equals(context.path)) {
				// context.property refers to CONTENT
				if (origin.contains(RDFaReader.CONTENT))
					content = b.getValue();
				if (b.getName().startsWith("_")) {
					Value val = b.getValue();
					Value current = context.assignments.get(b.getName());
					if (current!=null) {
						// append multiple property values
						String append = current.stringValue() + " " + val.stringValue();
						val = valueFactory.createLiteral(append);
					}
					context.assignments.put(b.getName(), val);
					consumed.add(b);
				}
			}
		}
		// identify attributes that MAY contain RDFa variables,
		// if they contain a bound variable, add the assignment
		for (Iterator<?> i = start.getAttributes(); i.hasNext();) {
			Attribute attr = (Attribute) i.next();
			String namespace = attr.getName().getNamespaceURI();
			String localPart = attr.getName().getLocalPart();
			String value = attr.getValue();
			if (RDFaVarAttributes.contains(localPart)
			&& namespace.isEmpty() && value.startsWith("?")) {
				String name = attr.getValue().substring(1);
				Value v = result.getValue(name);
				if (v!=null) {
					context.assignments.put(name, v);
					consumed.add(result.getBinding(name));
				}
			}
		}
		return content;
	}
	
	String getNamespaceURI(String prefix) {
		if (prefix==null) return null;
		String namespace = null;
		try {
			List<org.openrdf.model.Namespace> l = con.getNamespaces().asList();
			for (int i=0; i<l.size(); i++) {
				if (prefix.equals(l.get(i).getPrefix())) {
					namespace = l.get(i).getName();
					break;
				}
			}
		} 
		catch (Exception e) {}
		return namespace;
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
	private static final Pattern DATATYPE_PATTERN = Pattern.compile(DATATYPE_REGEX);
	
	private String getDatatype(Value content) {
		if (content!=null) {
			// use toString() to include the datatype
			Matcher m = DATATYPE_PATTERN.matcher(content.toString());
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
	
	// ^"[^\"]*"\@(.*)$
	private static final String LANG_REGEX = "^\"[^\\\"]*\"\\@(.*)$";
	private static final Pattern LANG_PATTERN = Pattern.compile(LANG_REGEX);

	private String getLang(Value content) {
		if (content!=null) {
			// use toString() to include lang
			Matcher m = LANG_PATTERN.matcher(content.toString());
			if (m.matches()) return m.group(1);
		}
		return null;
	}
	
	/* an iterator over attributes - substituting variable bindings 
	 * Only assigns content to @content if the attribute is present
	 * May return more attributes than in the input, adding e.g. datatype and xml:lang
	 */
	
	class AttributeIterator implements Iterator<Object> {
		Iterator<?> attributes;
		Value content;
		String datatype, lang;
		String path, tag;
		Attribute nextAttribute;
		boolean hasBody;
		
		public AttributeIterator
		(String tag,Iterator<?> attributes, Value content, String path, boolean hasBody, NamespaceContext ctx) {
			this.tag = tag;
			this.attributes = attributes;
			this.content = content;
			this.path = path;
			this.hasBody = hasBody;
			datatype = getDatatypeCurie(content,ctx);
			lang = getLang(content);
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
				String namespace = attr.getName().getNamespaceURI();
				String localPart = attr.getName().getLocalPart();
				
				Value newValue = substituteValue(tag,attr);
				if (newValue!=null) {
					// clear content to prevent it being added as text
					if (namespace.isEmpty() && localPart.equals("content")) 
						context.content = null;
					return eventFactory.createAttribute(attr.getName(), newValue.stringValue());
				}
				else return attr;
			}
			// opportunity to add additional attributes
			else if (datatype!=null) {
				Attribute a = eventFactory.createAttribute("datatype", datatype);
				datatype = null;
				return a;
			}
			else if (lang!=null) {
				QName q = new QName(XML_NAMESPACE,"lang","xml");
				Attribute a = eventFactory.createAttribute(q, lang);
				lang = null;
				return a;
			}
			return null;
		}
		/* If there is a content attribute there is no need to add text content */
		private Value substituteValue(String tag,Attribute attr) {
			String namespace = attr.getName().getNamespaceURI();
			String localPart = attr.getName().getLocalPart();
			String value = attr.getValue();
			Value v = substitute(tag,attr,context.path);
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
	
	/* NamespaceIterator is able to add an additional namespace declaration for content */
	
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
		String tag = name.getNamespaceURI()+name.getLocalPart();
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
		Iterator<?> attributes = new AttributeIterator(tag,start.getAttributes(), context.content, context.path, hasBody, ctx);
		XMLEvent e = eventFactory.createStartElement(name.getPrefix(), name.getNamespaceURI(), name.getLocalPart(), attributes, namespaces, ctx);
		add(e);
		
		// The AttributeIterator (above) clears context.content if it adds the content attribute
		if (!hasBody && context.content!=null)
			add(eventFactory.createCharacters(context.content.stringValue()));
	}

}
