package org.callimachusproject.setup;

import java.io.IOException;

import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;

public class SuperSystemProvider extends UpdateProvider {
	private static final String SUPER_GROUP = "/auth/groups/super";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_MEMBERSFROM = CALLI + "membersFrom";
	private final DomainNameSystemResolver dnsResolver = DomainNameSystemResolver
			.getInstance();

	@Override
	public Updater updateCallimachusWebapp(final String origin) throws IOException {
		return new Updater() {
			public boolean update(String webapp,
					CalliRepository repository) throws IOException,
					OpenRDFException {
				String group = origin + SUPER_GROUP;
				ObjectConnection con = repository.getConnection();
				try {
					con.setAutoCommit(false);
					ValueFactory vf = con.getValueFactory();
					URI subj = vf.createURI(group);
					URI pred = vf.createURI(CALLI_MEMBERSFROM);
					boolean modified = false;
					for (String host : dnsResolver.reverseAllLocalHosts()) {
						Literal obj = vf.createLiteral(host);
						if (!con.hasStatement(subj, pred, obj)) {
							con.add(subj, pred, obj);
							modified = true;
						}
					}
					con.setAutoCommit(true);
					return modified;
				} finally {
					con.close();
				}
			}
		};
	}

}
