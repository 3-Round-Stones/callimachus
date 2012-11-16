<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
    xmlns:p="http://www.w3.org/ns/xproc"
    xmlns:c="http://www.w3.org/ns/xproc-step"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
    xmlns:l="http://xproc.org/library">
    <p:serialization port="result" media-type="application/xhtml+xml" method="xml" />
    <p:option name="target"     required="true"  />

    <p:load>
        <p:with-option 
            name="href" 
            select="concat('../queries/menu-items.rq?results&amp;target=', encode-for-uri($target))"/>
    </p:load>
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/menu-items.xsl" />
        </p:input>
    </p:xslt>
</p:pipeline>
