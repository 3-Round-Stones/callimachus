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

public class RdfIntegrationTest extends TemporaryServerIntegrationTestCase {
	
	private static Map<String, String[]> parameters = new LinkedHashMap<String, String[]>() {
        private static final long serialVersionUID = -4308917786147773821L;

        {
        	put("Concept", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        		    " prefix skos: <http://www.w3.org/2004/02/skos/core#> \n " + 
        			" INSERT DATA {  \n <created-concept> a skos:Concept, </callimachus/Concept> ;  \n" +
        			" skos:prefLabel \"concept\" . }",
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        		    " prefix skos: <http://www.w3.org/2004/02/skos/core#> \n " + 
        			" DELETE { <> skos:prefLabel \"concept\" } INSERT { <> skos:prefLabel \"UPDATED\" } WHERE {}"
        	});
        	
        	put("Folder", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <created-test/> a calli:Folder, </callimachus/Folder> ;  \n" +
        			" rdfs:label \"test\" . }",
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" DELETE { <> rdfs:label \"test\" } INSERT { <> rdfs:label \"UPDATED\" } WHERE {}"
        	});
        	
        	put("Group", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <created-testGroup> a calli:Party, calli:Group, </callimachus/Group> ;  \n" +
        			" rdfs:label \"testGroup\" . }",
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" DELETE { <> rdfs:label \"testGroup\" } INSERT { <> rdfs:label \"UPDATED\" } WHERE {}"
        	});
        	
        	put("PURL", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <created-purl> a calli:Purl, </callimachus/PURL> ;  \n" +
        			" rdfs:label \"purl\" ; calli:alternate \"http://purl.org/\" . }",
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" DELETE { <> rdfs:label \"purl\" } INSERT { <> rdfs:label \"UPDATED\" } WHERE {}"
        	});
        }
    };

	public static TestSuite suite() throws Exception{
        TestSuite suite = new TestSuite(RdfIntegrationTest.class.getName());
        for (String name : parameters.keySet()) {
            suite.addTest(new RdfIntegrationTest(name));
        }
        return suite;
    }
	
	private String create;
	private String update;

	public RdfIntegrationTest(String name) throws Exception {
		super(name);
		String [] args = parameters.get(name);
		create = args[0];
		update = args[1];
	}
	
	public void runTest() throws Exception {
		String sparql = "BASE <" + getHomeFolder() + "> \n" + create;
		WebResource resource = getHomeFolder().link("describedby")
				.create("application/sparql-update", sparql.getBytes());
		resource.link("alternate", "text/html").get("text/html");
		resource.link("edit-form", "text/html").get("text/html");
		resource.link("comments").get("text/html");
		resource.link("describedby", "text/turtle").get("text/turtle");
		resource.link("describedby", "application/rdf+xml").get("application/rdf+xml");
		resource.link("describedby", "text/html").get("text/html");
		resource.link("version-history", "text/html").get("text/html");
		resource.link("version-history", "application/atom+xml").get("application/atom+xml");
		resource.ref("?permissions").get("text/html");
		resource.ref("?rdftype").get("text/uri-list");
		resource.ref("?relatedchanges").get("text/html");
		resource.ref("?whatlinkshere").get("text/html");
		resource.ref("?introspect").get("text/html");
		resource.link("describedby").patch("application/sparql-update", update.getBytes());
		resource.link("describedby").delete();
	}

}
