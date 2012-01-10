/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.http.object.behaviours;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.http.object.client.RemoteConnection;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.exceptions.BehaviourException;

/**
 * Handles write operations on a set when loaded remotely.
 */
public class RemoteSetSupport implements Set {
	private String uri;
	private String qs;
	private String media;
	private Type gtype;
	private Set values;
	private ObjectConnection oc;
	private InetSocketAddress addr;

	public RemoteSetSupport(InetSocketAddress addr, String uri, String qs,
			String media, Type gtype, Set values, ObjectConnection oc) {
		this.addr = addr;
		this.uri = uri;
		this.qs = qs;
		this.media = media;
		this.gtype = gtype;
		this.values = values;
		this.oc = oc;
	}

	public boolean add(Object e) {
		boolean changed = values.add(e);
		if (changed) {
			store(values);
		}
		return changed;
	}

	public boolean addAll(Collection c) {
		boolean changed = values.addAll(c);
		if (changed) {
			store(values);
		}
		return changed;
	}

	public boolean remove(Object o) {
		boolean changed = values.remove(o);
		if (changed) {
			store(values);
		}
		return changed;
	}

	public boolean removeAll(Collection c) {
		boolean changed = values.removeAll(c);
		if (changed) {
			store(values);
		}
		return changed;
	}

	public boolean retainAll(Collection c) {
		boolean changed = values.retainAll(c);
		if (changed) {
			store(values);
		}
		return changed;
	}

	public void clear() {
		try {
			RemoteConnection con = openConnection("DELETE");
			try {
				int status = con.getResponseCode();
				if (status >= 300) {
					throw ResponseException.create(con.getHttpResponse(), con.toString());
				}
			} finally {
				con.close();
			}
			values.clear();
		} catch (IOException e) {
			throw new BehaviourException(e);
		}
	}

	public Iterator iterator() {
		final Iterator delegate = values.iterator();
		return new Iterator() {

			public boolean hasNext() {
				return delegate.hasNext();
			}

			public Object next() {
				return delegate.next();
			}

			public void remove() {
				delegate.remove();
				store(values);
			}
		};
	}

	public boolean contains(Object o) {
		return values.contains(o);
	}

	public boolean containsAll(Collection c) {
		return values.containsAll(c);
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public int size() {
		return values.size();
	}

	public Object[] toArray() {
		return values.toArray();
	}

	public Object[] toArray(Object[] a) {
		return values.toArray(a);
	}

	@Override
	public boolean equals(Object obj) {
		return values.equals(obj);
	}

	@Override
	public int hashCode() {
		return values.hashCode();
	}

	@Override
	public String toString() {
		return values.toString();
	}

	private void store(Set values) {
		try {
			RemoteConnection con = openConnection("PUT");
			try {
				con.write(media, Set.class, gtype, values);
				int status = con.getResponseCode();
				if (status >= 300) {
					throw ResponseException.create(con.getHttpResponse(), con.toString());
				}
			} finally {
				con.close();
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new BehaviourException(e);
		}
	}

	private RemoteConnection openConnection(String method) throws IOException {
		return new RemoteConnection(addr, method, uri, qs, oc);
	}

}
