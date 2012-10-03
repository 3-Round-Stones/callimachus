<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0" name="pipeline" type="calli:page-template"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:option name="systemId" select="''" />
    <p:option name="realm" required="true" />

    <p:variable name="systemIdOrBaseUri" select="if (string-length($systemId) &gt; 0) then $systemId else base-uri()" />

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
        <p:variable name="xsltId" select="substring-before(substring-after($xml-stylesheet, 'href=&quot;'), '&quot;')" />
        <p:sink />
        <p:choose>
            <p:when test="contains($xml-stylesheet, 'type=&quot;text/xsl&quot;') and string-length($xsltId) &gt; 0">
                <p:load name="load-stylesheet">
                    <p:with-option name="href" select="p:resolve-uri($xsltId, $systemIdOrBaseUri)" />
                </p:load>
                <p:xslt>
                    <p:with-param name="systemId" select="$systemIdOrBaseUri" />
                    <p:with-param name="xsltId" select="$xsltId" />
                    <p:with-param name="realm" select="$realm" />
                    <p:input port="stylesheet">
                        <p:pipe step="load-stylesheet" port="result" />
                    </p:input>
                    <p:input port="source">
                        <p:pipe step="pipeline" port="source" />
                    </p:input>
                </p:xslt>
            </p:when>
            <p:otherwise>
                <p:identity>
                    <p:input port="source">
                        <p:pipe step="pipeline" port="source" />
                    </p:input>
                </p:identity>
            </p:otherwise>
        </p:choose>
    </p:group>
</p:pipeline>
