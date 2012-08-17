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

    <p:option name="template" required="true" />
    <p:option name="this" required="true" />
    <p:option name="query" required="true" />
    
    <p:declare-step type="calli:render">
        <p:input port="source" sequence="true" primary="true" />
        <p:input port="template"/>
        <p:option name="output-base-uri"/>
        <p:output port="result" sequence="true" />
    </p:declare-step>
    
    <p:load name="template-load">
        <p:with-option name="href" select="$template" />
    </p:load>
    
    <calli:render>
        <p:input port="template">
            <p:pipe step="template-load" port="result" />
        </p:input>
        <p:input port="source">
            <p:pipe step="pipeline" port="source" />
        </p:input>
    </calli:render>
    
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/page.xsl" />
        </p:input>
        <p:with-param name="this" select="$this"/>
        <p:with-param name="query" select="$query"/>
    </p:xslt>
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/xhtml-to-html.xsl" />
        </p:input>
    </p:xslt>

</p:pipeline>
