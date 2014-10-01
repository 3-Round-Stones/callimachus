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
			"",
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
			my_resource = getHomeFolder().link("describedby").create("text/turtle", RESOURCE_TURTLE.getBytes());
			return;
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.link("describedby").delete();
		}
	}

	public void testEquivalentConflict() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().link("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().link("describedby").create("text/turtle", CLASS_CONFLICT.getBytes());
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
				return getHomeFolder().link("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().link("describedby").create("text/turtle", RESOURCE_TURTLE.getBytes());
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
				return getHomeFolder().link("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().link("describedby").create("application/rdf+xml", RESOURCE_RDF.getBytes());
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
				return getHomeFolder().link("describedby").create("text/turtle", CALLI_CLASS.getBytes());
			}
		});
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().link("describedby").create("application/ld+json", RESOURCE_JSON.getBytes());
			return;
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.delete();
		}
	}

	private static String cat(String... strings) {
		StringBuilder sb = new StringBuilder();
		for (String string : strings) {
			sb.append(string).append("\n");
		}
		return sb.toString();
	}

}
