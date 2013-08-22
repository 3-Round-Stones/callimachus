package org.callimachusproject.xproc;

import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;

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
import org.callimachusproject.xml.DocumentFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcStep;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.HttpUtils;

public class DecodeTextStep implements XProcStep {
	private static final String XPROC_STEP = XProcConstants.c_data.getNamespaceURI();
	private static final String DATA = XProcConstants.c_data.getLocalName();
    private static final QName _content_type = new QName("content-type");
    private static final QName _encoding = new QName("encoding");
    private static final QName _charset = new QName("charset");
    protected XProcRuntime runtime = null;
    protected XAtomicStep step = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;
	private String contentType = "text/plain";
	private String charset;
	private String encoding;

    /**
     * Creates a new instance
     */
    public DecodeTextStep(XProcRuntime runtime, XAtomicStep step) {
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
	        String text = extractText(source.read());
	        if ("base64".equals(encoding)) {
	            if (charset == null) {
	                throw XProcException.stepError(10);
	            }
	            byte[] decoded = Base64.decodeBase64(text);
	            text = new String(decoded, charset);
	        } else if ("base32".equals(encoding)) {
	            if (charset == null) {
	                throw XProcException.stepError(10);
	            }
	            byte[] decoded = new Base32().decode(text);
	            text = new String(decoded, charset);
	        } else if ("hex".equals(encoding)) {
	            if (charset == null) {
	                throw XProcException.stepError(10);
	            }
	            byte[] decoded = Hex.decodeHex(text.toCharArray());
	            text = new String(decoded, charset);
	        } else if ("binary".equals(encoding)) {
	            if (charset == null) {
	                throw XProcException.stepError(10);
	            }
	            byte[] decoded = BinaryCodec.fromAscii(text.toCharArray());
	            text = new String(decoded, charset);
	        } else if ("quoted-printable".equals(encoding)) {
	            if (charset == null) {
	                throw XProcException.stepError(10);
	            }
	            text = new QuotedPrintableCodec(charset).decode(text);
	        } else if ("www-form-urlencoded".equals(encoding)) {
	            if (charset == null) {
	                throw XProcException.stepError(10);
	            }
	            text = new URLCodec(charset).decode(text);
	        } else if (encoding != null && encoding.length() != 0) {
	            throw new XProcException(step.getNode(), "Unexpected encoding: " + encoding);
	        }

			Document doc = DocumentFactory.newInstance().newDocument();
			doc.setDocumentURI(doc.getBaseURI());
			Element data = doc.createElementNS(XPROC_STEP, DATA);
			data.setAttribute("content-type", contentType);
			data.appendChild(doc.createTextNode(text));
			doc.appendChild(data);
	        result.write(runtime.getProcessor().newDocumentBuilder().wrap(doc));
        } catch (ParserConfigurationException e) {
			throw new XProcException(step.getNode(), e);
        } catch (UnsupportedEncodingException uee) {
            throw XProcException.stepError(10, uee);
        } catch (DecoderException e) {
            throw new XProcException(step.getNode(), e);
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
