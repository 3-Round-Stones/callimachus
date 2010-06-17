package org.callimachusproject.behaviours;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.sparql;
import org.openrdf.repository.object.annotations.triggeredBy;

public abstract class UniqueCredentialNameTrigger implements RDFObject {
	private final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";

	@triggeredBy("http://callimachusproject.org/rdf/2009/framework#credential")
	public void checkCredentialNames() {
		if (isDuplicateCredentialName())
			throw new IllegalStateException("Username Already Exists");
	}

	@sparql(PREFIX
			+ "ASK { $this calli:credential ?credential1; calli:credential ?credential2 .\n"
			+ "?credential1 calli:name ?name . ?credential2 calli:name ?name\n"
			+ "FILTER (?credential1 != ?credential2) }")
	protected abstract boolean isDuplicateCredentialName();
}
