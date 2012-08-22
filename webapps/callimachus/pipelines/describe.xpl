<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="target" required="true"  />

    <p:xslt>
        <p:with-param name="target"   select="$target" />
        <p:with-param name="systemId" select="$target" />
        <p:input port="stylesheet">
            <p:document href="../transforms/describe.xsl" />
        </p:input>
    </p:xslt>

    <p:import href = "transform-layout.xpl" />
    <calli:transform-layout>
        <p:with-option name="target"     select="$target" />
        <p:with-option name="query"    select="'describe'" />
        <p:with-option name="systemId" select="resolve-uri('../transforms/describe.xsl')" />
    </calli:transform-layout>
</p:pipeline>
