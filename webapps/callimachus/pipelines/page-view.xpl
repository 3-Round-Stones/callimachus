<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="this"  required="true"  />
    <p:option name="query" required="true" />

    <p:import href="template.xpl" />
    <calli:template>
        <p:with-option name="this" select="$this" />
    </calli:template>

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