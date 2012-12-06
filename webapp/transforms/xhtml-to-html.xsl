<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml"
        version="1.0" exclude-result-prefixes="xhtml">
    <xsl:output indent="no" method="html" doctype-system="about:legacy-compat" />

    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*|*|comment()|text()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|comment()">
        <xsl:copy />
    </xsl:template>

    <xsl:template match="@xml:lang">
        <xsl:if test="not(../@lang)">
            <xsl:attribute name="lang">
                <xsl:value-of select="." />
            </xsl:attribute>
        </xsl:if>
    </xsl:template>

    <!-- strip xml:space attributes -->
    <xsl:template match = '@xml:space' />

    <!-- strip all processing instructions -->
    <xsl:template match = 'processing-instruction()' />

    <!-- strip meta charset -->
    <xsl:template match = 'xhtml:meta[translate(@http-equiv,"ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz") = "content-type" ]' />

    <xsl:template match="title|script|style|iframe|noembed|noframes">
        <!-- Some XSLT engines may not output HTML and this can help an HTML parser parse XML. -->
        <xsl:copy>
            <xsl:apply-templates select="@*|*|comment()|text()"/>
            <xsl:if test="not(text())">
                <xsl:text>&#160;</xsl:text>
            </xsl:if>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xhtml:title|xhtml:script|xhtml:style|xhtml:iframe|xhtml:noembed|xhtml:noframes">
        <!-- Some XSLT engines may not output HTML and this can help an HTML parser parse XML. -->
        <xsl:copy>
            <xsl:apply-templates select="@*|*|comment()|text()"/>
            <xsl:if test="not(text())">
                <xsl:text>&#160;</xsl:text>
            </xsl:if>
        </xsl:copy>
    </xsl:template>

</xsl:transform>
