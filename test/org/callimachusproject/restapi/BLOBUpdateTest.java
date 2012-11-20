package org.callimachusproject.restapi;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.test.TemporaryServerTestCase;

public class BLOBUpdateTest extends TemporaryServerTestCase {
	
	private static Map<String, String[]> parameters = new LinkedHashMap<String, String[]>() {
        private static final long serialVersionUID = -4308917786147773821L;

        {
        	put("article", new String[] { "updated-article.docbook", "application/docbook+xml",
        					"<article version='5.0'  xmlns='http://docbook.org/ns/docbook' xmlns:xl='http://www.w3.org/1999/xlink'> \n" +
        					"<title>LS command</title> \n " +
        					"<para>This command is a synonym for command. \n" +
        					"</para> \n </article>", //End original
        					"<article version='5.0'  xmlns='http://docbook.org/ns/docbook' xmlns:xl='http://www.w3.org/1999/xlink'> \n" +
        					"<title>UPDATED LS command</title> \n " + //Begin update
        					"<para>This command (UPDATED) is a synonym for command. \n" +
        					"</para> \n </article>"
        	});
        	
        	put("font", new String[] { "updated-font.woff", "application/font-woff",
        					"@font-face { \n font-family: GentiumTest; \n" +
        					"src: url(fonts/GenR102.woff) format(\"woff\")," +
        					"url(fonts/GenR102.ttf) format(\"truetype\"); }", //End original
        					"@font-face { \n font-family: GentiumTest; \n" + //Begin update
        					"src: url(fonts/GenR102.woff) format(\"woff\")," +
        					"url(fonts/GenR102.ttf) format(\"truetype\"); }"
        	});
        	
        	put("namedQuery", new String[] { "updated-query.rq", "application/sparql-query",
							"SELECT ?title WHERE { \n" +
							"<http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title . \n" +
        					"}", //End original
        					"SELECT ?updatedTitle WHERE { \n" + //Begin update
							"<http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?updatedTitle . \n" +
        					"}" 
			});
        	
        	put("page", new String[] { "updated-page.xhtml", "application/xhtml+xml",
							"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
							"<head> <title> Wikipedia </title> </head> \n" +
							"<body> <p> Wikipedia is a great website. </p> </body> </html>", //End original
							"<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n " + //Begin update
							"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
							"<head> <title> Wikipedia has been UPDATED </title> </head> \n" +
							"<body> <p> The UPDATED Wikipedia is a great website. </p> </body> </html>"
			});
        	
        	put("animatedGraphic", new String[] { "updated-graphic.gif", "image/gif",
							"binary", //End original 
							"UPDATED BINARY" //Begin update
			});
        	
        	put("photo", new String[] { "updated-photo.jpg", "image/jpeg",
							"binary", //End original 
							"UPDATED BINARY" //Begin update
			});
        	
        	put("networkGraphic", new String[] { "updated-network.png", "image/png",
							"binary", //End original 
							"UPDATED BINARY" //Begin update
			});
        	
        	put("vectorGraphic", new String[] { "updated-vector.svg", "image/svg+xml",
							"<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">" +
							"</svg> \n ", //End original
							"<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">" + //Begin update
							"<!-- UPDATED --></svg> \n "
			});
        	
        	put("iconGraphic", new String[] { "updated-logo.ico", "image/vnd.microsoft.icon",
							"binary", //End original 
							"UPDATED BINARY" //Begin update
			});
        	
        	put("style", new String[] { "updated-style.css", "text/css",
							"hr {color:sienna;} \n" +
						    "p {margin-left:20px;} \n" +
						    "body {background-color:blue}", //End original
							"hr {color:black;} \n" + //Begin Update
						    "p {margin-left:0px;} \n" +
						    "body {background-color:black}"
			});
        	
            put("hypertext", new String[] { "updated-file.html", "text/html",
                            "<html><head><title>Output Page</title></head><body></body></html>", //End original
                            "<html><head><title>Updated Page</title></head><body></body></html>" //Begin update
            });
            
            put("script", new String[] { "updated-script.js", "text/javascript",
							"if (response == true) { \n" +
							"return true; \n" +
							"}", //End original 
							"if (response == UPDATED) { \n" + //Begin update
							"return true; \n" +
							"}"
			});
            
            put("text", new String[] { "updated-file.txt", "text/plain",
            				"Body of a text document", //End original
            				"UPDATED body of a document" //Begin update
            });
            
            put("graphDocument", new String[] { "updated-file.ttl", "text/turtle",
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
            
            put("transform", new String[] { "updated-transform.xsl", "text/xsl",
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
            
            put("pipeline", new String[] { "updated-pipeline.xpl", "application/xproc+xml",
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
        TestSuite suite = new TestSuite(BLOBUpdateTest.class.getName());
        for (String name : parameters.keySet()) {
            suite.addTest(new BLOBUpdateTest(name));
        }
        return suite;
    }

	private String requestSlug;
	private String requestContentType;
	private String outputString;
	private String updateOutputString;

	public BLOBUpdateTest(String name) throws Exception {
		super(name);
		String [] args = parameters.get(name);
		requestSlug = args[0];
		requestContentType = args[1];
		outputString = args[2];
		updateOutputString = args[3];
	}
	
	public void runTest() throws Exception {
		getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection()
				.create(requestSlug, requestContentType,
						outputString.getBytes())
				.link("edit-media", requestContentType)
				.put(requestContentType, updateOutputString.getBytes());
	}
}
