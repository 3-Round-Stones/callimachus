<?xml version="1.0" encoding="UTF-8" ?>
 <p:pipeline type="calli:page-layout-html" version="1.0"
        xmlns:p     ="http://www.w3.org/ns/xproc"
        xmlns:c     ="http://www.w3.org/ns/xproc-step"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:l     ="http://xproc.org/library">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="target" select="''" />
    <p:option name="query" select="''" />

    <p:import href="page-layout.xpl" />

    <p:variable name="targetOrSystem" select="if($target)then $target else base-uri()" />
    <p:variable name="find-realm-uri" select="concat('../queries/find-realm.rq?results&amp;target=', encode-for-uri($targetOrSystem))" />
    <p:variable name="realm" select="doc($find-realm-uri)//sparql:binding[@name='realm']/sparql:uri" />

    <calli:page-layout>
        <p:with-option name="realm" select="$realm" />
    </calli:page-layout>

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/flatten.xsl" />
        </p:input>
    </p:xslt>

    <p:xslt>
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
