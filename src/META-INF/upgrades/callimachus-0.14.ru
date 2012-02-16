PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>
PREFIX msg:<http://www.openrdf.org/rdf/2011/messaging#>
PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>
PREFIX audit:<http://www.openrdf.org/rdf/2009/auditing#>

INSERT {
	?revision a audit:ObsoleteTransaction
} WHERE {
	?revision a audit:Transaction
	FILTER NOT EXISTS { ?revision a audit:ObsoleteTransaction }
	FILTER NOT EXISTS {GRAPH ?revision {
		?s ?p ?o
		FILTER (?p != rdf:subject)
		FILTER (?p != rdf:predicate)
		FILTER (?p != rdf:object)
		FILTER (?p != audit:committedOn)
		FILTER (?p != audit:contained)
		FILTER (?p != audit:modified)
		FILTER (?p != audit:predecessor)
		FILTER (?p != audit:contributedTo)
		FILTER (?p != rdf:type || (
			?o != audit:Transaction &&
			?o != audit:RecentTransaction &&
			?o != audit:ObsoleteTransaction &&
			?o != rdf:Statement))
	}}
	FILTER strstarts(str(?revision), str(</>))
	</callimachus> owl:versionInfo "0.14"
};

INSERT {
	?revision audit:contributedTo ?contributedTo
} WHERE {
	?revision a audit:Transaction
	{
		GRAPH ?revision { ?contributedTo ?p ?o FILTER (?p != audit:contained) }
	} UNION {
		?revision audit:contained [rdf:subject ?contributedTo]
	}
	FILTER NOT EXISTS { ?revision audit:contributedTo ?c }
	FILTER strstarts(str(?revision), str(</>))
	</callimachus> owl:versionInfo "0.14"
};

DELETE {
</callimachus/manifest> a </callimachus/Manifest>, calli:Manifest;
	rdfs:label "manifest";
	calli:reader </group/users>;
	calli:editor </group/staff>;
	calli:administrator </group/admin>;
	calli:template </callimachus/styles/layout.xsl>;
	calli:unauthorized </callimachus/pages/unauthorized.xhtml>;
	calli:forbidden </callimachus/pages/forbidden.xhtml>;
	calli:authentication </callimachus/accounts>;
	calli:logo ?logo ;
	calli:favicon ?favicon ;
	calli:theme ?theme.
} INSERT {
</> calli:hasComponent </manifest>, </callimachus>.

</callimachus> a owl:Ontology;
	rdfs:label "Callimachus";
	rdfs:comment "Vocabulary used to create local Callimachus applications".

</manifest> a </callimachus/Manifest>, calli:Manifest;
	rdfs:label "manifest";
	calli:reader </group/users>;
	calli:editor </group/staff>;
	calli:administrator </group/admin>;
	calli:unauthorized </callimachus/pages/unauthorized.xhtml>;
	calli:forbidden </callimachus/pages/forbidden.xhtml>;
	calli:authentication </manifest/accounts>;
	calli:menu </manifest/menu>;
	calli:favicon ?favicon ;
	calli:theme </callimachus/theme/default>;
	calli:hasComponent </manifest/accounts>, </manifest/menu>.
	
</sparql> a </callimachus/SparqlService>, sd:Service;
	rdfs:label "sparql";
	calli:administrator </group/admin>;
	sd:endpoint </sparql>;
	sd:supportedLanguage sd:SPARQL11Query, sd:SPARQL11Update;
	sd:feature sd:UnionDefaultGraph, sd:BasicFederatedQuery;
	sd:inputFormat <http://www.w3.org/ns/formats/RDF_XML>, <http://www.w3.org/ns/formats/Turtle>;
	sd:resultFormat <http://www.w3.org/ns/formats/RDF_XML>, <http://www.w3.org/ns/formats/SPARQL_Results_XML>.
} WHERE {
	{
		</callimachus/manifest> a calli:Manifest
	} OPTIONAL {
		</callimachus/manifest> calli:logo ?logo
	} OPTIONAL {
		</callimachus/manifest> calli:favicon ?favicon
	} OPTIONAL {
		</callimachus/manifest> calli:theme ?theme
	}
	</callimachus> owl:versionInfo "0.14"
};

DELETE {
	</callimachus/accounts> a ?type; rdfs:label ?label; calli:authName ?authName; calli:authNamespace ?authNamespace
} INSERT {
	</manifest/accounts> a ?type; rdfs:label ?label; calli:authName ?authName; calli:authNamespace ?authNamespace .
	</manifest/accounts> calli:administrator </group/admin>
} WHERE {
	</callimachus/accounts> a calli:AccountManager; a ?type; rdfs:label ?label; calli:authName ?authName; calli:authNamespace ?authNamespace .
	</callimachus> owl:versionInfo "0.14"
};

