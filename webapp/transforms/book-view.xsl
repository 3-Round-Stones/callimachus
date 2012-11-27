<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns="http://www.w3.org/1999/xhtml" xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:d="http://docbook.org/ns/docbook" exclude-result-prefixes="xsl d xhtml">

<xsl:import href="article-edit-xhtml.xsl" />

<xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>

<xsl:template match="/">
    <html xmlns="http://www.w3.org/1999/xhtml"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#">
    <head>
        <title><xsl:value-of select="/*/d:title|/*/d:info/d:title" /></title>
        <link rel="edit-form" href="?edit" />
        <link rel="describedby" href="?describe" />
        <link rel="version-history" href="?history" />
        <xsl:apply-templates mode="rel" />
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

<xsl:template mode="rel" match="node()" />

<xsl:template mode="rel" match="d:actknowledgements|d:appendix|d:article|d:bibliography|d:book|d:chapter|d:colophon|d:dedication|d:glossary|d:index|d:para|d:part|d:preface|d:refentry|d:reference|d:section|d:toc|d:set|d:setindex">
    <xsl:if test="base-uri()!=base-uri(..)">
        <link rel="section" href="{base-uri()}" type="application/docbook+xml" />
    </xsl:if>
    <xsl:apply-templates mode="rel" />
</xsl:template>

<xsl:template match="d:book">
    <div class="book">
        <xsl:apply-templates select="@xml:id" />
        <xsl:apply-templates select="d:info|d:title|d:preface" />
        <nav>
            <h3>Table of Contents</h3>
            <ul class="toc">
                <xsl:apply-templates mode="toc" />
            </ul>
        </nav>
        <xsl:apply-templates select="*[not(self::d:info|self::d:title|self::d:preface)]" />
    </div>
</xsl:template>

<xsl:template match="d:part">
    <div class="part">
        <xsl:apply-templates select="@xml:id" />
        <xsl:apply-templates select="d:info|d:title|d:preface|d:partintro" />
        <nav>
            <h3>Contents</h3>
            <ul class="toc">
                <xsl:apply-templates mode="toc" />
            </ul>
        </nav>
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
        <nav>
            <h3>Contents</h3>
            <ul class="toc">
                <xsl:apply-templates mode="toc" />
            </ul>
        </nav>
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

<xsl:template mode="heading-prefix" match="*/d:chapter/d:section/d:title|*/d:chapter/d:section/d:info/d:title">
    <xsl:number format="1." level="any" count="d:chapter" />
    <xsl:number format="1. " level="multiple" count="d:section" />
</xsl:template>

<xsl:template mode="heading-prefix" match="*/d:chapter/d:title|*/d:chapter/d:info/d:title">
    <xsl:text>Chapter </xsl:text>
    <xsl:number format="1. " level="any" count="d:chapter" />
</xsl:template>

<xsl:template mode="heading-prefix" match="*/d:article/d:title|*/d:article/d:info/d:title">
    <xsl:number format="A. " level="single" count="d:part|d:article|d:chapter|d:section" />
</xsl:template>

<xsl:template mode="heading-prefix" match="*/d:appendix/d:title|*/d:appendix/d:info/d:title">
    <xsl:text>Appendix </xsl:text>
    <xsl:number format="A. " level="single" count="d:part|d:article|d:chapter|d:section" />
</xsl:template>

<xsl:template mode="heading-prefix" match="*/d:part/d:title|*/d:part/d:info/d:title">
    <xsl:text>Part </xsl:text>
    <xsl:number format="I. " level="single" count="d:part|d:article|d:chapter|d:section" />
</xsl:template>

</xsl:stylesheet>
