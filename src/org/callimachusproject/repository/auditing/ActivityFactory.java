/*
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.repository.auditing;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * Assigns the activity URI and is notified of when an activity starts and ends.
 * 
 * @author James Leigh
 * 
 */
public interface ActivityFactory {

	/**
	 * Assigns an activity URI to the given insert context. If the given insert
	 * context is null, the resulting URI upto, but not including, the first '#'
	 * character will be used as the default insert context.
	 * 
	 * @param bundle
	 *            the default insert context (a prov:Bundle)
	 * @param vf
	 * @return the prov:Activity URI for this insert context or null if no
	 *         activity
	 */
	URI createActivityURI(URI bundle, ValueFactory vf);

	/**
	 * Indicates that the given activity has started. Implementations may choose
	 * to assign the prov:startedAtTime to the activity within the given bundle.
	 * 
	 * @param activity
	 *            the prov:Activity URI created from
	 *            {@link #createActivityURI(URI, ValueFactory)}.
	 * @param bundle
	 *            the defalut insert context or the activity URI upto, but not
	 *            including, the first '#'.
	 * @param con
	 * @throws RepositoryException
	 */
	void activityStarted(URI activity, URI bundle, RepositoryConnection con)
			throws RepositoryException;

	/**
	 * Indicates that the given activity has ended. Implementations should
	 * assign the prov:endedAtTime to the activity within the given bundle.
	 * 
	 * @param activity
	 *            the prov:Activity URI created from
	 *            {@link #createActivityURI(URI, ValueFactory)}.
	 * @param bundle
	 *            the defalut insert context or the activity URI upto, but not
	 *            including, the first '#'.
	 * @param con
	 * @throws RepositoryException
	 */
	void activityEnded(URI activity, URI bundle, RepositoryConnection con)
			throws RepositoryException;
}
