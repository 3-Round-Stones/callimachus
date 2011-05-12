/*
         * $Id: SimpleNamespaceContext.java,v 1.7 2004/07/14 13:23:54 cniles Exp $
         * 
         * Copyright (c) 2004, Christian Niles, unit12.net
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

        import java.util.Collections;
        import java.util.LinkedHashMap;
        import java.util.HashSet;
        import java.util.Iterator;
        import java.util.Map;
        import java.util.Set;

        import javax.xml.XMLConstants;
        import javax.xml.namespace.NamespaceContext;

        /**
         * Simple NamespaceContext implementation backed by a HashMap.
         * 
         * @author Christian Niles
         * @version $Revision: 1.7 $
         */
        public class SimpleNamespaceContext implements 
                ExtendedNamespaceContext, StaticNamespaceContext {

            /**
             * The parent context, which may be <code>null</code>
             */
            protected NamespaceContext parent;

            /**
             * map containing bound namespaces, keyed by their prefix. A LinkedHashMap
             * is used to ensure that {@link #getPrefix(String)} always returns the same
             * prefix, unless that prefix is removed.
             */
            protected Map namespaces = new LinkedHashMap();

            /**
             * Constructs a SimpleNamespaceContext with no parent context or namespace
             * declarations.
             */
            public SimpleNamespaceContext() {

            }

            /**
             * Constructs a SimpleNamespaceContext with no parent context that contains
             * the specified prefixes.
             * 
             * @param namespaces A Map of namespace URIs, keyed by their prefixes.
             */
            public SimpleNamespaceContext(Map namespaces) {

                if (namespaces != null) {

                    this .namespaces.putAll(namespaces);

                }

            }

            /**
             * Constructs an empty SimpleNamespaceContext with the given parent. The
             * parent context will be forwarded any requests for namespaces not declared
             * in this context.
             * 
             * @param parent The parent context.
             */
            public SimpleNamespaceContext(NamespaceContext parent) {

                this .parent = parent;

            }

            /**
             * Constructs an empty SimpleNamespaceContext with the given parent. The
             * parent context will be forwarded any requests for namespaces not declared
             * in this context.
             * 
             * @param parent The parent context.
             * @param namespaces A Map of namespace URIs, keyed by their prefixes.
             */
            public SimpleNamespaceContext(NamespaceContext parent,
                    Map namespaces) {

                this .parent = parent;

                if (namespaces != null) {

                    this .namespaces.putAll(namespaces);

                }

            }

            /**
             * Returns a reference to the parent of this context.
             * 
             * @return The parent context, or <code>null</code> if this is a root
             * 		context.
             */
            public NamespaceContext getParent() {

                return parent;

            }

            /**
             * Sets the parent context used to inherit namespace bindings.
             * 
             * @param parent The new parent context.
             */
            public void setParent(NamespaceContext parent) {

                this .parent = parent;

            }

            /**
             * Determines if this is a root context.
             * 
             * @return <code>true</code> if this is a root context, <code>false</code>
             * 		otherwise.
             */
            public boolean isRootContext() {

                return parent == null;

            }

            public String getNamespaceURI(String prefix) {

                if (prefix == null) {

                    throw new IllegalArgumentException(
                            "prefix argument was null");

                } else if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {

                    return XMLConstants.XML_NS_URI;

                } else if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {

                    return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

                } else if (namespaces.containsKey(prefix)) {

                    // The StAX spec says to return null for any undefined prefix. To support
                    // undefining a prefix as specified in the Namespaces spec, we store
                    // undefined prefixes as the empty string.

                    String uri = (String) namespaces.get(prefix);
                    if (uri.length() == 0) {

                        return null;

                    } else {

                        return uri;

                    }

                } else if (parent != null) {

                    return parent.getNamespaceURI(prefix);

                } else {

                    return null;

                }

            }

            public String getPrefix(String nsURI) {

                if (nsURI == null) {

                    throw new IllegalArgumentException("nsURI was null");

                } else if (nsURI.length() == 0) {

                    throw new IllegalArgumentException("nsURI was empty");

                } else if (nsURI.equals(XMLConstants.XML_NS_URI)) {

                    return XMLConstants.XML_NS_PREFIX;

                } else if (nsURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {

                    return XMLConstants.XMLNS_ATTRIBUTE;

                }

                Iterator iter = namespaces.entrySet().iterator();
                while (iter.hasNext()) {

                    Map.Entry entry = (Map.Entry) iter.next();
                    String uri = (String) entry.getValue();

                    if (uri.equals(nsURI)) {

                        return (String) entry.getKey();

                    }

                }

                if (parent != null) {

                    return parent.getPrefix(nsURI);

                } else if (nsURI.length() == 0) {

                    return "";

                } else {

                    return null;

                }

            }

            /**
             * Determines if the specified prefix is declared within this context,
             * irrespective of any ancestor contexts.
             * 
             * @param prefix The prefix to check.
             * @return <code>true</code> if the prefix is declared in this context,
             * 		<code>false</code> otherwise.
             */
            public boolean isPrefixDeclared(String prefix) {

                return namespaces.containsKey(prefix);

            }

            public Iterator getDeclaredPrefixes() {

                return Collections.unmodifiableCollection(
                        this .namespaces.keySet()).iterator();

            }

            /**
             * Returns the number of namespace prefixes declared in this context.
             * 
             * @return The number of namespace prefixes declared in this context.
             */
            public int getDeclaredPrefixCount() {

                return namespaces.size();

            }

            public Iterator getPrefixes() {

                if (parent == null
                        || !(parent instanceof  ExtendedNamespaceContext)) {

                    return getDeclaredPrefixes();

                } else {

                    Set prefixes = new HashSet(this .namespaces.keySet());

                    ExtendedNamespaceContext Ctx = (ExtendedNamespaceContext) parent;
                    for (Iterator i = Ctx.getPrefixes(); i.hasNext();) {

                        String prefix = (String) i.next();
                        prefixes.add(prefix);

                    }

                    return prefixes.iterator();

                }

            }

            public Iterator getPrefixes(String nsURI) {

                if (nsURI == null) {

                    throw new IllegalArgumentException("nsURI was null");

                } else if (nsURI.equals(XMLConstants.XML_NS_URI)) {

                    return Collections.singleton(XMLConstants.XML_NS_PREFIX)
                            .iterator();

                } else if (nsURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {

                    return Collections.singleton(XMLConstants.XMLNS_ATTRIBUTE)
                            .iterator();

                }

                Set prefixes = null;
                Iterator iter = namespaces.entrySet().iterator();
                while (iter.hasNext()) {

                    Map.Entry entry = (Map.Entry) iter.next();
                    String uri = (String) entry.getValue();
                    if (uri.equals(nsURI)) {

                        if (prefixes == null) {

                            prefixes = new HashSet();

                        }
                        prefixes.add(entry.getKey());

                    }

                }

                if (parent != null) {

                    for (Iterator i = parent.getPrefixes(nsURI); i.hasNext();) {

                        String prefix = (String) i.next();

                        if (prefixes == null) {

                            prefixes = new HashSet();

                        }
                        prefixes.add(prefix);

                    }

                }

                if (prefixes != null) {

                    return Collections.unmodifiableSet(prefixes).iterator();

                } else if (nsURI.length() == 0) {

                    return Collections.singleton("").iterator();

                } else {

                    return Collections.EMPTY_LIST.iterator();

                }

            }

            /**
             * Sets the default namespace in this context.
             * 
             * @param nsURI The default namespace URI.
             * @return The previously declared namespace uri, or <code>null</code> if
             * 		the default prefix wasn't previously declared in this context.
             */
            public String setDefaultNamespace(String nsURI) {

                if (nsURI != null) {

                    if (nsURI.equals(XMLConstants.XML_NS_URI)) {

                        throw new IllegalArgumentException(
                                "Attempt to map 'xml' uri");

                    } else if (nsURI
                            .equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {

                        throw new IllegalArgumentException(
                                "Attempt to map 'xmlns' uri");

                    }

                    return (String) namespaces.put(
                            XMLConstants.DEFAULT_NS_PREFIX, nsURI);

                } else {

                    // put the empty string to record an undefined prefix
                    return (String) namespaces.put(
                            XMLConstants.DEFAULT_NS_PREFIX, "");

                }

            }

            /**
             * Declares a namespace binding in this context.
             * 
             * @param prefix The namespace prefix.
             * @param nsURI The namespace URI.
             * @return The previously declared namespace uri, or <code>null</code> if
             * 		the prefix wasn't previously declared in this context.
             */
            public String setPrefix(String prefix, String nsURI) {

                if (prefix == null) {

                    throw new NullPointerException("Namespace Prefix was null");

                } else if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {

                    return setDefaultNamespace(nsURI);

                } else if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {

                    throw new IllegalArgumentException(
                            "Attempt to map 'xml' prefix");

                } else if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {

                    throw new IllegalArgumentException(
                            "Attempt to map 'xmlns' prefix");

                } else if (nsURI != null) {

                    if (nsURI.equals(XMLConstants.XML_NS_URI)) {

                        throw new IllegalArgumentException(
                                "Attempt to map 'xml' uri");

                    } else if (nsURI
                            .equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {

                        throw new IllegalArgumentException(
                                "Attempt to map 'xmlns' uri");

                    } else {

                        return (String) namespaces.put(prefix, nsURI);

                    }

                } else {

                    // put the empty string to record an undefined prefix
                    return (String) namespaces.put(prefix, "");

                }

            }

        }
