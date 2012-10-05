<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:import href="page-template.xpl" />

    <p:variable name="systemId" select="p:base-uri()" />
    <p:variable name="find-realm-uri" select="concat('../queries/find-realm.rq?results&amp;target=', encode-for-uri($systemId))" />
    <p:variable name="realm" select="doc($find-realm-uri)//sparql:uri" />

    <calli:page-template>
        <p:with-option name="systemId" select="$systemId" />
        <p:with-option name="realm" select="$realm" />
    </calli:page-template>

    <p:xslt>
        <p:with-param name="realm" select="$realm" />
        <p:with-param name="target" select="$systemId"/>
        <p:with-param name="query" select="'view'"/>
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
