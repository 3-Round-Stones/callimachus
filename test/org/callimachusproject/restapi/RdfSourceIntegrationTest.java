package org.callimachusproject.restapi;

import java.util.concurrent.Callable;

import junit.framework.AssertionFailedError;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;

public class RdfSourceIntegrationTest extends
		TemporaryServerIntegrationTestCase {
	private static String OWL_CLASS = cat("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix owl:<http://www.w3.org/2002/07/owl#>.",
			"@prefix calli:<http://callimachusproject.org/rdf/2009/framework#>.",
			"",
			"<RDFSource> a owl:Class;",
			"		rdfs:label \"RDFSource\";",
			"		owl:equivalentClass <http://www.w3.org/ns/ldp#RDFSource>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Serviceable>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Editable>;",
			"		calli:author </auth/groups/users>, </auth/groups/admin>, </auth/groups/staff>.");
	private static String CALLI_CLASS = cat("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix owl:<http://www.w3.org/2002/07/owl#>.",
			"@prefix calli:<http://callimachusproject.org/rdf/2009/framework#>.",
			"",
			"<RDFSource> a </callimachus/1.4/types/Class>, owl:Class;",
			"		rdfs:label \"RDFSource\";",
			"		owl:equivalentClass <http://www.w3.org/ns/ldp#RDFSource>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Serviceable>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Editable>;",
			"		calli:author </auth/groups/users>, </auth/groups/admin>, </auth/groups/staff>.");
	private static String CLASS_CONFLICT = cat("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix owl:<http://www.w3.org/2002/07/owl#>.",
			"@prefix calli:<http://callimachusproject.org/rdf/2009/framework#>.",
			"",
			"<RDFSource2> a </callimachus/1.4/types/Class>, owl:Class;",
			"		rdfs:label \"RDFSource\";",
			"		owl:equivalentClass <http://www.w3.org/ns/ldp#RDFSource>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Serviceable>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Editable>;",
			"		calli:author </auth/groups/users>, </auth/groups/admin>, </auth/groups/staff>.");
	private static String RESOURCE_TURTLE = cat("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"<my-resource> a <http://www.w3.org/ns/ldp#RDFSource>; rdfs:label \"my resource\".");
	private static String RESOURCE_RDF = cat(
			"<?xml version='1.0' encoding='UTF-8'?>",
			"<rdf:RDF",
			"	xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'",
			"	xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>",
			"",
			"<rdf:Description rdf:about='my-resource'>",
			"	<rdf:type rdf:resource='http://www.w3.org/ns/ldp#RDFSource'/>",
			"	<rdfs:label>my-resource</rdfs:label>",
			"</rdf:Description>", "", "</rdf:RDF>");
	private static String RESOURCE_JSON = cat("[ {",
			"  \"@id\" : \"my-resource\",",
			"  \"@type\" : [ \"http://www.w3.org/ns/ldp#RDFSource\" ],",
			"  \"http://www.w3.org/2000/01/rdf-schema#label\" : [ {",
			"    \"@value\" : \"my-resource\"", "  } ]", "} ]");
	private static String NULL_RESOURCE = cat("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"<> a <http://www.w3.org/ns/ldp#RDFSource>; rdfs:label \"Who am I?\".");
	private static String TOPIC_RESOURCE = cat("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix foaf:<http://xmlns.com/foaf/0.1/>.",
			"<> foaf:primaryTopic <my-topic>.",
			"<my-topic> a <http://www.w3.org/ns/ldp#RDFSource>; rdfs:label \"my topic\".");
	private static String RESOURCE_CONTAINS = cat("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix ldp:<http://www.w3.org/ns/ldp#>.",
			"<my-resource> a <http://www.w3.org/ns/ldp#RDFSource>; rdfs:label \"my resource\"; ldp:contains <my-topic>.");

	public RdfSourceIntegrationTest(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testClassCreate() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				String createClass = "?create=/callimachus/1.4/types/Class";
				return getHomeFolder().ref(createClass).create("text/turtle", OWL_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("text/turtle", RESOURCE_TURTLE.getBytes());
			return;
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.rel("describedby").delete();
		}
	}

	public void testEquivalentConflict() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("text/turtle", CLASS_CONFLICT.getBytes());
		} catch (AssertionFailedError e) {
			return;
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
		fail();
	}

	public void testTurtleCreate() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("text/turtle", RESOURCE_TURTLE.getBytes());
			return;
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
	}

	public void testRdfCreate() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("application/rdf+xml", RESOURCE_RDF.getBytes());
			return;
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
	}

	public void testJsonCreate() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("application/ld+json", RESOURCE_JSON.getBytes());
			return;
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
	}

	public void testNullCreate() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("text/turtle", NULL_RESOURCE.getBytes());
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
	}

	public void testSlugCreate() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("slug", "text/turtle", NULL_RESOURCE.getBytes());
			if (!my_resource.toString().contains("slug")) {
				assertEquals(getHomeFolder().toString() + "slug?describe", my_resource.toString());
			}
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
	}

	public void testTopicCreate() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("text/turtle", TOPIC_RESOURCE.getBytes());
			if (!my_resource.toString().contains("my-topic")) {
				assertEquals(getHomeFolder().toString() + "my-topic?describe", my_resource.toString());
			}
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
	}

	public void testSlugTopicCreate() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("slug", "text/turtle", TOPIC_RESOURCE.getBytes());
			if (!my_resource.toString().contains("my-topic")) {
				assertEquals(getHomeFolder().toString() + "my-topic?describe", my_resource.toString());
			}
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
	}

	public void testCreateContainsConflict() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("text/turtle", RESOURCE_CONTAINS.getBytes());
		} catch (AssertionFailedError e) {
			return;
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
		fail();
	}

	public void testPutResource() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("text/turtle", RESOURCE_TURTLE.getBytes());
			String turtle = new String(my_resource.get("text/turtle"));
			my_resource.put("text/turtle", (turtle.replace("my resource", "My resource")).getBytes());
			String alt = new String(my_resource.get("text/turtle"));
			if (!alt.contains("My resource")) {
				assertEquals(turtle.replace("my resource", "My resource"), alt);
			}
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
	}

	public void testPutOtherResource() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().rel("describedby").create("text/turtle", RESOURCE_TURTLE.getBytes());
			my_resource.put("text/turtle", TOPIC_RESOURCE.getBytes());
		} catch (AssertionFailedError e) {
			return;
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
		fail();
	}

	private static String cat(String... strings) {
		StringBuilder sb = new StringBuilder();
		for (String string : strings) {
			sb.append(string).append("\n");
		}
		return sb.toString();
	}

}
