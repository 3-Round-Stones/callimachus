<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">
    <xsl:template match="sparql:sparql">
        <c:data>
            <xsl:apply-templates select="sparql:head" />
            <xsl:apply-templates select="sparql:results" />
        </c:data>
    </xsl:template>
    <xsl:template match="sparql:head">
        <xsl:apply-templates select="sparql:variable" />
        <xsl:text>&#xD;&#xA;</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:variable">
        <xsl:value-of select="@name" />
        <xsl:if test="position() != last()">
            <xsl:text>,</xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sparql:results">
        <xsl:apply-templates select="sparql:result" />
    </xsl:template>
    <xsl:template match="sparql:result">
        <xsl:variable name="current" select="."/> 
        <xsl:for-each select="../../sparql:head/sparql:variable">
            <xsl:variable name="name" select="@name"/> 
            <xsl:apply-templates select="$current/sparql:binding[@name=$name]" /> 
            <xsl:if test="position() != last()">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:text>&#xD;&#xA;</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:binding">
        <xsl:apply-templates select="*" />
    </xsl:template>
    <xsl:template match="sparql:uri">
        <xsl:text>"</xsl:text>
        <xsl:apply-templates select="text()" />
        <xsl:text>"</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:bnode">
        <xsl:text>"</xsl:text>
        <xsl:apply-templates select="text()" />
        <xsl:text>"</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:literal">
        <xsl:text>"</xsl:text>
        <xsl:apply-templates select="text()" />
        <xsl:text>"</xsl:text>
    </xsl:template>
</xsl:stylesheet>
