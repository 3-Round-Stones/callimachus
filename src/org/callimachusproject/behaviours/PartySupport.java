package org.callimachusproject.behaviours;

import org.callimachusproject.concepts.Party;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.repository.RepositoryException;

public abstract class PartySupport implements Party, CalliObject {

	public void resetCache() throws RepositoryException {
		this.getObjectConnection().commit();
		getCalliRepository().resetCache();
	}

}
