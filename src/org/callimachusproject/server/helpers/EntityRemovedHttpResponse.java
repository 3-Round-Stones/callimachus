package org.callimachusproject.server.helpers;

import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;

/**
 * Closes the previous entity if replaced with null.
 * 
 * @author James Leigh
 *
 */
public class EntityRemovedHttpResponse extends BasicHttpResponse {

	public EntityRemovedHttpResponse(ProtocolVersion ver, int code, String reason) {
		super(ver, code, reason);
	}

	public EntityRemovedHttpResponse(StatusLine statusline) {
		super(statusline);
	}

	@Override
	public void setEntity(HttpEntity entity) {
		HttpEntity previously = getEntity();
		if (entity == null && previously != null) {
			EntityUtils.consumeQuietly(previously);
		}
		super.setEntity(entity);
	}

}
