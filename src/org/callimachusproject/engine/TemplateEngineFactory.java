package org.callimachusproject.engine;

import java.lang.ref.Reference;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openrdf.repository.RepositoryConnection;
import org.callimachusproject.xslt.XSLTransformer;

public class TemplateEngineFactory {
	private static final int MAX_XSLT = 16;
	private final Map<String, Reference<XSLTransformer>> transformers = new LinkedHashMap<String, Reference<XSLTransformer>>(
			16, 0.75f, true) {
		private static final long serialVersionUID = 1362917757653811798L;

		protected boolean removeEldestEntry(
				Map.Entry<String, Reference<XSLTransformer>> eldest) {
			return size() > MAX_XSLT;
		}
	};

	public static TemplateEngineFactory newInstance() {
		return new TemplateEngineFactory();
	}

	public TemplateEngine createTemplateEngine(RepositoryConnection con) {
		return new TemplateEngine(con, transformers);
	}

	private TemplateEngineFactory() {
		super();
	}

}
