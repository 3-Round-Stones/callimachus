<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0" name="pipeline"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="realm" required="true" />
    <p:option name="template" required="true" />
    <p:option name="target" required="true" />
    <p:option name="query" required="true" />
    <p:option name="element" select="'/1'" />

    <p:import href="../library.xpl" />

    <p:load>
        <p:with-option name="href" select="$template" />
    </p:load>

    <p:xslt name="template-element">
        <p:with-param name="element" select="$element" />
        <p:input port="stylesheet">
            <p:document href="../transforms/element.xsl" />
        </p:input>
    </p:xslt>

    <calli:render>
        <p:input port="template">
            <p:pipe step="template-element" port="result" />
        </p:input>
        <p:input port="source">
            <p:pipe step="pipeline" port="source" />
        </p:input>
    </calli:render>

    <p:xslt>
        <p:with-param name="realm" select="$realm" />
        <p:with-param name="target" select="$target"/>
        <p:with-param name="query" select="$query"/>
        <p:input port="stylesheet">
            <p:document href="../transforms/page-info.xsl" />
        </p:input>
    </p:xslt>
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/xhtml-to-html.xsl" />
        </p:input>
    </p:xslt>

</p:pipeline>
