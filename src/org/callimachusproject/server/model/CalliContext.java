/*
 * Copyright 2013, 3 Round Stones Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
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
package org.callimachusproject.server.model;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.process.Exchange;

public class CalliContext implements HttpContext {

    private static final String NS = CalliContext.class.getName() + "#";
    private static final String RECEIVED_ATTR = NS + "receivedOn";
    private static final String CLIENT_ATTR = NS + "clientAddr";
    private static final String TRANSACTION_ATTR = NS + "resourceTransaction";
	private static final String EXCHANGE_ATTR = NS + "#exchange";
	private static final String PROCESSING_ATTR = NS + "#processing";

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

    private final HttpContext context;

    public CalliContext(final HttpContext context) {
        this.context = context;
    }

    public CalliContext() {
        this.context = new BasicHttpContext();
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
		assert trans != null;
		setAttribute(TRANSACTION_ATTR, trans);
	}

	public synchronized void removeResourceTransaction(ResourceOperation trans) {
		Object removed = removeAttribute(TRANSACTION_ATTR);
		assert removed == trans;
	}

	public Exchange getExchange() {
		return getAttribute(EXCHANGE_ATTR, Exchange.class);
	}

	public void setExchange(Exchange exchange) {
		if (exchange == null) {
			removeAttribute(EXCHANGE_ATTR);
		} else {
			setAttribute(EXCHANGE_ATTR, exchange);
		}
	}

	public Exchange[] getPendingExchange() {
		Queue<Exchange> queue = (Queue<Exchange>) getAttribute(PROCESSING_ATTR);
		if (queue == null)
			return null;
		synchronized (queue) {
			return queue.toArray(new Exchange[queue.size()]);
		}
	}

	public Queue<Exchange> getOrCreateProcessingQueue() {
		Queue<Exchange> queue = (Queue<Exchange>) getAttribute(PROCESSING_ATTR);
		if (queue == null) {
			setAttribute(PROCESSING_ATTR, queue = new LinkedList<Exchange>());
		}
		return queue;
	}

    @Override
	public Object getAttribute(String id) {
		return context.getAttribute(id);
	}

	@Override
	public void setAttribute(String id, Object obj) {
		context.setAttribute(id, obj);
	}

	@Override
	public Object removeAttribute(String id) {
		return context.removeAttribute(id);
	}

	private <T> T getAttribute(final String attribname, final Class<T> clazz) {
        final Object obj = getAttribute(attribname);
        if (obj == null) {
            return null;
        }
        return clazz.cast(obj);
    }
}
