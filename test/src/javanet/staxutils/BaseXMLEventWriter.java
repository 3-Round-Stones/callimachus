/*
         * $Id: BaseXMLEventWriter.java,v 1.9 2005/03/09 22:34:34 cniles Exp $
         * 
         * Copyright (c) 2004, Christian Niles, Unit12
         * All rights reserved.
         *
         * Redistribution and use in source and binary forms, with or without
         * modification, are permitted provided that the following conditions are met:
         * 
         *		*   Redistributions of source code must retain the above copyright
         *          notice, this list of conditions and the following disclaimer.
         * 
         *	    *	Redistributions in binary form must reproduce the above copyright
         *          notice, this list of conditions and the following disclaimer in the
         *          documentation and/or other materials provided with the distribution.
         * 
         *      *   Neither the name of Christian Niles, Unit12, nor the names of its
         *          contributors may be used to endorse or promote products derived from
         *          this software without specific prior written permission.
         *
         * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
         * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
         * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
         * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
         * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
         * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
         * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
         * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
         * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
         * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
         * POSSIBILITY OF SUCH DAMAGE.
         * 
         */
        package javanet.staxutils;

        import java.util.ArrayList;
        import java.util.Iterator;
        import java.util.LinkedHashMap;
        import java.util.List;
        import java.util.Map;

        import javax.xml.namespace.NamespaceContext;
        import javax.xml.namespace.QName;
        import javax.xml.stream.XMLEventFactory;
        import javax.xml.stream.XMLEventReader;
        import javax.xml.stream.XMLEventWriter;
        import javax.xml.stream.XMLStreamException;
        import javax.xml.stream.events.*;

        /**
         * Base class for {@link XMLEventWriter} implementations. This implemenation
         * buffers Attribute and Namespace events as specified in the specification,
         * maintains a stack of NamespaceContext instances based on the events it
         * receives, and repairs any missing namespaces. Subclasses should implement the
         * {@link #sendEvent(XMLEvent)} method to receive the processed events and
         * perform additional processing.
         * 
         * @author Christian Niles
         * @version $Revision: 1.9 $
         */
        public abstract class BaseXMLEventWriter implements  XMLEventWriter {

            /**
             * XMLEventFactory used to construct XMLEvent instances.
             */
            protected XMLEventFactory factory;

            /** list of {@link SimpleNamespaceContext}s. */
            protected List nsStack = new ArrayList();

            /** 
             * Reference to the last StartElement sent. This will be null if no
             * StartElement has been sent, or after a non Attribute/Namespace
             * event is received.
             */
            protected StartElement lastStart;

            /**
             * LinkedHashMap of attribute events sent surrounding the last
             * StartElement. By using LinkedHashMap, the attributes will stay in the
             * order they were defined.
             */
            protected Map attrBuff = new LinkedHashMap();

            /**
             * LinkedHashMap of namespace events sent surrounding the last
             * StartElement. By using LinkedHashMap, the namespaces will stay in the
             * order they were defined.
             */
            protected Map nsBuff = new LinkedHashMap();

            /**
             * Whether this writer has been closed or not.
             */
            protected boolean closed;

            protected BaseXMLEventWriter() {

                this (null, null);

            }

            protected BaseXMLEventWriter(XMLEventFactory eventFactory,
                    NamespaceContext nsCtx) {

                if (nsCtx != null) {

                    nsStack.add(new SimpleNamespaceContext(nsCtx));

                } else {

                    nsStack.add(new SimpleNamespaceContext());

                }

                if (eventFactory != null) {

                    factory = eventFactory;

                } else {

                    factory = XMLEventFactory.newInstance();

                }

            }

            public synchronized void flush() throws XMLStreamException {

                if (!closed) {

                    sendCachedEvents();

                }

            }

            /**
             * Sends any events that have been cached in anticipation of further events.
             * 
             * @throws XMLStreamException If an error occurs sending the events.
             */
            private void sendCachedEvents() throws XMLStreamException {

                if (lastStart != null) {

                    // A StartElement, and possibly attributes and namespaces, have been
                    // cached. We need to combine them all into a single StartElement
                    // event to send to sendEvent(XMLEvent)

                    // First, construct the new NamespaceContext for the tag
                    SimpleNamespaceContext nsCtx = this .pushNamespaceStack();

                    // List used to store any defaulted/rewritten namespaces
                    List namespaces = new ArrayList();

                    // merge namespaces
                    mergeNamespaces(lastStart.getNamespaces(), namespaces);
                    mergeNamespaces(nsBuff.values().iterator(), namespaces);
                    nsBuff.clear();

                    // merge attributes
                    List attributes = new ArrayList();
                    mergeAttributes(lastStart.getAttributes(), namespaces,
                            attributes);
                    mergeAttributes(attrBuff.values().iterator(), namespaces,
                            attributes);
                    attrBuff.clear();

                    // determine the name of the new start tag
                    QName tagName = lastStart.getName();
                    QName newName = processQName(tagName, namespaces);

                    // construct new element
                    StartElement newStart = factory.createStartElement(newName
                            .getPrefix(), newName.getNamespaceURI(), newName
                            .getLocalPart(), attributes.iterator(), namespaces
                            .iterator(), nsCtx);

                    lastStart = null;
                    sendEvent(newStart);

                } else {

                    // no start element was cached, but we may have cached some
                    // namespaces and attributes that need to be written.

                    for (Iterator i = nsBuff.values().iterator(); i.hasNext();) {

                        XMLEvent evt = (XMLEvent) i.next();
                        sendEvent(evt);

                    }
                    nsBuff.clear();

                    for (Iterator i = attrBuff.values().iterator(); i.hasNext();) {

                        XMLEvent evt = (XMLEvent) i.next();
                        sendEvent(evt);

                    }
                    attrBuff.clear();

                }

            }

            /**
             * Merges a set of {@link Attribute} events, possibly adding additional
             * {@link Namespace} events if the attribute's prefix isn't bound in the
             * provided context.
             * 
             * @param iter An {@link Iterator} of {@link Attribute} events.
             * @param namespaces A {@link List} to which any new {@link Namespace}
             * 		events should be added.
             * @param attributes A {@link List} to which all {@link Attributes} events
             * 		should be merged.
             */
            private void mergeAttributes(Iterator iter, List namespaces,
                    List attributes) {

                while (iter.hasNext()) {

                    Attribute attr = (Attribute) iter.next();

                    // check if the attribute QName has the proper mapping
                    QName attrName = attr.getName();
                    QName newName = processQName(attrName, namespaces);
                    if (!attrName.equals(newName)) {

                        // need to generate a new attribute with the new qualified name
                        Attribute newAttr = factory.createAttribute(newName,
                                attr.getValue());
                        attributes.add(newAttr);

                    } else {

                        // the attribute is fine
                        attributes.add(attr);

                    }

                }

            }

            /**
             * Merges a set of {@link Namespaces} events into the provided {@link List}.
             * 
             * @param iter An {@link Iterator} of {@link Namespace}s to merge.
             * @param namespaces A {@link List} containing all added {@link Namespace}
             * 		events.
             * @throws XMLStreamException If a conflicting namespace binding is
             * 		encountered.
             */
            private void mergeNamespaces(Iterator iter, List namespaces)
                    throws XMLStreamException {

                // for each namespace, add it to the context, and place it in the list
                while (iter.hasNext()) {

                    Namespace ns = (Namespace) iter.next();
                    String prefix = ns.getPrefix();
                    String nsURI = ns.getNamespaceURI();
                    SimpleNamespaceContext nsCtx = this .peekNamespaceStack();

                    if (!nsCtx.isPrefixDeclared(prefix)) {

                        // mapping doesn't exist, so add it to the context/list
                        if (prefix == null || prefix.length() == 0 || prefix.equals("xmlns")) {

                            nsCtx.setDefaultNamespace(nsURI);

                        } else {

                            nsCtx.setPrefix(prefix, nsURI);

                        }

                        namespaces.add(ns);

                    } else if (!nsCtx.getNamespaceURI(prefix).equals(nsURI)) {

                        throw new XMLStreamException(
                                "Prefix already declared: " + ns, ns
                                        .getLocation());

                    } else {

                        // duplicate namespace declaration

                    }

                }

            }

            /**
             * Processes a {@link QName}, possibly rewriting it to match the current
             * namespace context.
             * 
             * @param name The {@link QName} to process.
             * @param namespaces A {@link List} of {@link Namespace} events to which any
             * 		new namespace bindings should be added.
             * @return The new name, or the <code>name</code> parameter if no changes
             * 		were necessary.
             */
            private QName processQName(QName name, List namespaces) {

                // get current context
                SimpleNamespaceContext nsCtx = this .peekNamespaceStack();

                // get name parts
                String nsURI = name.getNamespaceURI();
                String prefix = name.getPrefix();
                if (prefix != null && prefix.length() > 0) {

                    // prefix provided; see if it is okay in current context, otherwise we'll
                    // have to rewrite it
                    String resolvedNS = nsCtx.getNamespaceURI(prefix);
                    if (resolvedNS != null) {

                        if (!resolvedNS.equals(nsURI)) {

                            // prefix is already bound to a different namespace; we have to
                            // find or generate and alternative prefix
                            String newPrefix = nsCtx.getPrefix(nsURI);
                            if (newPrefix == null) {

                                // no existing prefix; need to generate a new prefix
                                newPrefix = generatePrefix(nsURI, nsCtx,
                                        namespaces);

                            }

                            // return the newly prefixed name
                            return new QName(nsURI, name.getLocalPart(),
                                    newPrefix);

                        } else {

                            // prefix bound to proper namespace; name is already ok

                        }

                    } else if (nsURI != null && nsURI.length() > 0) {

                        // prefix isn't bound yet; bind it and the name is good to go
                        nsCtx.setPrefix(prefix, nsURI);
                        namespaces.add(factory.createNamespace(prefix, nsURI));

                    }

                    return name;

                } else if (nsURI != null && nsURI.length() > 0) {

                    // name is namespaced, but has no prefix associated with it. Look for an
                    // existing prefix, or generate one.
                    String newPrefix = nsCtx.getPrefix(nsURI);
                    if (newPrefix == null) {

                        // no existing prefix; need to generate a new prefix
                        newPrefix = generatePrefix(nsURI, nsCtx, namespaces);

                    }

                    // return the newly prefixed name
                    return new QName(nsURI, name.getLocalPart(), newPrefix);

                } else {

                    // name belongs to no namespace and has no prefix.
                    return name;

                }

            }

            /**
             * Generates a new namespace prefix for the specified namespace URI that
             * doesn't collide with any existing prefix.
             * 
             * @param nsURI The URI for which to generate a prefix.
             * @param nsCtx The namespace context in which to set the prefix.
             * @param namespaces A {@link List} of {@link Namespace} events to which the
             * 		new prefix will be added.
             * @return The new prefix.
             */
            private String generatePrefix(String nsURI,
                    SimpleNamespaceContext nsCtx, List namespaces) {

                String newPrefix;
                int nsCount = 0;
                do {

                    newPrefix = "ns" + nsCount;
                    nsCount++;

                } while (nsCtx.getNamespaceURI(newPrefix) != null);

                nsCtx.setPrefix(newPrefix, nsURI);
                namespaces.add(factory.createNamespace(newPrefix, nsURI));
                return newPrefix;

            }

            public synchronized void close() throws XMLStreamException {

                if (closed) {

                    return;

                }

                try {

                    flush();

                } finally {

                    closed = true;

                }

            }

            public synchronized void add(XMLEvent event)
                    throws XMLStreamException {

                if (closed) {

                    throw new XMLStreamException("Writer has been closed");

                }

                // If the event is an Attribute or Namespace, cache it; otherwise, we
                // should send any previously cached items
                switch (event.getEventType()) {

                case XMLEvent.NAMESPACE:
                    cacheNamespace((Namespace) event);
                    return;

                case XMLEvent.ATTRIBUTE:
                    cacheAttribute((Attribute) event);
                    return;

                default:
                    // send any cached events
                    sendCachedEvents();

                }

                if (event.isStartElement()) {

                    // cache the start element in case any following Attribute or
                    // Namespace events occur
                    lastStart = event.asStartElement();
                    return;

                } else if (event.isEndElement()) {

                    if (nsStack.isEmpty()) {

                        throw new XMLStreamException(
                                "Mismatched end element event: " + event);

                    }

                    SimpleNamespaceContext nsCtx = this .peekNamespaceStack();
                    EndElement endTag = event.asEndElement();
                    QName endElemName = endTag.getName();

                    String prefix = endElemName.getPrefix();
                    String nsURI = endElemName.getNamespaceURI();
                    if (nsURI != null && nsURI.length() > 0) {

                        // check that the prefix used in the name is bound to the same
                        // namespace
                        String boundURI = nsCtx.getNamespaceURI(prefix);
                        if (!nsURI.equals(boundURI)) {

                            // not equal! now we must see what prefix it's actually
                            // bound to
                            String newPrefix = nsCtx.getPrefix(nsURI);
                            if (newPrefix != null) {

                                QName newName = new QName(nsURI, endElemName
                                        .getLocalPart(), newPrefix);
                                event = factory.createEndElement(newName,
                                        endTag.getNamespaces());

                            } else {

                                // no prefix is bound! report an error
                                throw new XMLStreamException(
                                        "EndElement namespace (" + nsURI
                                                + ") isn't bound [" + endTag
                                                + "]");

                            }

                        }

                    } else {

                        // default namespace
                        String defaultURI = nsCtx.getNamespaceURI("");
                        if (defaultURI != null && defaultURI.length() > 0) {

                            throw new XMLStreamException(
                                    "Unable to write "
                                            + event
                                            + " because default namespace is occluded by "
                                            + defaultURI);

                        }

                        // else, namespace matches and can be written directly

                    }

                    // pop the stack
                    popNamespaceStack();

                }

                sendEvent(event);

            }

            public void add(XMLEventReader reader) throws XMLStreamException {

                while (reader.hasNext()) {

                    add(reader.nextEvent());

                }

            }

            public synchronized String getPrefix(String nsURI)
                    throws XMLStreamException {

                return getNamespaceContext().getPrefix(nsURI);

            }

            public synchronized void setPrefix(String prefix, String nsURI)
                    throws XMLStreamException {

                peekNamespaceStack().setPrefix(prefix, nsURI);

            }

            public synchronized void setDefaultNamespace(String nsURI)
                    throws XMLStreamException {

                peekNamespaceStack().setDefaultNamespace(nsURI);

            }

            public synchronized void setNamespaceContext(NamespaceContext root)
                    throws XMLStreamException {

                SimpleNamespaceContext parent = (SimpleNamespaceContext) nsStack
                        .get(0);
                parent.setParent(root);

            }

            public synchronized NamespaceContext getNamespaceContext() {

                return peekNamespaceStack();

            }

            /**
             * Removes the active {@link SimpleNamespaceContext} from the top of the
             * stack.
             * 
             * @return The {@link SimpleNamespaceContext} removed from the namespace 
             * 		stack.
             */
            protected SimpleNamespaceContext popNamespaceStack() {

                return (SimpleNamespaceContext) nsStack
                        .remove(nsStack.size() - 1);

            }

            /**
             * Returns the active {@link SimpleNamespaceContext} from the top of the
             * stack.
             * 
             * @return The active {@link SimpleNamespaceContext} from the top of the
             * stack.
             */
            protected SimpleNamespaceContext peekNamespaceStack() {

                return (SimpleNamespaceContext) nsStack.get(nsStack.size() - 1);

            }

            /**
             * Creates a new {@link SimpleNamespaceContext} and adds it to the top of
             * the stack.
             * 
             * @return The new {@link SimpleNamespaceContext}.
             */
            protected SimpleNamespaceContext pushNamespaceStack() {

                SimpleNamespaceContext nsCtx;

                SimpleNamespaceContext parent = peekNamespaceStack();
                if (parent != null) {

                    nsCtx = new SimpleNamespaceContext(parent);

                } else {

                    nsCtx = new SimpleNamespaceContext();

                }

                nsStack.add(nsCtx);
                return nsCtx;

            }

            /**
             * Adds the specified {@link Attribute} to the attribute cache.
             * 
             * @param attr The attribute to cache.
             */
            protected void cacheAttribute(Attribute attr) {

                attrBuff.put(attr.getName(), attr);

            }

            /**
             * Adds the provided {@link Namespace} event to the namespace cache. The
             * current namespace context will not be affected.
             * 
             * @param ns The namespace to add to the cache.
             */
            protected void cacheNamespace(Namespace ns) {

                nsBuff.put(ns.getPrefix(), ns);

            }

            /**
             * Called by the methods of this class to write the event to the stream.
             * 
             * @param event The event to write.
             * @throws XMLStreamException If an error occurs processing the event.
             */
            protected abstract void sendEvent(XMLEvent event)
                    throws XMLStreamException;

        }

