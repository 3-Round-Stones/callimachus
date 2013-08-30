package org.callimachusproject.io;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URISyntaxException;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.turtle.TurtleParserFactory;

public class TurtleStreamWriterTest extends RDFWriterTest {

	public TurtleStreamWriterTest() {
		super(new RDFWriterFactory() {
			public RDFFormat getRDFFormat() {
				return RDFFormat.TURTLE;
			}

			public RDFWriter getWriter(OutputStream out) {
				try {
					return new TurtleStreamWriterFactory().createWriter(out,
							"http://example.org/");
				} catch (URISyntaxException e) {
					throw new UndeclaredThrowableException(e);
				}
			}

			public RDFWriter getWriter(Writer writer) {
				try {
					return new TurtleStreamWriterFactory().createWriter(writer,
							"http://example.org/");
				} catch (URISyntaxException e) {
					throw new UndeclaredThrowableException(e);
				}
			}
		}, new TurtleParserFactory());
	}

}
