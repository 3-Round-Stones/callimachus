PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>
DROP SILENT GRAPH <urn:test:graph>;
INSERT DATA {
 GRAPH <urn:test:graph> {
  </data?graph=urn:test:graph> a sd:Service .
 }
};