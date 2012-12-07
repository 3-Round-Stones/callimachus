<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:import href="docbook-inclusion.xpl" />
    <p:import href="page-layout-html.xpl" />

    <calli:docbook-inclusion />

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/book-view.xsl" />
        </p:input>
    </p:xslt>

    <calli:page-layout-html query="view">
        <p:with-option name="target" select="p:base-uri()" />
    </calli:page-layout-html>

</p:pipeline>
