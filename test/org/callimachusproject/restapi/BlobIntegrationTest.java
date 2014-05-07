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
package org.callimachusproject.restapi;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;

public class BlobIntegrationTest extends TemporaryServerIntegrationTestCase {
	
	private static Map<String, String[]> parameters = new LinkedHashMap<String, String[]>() {
        private static final long serialVersionUID = -4308917786147773821L;

        {
        	put("article", new String[] { "blobtest-article.docbook", "application/docbook+xml",
        					"<article version='5.0'  xmlns='http://docbook.org/ns/docbook' xmlns:xl='http://www.w3.org/1999/xlink'> \n" +
        					"<title>LS command</title> \n " +
        					"<para>This command is a synonym for command. \n" +
        					"</para> \n </article>", //End original
        					"<article version='5.0'  xmlns='http://docbook.org/ns/docbook' xmlns:xl='http://www.w3.org/1999/xlink'> \n" +
        					"<title>UPDATED LS command</title> \n " + //Begin update
        					"<para>This command (UPDATED) is a synonym for command. \n" +
        					"</para> \n </article>"
        	});
        	
        	put("font", new String[] { "blobtest-font.woff", "application/font-woff",
        					"@font-face { \n font-family: GentiumTest; \n" +
        					"src: url(fonts/GenR102.woff) format(\"woff\")," +
        					"url(fonts/GenR102.ttf) format(\"truetype\"); }", //End original
        					"@font-face { \n font-family: GentiumTest; \n" + //Begin update
        					"src: url(fonts/GenR102.woff) format(\"woff\")," +
        					"url(fonts/GenR102.ttf) format(\"truetype\"); }"
        	});
        	
        	put("namedQuery", new String[] { "blobtest-query.rq", "application/sparql-query",
							"SELECT ?title WHERE { \n" +
							"<http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title . \n" +
        					"}", //End original
        					"SELECT ?updatedTitle WHERE { \n" + //Begin update
							"<http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?updatedTitle . \n" +
        					"}" 
			});
        	
        	put("page", new String[] { "blobtest-page.xhtml", "application/xhtml+xml",
							"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
							"<head> <title> Wikipedia </title> </head> \n" +
							"<body> <p> Wikipedia is a great website. </p> </body> </html>", //End original
							"<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n " + //Begin update
							"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
							"<head> <title> Wikipedia has been UPDATED </title> </head> \n" +
							"<body> <p> The UPDATED Wikipedia is a great website. </p> </body> </html>"
			});
        	
        	put("xquery", new String[] { "blobtest-xquery.xq", "application/xquery",
							"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
							"<head> <title> Wikipedia </title> </head> \n" +
							"<body> <p> Wikipedia is a great website. </p> </body> </html>", //End original
							"" + //Begin update
							"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
							"<head> <title> Wikipedia has been UPDATED </title> </head> \n" +
							"<body> <p> The UPDATED Wikipedia is a great website. </p> </body> </html>"
			});
        	
        	put("vectorGraphic", new String[] { "blobtest-vector.svg", "image/svg+xml",
							"<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">" +
							"</svg> \n ", //End original
							"<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">" + //Begin update
							"<!-- UPDATED --></svg> \n "
			});
        	
        	put("style", new String[] { "blobtest-style.css", "text/css",
							"hr {color:sienna;} \n" +
						    "p {margin-left:20px;} \n" +
						    "body {background-color:blue}", //End original
							"hr {color:black;} \n" + //Begin Update
						    "p {margin-left:0px;} \n" +
						    "body {background-color:black}"
			});
        	
            put("hypertext", new String[] { "blobtest-file.html", "text/html",
                            "<html><head><title>Output Page</title></head><body></body></html>", //End original
                            "<html><head><title>Updated Page</title></head><body></body></html>" //Begin update
            });
            
            put("script", new String[] { "blobtest-script.js", "text/javascript",
							"alert('boo');", //End original 
							"alert('UPDATED');"
			});
            
            put("text", new String[] { "blobtest-file.txt", "text/plain",
            				"Body of a text document", //End original
            				"UPDATED body of a document" //Begin update
            });
            
            put("graphDocument", new String[] { "blobtest-file.ttl", "text/turtle",
							"@prefix foaf: <http://xmlns.com/foaf/0.1/> . \n" +
							"<http://example.org/joe#me> a foaf:Person . \n" +
							"<http://example.org/joe#me> foaf:homepage <http://example.org/joe/INDEX.html> . \n" +
							"<http://example.org/joe#me> foaf:mbox <mailto:joe@example.org> . \n" +
							"<http://example.org/joe#me> foaf:name \"Joe Lambda\" .", //End original
							"@prefix foaf: <http://xmlns.com/foaf/0.1/> . \n" + //Begin update
							"<http://example.org/joe#me> a foaf:Person . \n" +
							"<http://example.org/joe#me> foaf:homepage <http://example.org/joe/UPDATED/INDEX.html> . \n" +
							"<http://example.org/joe#me> foaf:mbox <mailto:joe@example.org> . \n" +
							"<http://example.org/joe#me> foaf:name \"UPDTAED Joe Lambda\" ."
							
            });
            
            put("transform", new String[] { "blobtest-transform.xsl", "text/xsl",
							"<?xml version=\"1.0\"?>" +
						    "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns=\"http://www.w3.org/1999/xhtml\" version=\"1.0\"> \n" +
						    "<xsl:template match=\"/\"> <html> <body> <h2>My CD Collection</h2> \n" +
						    "<table border=\"1\"> <tr bgcolor=\"#9acd32\"> <th>Title</th> \n" +
						    "<th>Artist</th>  </tr> </table> </body> </html> </xsl:template> \n" +
						    "</xsl:stylesheet>", //End original
						    "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \n" + //Begin update
						    "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns=\"http://www.w3.org/1999/xhtml\" version=\"1.0\"> \n" +
						    "<xsl:template match=\"/\"> <html> <body> <h2>My CD Collection</h2> \n" +
						    "<table border=\"1\"> <tr bgcolor=\"#9acd32\"> <th>UPDATED Title</th> \n" +
						    "<th>UPDATED Artist</th>  </tr> </table> </body> </html> </xsl:template> \n" +
						    "</xsl:stylesheet>"
			});
            
            put("pipeline", new String[] { "blobtest-pipeline.xpl", "application/xproc+xml",
            		"<?xml version=\"1.0\" encoding=\"UTF-8\" ?> \n" +
            	    "<p:pipeline version=\"1.0\" \n" +
                    "xmlns:p=\"http://www.w3.org/ns/xproc\" \n " + 
                    "xmlns:c=\"http://www.w3.org/ns/xproc-step\" \n" +
                    "xmlns:l=\"http://xproc.org/library\"> \n " +
                    "<p:identity /> </p:pipeline> ", //End original
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> \n" + //Begin update
            	    "<p:pipeline version=\"1.0\" \n" +
                    "xmlns:p=\"http://www.w3.org/ns/xproc\" \n " + 
                    "xmlns:c=\"http://www.w3.org/ns/xproc-step\" \n" +
                    "xmlns:l=\"http://xproc.org/library\"> \n " +
                    "<p:identity /><!-- UPDATED --> </p:pipeline> ",
            });
        }
    };

    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite(BlobIntegrationTest.class.getName());
        for (String name : parameters.keySet()) {
            suite.addTest(new BlobIntegrationTest(name));
        }
        return suite;
    }

	private String requestSlug;
	private String requestContentType;
	private String outputString;
	private String updateOutputString;

	public BlobIntegrationTest(String name) throws Exception {
		super(name);
		String [] args = parameters.get(name);
		requestSlug = args[0];
		requestContentType = args[1];
		outputString = args[2];
		updateOutputString = args[3];
	}
	
	public void runTest() throws Exception {
		WebResource blob = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection()
				.create(requestSlug, requestContentType,
						outputString.getBytes());
		WebResource edit = blob.link("edit-media", requestContentType);
		edit.get(requestContentType);
		blob.link("alternate", "text/html").get("text/html");
		blob.link("edit-form", "text/html").get("text/html");
		blob.link("comments").get("text/html");
		blob.link("describedby", "text/turtle").get("text/turtle");
		blob.link("describedby", "application/rdf+xml").get("application/rdf+xml");
		blob.link("describedby", "text/html").get("text/html");
		blob.link("version-history", "text/html").get("text/html");
		blob.link("version-history", "application/atom+xml").get("application/atom+xml");
		blob.ref("?permissions").get("text/html");
		blob.ref("?rdftype").get("text/uri-list");
		blob.ref("?relatedchanges").get("text/html");
		blob.ref("?whatlinkshere").get("text/html");
		blob.ref("?introspect").get("text/html");
		edit.put(requestContentType, updateOutputString.getBytes());
		edit.delete();
	}
}
