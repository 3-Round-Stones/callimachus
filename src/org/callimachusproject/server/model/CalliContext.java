package org.callimachusproject.server.model;

import java.net.InetAddress;

import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

public class CalliContext extends HttpCoreContext {

    private static final String NS = CalliContext.class.getName() + "#";
    private static final String INTERNAL_ATTR = NS + "internal";
    private static final String RECEIVED_ATTR = NS + "receivedOn";
    private static final String CLIENT_ATTR = NS + "clientAddr";
    private static final String TRANSACTION_ATTR = NS + "resourceTransaction";

    public static CalliContext adapt(final HttpContext context) {
        if (context instanceof CalliContext) {
            return (CalliContext) context;
        } else {
            return new CalliContext(context);
        }
    }

    public static CalliContext create() {
        return new CalliContext(new BasicHttpContext());
    }

    public CalliContext(final HttpContext context) {
        super(context);
    }

    public CalliContext() {
        super();
    }

	public boolean isInternal() {
		Boolean ret = getAttribute(INTERNAL_ATTR, Boolean.class);
		return ret != null && ret;
	}

	public void setInternal(boolean internal) {
		setAttribute(INTERNAL_ATTR, internal);
	}

	public long getReceivedOn() {
		Long ret = getAttribute(RECEIVED_ATTR, Long.class);
		return ret == null ? 0 : ret;
	}

	public void setReceivedOn(long received) {
		setAttribute(RECEIVED_ATTR, received);
	}

	public InetAddress getClientAddr() {
		return getAttribute(CLIENT_ATTR, InetAddress.class);
	}

	public void setClientAddr(InetAddress addr) {
		setAttribute(CLIENT_ATTR, addr);
	}

	public ResourceOperation getResourceTransaction() {
		return getAttribute(TRANSACTION_ATTR, ResourceOperation.class);
	}

	public void setResourceTransaction(ResourceOperation trans) {
		setAttribute(TRANSACTION_ATTR, trans);
	}
}