DELETE {
	?item calli:link </callimachus/menu>
} INSERT {
	?item calli:link </manifest/menu>
} WHERE {
	?item calli:link </callimachus/menu>
	FILTER strstarts(str(?item), str(</>))
	</callimachus> owl:versionInfo "0.14"
};

DELETE {
	?item calli:link </callimachus/manifest>
} INSERT {
	?item calli:link </manifest>
} WHERE {
	?item calli:link </callimachus/manifest>
	FILTER strstarts(str(?item), str(</>))
	</callimachus> owl:versionInfo "0.14"
};

DELETE {
	</callimachus/menu> a calli:Menu; a ?menu_type; calli:reader ?menu_reader; calli:editor ?menu_editor; calli:administrator ?menu_administrator;
		rdfs:label ?menu_label; calli:link ?menu_link; calli:item ?menu_nav .
	?menu_nav rdfs:label ?menu_nav_label; calli:position ?menu_nav_position; calli:item ?menu_item .
	?menu_item rdfs:label ?menu_item_label; calli:position ?menu_item_position; calli:link ?menu_item_link .
	?menu_nav calli:link ?menu_nav_ink
} INSERT {
	</manifest/menu> a calli:Menu; a ?menu_type; calli:reader ?menu_reader; calli:editor ?menu_editor; calli:administrator ?menu_administrator;
		rdfs:label ?menu_label; calli:link ?menu_link; calli:item ?manifest_menu_nav .
	?manifest_menu_nav rdfs:label ?menu_nav_label; calli:position ?menu_nav_position; calli:item ?manifest_menu_item .
	?manifest_menu_item rdfs:label ?menu_item_label; calli:position ?menu_item_position; calli:link ?menu_item_link .
	?manifest_menu_nav calli:link ?menu_nav_ink
} WHERE {
	</callimachus/menu> a calli:Menu; a ?menu_type; calli:reader ?menu_reader; calli:editor ?menu_editor; calli:administrator ?menu_administrator;
		rdfs:label ?menu_label; calli:link ?menu_link; calli:item ?menu_nav .
	?menu_nav rdfs:label ?menu_nav_label; calli:position ?menu_nav_position; calli:item ?menu_item .
	?menu_item rdfs:label ?menu_item_label; calli:position ?menu_item_position; calli:link ?menu_item_link .
	OPTIONAL {
		?menu_nav calli:link ?menu_nav_ink
	}
	FILTER strstarts(str(?menu_nav), str(</callimachus/>))
	FILTER strstarts(str(?menu_item), str(</callimachus/>))
	BIND (iri(concat(str(</manifest/>), strafter(str(?menu_nav), str(</callimachus/>)))) AS ?manifest_menu_nav)
	BIND (iri(concat(str(</manifest/>), strafter(str(?menu_item), str(</callimachus/>)))) AS ?manifest_menu_item)
	</callimachus> owl:versionInfo "0.14"
};

INSERT {
	?item calli:link </callimachus/TextFile>
} WHERE {
	?item calli:link </callimachus/Text>
	FILTER NOT EXISTS { ?item calli:link </callimachus/TextFile> }
	</callimachus> owl:versionInfo "0.14"
};
DELETE {
	?item calli:link </callimachus/Text>
} WHERE {
	?item calli:link </callimachus/Text> .
	</callimachus> owl:versionInfo "0.14"
};

INSERT {
	?item calli:link </main-article.docbook?view>
} WHERE {
	?item calli:link </page/main-page.xhtml?view>
	FILTER NOT EXISTS { ?item calli:link </main-article.docbook?view> }
	</callimachus> owl:versionInfo "0.14"
};
DELETE {
	?item calli:link </page/main-page.xhtml?view>
} WHERE {
	?item calli:link </page/main-page.xhtml?view> .
	</callimachus> owl:versionInfo "0.14"
};

INSERT {
	?item calli:position 0
} WHERE {
	?menu calli:item ?item
	FILTER NOT EXISTS { ?item calli:position ?p }
	</callimachus> owl:versionInfo "0.14"
};
DELETE {
	?item calli:position ?gt
} WHERE {
	?item calli:position ?lt, ?gt
	FILTER (?lt < ?gt)
	</callimachus> owl:versionInfo "0.14"
};

INSERT DATA {
</> calli:hasComponent </.well-known/>.

</.well-known/> a </callimachus/Folder>, calli:Folder;
	rdfs:label ".well known";
	calli:reader </group/users>,</group/staff>;
	calli:administrator </group/admin>;
	calli:hasComponent </.well-known/void>.

</.well-known/void> a void:Dataset;
	rdfs:label "void";
	foaf:homepage </>;
	void:sparqlEndpoint </sparql>.
};

DELETE {
	</callimachus> owl:versionInfo "0.14"
} INSERT {
	</callimachus> owl:versionInfo "0.15"
} WHERE {
	</callimachus> owl:versionInfo "0.14"
};

