package org.callimachusproject.server.model;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Closes the previous entity if replaced with null.
 * 
 * @author James Leigh
 *
 */
public class EntityRemovedHttpResponse extends BasicHttpResponse {
	private Logger logger = LoggerFactory.getLogger(EntityRemovedHttpResponse.class);

	public EntityRemovedHttpResponse(ProtocolVersion ver, int code, String reason) {
		super(ver, code, reason);
	}

	@Override
	public void setEntity(HttpEntity entity) {
		HttpEntity previously = getEntity();
		if (entity == null && previously != null) {
			try {
				EntityUtils.consume(previously);
			} catch (IOException e) {
				logger.warn(e.toString(), e);
			}
		}
		super.setEntity(entity);
	}

}
