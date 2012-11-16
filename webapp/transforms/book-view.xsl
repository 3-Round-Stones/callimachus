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
        <style type="text/css">
            .anchor { font-size: smaller; }
            ul.toc { list-style: none; }
        </style>
    </head>
    <body>
        <xsl:apply-templates />
    </body>
    </html>
</xsl:template>

<xsl:template match="d:book">
    <div class="book">
        <xsl:apply-templates select="@xml:id" />
        <xsl:apply-templates select="d:info|d:title|d:preface" />
        <ul class="toc">
            <xsl:apply-templates mode="toc" />
        </ul>
        <xsl:apply-templates select="*[not(self::d:info|self::d:title|self::d:preface)]" />
    </div>
</xsl:template>

<xsl:template match="d:part">
    <div class="part">
        <xsl:apply-templates select="@xml:id" />
        <xsl:apply-templates select="d:info|d:title|d:preface|d:partintro" />
        <ul class="toc">
            <xsl:apply-templates mode="toc" />
        </ul>
        <xsl:apply-templates select="*[not(self::d:info|self::d:title|self::d:preface|self::d:partintro)]" />
    </div>
    <xsl:if test="following-sibling::*">
        <hr />
    </xsl:if>
</xsl:template>

<xsl:template match="d:chapter">
    <div class="chapter">
        <xsl:apply-templates select="@xml:id" />
        <xsl:apply-templates select="d:info|d:title" />
        <ul class="unstyled toc">
            <xsl:apply-templates mode="toc" />
        </ul>
        <xsl:apply-templates select="*[not(self::d:info|self::d:title)]" />
    </div>
    <xsl:if test="following-sibling::*">
        <hr />
    </xsl:if>
</xsl:template>

<xsl:template match="d:info">
    <xsl:apply-templates select="d:title" />
    <xsl:apply-templates select="d:abstract" />
</xsl:template>

<xsl:template mode="toc" match="node()" />

<xsl:template mode="toc" match="d:chapter|d:part|d:article|d:appendix|d:chapter/d:section">
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
    <li>
        <xsl:apply-templates mode="heading-prefix" select="d:info/d:title|d:title" />
        <a href="#{$id}">
            <xsl:value-of select="$text" />
        </a>
        <xsl:if test="d:chapter|d:part|d:article|d:appendix or self::d:chapter/d:section">
            <ul class="toc">
                <xsl:apply-templates mode="toc" />
            </ul>
        </xsl:if>
    </li>
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
    <xsl:apply-templates mode="heading-prefix" select="." />
    <xsl:value-of select="." />
    <xsl:if test="../.. and base-uri(.)!=base-uri(../..) or parent::d:info and base-uri(.)!=base-uri(parent::d:info/../..)">
        <xsl:text> </xsl:text>
        <a href="{concat(base-uri(),'?view#', $id)}" class="anchor">#</a>
    </xsl:if>
</xsl:template>

<xsl:template mode="heading-prefix" match="node()" />

<xsl:template mode="heading-prefix" match="d:title">
    <xsl:if test="parent::d:section/parent::d:chapter or parent::d:info/parent::d:section/parent::d:chapter">
        <xsl:if test="ancestor::d:chapter/parent::*">
            <xsl:value-of select="count(ancestor::d:chapter/preceding-sibling::*[self::d:part or self::d:article or self::d:chapter or self::d:section]) + 1" />
            <xsl:text>.</xsl:text>
        </xsl:if>
        <xsl:value-of select="count(ancestor::d:section/preceding-sibling::*[self::d:part or self::d:article or self::d:chapter or self::d:section]) + 1" />
        <xsl:text>. </xsl:text>
    </xsl:if>
    <xsl:if test="parent::d:chapter/parent::* or parent::d:info/parent::d:chapter/parent::*">
        <xsl:text>Chapter </xsl:text>
        <xsl:value-of select="count(ancestor::d:chapter/preceding-sibling::*[self::d:part or self::d:article or self::d:chapter or self::d:section]) + 1" />
        <xsl:text>. </xsl:text>
    </xsl:if>
    <xsl:if test="parent::d:article/parent::* or parent::d:info/parent::d:article/parent::*">
        <xsl:value-of select="count(ancestor::d:article/preceding-sibling::*[self::d:part or self::d:article or self::d:chapter or self::d:section]) + 1" />
        <xsl:text>. </xsl:text>
    </xsl:if>
    <xsl:if test="parent::d:part/parent::* or parent::d:info/parent::d:part/parent::*">
        <xsl:text>Part </xsl:text>
        <xsl:value-of select="count(ancestor::d:part/preceding-sibling::*[self::d:part or self::d:article or self::d:chapter or self::d:section]) + 1" />
        <xsl:text>. </xsl:text>
    </xsl:if>
</xsl:template>

</xsl:stylesheet>
