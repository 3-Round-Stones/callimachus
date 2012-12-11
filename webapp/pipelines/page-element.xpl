<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="realm" select="''" />
    <p:option name="element" select="'/1'" />

    <p:import href="page-layout.xpl" />
    <p:import href="page-template.xpl" />

    <p:choose>
        <p:when test="string-length($realm) &gt; 0">
            <calli:page-layout>
                <p:with-option name="realm" select="$realm" />
            </calli:page-layout>
        </p:when>
        <p:otherwise>
            <calli:page-template />
        </p:otherwise>
    </p:choose>

    <p:xslt>
        <p:with-param name="element" select="$element" />
        <p:input port="stylesheet">
            <p:document href="../transforms/element.xsl" />
        </p:input>
    </p:xslt>

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/flatten.xsl" />
        </p:input>
    </p:xslt>

    <p:xslt>
        <p:with-param name="realm" select="$realm" />
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
