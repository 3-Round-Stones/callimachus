/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.callimachusproject.server.handlers;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.callimachusproject.server.model.BodyEntity;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.Response;
import org.callimachusproject.server.model.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a Java Method on the request target and response with the result.
 * 
 * @author James Leigh
 * 
 */
public class InvokeHandler implements Handler {
	private Logger logger = LoggerFactory.getLogger(InvokeHandler.class);

	public Response verify(ResourceOperation request) throws Exception {
		Method method = request.getJavaMethod();
		assert method != null;
		return null;
	}

	public Response handle(ResourceOperation request) throws Exception {
		Method method = request.getJavaMethod();
		assert method != null;
		return invoke(request, method, request.isSafe());
	}

	private Response invoke(ResourceOperation req, Method method, boolean safe)
			throws Exception {
		BodyEntity body = req.getBody();
		try {
			Object[] args;
			try {
				args = req.getParameters(method, body);
			} catch (ParserConfigurationException e) {
				throw e;
			} catch (TransformerConfigurationException e) {
				throw e;
			} catch (Exception e) {
				return new Response().badRequest(e);
			}
			try {
				ResponseEntity entity = req.invoke(method, args, true);
				if (!safe) {
					req.flush();
				}
				return createResponse(req, method, entity);
			} finally {
				for (Object arg : args) {
					if (arg instanceof Closeable) {
						((Closeable) arg).close();
					}
				}
			}
		} catch (InvocationTargetException e) {
			try {
				throw e.getCause();
			} catch (Error cause) {
				throw cause;
			} catch (Exception cause) {
				throw cause;
			} catch (Throwable cause) {
				throw e;
			}
		} finally {
			body.close();
		}
	}

	private Response createResponse(ResourceOperation req, Method method,
			ResponseEntity entity) throws Exception {
		Response rb = new Response();
		if (entity.isNoContent()) {
			rb = rb.noContent();
		}
		for (Map.Entry<String, String> e : entity.getOtherHeaders().entrySet()) {
			rb.header(e.getKey(), e.getValue());
		}
		for (String expect : entity.getExpects()) {
			String[] values = expect.split("[\\s\\-]+");
			try {
				StringBuilder sb = new StringBuilder();
				for (int i = 1; i < values.length; i++) {
					sb.append(values[i].substring(0, 1).toUpperCase());
					sb.append(values[i].substring(1));
					if (i < values.length - 1) {
						sb.append(" ");
					}
				}
				if (sb.length() > 1) {
					int code = Integer.parseInt(values[0]);
					String phrase = sb.toString();
					if (code >= 300 && code <= 303 || code == 307
							|| code == 201) {
						Set<String> locations = entity.getLocations();
						if (locations != null && !locations.isEmpty()) {
							rb = rb.status(code, phrase);
							for (String location : locations) {
								rb.header("Location", location);
							}
							break;
						}
					} else if (code == 204 || code == 205) {
						if (entity.isNoContent()) {
							rb = rb.status(code, phrase);
							break;
						}
					} else if (code >= 300 && code <= 399) {
						rb = rb.status(code, phrase);
						Set<String> locations = entity.getLocations();
						if (locations != null && !locations.isEmpty()) {
							for (String location : locations) {
								rb = rb.header("Location", location);
							}
						}
						break;
					} else {
						rb = rb.status(code, phrase);
					}
				}
			} catch (NumberFormatException e) {
				logger.error(expect, e);
			} catch (IndexOutOfBoundsException e) {
				logger.error(expect, e);
			}
		}
		if (entity.isNoContent())
			return rb;
		return rb.entity(entity);
	}

}
