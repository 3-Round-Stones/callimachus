<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    exclude-result-prefixes="xhtml sparql">
    <xsl:output indent="no" method="xml" />

    <xsl:template match="sparql:sparql">
        <html>
            <head>
                <title>Menu</title>
            </head>
            <body>
                <xsl:apply-templates mode="nav" select="sparql:results/sparql:result[not(sparql:binding/@name='heading')]" />
            </body>
        </html>
    </xsl:template>
    <xsl:template match="sparql:result">
    </xsl:template>
    <xsl:template mode="nav" match="sparql:result">
        <nav>
            <xsl:variable name="label" select="sparql:binding[@name='label']/*/text()" />
            <h3 class="nav-header">
                <xsl:if test="sparql:binding[@name='link']">
                    <a href="{sparql:binding[@name='link']/*}">
                        <xsl:value-of select="$label" />
                    </a>
                </xsl:if>
                <xsl:if test="not(sparql:binding[@name='link'])">
                    <xsl:value-of select="$label" />
                </xsl:if>
            </h3>
            <xsl:if test="../sparql:result[sparql:binding[@name='heading']/*/text()=$label]">
                <ul class="nav nav-list">
                    <xsl:apply-templates mode="children" select="../sparql:result[sparql:binding[@name='heading']/*/text()=$label]" />
                </ul>
            </xsl:if>
        </nav>
    </xsl:template>
    <xsl:template mode="children" match="sparql:result">
        <li>
            <xsl:variable name="label" select="sparql:binding[@name='label']/*/text()" />
            <a>
                <xsl:if test="sparql:binding[@name='link']">
                    <xsl:attribute name="href">
                        <xsl:value-of select="sparql:binding[@name='link']/*" />
                    </xsl:attribute>
                </xsl:if>
                <xsl:value-of select="$label" />
            </a>
            <xsl:if test="../sparql:result[sparql:binding[@name='heading']/*/text()=$label]">
                <ul>
                    <xsl:apply-templates mode="children" select="../sparql:result[sparql:binding[@name='heading']/*/text()=$label]" />
                </ul>
            </xsl:if>
        </li>
    </xsl:template>
</xsl:stylesheet>
