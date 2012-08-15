<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
    xmlns:p="http://www.w3.org/ns/xproc"
    xmlns:c="http://www.w3.org/ns/xproc-step"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
    xmlns:l="http://xproc.org/library">
    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />
    <p:option name="this"  required="true"  />
    <p:import href = "transform-layout.xpl" />
    <p:load>
        <p:with-option 
            name="href" 
            select="concat('../queries/history.rq?results&amp;this=', encode-for-uri($this))"/>
    </p:load>
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../toolbox/history.xsl" />
        </p:input>
    </p:xslt>
    <calli:transform-layout>
        <p:with-option name="this"  select="$this"  />
        <p:with-option name="query" select="'history'" />
        <p:with-option name="systemId" select="resolve-uri('../toolbox/history.xsl')" />
    </calli:transform-layout>
</p:pipeline>