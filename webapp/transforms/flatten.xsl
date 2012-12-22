<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml" xmlns:xhtml="http://www.w3.org/1999/xhtml"
    exclude-result-prefixes="xhtml">

<xsl:template match="*[starts-with(@about,'?') or starts-with(@resource,'?') or starts-with(@content,'?')]">
    <xsl:if test="not(ancestor::*[@about or @resource or @typeof])">
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
        </xsl:copy>
    </xsl:if>
</xsl:template>

<xsl:template match="*|processing-instruction()|comment()">
    <xsl:copy>
        <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
    </xsl:copy>
</xsl:template>

<xsl:template match="@*">
    <xsl:attribute name="{name()}" namespace="{namespace-uri()}">
        <xsl:call-template name="remove-expressions-string">
            <xsl:with-param name="string" select="." />
        </xsl:call-template>
    </xsl:attribute>
</xsl:template>

<xsl:template match="text()">
    <xsl:call-template name="remove-expressions-string">
        <xsl:with-param name="string" select="." />
    </xsl:call-template>
</xsl:template>

<xsl:template name="remove-expressions-string">
    <xsl:param name="string" />
    <xsl:variable name="before" select="substring-before(substring-before($string,'}'), '{')" />
    <xsl:variable name="next" select="substring-after(substring-before($string,'}'),'{')" />
    <xsl:variable name="rest" select="substring-after($string,'}')" />
    <xsl:choose>
        <xsl:when test="matches($next,'^\?[a-zA-Z]\w*$')">
            <xsl:value-of select="$before" />
            <xsl:call-template name="remove-expressions-string">
                <xsl:with-param name="string" select="$rest" />
            </xsl:call-template>
        </xsl:when>
        <xsl:when test="matches($next,'^&quot;([^&quot;\n]|\\&quot;)*&quot;$')">
            <xsl:value-of select="concat($before,substring($next,2,string-length($next)-2))" />
            <xsl:call-template name="remove-expressions-string">
                <xsl:with-param name="string" select="$rest" />
            </xsl:call-template>
        </xsl:when>
        <xsl:when test="matches($next,&quot;^'([^'\n]|\\')*'$&quot;)">
            <xsl:value-of select="concat($before,substring($next,2,string-length($next)-2))" />
            <xsl:call-template name="remove-expressions-string">
                <xsl:with-param name="string" select="$rest" />
            </xsl:call-template>
        </xsl:when>
        <xsl:when test="matches($next,'^[^\s,]*:[^\s,]*$')">
            <xsl:value-of select="$before" />
            <xsl:call-template name="remove-expressions-string">
                <xsl:with-param name="string" select="$rest" />
            </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$string" />
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

</xsl:stylesheet>
