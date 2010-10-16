package org.callimachusproject.webapps;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectDatesetListener extends UploadListener {
	private Logger logger = LoggerFactory.getLogger(ObjectDatesetListener.class);
	private ObjectRepository repository;
	private ValueFactory vf;

	public ObjectDatesetListener(ObjectRepository repository) {
		this.repository = repository;
		vf = repository.getValueFactory();
	}

	public void notifyRemoving(String url) {
		try {
			repository.removeSchemaDataset(vf.createURI(url));
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
		}
	}

	public void notifyUploading(String url, String type) {
		int idx = type.indexOf(';');
		if (idx > 0) {
			type = type.substring(0, idx);
		}
		if (RDFFormat.forMIMEType(type) != null) {
			try {
				repository.addSchemaDataset(vf.createURI(url));
			} catch (RepositoryException e) {
				logger.error(e.toString(), e);
			}
		}
	}

}
