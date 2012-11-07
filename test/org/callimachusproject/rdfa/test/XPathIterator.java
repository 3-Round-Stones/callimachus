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
package org.callimachusproject.rdfa.test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * @author Steve Battle
 */
public class XPathIterator implements Iterator<Object> {	
	Queue<XPathExpression> queue = (Queue<XPathExpression>) new LinkedList<XPathExpression>();
	List<NamespaceContext> contexts = new LinkedList<NamespaceContext>();
	String base;
	boolean useDataAttributes = false;
	static XPathFactory xPathFactory;
	static {
		try {
			xPathFactory = XPathFactory.newInstance(javax.xml.xpath.XPathFactory.DEFAULT_OBJECT_MODEL_URI, "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl", XPathIterator.class.getClassLoader());
		} catch (XPathFactoryConfigurationException e) {
			xPathFactory = XPathFactory.newInstance();
		}
	}
	
	final String[] DISTINGUISHING_VALUE_ATTRIBUTES = { "id", "name", "class", "content", "for", "lang", "value" };
	List<String> distinguishingValueAttributes = Arrays.asList(DISTINGUISHING_VALUE_ATTRIBUTES);

	final String[] DISTINGUISHING_URI_ATTRIBUTES = { "about", "resource", "href", "src", "datatype" };
	List<String> distinguishingURIAttributes = Arrays.asList(DISTINGUISHING_URI_ATTRIBUTES);

	final String[] DISTINGUISHING_DATA_ATTRIBUTES = { "data-rel", "data-add", "data-search", "data-more", "data-options" };
	List<String> distinguishingDataAttributes = Arrays.asList(DISTINGUISHING_DATA_ATTRIBUTES);

	// enumerate XPaths to all leaf nodes in the document
	XPathIterator(Document doc, String base) throws Exception {
		this.base = base;
		enumerate(doc.getDocumentElement(),"",true);
	}
	
	// enumerate XPaths to all leaf nodes in the document
	XPathIterator(Document doc, String base, boolean useDataAttributes) throws Exception {
		this.base = base;
		this.useDataAttributes = useDataAttributes;
		enumerate(doc.getDocumentElement(),"",true);
	}

	XPathIterator(Element element, String base) throws Exception {
		this.base = base;
		enumerate(element,"",true);
	}

