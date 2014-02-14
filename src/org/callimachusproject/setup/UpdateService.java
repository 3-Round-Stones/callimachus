/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
		throw new AssertionError("Cannot determine default Callimachus webapp folder");
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
