<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0" type="calli:template"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:option name="this" required="true" />

    <p:variable name="find-realm-uri" select="concat('../queries/find-realm.rq?results&amp;this=', encode-for-uri($this))" />
    <p:variable name="realm" select="doc($find-realm-uri)//sparql:uri" />

    <p:xinclude name="xinclude" />

    <p:xslt name="xml-stylesheet">
        <p:input port="stylesheet">
            <p:inline>
                <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
                    <xsl:template match="/">
                        <processing-instructions>
                            <xsl:apply-templates select="processing-instruction('xml-stylesheet')" />
                        </processing-instructions>
                    </xsl:template>
                    <xsl:template match="processing-instruction('xml-stylesheet')">
                        <xml-stylesheet>
                            <xsl:value-of select="." />
                        </xml-stylesheet>
                    </xsl:template>
                </xsl:stylesheet>
            </p:inline>
        </p:input>
    </p:xslt>
    <p:group>
        <p:variable name="xml-stylesheet" select="processing-instructions/xml-stylesheet[1]">
            <p:pipe step="xml-stylesheet" port="result" />
        </p:variable>
        <p:sink />
        <p:choose>
            <p:when test="contains($xml-stylesheet, 'type=&quot;text/xsl&quot;')">
                <p:load name="load-stylesheet">
                    <p:with-option name="href" select="substring-before(substring-after($xml-stylesheet, 'href=&quot;'), '&quot;')" />
                </p:load>
                <p:xslt>
                    <p:with-param name="realm" select="$realm"/>
                    <p:input port="stylesheet">
                        <p:pipe step="load-stylesheet" port="result" />
                    </p:input>
                    <p:input port="source">
                        <p:pipe step="xinclude" port="result" />
                    </p:input>
                </p:xslt>
            </p:when>
            <p:otherwise>
                <p:identity>
                    <p:input port="source">
                        <p:pipe step="xinclude" port="result" />
                    </p:input>
                </p:identity>
            </p:otherwise>
        </p:choose>
    </p:group>
</p:pipeline>