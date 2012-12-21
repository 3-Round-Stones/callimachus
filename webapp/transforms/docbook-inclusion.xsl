<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://docbook.org/ns/docbook" xmlns:d="http://docbook.org/ns/docbook"
    xmlns:xl="http://www.w3.org/1999/xlink" exclude-result-prefixes="xsl d">
<xsl:output method="xml" indent="yes" />

<xsl:template match="@*|node()">
    <xsl:copy>
        <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
</xsl:template>

<xsl:template match="d:preface/d:article|d:partintro/d:article|d:article/d:article|d:chapter/d:article|d:appendix/d:article|d:section/d:article|d:topic/d:article">
    <section>
        <xsl:apply-templates select="@*|node()" />
    </section>
</xsl:template>

<xsl:template match="@xml:id">
    <xsl:attribute name="xml:id">
        <xsl:call-template name="id">
            <xsl:with-param name="target" select=".." />
        </xsl:call-template>
    </xsl:attribute>
</xsl:template>

<xsl:template match="d:link/@linkend">
    <xsl:variable name="id" select="." />
    <xsl:variable name="base" select="base-uri(.)" />
    <xsl:variable name="article" select="ancestor::*[base-uri()=$base][last()]" />
    <xsl:variable name="target" select="$article//*[@xml:id=$id]" />
    <xsl:choose>
        <xsl:when test="count($target)=1">
            <xsl:attribute name="linkend">
                <xsl:call-template name="id">
                    <xsl:with-param name="target" select="$target" />
                </xsl:call-template>
            </xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
            <xsl:copy />
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template match="d:link/@xl:href">
    <xsl:choose>
        <xsl:when test="starts-with(.,concat(base-uri(/),'#')) or starts-with(.,concat(base-uri(/),'?view#'))">
            <xsl:attribute name="linkend">
                <xsl:value-of select="substring-after(., '#')" />
            </xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
            <xsl:copy />
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template name="id">
    <xsl:param name="target" />
    <xsl:variable name="id" select="$target/@xml:id" />
    <xsl:variable name="preceding" select="count($target/preceding::*[@xml:id = $id])" />
    
    <xsl:choose>
        <xsl:when test="$preceding != 0">
            <xsl:value-of select="concat($id, $preceding)" />
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$id" />
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

</xsl:stylesheet>