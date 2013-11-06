package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.BinaryCodec;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.commons.codec.net.URLCodec;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleSheet;

import com.steadystate.css.parser.CSSOMParser;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcStep;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.HttpUtils;
import com.xmlcalabash.util.TreeWriter;

public class DeserializeCascadingStyleSheetStep implements XProcStep {
    private static final QName _content_type = new QName("content-type");
    private static final QName _encoding = new QName("encoding");
    private static final QName _charset = new QName("charset");

    private XProcRuntime runtime = null;
    private XAtomicStep step = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;
	private String contentType = "text/css";
	private String charset;
	private String encoding;

    /**
     * Creates a new instance
     */
    public DeserializeCascadingStyleSheetStep(XProcRuntime runtime, XAtomicStep step) {
        this.runtime = runtime;
        this.step =step;
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    @Override
	public void setParameter(QName name, RuntimeValue value) {
		// no parameters
	}

	@Override
	public void setParameter(String port, QName name, RuntimeValue value) {
		// no parameters
	}

	@Override
	public void setOption(QName name, RuntimeValue value) {
		if (_content_type.equals(name)) {
	        contentType = value.getString();
	        if (charset == null) {
	        	charset = HttpUtils.getCharset(value.getString());
	        }
        } else if (_encoding.equals(name)) {
            encoding = value.getString();
        } else if (_charset.equals(name)) {
            charset = value.getString();
        }
	}

	public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        try {
			while (source.moreDocuments()) {
				XdmNode doc = source.read();
				String text = decodeText(doc);

				TreeWriter tree = new TreeWriter(runtime);
				tree.startDocument(doc.getBaseURI());

				XdmSequenceIterator iter = doc.axisIterator(Axis.CHILD);
				XdmNode child = (XdmNode) iter.next();
				while (child.getNodeKind() != XdmNodeKind.ELEMENT) {
					tree.addSubtree(child);
					child = (XdmNode) iter.next();
				}
				tree.addStartElement(child);
				tree.addAttributes(child);
				tree.startContent();

				CSSStyleSheet sheet = parse(text, doc.getBaseURI()
						.toASCIIString(), contentType, charset);
				new CSStoXML(tree).writeStyleSheet(sheet);

				tree.addEndElement();
				tree.endDocument();
				result.write(tree.getResult());
			}
        } catch (UnsupportedEncodingException uee) {
            throw XProcException.stepError(10, uee);
        } catch (DecoderException e) {
            throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		}
	}

	private CSSStyleSheet parse(String text, String baseURI, String contentType,
			String charset) {
		CSSOMParser parser = new CSSOMParser();
		InputSource css = new InputSource(new StringReader(text));
		css.setURI(baseURI);
		if (contentType != null) {
			css.setMedia(contentType);
		}
		if (charset != null) {
			css.setEncoding(charset);
		}
		parser.setErrorHandler(new XProcErrorHandler(step.getNode()));
		try {
			return parser.parseStyleSheet(css, null, css.getURI());
		} catch (IOException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		}
	}

	private String decodeText(XdmNode doc) throws UnsupportedEncodingException,
			DecoderException {
		String text = extractText(doc);
		if ("base64".equals(encoding)) {
		    if (charset == null) {
		        throw XProcException.stepError(10);
		    }
		    byte[] decoded = Base64.decodeBase64(text);
		    return new String(decoded, charset);
		} else if ("base32".equals(encoding)) {
		    if (charset == null) {
		        throw XProcException.stepError(10);
		    }
		    byte[] decoded = new Base32().decode(text);
		    return new String(decoded, charset);
		} else if ("hex".equals(encoding)) {
		    if (charset == null) {
		        throw XProcException.stepError(10);
		    }
		    byte[] decoded = Hex.decodeHex(text.toCharArray());
		    return new String(decoded, charset);
		} else if ("binary".equals(encoding)) {
		    if (charset == null) {
		        throw XProcException.stepError(10);
		    }
		    byte[] decoded = BinaryCodec.fromAscii(text.toCharArray());
		    return new String(decoded, charset);
		} else if ("quoted-printable".equals(encoding)) {
		    if (charset == null) {
		        throw XProcException.stepError(10);
		    }
		    return new QuotedPrintableCodec(charset).decode(text);
		} else if ("www-form-urlencoded".equals(encoding)) {
		    if (charset == null) {
		        throw XProcException.stepError(10);
		    }
		    return new URLCodec(charset).decode(text);
		} else if (encoding != null && encoding.length() != 0) {
		    throw new XProcException(step.getNode(), "Unexpected encoding: " + encoding);
		} else {
			return text;
		}
	}

    private String extractText(XdmNode doc) {
        StringBuilder content = new StringBuilder();
        XdmSequenceIterator iter = doc.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            if (child.getNodeKind() == XdmNodeKind.ELEMENT || child.getNodeKind() == XdmNodeKind.TEXT) {
                content.append(child.getStringValue());
            }
        }
        return content.toString();
    }
}
