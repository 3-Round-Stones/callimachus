PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>
PREFIX void:<http://rdfs.org/ns/void#>
PREFIX foaf:<http://xmlns.com/foaf/0.1/>
PREFIX msg:<http://www.openrdf.org/rdf/2011/messaging#>
PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>
PREFIX prov:<http://www.w3.org/ns/prov#>
PREFIX audit:<http://www.openrdf.org/rdf/2012/auditing#>

INSERT {
</auth/> calli:hasComponent </auth/secrets/>.
</auth/secrets/> a <types/Folder>, calli:Folder;
    rdfs:label "secrets".
} WHERE {
	FILTER NOT EXISTS { </auth/secrets/> a calli:Folder }
};
</auth/groups/> calli:hasComponent </auth/groups/super>.
</auth/groups/super> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "super";
    rdfs:comment "The user accounts in this group have heightened privileges to change or patch the system itself".

</auth/groups/> calli:hasComponent </auth/groups/power>.
</auth/groups/power> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "power";
    rdfs:comment "Members of this group can access all data in the underlying data store";
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:membersFrom ".".