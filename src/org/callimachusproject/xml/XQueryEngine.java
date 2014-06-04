/*
 * Copyright (c) 2013-2014 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMLocator;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.instruct.AttributeSet;
import net.sf.saxon.expr.instruct.Instruction;
import net.sf.saxon.expr.instruct.Procedure;
import net.sf.saxon.expr.instruct.Template;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trans.KeyDefinition;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.type.ValidationException;

import org.callimachusproject.client.HttpUriClient;
import org.callimachusproject.io.ProducerStream;
import org.callimachusproject.io.ProducerStream.OutputProducer;
import org.xml.sax.SAXException;

public class XQueryEngine implements ErrorListener {
	private static final String XQUERY_MEDIA = "application/xquery, application/xml, application/xslt+xml, text/xml, text/xsl";
	private static final String MODULE = "Module declaration must not be used in a main module";
	private static final Pattern[] MODULE_ERRORS = {
			Pattern.compile("Module declaration must not be used in a main module"),
			Pattern.compile("Unexpected token \"<string-literal>\" beyond end of query"),
			Pattern.compile("Unexpected token \"<eof>\" in path expression"),
			Pattern.compile("Prefix \\S+ has not been declared") };
	private final String baseURI;
	private final HttpUriClient client;
	private final List<String> messages = new ArrayList<String>();
	private boolean module;

	public XQueryEngine(String baseURI, HttpUriClient client) {
		this.baseURI = baseURI;
		this.client = client;
	}

	public void validate(InputStream queryStream) throws IOException {
		Configuration config = Configuration.newConfiguration();
        config.setHostLanguage(Configuration.XQUERY);
        StaticQueryContext staticEnv = config.newStaticQueryContext();
        staticEnv.setBaseURI(baseURI);
        staticEnv.setErrorListener(this);
        staticEnv.setModuleURIResolver(new InputSourceResolver(XQUERY_MEDIA, client));
        try {
			staticEnv.compileQuery(queryStream, null);
		} catch (XPathException e) {
			fatalError(e);
		}
	}

	public String[] getErrorMessages() {
		return messages.toArray(new String[messages.size()]);
	}

	public InputStream evaluateResult(InputStream queryStream,
			Map<String, String[]> parameters) throws IOException,
			SaxonApiException, SAXException {
		return evaluateResult(queryStream, parameters, null, null);
	}

	public InputStream evaluateResult(InputStream queryStream,
			Map<String, String[]> parameters, InputStream documentStream,
			String documentId) throws IOException, SaxonApiException, SAXException {
		Processor qtproc = new Processor(false);
		XdmNodeFactory resolver = new XdmNodeFactory(qtproc, client);
		XQueryCompiler xqcomp = qtproc.newXQueryCompiler();
		xqcomp.setBaseURI(URI.create(baseURI));
		xqcomp.setModuleURIResolver(resolver);
		XQueryExecutable xqexec = xqcomp.compile(queryStream);
		final XQueryEvaluator xqeval = xqexec.load();
		if (parameters != null) {
			for (Map.Entry<String, String[]> e : parameters.entrySet()) {
				QName name = new QName(e.getKey());
				ArrayList<XdmItem> values = new ArrayList<>(e.getValue().length);
				for (String value : e.getValue()) {
					values.add(new XdmAtomicValue(value, ItemType.STRING));
				}
				xqeval.setExternalVariable(name, new XdmValue(values));
			}
		}
		if (documentStream != null) {
			xqeval.setContextItem(resolver.parse(documentId, documentStream));
		}
		return new ProducerStream(new OutputProducer() {
			public void produce(OutputStream out) throws IOException {
				xqeval.setDestination(new Serializer(out));
				try {
					xqeval.run();
				} catch (SaxonApiException e) {
					throw new IOException(e);
				}
			}
		});
	}

	@Override
	public void warning(TransformerException exception) {
		messages.add(msg(exception));
	}

	@Override
	public void error(TransformerException exception) {
		messages.add(msg(exception));
	}

	@Override
	public void fatalError(TransformerException exception) {
		String message = exception.getMessage();
		if (MODULE.equals(message)) {
			module = true;
		}
		if (module) {
			for (Pattern regex: MODULE_ERRORS) {
				if (regex.matcher(message).matches())
					return; // ignore module errors
			}
		}
		messages.add(msg(exception));
	}

	private String msg(TransformerException exception) {
		return "Error " + getLocationMessage(exception) + " "
				+ getExpandedMessage(exception);
	}

    private String getLocationMessage(TransformerException err) {
        SourceLocator loc = err.getLocator();
        while (loc == null) {
            if (err.getException() instanceof TransformerException) {
                err = (TransformerException)err.getException();
                loc = err.getLocator();
            } else if (err.getCause() instanceof TransformerException) {
                err = (TransformerException)err.getCause();
                loc = err.getLocator();
            } else {
                return "";
            }
        }
        String locMessage = "";
		String systemId = null;
		NodeInfo node = null;
		String path = null;
		String nodeMessage = null;
		int lineNumber = -1;
		if (loc instanceof DOMLocator) {
		    nodeMessage = "at " + ((DOMLocator)loc).getOriginatingNode().getNodeName() + ' ';
		} else if (loc instanceof NodeInfo) {
		    node = (NodeInfo)loc;
		    nodeMessage = "at " + node.getDisplayName() + ' ';
		} else if (loc instanceof ValidationException && (node = ((ValidationException)loc).getNode()) != null) {
		    nodeMessage = "at " + node.getDisplayName() + ' ';
		} else if (loc instanceof ValidationException && (path = ((ValidationException)loc).getPath()) != null) {
		    nodeMessage = "at " + path + ' ';
		} else if (loc instanceof Instruction) {
		    String instructionName = ((Instruction)loc).getInstructionName();
		    if (!"".equals(instructionName)) {
		        nodeMessage = "at " + instructionName + ' ';
		    }
		    systemId = loc.getSystemId();
		    lineNumber = loc.getLineNumber();
		} else if (loc instanceof Procedure) {
		    String kind = "procedure";
		    if (loc instanceof UserFunction) {
		        kind = "function";
		    } else if (loc instanceof Template) {
		        kind = "template";
		    } else if (loc instanceof AttributeSet) {
		        kind = "attribute-set";
		    } else if (loc instanceof KeyDefinition) {
		        kind = "key";
		    }
		    systemId = loc.getSystemId();
		    lineNumber = loc.getLineNumber();
		    nodeMessage = "at " + kind + " ";
		    StructuredQName name = ((InstructionInfo)loc).getObjectName();
		    if (name != null) {
		        nodeMessage += name.toString();
		        nodeMessage += " ";
		    }
		}
		if (lineNumber == -1) {
		    lineNumber = loc.getLineNumber();
		}
		boolean containsLineNumber = lineNumber != -1;
		if (node != null && !containsLineNumber) {
		    nodeMessage = "at " + Navigator.getPath(node) + ' ';
		}
		if (nodeMessage != null) {
		    locMessage += nodeMessage;
		}
		if (containsLineNumber) {
		    locMessage += "on line " + lineNumber + ' ';
		    if (loc.getColumnNumber() != -1) {
		        locMessage += "column " + loc.getColumnNumber() + ' ';
		    }
		}
		
		if (systemId != null && systemId.length() == 0) {
		    systemId = null;
		}
		if (systemId == null) {
		    systemId = loc.getSystemId();
		}
		if (systemId != null && systemId.length() != 0) {
		    locMessage += (containsLineNumber ? "of " : "in ") + systemId + ':';
		}
		return locMessage;
    }

    private String getExpandedMessage(TransformerException err) {
        StructuredQName qCode = null;
        String additionalLocationText = null;
        if (err instanceof XPathException) {
            qCode = ((XPathException)err).getErrorCodeQName();
            additionalLocationText = ((XPathException)err).getAdditionalLocationText();
        }
        if (qCode == null && err.getException() instanceof XPathException) {
            qCode = ((XPathException)err.getException()).getErrorCodeQName();
        }
        String message = "";
        if (qCode != null) {
            if (qCode.getURI().equals(NamespaceConstant.ERR)) {
                message = qCode.getLocalPart();
            } else {
                message = qCode.getDisplayName();
            }
        }

        if (additionalLocationText != null) {
            message += " " + additionalLocationText;
        }

        Throwable e = err;
        while (true) {
            if (e == null) {
                break;
            }
            String next = e.getMessage();
            if (next == null) {
                next = "";
            }
            if (next.startsWith("net.sf.saxon.trans.XPathException: ")) {
                next = next.substring(next.indexOf(": ") + 2);
            }
            if (!("TRaX Transform Exception".equals(next) || message.endsWith(next))) {
                if (!"".equals(message) && !message.trim().endsWith(":")) {
                    message += ": ";
                }
                message += next;
            }
            if (e instanceof TransformerException) {
                e = ((TransformerException)e).getException();
            } else if (e instanceof SAXException) {
                e = ((SAXException)e).getException();
            } else {
                // e.printStackTrace();
                break;
            }
        }

        return message;
    }
}
