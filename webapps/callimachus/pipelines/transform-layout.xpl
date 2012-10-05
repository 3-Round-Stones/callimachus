<?xml version="1.0" encoding="UTF-8" ?>
 <p:pipeline type="calli:transform-layout" version="1.0"
        xmlns:p     ="http://www.w3.org/ns/xproc"
        xmlns:c     ="http://www.w3.org/ns/xproc-step"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:l     ="http://xproc.org/library">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="target"   required="true" />
    <p:option name="query"    required="true" />
    <p:option name="systemId" required="true" />

    <p:variable name="find-realm-uri" select="concat('../queries/find-realm.rq?results&amp;target=', encode-for-uri($target))" />
    <p:variable name="realm" select="doc($find-realm-uri)//sparql:uri" />

    <p:xslt>
        <p:with-param name="systemId" select="$systemId" />
        <p:with-param name="xsltId"   select="resolve-uri('../template.xsl')" />
        <p:with-param name="realm"    select="$realm" />
        <p:input port="stylesheet">
            <p:document href="../template.xsl" />
        </p:input>
    </p:xslt>

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
