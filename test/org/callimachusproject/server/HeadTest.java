package org.callimachusproject.server;

import java.net.HttpURLConnection;
import java.net.URL;

import org.callimachusproject.test.TemporaryServerTestCase;

public class HeadTest extends TemporaryServerTestCase {

	public void testHeadRoot() throws Exception {
		URL url = new java.net.URL(getHomeFolder().toString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("HEAD");
		assertEquals(con.getResponseMessage(), 303, con.getResponseCode());
	}

	public void testHeadRootCompress() throws Exception {
		URL url = new java.net.URL(getHomeFolder().toString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("HEAD");
		con.setRequestProperty("Accept-Encoding", "gzip");
		assertEquals(con.getResponseMessage(), 303, con.getResponseCode());
	}

	public void testHeadEdit() throws Exception {
		URL url = new java.net.URL(getHomeFolder().ref("?edit").toString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("HEAD");
		con.setRequestProperty("Accept-Encoding", "gzip");
		assertEquals(con.getResponseMessage(), 200, con.getResponseCode());
	}
}
