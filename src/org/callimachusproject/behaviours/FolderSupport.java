package org.callimachusproject.behaviours;

import org.callimachusproject.engine.model.TermFactory;

public abstract class FolderSupport {

	/**
	 * Called from folder.ttl and origin.ttl
	 */
	public String resolve(String reference) {
		return TermFactory.newInstance(this.toString()).resolve(reference);
	}

}
