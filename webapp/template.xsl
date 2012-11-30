<xsl:stylesheet version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <xsl:param name="xsltId" />
    <xsl:param name="realm" select="'/'" />

    <xsl:variable name="url">
        <xsl:choose>
            <xsl:when test="string-length($realm) &gt; 0">
                <xsl:value-of select="concat('queries/realm-info.rq?results&amp;realm=',encode-for-uri($realm))" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="'queries/realm-info.rq?results&amp;realm=/'" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="bindings" select="document($url)//sparql:binding" />

    <xsl:variable name="styles" select="$bindings[@name='styles']/*/text()" />
    <xsl:variable name="scripts" select="$bindings[@name='scripts']/*/text()" />
    <xsl:variable name="layout" select="$bindings[@name='layout']/*/text()" />

    <xsl:include href="transforms/layout.xsl" />

</xsl:stylesheet>
