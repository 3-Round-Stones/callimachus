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

import java.util.concurrent.Callable;

import junit.framework.AssertionFailedError;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;

public class EditResourceTypeIntegrationTest extends TemporaryServerIntegrationTestCase {
	private static String TYPES_TURTLE = cat(
			"@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix owl:<http://www.w3.org/2002/07/owl#>.",
			"@prefix ex:<http://example.com/>.",
			"",
			"ex:Tag a rdfs:Class; rdfs:subClassOf rdfs:Class.",
			"ex:Home a ex:Tag, rdfs:Class; rdfs:label \"Home\".",
			"ex:Work a ex:Tag, rdfs:Class; rdfs:label \"Work\".");
	private static String CLASS_TURTLE = cat("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix owl:<http://www.w3.org/2002/07/owl#>.",
			"@prefix calli:<http://callimachusproject.org/rdf/2009/framework#>.",
			"",
			"</MyClass> a owl:Class;",
			"		rdfs:label \"My Class\";",
			"		rdfs:subClassOf </callimachus/1.4/types/Serviceable>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Editable>;",
			"		calli:author </auth/groups/users>, </auth/groups/admin>, </auth/groups/staff>");
	private static String RESOURCE_TURTLE = cat("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix owl:<http://www.w3.org/2002/07/owl#>.",
			"@prefix calli:<http://callimachusproject.org/rdf/2009/framework#>.",
			"@prefix ex:<http://example.com/>.",
			"",
			"</my-resource> a ex:Work; rdfs:label \"my resource\".");
	private static String BAD_RESOURCE_TURTLE = cat("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix owl:<http://www.w3.org/2002/07/owl#>.",
			"@prefix calli:<http://callimachusproject.org/rdf/2009/framework#>.",
			"@prefix ex:<http://example.com/>.",
			"",
			"</my-resource> a ex:Tag; rdfs:label \"my resource\".");
	private static String RESOURCE_UPDATE = cat("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>",
			"PREFIX ex:<http://example.com/>",
			"DELETE {",
			"	</my-resource> a ex:Work; rdfs:label \"my resource\" .",
			"}",
			"WHERE {",
			"	</my-resource> a ex:Work; rdfs:label \"my resource\" .",
			"};",
			"INSERT {",
			"	</my-resource> a ex:Home; rdfs:label \"my modified resource\" .",
			"}",
			"WHERE {",
			"};");
	private static String BAD_RESOURCE_DELETE1 = cat("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>",
			"PREFIX ex:<http://example.com/>",
			"DELETE {",
			"	</my-resource> a ex:Tag; rdfs:label \"my resource\" .",
			"}",
			"WHERE {",
			"	</my-resource> a ex:Tag; rdfs:label \"my resource\" .",
			"};",
			"INSERT {",
			"	</my-resource> a ex:Home; rdfs:label \"my modified resource\" .",
			"}",
			"WHERE {",
			"};");
	private static String BAD_RESOURCE_DELETE2 = cat("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>",
			"PREFIX ex:<http://example.com/>",
			"DELETE {",
			"	</my-resource> a ex:Tag; rdfs:label \"my resource\" .",
			"}",
			"WHERE {",
			"	</my-resource> rdfs:label \"my resource\" .",
			"};",
			"INSERT {",
			"	</my-resource> a ex:Home; rdfs:label \"my modified resource\" .",
			"}",
			"WHERE {",
			"};");
	private static String BAD_RESOURCE_INSERT = cat("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>",
			"PREFIX ex:<http://example.com/>",
			"DELETE {",
			"	</my-resource> a ex:Work; rdfs:label \"my resource\" .",
			"}",
			"WHERE {",
			"	</my-resource> a ex:Work; rdfs:label \"my resource\" .",
			"};",
			"INSERT {",
			"	</my-resource> a ex:Tag; rdfs:label \"my modified resource\" .",
			"}",
			"WHERE {",
			"};");
	private static String CREATE_TEMPLATE = cat("<?xml version='1.0' encoding='UTF-8' ?>",
			"<html xmlns='http://www.w3.org/1999/xhtml'",
			"    xmlns:xsd='http://www.w3.org/2001/XMLSchema#'",
			"    xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'",
			"    xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>",
			"<head>",
			"    <title>New Resource</title>",
			"</head>",
			"<body>",
			"    <h1>New Resource</h1>",
			"    <form method='POST' action='' enctype='application/rdf+xml' typeof=''",
			"            onsubmit='return calli.saveResourceAs(event,calli.slugify($(&quot;#label&quot;).val()))'>",
			"        <fieldset>",
			"            <div class='control-group'>",
			"                <label for='label' class='control-label'>Label</label>",
			"                <div class='controls'>",
			"                    <input type='text' id='label' value='{rdfs:label}' class='auto-expand' required='required' autofocus='autofocus' />",
			"                </div>",
			"            </div>",
			"            <div class='control-group'>",
			"                <label for='comment' class='control-label'>Comment</label>",
			"                <div class='controls'>",
			"                    <textarea id='comment' class='auto-expand'>{rdfs:comment}</textarea>",
			"                </div>",
			"            </div>",
			"            <div class='control-group'>",
			"              <label for='type' class='control-label'>Type</label>",
			"              <div id='type' class='controls'>",
			"                <label class='checkbox' rel='rdf:type' resource='?type'>",
			"                  <input type='checkbox' name='type' checked='checked' />",
			"                  <span rel='rdf:type' resource='http://example.com/Tag' property='rdfs:label' />",
			"                </label>",
			"              </div>",
			"            </div>",
			"            <div class='form-actions'>",
			"                <button type='submit' class='btn btn-success'>Create</button>",
			"            </div>",
			"        </fieldset>",
			"    </form>",
			"</body>",
			"</html>");
	private static String EDIT_TEMPLATE = cat("<?xml version='1.0' encoding='UTF-8' ?>",
			"<html xmlns='http://www.w3.org/1999/xhtml'",
			"    xmlns:xsd='http://www.w3.org/2001/XMLSchema#'",
			"    xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'",
			"    xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>",
			"<head>",
			"    <title resource='?this'>{rdfs:label}</title>",
			"</head>",
			"<body resource='?this'>",
			"    <h1 property='rdfs:label' />",
			"    <form method='POST' action='' enctype='application/sparql-update' resource='?this'>",
			"        <fieldset>",
			"            <div class='control-group'>",
			"                <label for='label' class='control-label'>Label</label>",
			"                <div class='controls'>",
			"                    <input type='text' id='label' value='{rdfs:label}' class='auto-expand' required='required' />",
			"                </div>",
			"            </div>",
			"            <div class='control-group'>",
			"                <label for='comment' class='control-label'>Comment</label>",
			"                <div class='controls'>",
			"                    <textarea id='comment' class='auto-expand'>{rdfs:comment}</textarea>",
			"                </div>",
			"            </div>",
			"            <div class='control-group'>",
			"              <label for='type' class='control-label'>Type</label>",
			"              <div id='type' class='controls'>",
			"                <label class='checkbox' rel='rdf:type' resource='?type'>",
			"                  <input type='checkbox' name='type' checked='checked' />",
			"                  <span rel='rdf:type' resource='http://example.com/Tag' property='rdfs:label' />",
			"                </label>",
			"              </div>",
			"            </div>",
			"            <div class='form-actions'>",
			"                <button type='submit' class='btn btn-primary'>Save</button>",
			"                <button type='button' onclick='window.location.replace(&quot;?view&quot;)' class='btn'>Cancel</button>",
			"                <button type='button' onclick='calli.deleteResource(event)' class='btn btn-danger'>Delete</button>",
			"            </div>",
			"        </fieldset>",
			"    </form>",
			"</body>",
			"</html>");

