<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#">
    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />
    <p:option name="this"  required="true"  />
    <p:import href = "transform-layout.xpl" />
    <p:load>
        <p:with-option 
            name="href" 
            select="concat('../queries/activity-view.rq?results&amp;this=', encode-for-uri($this))"/>
    </p:load>
    <p:xslt>
        <p:with-param name="this" select="$this" />
        <p:input port="stylesheet">
            <p:document href="../transforms/activity-view.xsl" />
        </p:input>
    </p:xslt>
    <calli:transform-layout>
        <p:with-option name="this"  select="$this"  />
        <p:with-option name="query" select="'view'" />
        <p:with-option name="systemId" select="resolve-uri('../transforms/activity-view.xsl')" />
    </calli:transform-layout>
</p:pipeline>
