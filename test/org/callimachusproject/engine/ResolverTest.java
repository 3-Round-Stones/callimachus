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
package org.callimachusproject.engine;

import java.net.URISyntaxException;

import junit.framework.TestCase;

import org.callimachusproject.engine.model.TermFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResolverTest extends TestCase {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRoundTripQueryString() throws Exception {
		assertRoundTrip("http://localhost:8080/callimachus/pipelines/render-html.xpl?result&template=http%3A%2F%2Flocalhost%3A8080%2Fcallimachus%2Fconcept-view.xhtml%3Ftemplate%26realm%3Dhttp%3A%2F%2Flocalhost%3A8080%2F&this=http%3A%2F%2Flocalhost%3A8080%2Fsun&query=view");
	}

	private void assertRoundTrip(String uri) throws URISyntaxException {
		assertResolves(uri, "http://example.com/", uri);
	}

	@Test
	public void testParentFile() throws URISyntaxException {
		assertResolves("../dir", "http://example.com/dir/dir/file", "http://example.com/dir/dir");
	}

	@Test
	public void testRootFile() throws URISyntaxException {
		assertResolves("/dir", "http://example.com/dir/dir", "http://example.com/dir");
	}

	@Test
	public void testFrag() throws URISyntaxException {
		assertResolves("#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/file?qs#frag");
	}

	@Test
	public void testIdentity() throws URISyntaxException {
		assertResolves("", "http://example.com/dir/dir/file?qs", "http://example.com/dir/dir/file?qs");
	}

	@Test
	public void testOpaque() throws URISyntaxException {
		assertResolves("urn:test", "http://example.com/dir/dir/file?qs#frag", "urn:test");
	}

	@Test
	public void testFragment() throws URISyntaxException {
		assertResolves("#frag2", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/file?qs#frag2");
	}

	@Test
	public void testQueryString() throws URISyntaxException {
		assertResolves("?qs2#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/file?qs2#frag");
	}

	@Test
	public void testDirectory() throws URISyntaxException {
		assertResolves(".", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/");
	}

	@Test
	public void testSameDirectory() throws URISyntaxException {
		assertResolves("file2?qs#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/file2?qs#frag");
	}

	@Test
	public void testNestedDirectory() throws URISyntaxException {
		assertResolves("nested/file?qs#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/nested/file?qs#frag");
	}

	@Test
	public void testParentDirectory() throws URISyntaxException {
		assertResolves("../file?qs#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/file?qs#frag");
	}

	@Test
	public void testOtherDirectory() throws URISyntaxException {
		assertResolves("../dir2/file?qs#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir2/file?qs#frag");
	}

	@Test
	public void testSameAuthority() throws URISyntaxException {
		assertResolves("/dir2/dir/file?qs#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir2/dir/file?qs#frag");
	}

	@Test
	public void testIdentityDir() throws URISyntaxException {
		assertResolves("", "http://example.com/dir/dir/", "http://example.com/dir/dir/");
	}

	@Test
	public void testOpaqueDir() throws URISyntaxException {
		assertResolves("urn:test", "http://example.com/dir/dir/", "urn:test");
	}

	@Test
	public void testFragmentDir() throws URISyntaxException {
		assertResolves("#frag2", "http://example.com/dir/dir/", "http://example.com/dir/dir/#frag2");
	}

	@Test
	public void testQueryStringDir() throws URISyntaxException {
		assertResolves("?qs2", "http://example.com/dir/dir/", "http://example.com/dir/dir/?qs2");
	}

	@Test
	public void testDirectoryDir() throws URISyntaxException {
		assertResolves("file", "http://example.com/dir/dir/", "http://example.com/dir/dir/file");
	}

	@Test
	public void testSameDirectoryDir() throws URISyntaxException {
		assertResolves("file2?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir/dir/file2?qs#frag");
	}

	@Test
	public void testNestedDirectoryDir() throws URISyntaxException {
		assertResolves("nested/", "http://example.com/dir/dir/", "http://example.com/dir/dir/nested/");
	}

	@Test
	public void testNestedDirectoryFileDir() throws URISyntaxException {
		assertResolves("nested/file?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir/dir/nested/file?qs#frag");
	}

	@Test
	public void testParentDirectoryDir() throws URISyntaxException {
		assertResolves("../file?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir/file?qs#frag");
	}

	@Test
	public void testOtherDirectoryDir() throws URISyntaxException {
		assertResolves("../dir2/", "http://example.com/dir/dir/", "http://example.com/dir/dir2/");
	}

	@Test
	public void testOtherDirectoryFileDir() throws URISyntaxException {
		assertResolves("../dir2/file?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir/dir2/file?qs#frag");
	}

	@Test
	public void testSameAuthorityDir() throws URISyntaxException {
		assertResolves("/dir2/dir/file?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir2/dir/file?qs#frag");
	}

	private void assertResolves(String relative, String base, String absolute) throws URISyntaxException {
		assertEquals(absolute, TermFactory.newInstance(base).resolve(relative));
	}

}