	public EditResourceTypeIntegrationTest(String name) throws Exception {
		super(name);
	}

	public void testCreate() throws Exception {
		final WebResource my_create_xhtml = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection().create("my-create.xhtml", "application/xhtml+xml", CREATE_TEMPLATE.getBytes());
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				String createClass = "?create=/callimachus/1.4/types/Class";
				String turtle = CLASS_TURTLE + ";\ncalli:create <" + my_create_xhtml + ">.";
				return getHomeFolder().ref(createClass).create("text/turtle", turtle.getBytes());
			}
		});
		WebResource types_ttl = getHomeFolder().link("contents", "application/atom+xml")
				.getAppCollection()
				.create("types.ttl", "text/turtle", TYPES_TURTLE.getBytes());
		String createResource = "?create=" + MyClass;
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().ref(createResource).create("text/turtle", RESOURCE_TURTLE.getBytes());
			return;
		} finally {
			if (my_resource != null) {
				my_resource.link("describedby").delete();
			}
			MyClass.link("describedby").delete();
			my_create_xhtml.link("edit-media", "application/xhtml+xml").delete();
			types_ttl.link("describedby").delete();
		}
	}

	public void testBadCreate() throws Exception {
		final WebResource my_create_xhtml = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection().create("my-create.xhtml", "application/xhtml+xml", CREATE_TEMPLATE.getBytes());
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				String createClass = "?create=/callimachus/1.4/types/Class";
				String turtle = CLASS_TURTLE + ";\ncalli:create <" + my_create_xhtml + ">.";
				return getHomeFolder().ref(createClass).create("text/turtle", turtle.getBytes());
			}
		});
		WebResource types_ttl = getHomeFolder().link("contents", "application/atom+xml")
				.getAppCollection()
				.create("types.ttl", "text/turtle", TYPES_TURTLE.getBytes());
		String createResource = "?create=" + MyClass;
		WebResource my_resource = null;
		try {
			my_resource = getHomeFolder().ref(createResource).create("text/turtle", BAD_RESOURCE_TURTLE.getBytes());
		} catch(AssertionFailedError e) {
			return;
		} finally {
			if (my_resource != null) {
				my_resource.delete();
			}
			MyClass.link("describedby").delete();
			my_create_xhtml.link("edit-media", "application/xhtml+xml").delete();
			types_ttl.link("describedby").delete();
		}
		fail();
	}

	public void testEdit() throws Exception {
		final WebResource my_create_xhtml = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection().create("my-create.xhtml", "application/xhtml+xml", CREATE_TEMPLATE.getBytes());
		final WebResource my_edit_xhtml = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection().create("my-edit.xhtml", "application/xhtml+xml", EDIT_TEMPLATE.getBytes());
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				String createClass = "?create=/callimachus/1.4/types/Class";
				String turtle = CLASS_TURTLE + ";\ncalli:create <" + my_create_xhtml + ">;\ncalli:edit <" + my_edit_xhtml + ">.";
				return getHomeFolder().ref(createClass).create("text/turtle", turtle.getBytes());
			}
		});
		WebResource types_ttl = getHomeFolder().link("contents", "application/atom+xml")
				.getAppCollection()
				.create("types.ttl", "text/turtle", TYPES_TURTLE.getBytes());
		String createResource = "?create=" + MyClass;
		WebResource my_resource = getHomeFolder().ref(createResource).create("text/turtle", RESOURCE_TURTLE.getBytes());
		try {
			my_resource.ref("?edit").post("application/sparql-update", RESOURCE_UPDATE.getBytes(), "text/uri-list");
			return;
		} finally {
			my_resource.link("describedby").delete();
			MyClass.link("describedby").delete();
			my_edit_xhtml.link("edit-media", "application/xhtml+xml").delete();
			my_create_xhtml.link("edit-media", "application/xhtml+xml").delete();
			types_ttl.link("describedby").delete();
		}
	}

	public void testBadDelete1() throws Exception {
		final WebResource my_create_xhtml = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection().create("my-create.xhtml", "application/xhtml+xml", CREATE_TEMPLATE.getBytes());
		final WebResource my_edit_xhtml = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection().create("my-edit.xhtml", "application/xhtml+xml", EDIT_TEMPLATE.getBytes());
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				String createClass = "?create=/callimachus/1.4/types/Class";
				String turtle = CLASS_TURTLE + ";\ncalli:create <" + my_create_xhtml + ">;\ncalli:edit <" + my_edit_xhtml + ">.";
				return getHomeFolder().ref(createClass).create("text/turtle", turtle.getBytes());
			}
		});
		WebResource types_ttl = getHomeFolder().link("contents", "application/atom+xml")
				.getAppCollection()
				.create("types.ttl", "text/turtle", TYPES_TURTLE.getBytes());
		String createResource = "?create=" + MyClass;
		WebResource my_resource = getHomeFolder().ref(createResource).create("text/turtle", RESOURCE_TURTLE.getBytes());
		try {
			my_resource.ref("?edit").create("application/sparql-update", BAD_RESOURCE_DELETE1.getBytes());
		} catch (AssertionFailedError e) {
			return;
		} finally {
			my_resource.link("describedby").delete();
			MyClass.link("describedby").delete();
			my_edit_xhtml.link("edit-media", "application/xhtml+xml").delete();
			my_create_xhtml.link("edit-media", "application/xhtml+xml").delete();
			types_ttl.link("describedby").delete();
		}
		fail();
	}

	public void testBadDelete2() throws Exception {
		final WebResource my_create_xhtml = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection().create("my-create.xhtml", "application/xhtml+xml", CREATE_TEMPLATE.getBytes());
		final WebResource my_edit_xhtml = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection().create("my-edit.xhtml", "application/xhtml+xml", EDIT_TEMPLATE.getBytes());
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				String createClass = "?create=/callimachus/1.4/types/Class";
				String turtle = CLASS_TURTLE + ";\ncalli:create <" + my_create_xhtml + ">;\ncalli:edit <" + my_edit_xhtml + ">.";
				return getHomeFolder().ref(createClass).create("text/turtle", turtle.getBytes());
			}
		});
		WebResource types_ttl = getHomeFolder().link("contents", "application/atom+xml")
				.getAppCollection()
				.create("types.ttl", "text/turtle", TYPES_TURTLE.getBytes());
		String createResource = "?create=" + MyClass;
		WebResource my_resource = getHomeFolder().ref(createResource).create("text/turtle", RESOURCE_TURTLE.getBytes());
		try {
			my_resource.ref("?edit").create("application/sparql-update", BAD_RESOURCE_DELETE2.getBytes());
		} catch (AssertionFailedError e) {
			return;
		} finally {
			my_resource.link("describedby").delete();
			MyClass.link("describedby").delete();
			my_edit_xhtml.link("edit-media", "application/xhtml+xml").delete();
			my_create_xhtml.link("edit-media", "application/xhtml+xml").delete();
			types_ttl.link("describedby").delete();
		}
		fail();
	}

	public void testBadINSERT() throws Exception {
		final WebResource my_create_xhtml = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection().create("my-create.xhtml", "application/xhtml+xml", CREATE_TEMPLATE.getBytes());
		final WebResource my_edit_xhtml = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection().create("my-edit.xhtml", "application/xhtml+xml", EDIT_TEMPLATE.getBytes());
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				String createClass = "?create=/callimachus/1.4/types/Class";
				String turtle = CLASS_TURTLE + ";\ncalli:create <" + my_create_xhtml + ">;\ncalli:edit <" + my_edit_xhtml + ">.";
				return getHomeFolder().ref(createClass).create("text/turtle", turtle.getBytes());
			}
		});
		WebResource types_ttl = getHomeFolder().link("contents", "application/atom+xml")
				.getAppCollection()
				.create("types.ttl", "text/turtle", TYPES_TURTLE.getBytes());
		String createResource = "?create=" + MyClass;
		WebResource my_resource = getHomeFolder().ref(createResource).create("text/turtle", RESOURCE_TURTLE.getBytes());
		try {
			my_resource.ref("?edit").create("application/sparql-update", BAD_RESOURCE_INSERT.getBytes());
		} catch (AssertionFailedError e) {
			return;
		} finally {
			my_resource.link("describedby").delete();
			MyClass.link("describedby").delete();
			my_edit_xhtml.link("edit-media", "application/xhtml+xml").delete();
			my_create_xhtml.link("edit-media", "application/xhtml+xml").delete();
			types_ttl.link("describedby").delete();
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
