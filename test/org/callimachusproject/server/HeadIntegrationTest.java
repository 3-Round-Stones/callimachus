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
package org.callimachusproject.server;

import java.net.HttpURLConnection;
import java.net.URL;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;

public class HeadIntegrationTest extends TemporaryServerIntegrationTestCase {

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
