package org.callimachusproject.client;

import java.io.InputStream;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;

public class StreamEntity extends InputStreamEntity {

	public StreamEntity(InputStream instream,
			ContentType contentType) {
		super(instream, -1, contentType);
		setChunked(true);
	}

	public StreamEntity(InputStream instream) {
		super(instream, -1);
		setChunked(true);
	}

}
