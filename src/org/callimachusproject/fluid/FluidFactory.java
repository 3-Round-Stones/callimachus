/*
   Copyright (c) 2012 3 Round Stones Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.fluid;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;
import javax.xml.transform.TransformerConfigurationException;

import org.callimachusproject.fluid.consumers.BooleanMessageWriter;
import org.callimachusproject.fluid.consumers.BufferedImageWriter;
import org.callimachusproject.fluid.consumers.ByteArrayMessageWriter;
import org.callimachusproject.fluid.consumers.ByteArrayStreamMessageWriter;
import org.callimachusproject.fluid.consumers.DOMMessageWriter;
import org.callimachusproject.fluid.consumers.DatatypeWriter;
import org.callimachusproject.fluid.consumers.DocumentFragmentMessageWriter;
import org.callimachusproject.fluid.consumers.FormMapMessageWriter;
import org.callimachusproject.fluid.consumers.FormStringMessageWriter;
import org.callimachusproject.fluid.consumers.GraphMessageWriter;
import org.callimachusproject.fluid.consumers.HttpEntityWriter;
import org.callimachusproject.fluid.consumers.HttpJavaScriptResponseWriter;
import org.callimachusproject.fluid.consumers.HttpMessageWriter;
import org.callimachusproject.fluid.consumers.HttpResponseWriter;
import org.callimachusproject.fluid.consumers.InputStreamBodyWriter;
import org.callimachusproject.fluid.consumers.ModelMessageWriter;
import org.callimachusproject.fluid.consumers.PrimitiveBodyWriter;
import org.callimachusproject.fluid.consumers.RDFObjectURIWriter;
import org.callimachusproject.fluid.consumers.ReadableBodyWriter;
import org.callimachusproject.fluid.consumers.ReadableByteChannelBodyWriter;
import org.callimachusproject.fluid.consumers.StringBodyWriter;
import org.callimachusproject.fluid.consumers.TupleMessageWriter;
import org.callimachusproject.fluid.consumers.URIListWriter;
import org.callimachusproject.fluid.consumers.VoidWriter;
import org.callimachusproject.fluid.consumers.XMLEventMessageWriter;
import org.callimachusproject.fluid.producers.BooleanMessageReader;
import org.callimachusproject.fluid.producers.BufferedImageReader;
import org.callimachusproject.fluid.producers.ByteArrayMessageReader;
import org.callimachusproject.fluid.producers.ByteArrayStreamMessageReader;
import org.callimachusproject.fluid.producers.DOMMessageReader;
import org.callimachusproject.fluid.producers.DatatypeReader;
import org.callimachusproject.fluid.producers.DocumentFragmentMessageReader;
import org.callimachusproject.fluid.producers.FormMapMessageReader;
import org.callimachusproject.fluid.producers.FormStringMessageReader;
import org.callimachusproject.fluid.producers.GraphMessageReader;
import org.callimachusproject.fluid.producers.HttpEntityReader;
import org.callimachusproject.fluid.producers.HttpMessageReader;
import org.callimachusproject.fluid.producers.InputStreamBodyReader;
import org.callimachusproject.fluid.producers.ModelMessageReader;
import org.callimachusproject.fluid.producers.PrimitiveBodyReader;
import org.callimachusproject.fluid.producers.RDFObjectURIReader;
import org.callimachusproject.fluid.producers.ReadableBodyReader;
import org.callimachusproject.fluid.producers.ReadableByteChannelBodyReader;
import org.callimachusproject.fluid.producers.StringBodyReader;
import org.callimachusproject.fluid.producers.TupleMessageReader;
import org.callimachusproject.fluid.producers.VoidReader;
import org.callimachusproject.fluid.producers.XMLEventMessageReader;
import org.callimachusproject.fluid.producers.base.URIListReader;
import org.openrdf.repository.object.ObjectConnection;

/**
 * Creates {@link FluidBuilder} to convert between media types.
 * 
 * @author James Leigh
 * 
 */
public class FluidFactory {
	private static final FluidFactory instance = new FluidFactory();
	static {
		instance.init();
	}

	public static FluidFactory getInstance() {
		return instance;
	}

	private List<Consumer<?>> consumers = new ArrayList<Consumer<?>>();
	private List<Producer> producers = new ArrayList<Producer>();

	private void init() {
		consumers.add(new RDFObjectURIWriter());
		consumers.add(new BooleanMessageWriter());
		consumers.add(new ModelMessageWriter());
		consumers.add(new GraphMessageWriter());
		consumers.add(new TupleMessageWriter());
		consumers.add(new DatatypeWriter());
		consumers.add(new StringBodyWriter());
		consumers.add(new VoidWriter());
		consumers.add(new PrimitiveBodyWriter());
		consumers.add(new HttpMessageWriter());
		consumers.add(new HttpResponseWriter());
		consumers.add(new InputStreamBodyWriter());
		consumers.add(new ReadableBodyWriter());
		consumers.add(new ReadableByteChannelBodyWriter());
		consumers.add(new XMLEventMessageWriter());
		consumers.add(new ByteArrayMessageWriter());
		consumers.add(new ByteArrayStreamMessageWriter());
		consumers.add(new FormMapMessageWriter());
		consumers.add(new FormStringMessageWriter());
		consumers.add(new HttpEntityWriter());
		consumers.add(new BufferedImageWriter());
		consumers.add(URIListWriter.RDF_URI);
		consumers.add(URIListWriter.NET_URL);
		consumers.add(URIListWriter.NET_URI);
		try {
			consumers.add(new DocumentFragmentMessageWriter());
			consumers.add(new DOMMessageWriter());
			consumers.add(new HttpJavaScriptResponseWriter());
		} catch (TransformerConfigurationException e) {
			throw new AssertionError(e);
		} catch (ScriptException e) {
			throw new AssertionError(e);
		}
		producers.add(URIListReader.RDF_URI);
		producers.add(URIListReader.NET_URL);
		producers.add(URIListReader.NET_URI);
		producers.add(new RDFObjectURIReader());
		producers.add(new ModelMessageReader());
		producers.add(new GraphMessageReader());
		producers.add(new TupleMessageReader());
		producers.add(new BooleanMessageReader());
		producers.add(new DatatypeReader());
		producers.add(new StringBodyReader());
		producers.add(new VoidReader());
		producers.add(new PrimitiveBodyReader());
		producers.add(new FormMapMessageReader());
		producers.add(new FormStringMessageReader());
		producers.add(new HttpMessageReader());
		producers.add(new InputStreamBodyReader());
		producers.add(new ReadableBodyReader());
		producers.add(new ReadableByteChannelBodyReader());
		producers.add(new XMLEventMessageReader());
		producers.add(new ByteArrayMessageReader());
		producers.add(new ByteArrayStreamMessageReader());
		producers.add(new DOMMessageReader());
		producers.add(new DocumentFragmentMessageReader());
		producers.add(new HttpEntityReader());
		producers.add(new BufferedImageReader());
	}

	public FluidBuilder builder() {
		return new FluidBuilder(consumers, producers);
	}

	public FluidBuilder builder(ObjectConnection con) {
		return new FluidBuilder(consumers, producers, con);
	}

}
