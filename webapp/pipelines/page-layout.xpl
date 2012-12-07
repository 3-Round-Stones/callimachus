<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0" name="pipeline" type="calli:page-layout"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:option name="realm" required="true" />

    <p:xinclude name="xinclude" fixup-xml-base="true" fixup-xml-lang="true" />
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/make-paths-absolute.xsl" />
        </p:input>
    </p:xslt>
    <p:xslt name="template-expression">
        <p:input port="stylesheet">
            <p:document href="../transforms/page-form-expressions.xsl" />
        </p:input>
    </p:xslt>
    <p:load name="realm-load">
        <p:with-option name="href" select="concat('../queries/realm-layout.rq?results&amp;realm=', encode-for-uri($realm))" />
    </p:load>
    <p:add-attribute match="c:request" attribute-name="href">
        <p:with-option name="attribute-value" select="//sparql:binding[@name='layout']/*/text()">
            <p:pipe step="realm-load" port="result" />
        </p:with-option>
        <p:input port="source">
            <p:inline>
                <c:request method="GET">
                    <c:header name="Accept" value="application/xquery" />
                </c:request>
            </p:inline>
        </p:input>
    </p:add-attribute>
    <p:http-request name="xquery-request" />
    <p:xquery>
        <p:input port="source">
            <p:pipe step="template-expression" port="result" />
        </p:input>
        <p:input port="query">
            <p:pipe step="xquery-request" port="result" />
        </p:input>
    </p:xquery>
</p:pipeline>
