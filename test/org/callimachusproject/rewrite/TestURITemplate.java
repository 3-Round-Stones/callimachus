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
package org.callimachusproject.rewrite;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.callimachusproject.util.PercentCodec;
import org.openrdf.http.object.util.PathMatcher;

public class TestURITemplate extends TestCase {
	private static Map<String, ?> values = new LinkedHashMap<String, Object>() {
		{
			put("count", new String[] { "one", "two", "three" });
			put("dom", new String[] { "example", "com" });
			put("dub", "me/too");
			put("hello", "Hello World!");
			put("half", "50%");
			put("var", "value");
			put("who", "fred");
			put("base", "http://example.com/home/");
			put("path", "/foo/bar");
			put("list", new String[] { "red", "green", "blue" });
			put("keys", new LinkedHashMap<String, Object>() {
				{
					put("semi", ";");
					put("dot", ".");
					put("comma", ",");
				}
			});
			put("v", "6");
			put("x", "1024");
			put("y", "768");
			put("empty", "");
			put("empty_keys", Collections.emptyMap());
		}
	};

	private static Map<String, String> tests = new LinkedHashMap<String, String>() {
		{
			put("{count}", "one,two,three");
			put("{count*}", "one,two,three");
			put("{/count}", "/one,two,three");
			put("{/count*}", "/one/two/three");
			put("{;count}", ";count=one,two,three");
			put("{;count*}", ";count=one;count=two;count=three");
			put("{?count}", "?count=one,two,three");
			put("{?count*}", "?count=one&count=two&count=three");
			put("{&count*}", "&count=one&count=two&count=three");
			put("{var}", "value");
			put("{hello}", "Hello%20World%21");
			put("{half}", "50%25");
			put("O{empty}X", "OX");
			put("O{undef}X", "OX");
			put("{x,y}", "1024,768");
			put("{x,hello,y}", "1024,Hello%20World%21,768");
			put("?{x,empty}", "?1024,");
			put("?{x,undef}", "?1024");
			put("?{undef,y}", "?768");
			put("{var:3}", "val");
			put("{var:30}", "value");
			put("{list}", "red,green,blue");
			put("{list*}", "red,green,blue");
			put("{keys}", "semi,%3B,dot,.,comma,%2C");
			put("{keys*}", "semi=%3B,dot=.,comma=%2C");
			put("{+var}", "value");
			put("{+hello}", "Hello%20World!");
			put("{+half}", "50%25");
			put("{base}index", "http%3A%2F%2Fexample.com%2Fhome%2Findex");
			put("{+base}index", "http://example.com/home/index");
			put("O{+empty}X", "OX");
			put("O{+undef}X", "OX");
			put("{+path}/here", "/foo/bar/here");
			put("here?ref={+path}", "here?ref=/foo/bar");
			put("up{+path}{var}/here", "up/foo/barvalue/here");
			put("{+x,hello,y}", "1024,Hello%20World!,768");
			put("{+path,x}/here", "/foo/bar,1024/here");
			put("{+path:6}/here", "/foo/b/here");
			put("{+list}", "red,green,blue");
			put("{+list*}", "red,green,blue");
			put("{+keys}", "semi,;,dot,.,comma,,");
			put("{+keys*}", "semi=;,dot=.,comma=,");
			put("{#var}", "#value");
			put("{#hello}", "#Hello%20World!");
			put("{#half}", "#50%25");
			put("foo{#empty}", "foo#");
			put("foo{#undef}", "foo");
			put("{#x,hello,y}", "#1024,Hello%20World!,768");
			put("{#path,x}/here", "#/foo/bar,1024/here");
			put("{#path:6}/here", "#/foo/b/here");
			put("{#list}", "#red,green,blue");
			put("{#list*}", "#red,green,blue");
			put("{#keys}", "#semi,;,dot,.,comma,,");
			put("{#keys*}", "#semi=;,dot=.,comma=,");
			put("{.who}", ".fred");
			put("{.who,who}", ".fred.fred");
			put("{.half,who}", ".50%25.fred");
			put("www{.dom*}", "www.example.com");
			put("X{.var}", "X.value");
			put("X{.empty}", "X.");
			put("X{.undef}", "X");
			put("X{.var:3}", "X.val");
			put("X{.list}", "X.red,green,blue");
			put("X{.list*}", "X.red.green.blue");
			put("X{.keys}", "X.semi,%3B,dot,.,comma,%2C");
			put("X{.keys*}", "X.semi=%3B.dot=..comma=%2C");
			put("X{.empty_keys}", "X");
			put("X{.empty_keys*}", "X");
			put("{/who}", "/fred");
			put("{/who,who}", "/fred/fred");
			put("{/half,who}", "/50%25/fred");
			put("{/who,dub}", "/fred/me%2Ftoo");
			put("{/var}", "/value");
			put("{/var,empty}", "/value/");
			put("{/var,undef}", "/value");
			put("{/var,x}/here", "/value/1024/here");
			put("{/var:1,var}", "/v/value");
			put("{/list}", "/red,green,blue");
			put("{/list*}", "/red/green/blue");
			put("{/list*,path:4}", "/red/green/blue/%2Ffoo");
			put("{/keys}", "/semi,%3B,dot,.,comma,%2C");
			put("{/keys*}", "/semi=%3B/dot=./comma=%2C");
			put("{;who}", ";who=fred");
			put("{;half}", ";half=50%25");
			put("{;empty}", ";empty");
			put("{;v,empty,who}", ";v=6;empty;who=fred");
			put("{;v,bar,who}", ";v=6;who=fred");
			put("{;x,y}", ";x=1024;y=768");
			put("{;x,y,empty}", ";x=1024;y=768;empty");
			put("{;x,y,undef}", ";x=1024;y=768");
			put("{;hello:5}", ";hello=Hello");
			put("{;list}", ";list=red,green,blue");
			put("{;list*}", ";list=red;list=green;list=blue");
			put("{;keys}", ";keys=semi,%3B,dot,.,comma,%2C");
			put("{;keys*}", ";semi=%3B;dot=.;comma=%2C");
			put("{?who}", "?who=fred");
			put("{?half}", "?half=50%25");
			put("{?x,y}", "?x=1024&y=768");
			put("{?x,y,empty}", "?x=1024&y=768&empty=");
			put("{?x,y,undef}", "?x=1024&y=768");
			put("{?var:3}", "?var=val");
			put("{?list}", "?list=red,green,blue");
			put("{?list*}", "?list=red&list=green&list=blue");
			put("{?keys}", "?keys=semi,%3B,dot,.,comma,%2C");
			put("{?keys*}", "?semi=%3B&dot=.&comma=%2C");
			put("{&who}", "&who=fred");
			put("{&half}", "&half=50%25");
			put("?fixed=yes{&x}", "?fixed=yes&x=1024");
			put("{&x,y,empty}", "&x=1024&y=768&empty=");
			put("{&x,y,undef}", "&x=1024&y=768");
			put("{&var:3}", "&var=val");
			put("{&list}", "&list=red,green,blue");
			put("{&list*}", "&list=red&list=green&list=blue");
			put("{&keys}", "&keys=semi,%3B,dot,.,comma,%2C");
			put("{&keys*}", "&semi=%3B&dot=.&comma=%2C");
			put("{+var}", "value");
			put("{+hello}", "Hello%20World!");
			put("{+path}/here", "/foo/bar/here");
			put("here?ref={+path}", "here?ref=/foo/bar");
			put("X{#var}", "X#value");
			put("X{#hello}", "X#Hello%20World!");
			put("map?{x,y}", "map?1024,768");
			put("{x,hello,y}", "1024,Hello%20World%21,768");
			put("{#x,hello,y}", "#1024,Hello%20World!,768");
			put("{+path,x}/here", "/foo/bar,1024/here");
			put("X{.var}", "X.value");
			put("X{.x,y}", "X.1024.768");
			put("{/var}", "/value");
			put("{/var,x}/here", "/value/1024/here");
			put("{;x,y}", ";x=1024;y=768");
			put("{;x,y,empty}", ";x=1024;y=768;empty");
			put("{?x,y}", "?x=1024&y=768");
			put("{?x,y,empty}", "?x=1024&y=768&empty=");
			put("?fixed=yes{&x}", "?fixed=yes&x=1024");
			put("{&x,y,empty}", "&x=1024&y=768&empty=");
			put("{var:3}", "val");
			put("{var:30}", "value");
			put("{list}", "red,green,blue");
			put("{list*}", "red,green,blue");
			put("{keys}", "semi,%3B,dot,.,comma,%2C");
			put("{keys*}", "semi=%3B,dot=.,comma=%2C");
			put("{+path:6}/here", "/foo/b/here");
			put("{+list}", "red,green,blue");
			put("{+list*}", "red,green,blue");
			put("{+keys}", "semi,;,dot,.,comma,,");
			put("{+keys*}", "semi=;,dot=.,comma=,");
			put("{#path:6}/here", "#/foo/b/here");
			put("{#list}", "#red,green,blue");
			put("{#list*}", "#red,green,blue");
			put("{#keys}", "#semi,;,dot,.,comma,,");
			put("{#keys*}", "#semi=;,dot=.,comma=,");
			put("X{.var:3}", "X.val");
			put("X{.list}", "X.red,green,blue");
			put("X{.list*}", "X.red.green.blue");
			put("X{.keys}", "X.semi,%3B,dot,.,comma,%2C");
			put("X{.keys*}", "X.semi=%3B.dot=..comma=%2C");
			put("{/var:1,var}", "/v/value");
			put("{/list}", "/red,green,blue");
			put("{/list*}", "/red/green/blue");
			put("{/list*,path:4}", "/red/green/blue/%2Ffoo");
			put("{/keys}", "/semi,%3B,dot,.,comma,%2C");
			put("{/keys*}", "/semi=%3B/dot=./comma=%2C");
			put("{;hello:5}", ";hello=Hello");
			put("{;list}", ";list=red,green,blue");
			put("{;list*}", ";list=red;list=green;list=blue");
			put("{;keys}", ";keys=semi,%3B,dot,.,comma,%2C");
			put("{;keys*}", ";semi=%3B;dot=.;comma=%2C");
			put("{?var:3}", "?var=val");
			put("{?list}", "?list=red,green,blue");
			put("{?list*}", "?list=red&list=green&list=blue");
			put("{?keys}", "?keys=semi,%3B,dot,.,comma,%2C");
			put("{?keys*}", "?semi=%3B&dot=.&comma=%2C");
			put("{&var:3}", "&var=val");
			put("{&list}", "&list=red,green,blue");
			put("{&list*}", "&list=red&list=green&list=blue");
			put("{&keys}", "&keys=semi,%3B,dot,.,comma,%2C");
			put("{&keys*}", "&semi=%3B&dot=.&comma=%2C");
		}
	};

