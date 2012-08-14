<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="systemId"  required="true" />
    <p:option name="element" select="'/1'" />

    <p:option name="this" select="$systemId" />
    <p:option name="query" required="true" />

    <p:variable name="find-realm-uri" select="concat('../queries/find-realm.rq?results&amp;this=', encode-for-uri($this))" />
    <p:variable name="realm" select="doc($find-realm-uri)//sparql:uri" />

    <p:import href="template.xpl" />
    <calli:template>
        <p:with-option name="systemId" select="$systemId" />
        <p:with-option name="realm" select="$realm" />
    </calli:template>

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/element.xsl" />
        </p:input>
        <p:with-param name="element" select="$element" />
    </p:xslt>

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/flatten.xsl" />
        </p:input>
    </p:xslt>

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