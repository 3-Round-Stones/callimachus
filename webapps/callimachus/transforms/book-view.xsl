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
        <xsl:apply-templates select="@xml:id" />
        <xsl:apply-templates select="d:info|d:title" />
        <ol class="toc">
            <xsl:apply-templates mode="toc" select="d:info/following-sibling::node()|d:title/following-sibling::node()" />
        </ol>
        <xsl:apply-templates select="d:info/following-sibling::node()|d:title/following-sibling::node()" />
    </div>
</xsl:template>

<xsl:template match="d:part">
    <div class="part">
        <xsl:apply-templates select="@xml:id" />
        <xsl:apply-templates select="d:info|d:title" />
        <ol class="toc">
            <xsl:apply-templates mode="toc" select="d:info/following-sibling::node()|d:title/following-sibling::node()" />
        </ol>
        <xsl:apply-templates select="d:info/following-sibling::node()|d:title/following-sibling::node()" />
    </div>
    <xsl:if test="following-sibling::*">
        <hr />
    </xsl:if>
</xsl:template>

<xsl:template mode="toc" match="d:chapter|d:part|d:article|d:section|d:appendix">
    <xsl:variable name="text">
        <xsl:value-of select="d:info/d:title|d:title" />
    </xsl:variable>
    <xsl:variable name="id">
        <xsl:choose>
            <xsl:when test="@xml:id">
                <xsl:value-of select="@xml:id" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="replace(normalize-space($text), '\W','_')" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <li><a href="#{$id}"><xsl:value-of select="$text" /></a></li>
    <xsl:if test="d:chapter|d:part|d:article|d:section|d:appendix">
        <ol>
            <xsl:apply-templates mode="toc" select="d:chapter|d:part|d:article|d:section|d:appendix" />
        </ol>
    </xsl:if>
</xsl:template>

<xsl:template match="d:article">
    <article>
        <xsl:apply-templates select="@xml:id" />
        <xsl:apply-templates />
    </article>
    <xsl:if test="following-sibling::*">
        <hr />
    </xsl:if>
</xsl:template>

<!-- FIXME this should be part of docbook2xhtml -->
<xsl:template match="d:figure">
    <figure>
        <xsl:apply-templates select="@xml:id" />
        <xsl:apply-templates select="d:mediaobject|d:caption" />
        <xsl:if test="not(d:caption)">
            <figcaption>
                <xsl:apply-templates select="d:info/d:title/node()|d:title/node()" />
            </figcaption>
        </xsl:if>
    </figure>
</xsl:template>

<xsl:template match="d:informalfigure">
    <figure>
        <xsl:apply-templates select="@xml:id" />
        <xsl:apply-templates select="d:mediaobject|d:caption" />
    </figure>
</xsl:template>

<xsl:template match="d:figure/d:caption|d:informalfigure/d:caption">
    <figcaption>
        <xsl:apply-templates />
    </figcaption>
</xsl:template>

<xsl:template mode="heading" match="d:title">
    <xsl:variable name="id">
        <xsl:choose>
            <xsl:when test="parent::d:info/../@xml:id">
                <xsl:value-of select="parent::d:info/../@xml:id" />
            </xsl:when>
            <xsl:when test="../@xml:id">
                <xsl:value-of select="../@xml:id" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="replace(normalize-space(), '\W','_')" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:if test="$id=replace(normalize-space(),'\W','_')">
        <a name="{$id}" />
    </xsl:if>
    <xsl:if test="parent::d:article/parent::* or parent::d:info/parent::d:article/parent::*">
        <xsl:text>Article </xsl:text>
        <xsl:if test="ancestor::d:part/parent::*">
            <xsl:value-of select="count(ancestor::d:part/preceding-sibling::*)" />
            <xsl:text>.</xsl:text>
        </xsl:if>
        <xsl:value-of select="count(ancestor::d:article/preceding-sibling::*)" />
        <xsl:text>. </xsl:text>
    </xsl:if>
    <xsl:if test="parent::d:part/parent::* or parent::d:info/parent::d:part/parent::*">
        <xsl:text>Part </xsl:text>
        <xsl:value-of select="count(ancestor::d:part/preceding-sibling::*)" />
        <xsl:text>. </xsl:text>
    </xsl:if>
    <xsl:value-of select="." />
    <xsl:if test="ancestor::*[3] or parent::d:info/ancestor::*[2]">
        <xsl:text> </xsl:text>
        <a href="{concat(base-uri(),'?view#', $id)}" class="anchor">#</a>
    </xsl:if>
</xsl:template>

</xsl:stylesheet>
