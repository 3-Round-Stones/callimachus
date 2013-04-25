<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0" name="pipeline" type="calli:page-layout"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:option name="realm" required="true" />

    <p:import href="page-template.xpl" />

    <calli:page-template name="page-template" />

    <p:load name="realm-load">
        <p:with-option name="href" select="concat('../queries/realm-layout.rq?results&amp;realm=', encode-for-uri($realm))" />
    </p:load>
    <p:add-attribute match="c:request" attribute-name="href">
        <p:with-option name="attribute-value" select="//sparql:binding[@name='layout']/sparql:uri/text()">
            <p:pipe step="realm-load" port="result" />
        </p:with-option>
        <p:input port="source">
            <p:inline>
                <c:request method="GET" detailed="true">
                    <c:header name="Accept" value="application/xquery" />
                </c:request>
            </p:inline>
        </p:input>
    </p:add-attribute>
    <p:http-request />
    <p:choose>
        <p:when test="/c:response[@status='200' or @status='203']">
            <p:identity>
                <p:input port="source" select="/c:response/c:body" />
            </p:identity>
        </p:when>
        <p:otherwise>
            <p:identity>
                <p:input port="source">
                    <p:data href="../transforms/default-layout.xq" />
                </p:input>
            </p:identity>
        </p:otherwise>
    </p:choose>
    <p:identity name="xquery-response" />
    <p:xquery>
        <p:with-param name="calli:realm" select="$realm" />
        <p:input port="source">
            <p:pipe step="page-template" port="result" />
        </p:input>
        <p:input port="query">
            <p:pipe step="xquery-response" port="result" />
        </p:input>
    </p:xquery>
</p:pipeline>