	public static TestSuite suite() {
		Class<TestURITemplate> testcase = TestURITemplate.class;
		TestSuite suite = new TestSuite(testcase.getName());
		for (Method method : testcase.getMethods()) {
			if (method.getName().startsWith("test")
					&& method.getParameterTypes().length == 0
					&& method.getReturnType().equals(Void.TYPE)) {
				suite.addTest(new TestURITemplate(method.getName()));
			}
		}
		for (String sub : tests.keySet()) {
			suite.addTest(new TestURITemplate(sub + " " + tests.get(sub)));
		}
		return suite;
	}

	public TestURITemplate() {
		super();
	}

	public TestURITemplate(String name) {
		super(name);
	}

	@Override
	public void runTest() throws Throwable {
		String name = getName();
		try {
			Method runMethod = this.getClass().getMethod(name,
					(Class[]) null);
			
			try {
				runMethod.invoke(this, (Object[]) new Class[0]);
			} catch (InvocationTargetException e) {
				e.fillInStackTrace();
				throw e.getTargetException();
			} catch (IllegalAccessException e) {
				e.fillInStackTrace();
				throw e;
			}
		} catch (NoSuchMethodException e) {
			String sub = name.substring(0, name.indexOf(' '));
			String expected = name.substring(name.indexOf(' ') + 1);
			assertSubstitution(sub, expected);
		}
	}

