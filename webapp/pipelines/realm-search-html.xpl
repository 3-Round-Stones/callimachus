<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
    xmlns:p="http://www.w3.org/ns/xproc"
    xmlns:c="http://www.w3.org/ns/xproc-step"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
    xmlns:l="http://xproc.org/library">
    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />
    <p:option name="target"  required="true"  />
    <p:option name="q"     required="true"  />
    <p:import href = "transform-layout.xpl" />
    <p:load>
        <p:with-option 
            name="href" 
            select="concat(
                '../queries/realm-search.rq?results&amp;target=', 
                encode-for-uri($target),
                '&amp;q=',
                encode-for-uri($q)
            )"/>
    </p:load>
    <p:xslt>
        <p:with-param name="systemId" select="$target" />
        <p:input port="stylesheet">
            <p:document href="../transforms/realm-search.xsl" />
        </p:input>
    </p:xslt>
    <calli:transform-layout>
        <p:with-option name="target"  select="$target" />
        <p:with-option name="query" select="'q'" />
        <p:with-option name="systemId" select="resolve-uri('../transforms/realm-search.xsl')" />
    </calli:transform-layout>
</p:pipeline>
