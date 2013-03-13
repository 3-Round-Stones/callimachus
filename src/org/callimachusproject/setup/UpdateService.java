package org.callimachusproject.setup;

import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;

public class UpdateService extends UpdateProvider {
	private final ServiceLoader<UpdateProvider> updateProviders;

	public UpdateService(ClassLoader cl) {
		this.updateProviders = ServiceLoader.load(UpdateProvider.class, cl);
	}

	@Override
	public String getDefaultWebappLocation(final String origin) throws IOException {
		Iterator<UpdateProvider> iter = updateProviders.iterator();
		while (iter.hasNext()) {
			String webapp = iter.next().getDefaultWebappLocation(origin);
			if (webapp != null)
				return webapp;
		}
		throw new AssertionError("Cannot determine Callimachus webapp folder");
	}

	@Override
	public Updater prepareCallimachusWebapp(final String origin) throws IOException {
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				boolean modified = false;
				Iterator<UpdateProvider> iter = updateProviders.iterator();
				while (iter.hasNext()) {
					Updater updater = iter.next().prepareCallimachusWebapp(
							origin);
					if (updater != null) {
						modified |= updater.update(webapp, repository);
					}
				}
				return modified;
			}
		};
	}

	@Override
	public Updater updateFrom(final String origin, final String version)
			throws IOException {
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				boolean modified = false;
				Iterator<UpdateProvider> iter = updateProviders.iterator();
				while (iter.hasNext()) {
					Updater updater = iter.next().updateFrom(origin, version);
					if (updater != null) {
						modified |= updater.update(webapp, repository);
					}
				}
				return modified;
			}
		};
	}

	@Override
	public Updater updateCallimachusWebapp(final String origin) throws IOException {
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				boolean modified = false;
				Iterator<UpdateProvider> iter = updateProviders.iterator();
				while (iter.hasNext()) {
					Updater updater = iter.next().updateCallimachusWebapp(
							origin);
					if (updater != null) {
						modified |= updater.update(webapp, repository);
					}
				}
				return modified;
			}
		};
	}

	@Override
	public Updater updateOrigin(final String virtual) throws IOException {
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				boolean modified = false;
				Iterator<UpdateProvider> iter = updateProviders.iterator();
				while (iter.hasNext()) {
					Updater updater = iter.next().updateOrigin(virtual);
					if (updater != null) {
						modified |= updater.update(webapp, repository);
					}
				}
				return modified;
			}
		};
	}

	@Override
	public Updater finalizeCallimachusWebapp(final String origin) throws IOException {
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				boolean modified = false;
				Iterator<UpdateProvider> iter = updateProviders.iterator();
				while (iter.hasNext()) {
					Updater updater = iter.next().finalizeCallimachusWebapp(
							origin);
					if (updater != null) {
						modified |= updater.update(webapp, repository);
					}
				}
				return modified;
			}
		};
	}

}
