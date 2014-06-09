/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.behaviours;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.tools.FileObject;

import net.sf.saxon.s9api.SaxonApiException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.xml.XslTransformEngine;
import org.callimachusproject.xml.XslTransformer;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

public abstract class XslTransformSupport implements CalliObject, FileObject {
	private static final BasicStatusLine OK = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 200, "OK");

	public HttpResponse GetResult(Map<String, String[]> parameters,
			String templateName) throws SaxonApiException, OpenRDFException,
			IOException, SAXException {
		XslTransformEngine xsl = new XslTransformEngine(this.toString(),
				this.getHttpClient());
		XslTransformer xslt = xsl.compile(this.openInputStream());
		BasicHttpEntity entity = new BasicHttpEntity();
		String mediaType = xslt.getMediaType();
		String encoding = xslt.getEncoding();
		entity.setContentEncoding(encoding);
		if (mediaType.startsWith("text/")) {
			entity.setContentType(mediaType + ";charset=" + encoding);
		} else {
			entity.setContentType(mediaType);
		}
		BasicHttpResponse response = new BasicHttpResponse(OK);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		xslt.transformToStream(templateName, parameters, out);
		entity.setContent(new ByteArrayInputStream(out.toByteArray()));
		response.setEntity(entity);
		return response;
	}

	public HttpResponse PostResult(Map<String, String[]> parameters,
			String documentId, InputStream documentStream)
			throws SaxonApiException, OpenRDFException, IOException,
			SAXException {
		XslTransformEngine xsl = new XslTransformEngine(this.toString(),
				this.getHttpClient());
		XslTransformer xslt = xsl.compile(this.openInputStream());
		BasicHttpEntity entity = new BasicHttpEntity();
		String mediaType = xslt.getMediaType();
		String encoding = xslt.getEncoding();
		entity.setContentEncoding(encoding);
		if (mediaType.startsWith("text/")) {
			entity.setContentType(mediaType + ";charset=" + encoding);
		} else {
			entity.setContentType(mediaType);
		}
		BasicHttpResponse response = new BasicHttpResponse(OK);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		xslt.transformToStream(documentStream, documentId, parameters, out);
		entity.setContent(new ByteArrayInputStream(out.toByteArray()));
		response.setEntity(entity);
		return response;
	}
}
