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

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

/**
 * When a {@link Fluid} does not support a desired Java or media type, it is
 * converted into a {@link ReadableByteChannel} and parsed to the desired Java
 * type.
 * 
 * @author James Leigh
 * 
 */
class ChannelFluid extends AbstractFluid {
	private final Fluid fluid;
	private final FluidBuilder builder;

	public ChannelFluid(Fluid fluid, FluidBuilder builder) {
		assert fluid != null;
		assert builder != null;
		this.fluid = fluid;
		this.builder = builder;
	}

	public FluidType getFluidType() {
		return fluid.getFluidType();
	}

	public String getSystemId() {
		return fluid.getSystemId();
	}

	public String toString() {
		return fluid.toString();
	}

	public void asVoid() throws TransformerConfigurationException,
			OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException {
		fluid.asVoid();
	}

	@Override
	public String toMedia(FluidType ftype) {
		String ret = fluid.toMedia(ftype);
		if (ret != null)
			return ret;
		String[] cmt = getChannelMedia(ftype.media());
		return builder.media(cmt).toMedia(ftype);
	}

	@Override
	public Object as(FluidType ftype) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		try {
			if (fluid.toMedia(ftype) != null)
				return fluid.as(ftype);
			String[] cmt = getChannelMedia(ftype.media());
			ReadableByteChannel in = fluid.asChannel(cmt);
			return builder.channel(in, fluid.getSystemId(), cmt).as(ftype);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		} catch (OpenRDFException e) {
			throw handle(e);
		} catch (XMLStreamException e) {
			throw handle(e);
		} catch (ParserConfigurationException e) {
			throw handle(e);
		} catch (SAXException e) {
			throw handle(e);
		} catch (TransformerConfigurationException e) {
			throw handle(e);
		} catch (IOException e) {
			throw handle(e);
		} catch (TransformerException e) {
			throw handle(e);
		}
	}

	private String[] getChannelMedia(String[] media) {
		if (getFluidType().is(media))
			return getFluidType().as(media).media();
		return getFluidType().media();
	}

	protected <E extends Throwable> E handle(E cause) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		try {
			asVoid();
			return cause;
		} catch (RuntimeException e) {
			e.initCause(cause);
			throw e;
		} catch (Error e) {
			e.initCause(cause);
			throw e;
		} catch (OpenRDFException e) {
			e.initCause(cause);
			throw e;
		} catch (XMLStreamException e) {
			e.initCause(cause);
			throw e;
		} catch (ParserConfigurationException e) {
			e.initCause(cause);
			throw e;
		} catch (SAXException e) {
			e.initCause(cause);
			throw e;
		} catch (TransformerConfigurationException e) {
			e.initCause(cause);
			throw e;
		} catch (IOException e) {
			e.initCause(cause);
			throw e;
		} catch (TransformerException e) {
			e.initCause(cause);
			throw e;
		}
	}

}
