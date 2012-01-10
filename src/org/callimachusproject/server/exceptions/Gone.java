/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.callimachusproject.server.exceptions;


/**
 * The requested resource is no longer available at the server and no forwarding
 * address is known. This condition is expected to be considered permanent.
 * Clients with link editing capabilities SHOULD delete references to the
 * request-target after user approval. If the server does not know, or has no
 * facility to determine, whether or not the condition is permanent, the status
 * code 404 (Not Found) SHOULD be used instead. This response is cacheable
 * unless indicated otherwise.
 */
public class Gone extends ResponseException {
	private static final long serialVersionUID = 3422241245426476225L;

	public Gone() {
		super("Gone");
	}

	public Gone(String message) {
		super(message);
	}

	public Gone(String message, Throwable cause) {
		super(message, cause);
	}

	public Gone(Throwable cause) {
		super(cause);
	}

	public Gone(String message, String stack) {
		super(message, stack);
	}

	@Override
	public int getStatusCode() {
		return 410;
	}

	@Override
	public boolean isCommon() {
		return true;
	}

}