	public void testRequestBody() throws Exception {
		String pattern = "(?<pres>.*)";
		String template = "/request\nContent-Type: application/sparql\n\nSELECT * { <{+pres}> rdfs:label ?label}";
		Map<String, String> vars = new PathMatcher("http://example.com/", 0).match(pattern);
		String actual = new URITemplate(template).process(vars).toString();
		assertEquals("/request\nContent-Type: application/sparql\n\nSELECT * { <http://example.com/> rdfs:label ?label}", actual);
	}

	public void testNamedGroupReference() throws Exception {
		String regex = "^https?://(?<server>[\\w\\.-]+)(?::\\d+)?/[^\\?]*\\?url=https?(://|%3A%2F%2F)\\k<server>(?::\\d+|%3A\\d+)?([/\\w\\.\\-\\_\\!\\~\\*\\'\\(\\)]|%2F|%25|%24|%2B|%3B|%2C|%26|%3D|%24|%5B|%5D)*$";
		String target = "http://example.org/target";
		String url = "http://example.org/redirect?url=" + URLEncoder.encode(target, "UTF-8");
		assertTrue(new PathMatcher(url, 0).matches(regex));
		Map<String, String> param = Collections.singletonMap("url", target);
		String actual = new URITemplate("{+url}").process(param).toString();
		assertEquals(target, actual);
		String com = "http://example.com/redirect?url=" + URLEncoder.encode(target, "UTF-8");
		assertNull(new PathMatcher(com, 0).match(regex));
	}

	private void assertSubstitution(String sub, String expected)
			throws UnsupportedEncodingException {
		String actual = new URITemplate(sub).process(values).toString();
		assertEquals(expected,
				PercentCodec.encodeOthers(actual, PercentCodec.ALLOWED).replaceAll("%(?!\\w\\w)", "%25"));
	}
}
