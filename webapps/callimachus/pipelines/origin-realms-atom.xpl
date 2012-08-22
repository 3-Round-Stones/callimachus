<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#">
    <p:serialization port="result" media-type="application/sparql-results+xml" method="xml" />
    <p:option name="target"  required="true"  />
    <p:load>
        <p:with-option 
            name="href" 
            select="concat('../queries/origin-realms.rq?results&amp;target=', encode-for-uri($target))"/>
    </p:load>
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/sparql-results-atom.xsl" />
        </p:input>
    </p:xslt>
    <p:xslt>
        <p:with-param name="xsltId" select="resolve-uri('../transforms/divert-atom-links.xsl')" />
        <p:input port="stylesheet">
            <p:document href="../transforms/divert-atom-links.xsl" />
        </p:input>
    </p:xslt>
</p:pipeline>
