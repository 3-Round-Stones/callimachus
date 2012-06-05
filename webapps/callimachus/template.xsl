<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="systemId" />
    <xsl:param name="xsltId" />
    <xsl:param name="this" select="'/'" />
    <xsl:param name="realm" select="'.'" />
    <xsl:variable name="url">
        <xsl:choose>
            <xsl:when test="$realm='.'">
                <xsl:value-of select="concat($this,'?realm')" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat($realm,'?layout')" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="stylesheet" select="document($url)/xsl:stylesheet" />
    <xsl:variable name="styles" select="$stylesheet/xsl:variable[@name='styles']/node()" />
    <xsl:variable name="scripts" select="$stylesheet/xsl:variable[@name='scripts']/node()" />
    <xsl:variable name="layout" select="$stylesheet/xsl:variable[@name='layout']/node()" />
    <xsl:variable name="favicon" select="$stylesheet/xsl:variable[@name='favicon']/node()" />
    <xsl:variable name="menu" select="$stylesheet/xsl:variable[@name='menu']/node()" />
    <xsl:variable name="variation" select="$stylesheet/xsl:variable[@name='variation']/node()" />
    <xsl:variable name="rights" select="$stylesheet/xsl:variable[@name='rights']/node()" />
    <xsl:include href="transforms/layout.xsl" />
</xsl:stylesheet>
