<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns="http://www.w3.org/1999/xhtml" xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:d="http://docbook.org/ns/docbook" exclude-result-prefixes="xsl d xhtml">

<xsl:import href="../editor/docbook2xhtml.xsl" />

<xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>

<xsl:template match="/">
    <html xmlns="http://www.w3.org/1999/xhtml"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#">
    <head>
        <title><xsl:value-of select="/*/d:title|/*/d:info/d:title" /></title>
        <link rel="edit" href="?edit" />
        <link rel="describedby" href="?describe" />
        <link rel="version-history" href="?history" />
    </head>
    <body>
        <xsl:apply-templates />
    </body>
    </html>
</xsl:template>

<xsl:template match="d:book">
    <div class="book">
        <xsl:apply-templates select="d:info|d:title" />
        <ol class="toc">
            <xsl:apply-templates mode="toc" select="d:info/following-sibling::node()|d:title/following-sibling::node()" />
        </ol>
        <xsl:apply-templates select="d:info/following-sibling::node()|d:title/following-sibling::node()" />
    </div>
</xsl:template>

<xsl:template mode="toc" match="d:chapter|d:part|d:article|d:section|d:appendix">
    <xsl:variable name="text">
        <xsl:value-of select="d:info/d:title|d:title" />
    </xsl:variable>
    <li><a href="#{replace($text, '\W','_')}"><xsl:value-of select="$text" /></a></li>
    <xsl:if test="d:section">
        <ol>
            <xsl:apply-templates mode="toc" select="d:section" />
        </ol>
    </xsl:if>
</xsl:template>

<xsl:template match="d:article">
    <article>
        <xsl:apply-templates />
    </article>
</xsl:template>

<xsl:template mode="heading" match="d:title">
    <a name="{replace(text(),'\W','_')}" />
    <xsl:value-of select="." />
    <xsl:if test="ancestor::*[2]">
        <xsl:text> </xsl:text>
        <a href="{concat(base-uri(),'?view#', replace(text(),'\W','_'))}" class="anchor">#</a>
    </xsl:if>
</xsl:template>

</xsl:stylesheet>
