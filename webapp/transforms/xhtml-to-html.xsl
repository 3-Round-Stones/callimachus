<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml"
        version="1.0" exclude-result-prefixes="xhtml">
    <xsl:output indent="no" method="html" doctype-system="about:legacy-compat" />

    <!-- normal elements -->
    <xsl:template match="*">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
    </xsl:template>

    <!-- strip meta charset -->
    <xsl:template match = 'xhtml:meta[translate(@http-equiv,"ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz") = "content-type" ]' />

    <!-- strip xmlns from head -->
    <xsl:template match="xhtml:head|xhtml:head/*">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
    </xsl:template>

    <!-- void elements -->
    <xsl:template match="xhtml:area|xhtml:base|xhtml:br|xhtml:col|xhtml:command|xhtml:embed|xhtml:hr|xhtml:img|xhtml:input|xhtml:keygen|xhtml:link|xhtml:meta|xhtml:param|xhtml:source|xhtml:track|xhtml:wbr">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@*"/>
        </xsl:element>
    </xsl:template>

    <!-- Raw text elements -->
    <xsl:template match="xhtml:script|xhtml:style">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@*|comment()|text()"/>
        </xsl:element>
    </xsl:template>

    <!-- End tag optional elements -->
    <xsl:template match="xhtml:li|xhtml:dt|xhtml:dd|xhtml:p|xhtml:rt|xhtml:rp|xhtml:optgroup|xhtml:option|xhtml:colgroup|xhtml:thead|xhtml:tbody|xhtml:tfoot|xhtml:tr|xhtml:td|xhtml:th">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="@*|comment()">
        <xsl:copy />
    </xsl:template>

    <!-- strip xml:space attributes -->
    <xsl:template match = '@xml:space' />

    <!-- convert lang attributes -->
    <xsl:template match="@xml:lang">
        <xsl:if test="not(../@lang)">
            <xsl:attribute name="lang">
                <xsl:value-of select="." />
            </xsl:attribute>
        </xsl:if>
    </xsl:template>

    <!-- strip all processing instructions -->
    <xsl:template match = 'processing-instruction()' />

</xsl:transform>