	private void enumerate(final Element element, String path, boolean positive) throws Exception {
		String prefix = element.getPrefix();
		// if this element has no prefix and the default namespace is defined then the prefix is empty
		if (prefix==null && element.lookupNamespaceURI(null)!=null) prefix = "";
		path += "/"+(prefix==null?"":prefix+":")+element.getLocalName();
		String content=null, text=null;
		boolean leaf = true;

		// add distinguishing attributes
		NamedNodeMap attributes = element.getAttributes();
		for (int i=0; i<attributes.getLength(); i++) {
			Attr a = (Attr) attributes.item(i);
			if (a.getName().equals("content")) 
				content = a.getValue();
			// the @class may denote this as a negative-test
			else if (a.getName().equals("class") && a.getValue().contains("negative-test"))
				positive = false;
			else if (distinguishingValueAttributes.contains(a.getName())) {
				path += "[@"+a.getName()+"='"+a.getValue()+"']";
			}
			else if (distinguishingURIAttributes.contains(a.getName())) {
				// for testing don't check text values starting '?' probably a relative URI
				// typically derived from an expression {...} which won't correspond
				if (a.getValue().startsWith("?") || a.getValue().contains("{")) continue;
				
				java.net.URI relBase = new java.net.URI(base.substring(0,base.lastIndexOf('/')));
				java.net.URI resBase = new java.net.URI(base);
				String resolved = a.getValue();
				// we may use the disguise href/src={URI} to prevent href and src generating undesired subjects
				if (resolved.startsWith("{") && resolved.endsWith("}")) 
					resolved = resolved.substring(1,resolved.length()-1);
				try {
					resolved = a.getValue().isEmpty()?base:resBase.resolve(a.getValue()).toString();
				}
				catch (Exception e) {}
				String relative = a.getValue();
				try {
					relative = a.getValue().equals(base)?"":relBase.relativize(new java.net.URI(a.getValue())).toString();
				}
				catch (Exception e) {}

				if (relative.indexOf('#')>=0) relative = relative.substring(relative.lastIndexOf('#'));
				//if (relative.indexOf('?')>=0) relative = relative.substring(relative.lastIndexOf('?'));
				
				// legacy RDFa generation removes about="?this" where there are no solutions
				// in future retain @about with document URI (ending with .xhtml)
				// don't report missing @about as an error
				if (!resolved.endsWith(".xhtml")) {
					path += "[@"+a.getName()+"='"+resolved+"'";
					// add possible {...} to match disguised URIs in target
					if (a.getName().equals("href") || a.getName().equals("src")) 
						path += " or @"+a.getName()+"='{"+resolved+"}'";
					path += " or @"+a.getName()+"='"+relative+"']";
				}
			}
			else if (useDataAttributes && distinguishingDataAttributes.contains(a.getName())) {
				path += "[@"+a.getName()+"='"+a.getValue()+"']";
			}

		}
		// iterate over any children
		NodeList children = element.getChildNodes();
		for (int j=0; j<children.getLength(); j++) {
			org.w3c.dom.Node node = children.item(j);
			switch (node.getNodeType()) {
			case org.w3c.dom.Node.ELEMENT_NODE:
				enumerate((Element)node,path,positive);
				leaf = false;
				break;
			case org.w3c.dom.Node.TEXT_NODE:
			case org.w3c.dom.Node.CDATA_SECTION_NODE:
				Text t = (Text)node;
				if (text!=null) leaf = false;
				text = t.getTextContent().trim();					
			}
		}
		if (leaf) {

			XPath xpath = xPathFactory.newXPath();
			// add namespace prefix resolver to the xpath based on the current element
			xpath.setNamespaceContext(new AbstractNamespaceContext(){
				public String getNamespaceURI(String prefix) {
					// for the empty prefix lookup the default namespace
					if (prefix.isEmpty()) return element.lookupNamespaceURI(null);
					return element.lookupNamespaceURI(prefix);
				}
			});
			
			// content and text
			if (content!=null && text!=null) {
				path = path + "[@content='"+content+"' and normalize-space(text())='"+text+"']";
			}
			// no content or text
			else if (content==null && (text==null || text.isEmpty())) {
				path = path + "[not(*) and normalize-space(text())='']";
			}
			// text but no content
			else if (content==null && text!=null && !text.isEmpty()) {
				path = path + "[@content='"+text+"' or normalize-space(text())='"+text+"']";				
			}
			// content but no text
			else if (content!=null && !whitespace(content)) {
				path = path + "[@content='"+content+"' or normalize-space(text())='"+content+"']";
			}
				
			if (!path.contains("\n")) {
				final String exp = path;
				final XPathExpression compiled = xpath.compile(exp);
				final boolean negative = !positive;
				// implements XPathExpression toString() returning the string expression
				queue.add(new XPathExpression() {
					public String evaluate(Object source) throws XPathExpressionException {
						return compiled.evaluate(source);
					}
					public String evaluate(InputSource source) throws XPathExpressionException {
						return compiled.evaluate(source);
					}
					public Object evaluate(Object source, QName returnType) throws XPathExpressionException {
						return compiled.evaluate(source, returnType);
					}
					public Object evaluate(InputSource source, QName returnType) throws XPathExpressionException {
						return compiled.evaluate(source, returnType);
					}
					public String toString() {
						// prefix XPaths for negative tests with '-'
						return (negative?"-":"")+exp;
					}
				});
				contexts.add(xpath.getNamespaceContext());
			}
		}								
	}
	private boolean whitespace(String content) {
		content.replaceAll("\n", "");
		content.replaceAll("\t", "");
		return content.trim().isEmpty();
	}

	@Override
	public boolean hasNext() {
		return !queue.isEmpty();
	}
	@Override
	public XPathExpression next() {
		return queue.remove();
	}
	@Override
	public void remove() {}
}

