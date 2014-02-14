<?xml version="1.0" encoding="UTF-8"?>
<!--
  - Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -
  -->
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
        <link rel="alternate" href="?docbook" title="Download Docbook" />
        <link href="http://www.docbookpublishing.com/" title="DocBookPublishing.com" />
        <xsl:apply-templates mode="rel" />
        <link href="{resolve-uri('../assets/google-code-prettify/prettify.css')}" type="text/css" rel="stylesheet" />
        <style type="text/css">
            .anchor { font-size: smaller; }
            ul.toc { list-style: none; }
        </style>
        <script type="text/javascript" src="{resolve-uri('../assets/google-code-prettify/prettify.js')}"></script>
        <script type="text/javascript">
            jQuery(function($) {
                prettyPrint();
            });
        </script>
    </head>
    <body>
        <div class="container">
            <xsl:apply-templates />
        </div>
    </body>
    </html>
</xsl:template>

<xsl:template match="/*/d:title|/*/d:info/d:title">
    <hgroup class="page-header">
        <xsl:apply-imports/>
    </hgroup>
</xsl:template>

<xsl:template mode="rel" match="node()" />

<xsl:template mode="rel" match="d:actknowledgements|d:appendix|d:article|d:bibliography|d:book|d:chapter|d:colophon|d:dedication|d:glossary|d:index|d:para|d:part|d:preface|d:refentry|d:reference|d:section|d:toc|d:set|d:setindex">
    <xsl:if test="base-uri()!=base-uri(..)">
        <link rel="section" href="{base-uri()}" type="application/docbook+xml" />
    </xsl:if>
    <xsl:apply-templates mode="rel" />
</xsl:template>

<xsl:template match="d:book">
    <xsl:apply-templates select="@xml:id" />
    <xsl:apply-templates select="d:info|d:title|d:preface" />
    <nav>
        <h3>Table of Contents</h3>
        <ul class="toc">
            <xsl:apply-templates mode="toc" />
        </ul>
    </nav>
    <xsl:apply-templates select="*[not(self::d:info|self::d:title|self::d:preface)]" />
</xsl:template>

<xsl:template match="d:part">
    <xsl:apply-templates select="@xml:id" />
    <xsl:apply-templates select="d:info|d:title|d:preface|d:partintro" />
    <nav>
        <h3>Contents</h3>
        <ul class="toc">
            <xsl:apply-templates mode="toc" />
        </ul>
    </nav>
    <xsl:apply-templates select="*[not(self::d:info|self::d:title|self::d:preface|self::d:partintro)]" />
    <xsl:if test="following-sibling::*">
        <hr />
    </xsl:if>
</xsl:template>

<xsl:template mode="toc" match="node()" />

<xsl:template mode="toc" match="d:chapter|d:part|d:article|d:appendix|d:chapter/d:section">
    <xsl:variable name="text">
        <xsl:value-of select="d:info/d:title|d:title" />
    </xsl:variable>
    <xsl:variable name="id">
        <xsl:call-template name="id">
            <xsl:with-param name="target" select="." />
        </xsl:call-template>
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

<xsl:template match="d:chapter">
    <xsl:apply-templates select="@xml:id" />
    <xsl:apply-templates select="d:info|d:title" />
    <nav>
        <h3>Contents</h3>
        <ul class="toc">
            <xsl:apply-templates mode="chapter-toc" />
        </ul>
    </nav>
    <xsl:apply-templates select="*[not(self::d:info|self::d:title)]" />
    <xsl:if test="following-sibling::*">
        <hr />
    </xsl:if>
</xsl:template>

<xsl:template match="d:info">
    <xsl:apply-templates select="d:title" />
    <xsl:apply-templates select="d:abstract" />
</xsl:template>

<xsl:template mode="chapter-toc" match="node()" />

<xsl:template mode="chapter-toc" match="d:chapter|d:part|d:article|d:appendix|d:chapter/d:section|d:chapter/d:section/d:section">
    <xsl:variable name="text">
        <xsl:value-of select="d:info/d:title|d:title" />
    </xsl:variable>
    <xsl:variable name="id">
        <xsl:call-template name="id">
            <xsl:with-param name="target" select="." />
        </xsl:call-template>
    </xsl:variable>
    <li>
        <xsl:apply-templates mode="heading-prefix" select="d:info/d:title|d:title" />
        <a href="#{$id}">
            <xsl:value-of select="$text" />
        </a>
        <xsl:if test="d:chapter|d:part|d:article|d:appendix or d:section and (self::d:chapter or parent::d:chapter)">
            <ul class="toc">
                <xsl:apply-templates mode="chapter-toc" />
            </ul>
        </xsl:if>
    </li>
</xsl:template>

<xsl:template match="d:article">
    <xsl:apply-templates select="@xml:id" />
    <xsl:apply-templates />
    <xsl:if test="following-sibling::*">
        <hr />
    </xsl:if>
</xsl:template>

<!-- FIXME this should be part of docbook2xhtml -->
<xsl:template match="d:figure">
    <figure>
        <xsl:apply-templates select="@*" />
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
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates select="d:mediaobject|d:caption" />
    </figure>
</xsl:template>

<xsl:template match="d:figure/d:caption|d:informalfigure/d:caption">
    <figcaption>
        <xsl:apply-templates select="@*" />
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
            <xsl:when test="parent::d:info">
                <xsl:call-template name="id">
                    <xsl:with-param name="target" select="parent::d:info/.." />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="id">
                    <xsl:with-param name="target" select=".." />
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:if test="not(parent::d:info/../@xml:id) and not(../@xml:id)">
        <a name="{$id}" />
    </xsl:if>
    <xsl:apply-templates mode="heading-prefix" select="." />
    <xsl:apply-templates />
    <xsl:if test="../.. and base-uri(.)!=base-uri(../..) or parent::d:info and base-uri(.)!=base-uri(parent::d:info/../..)">
        <xsl:text> </xsl:text>
        <a href="{concat(base-uri(),'?view')}" class="anchor">¶</a>
    </xsl:if>
</xsl:template>

<xsl:template mode="heading-prefix" match="node()" />

<xsl:template mode="heading-prefix" match="*/d:chapter/d:section/d:section/d:title|*/d:chapter/d:section/d:section/d:info/d:title">
    <xsl:number format="1." level="any" count="d:chapter" />
    <xsl:number format="1. " level="multiple" count="d:section" />
</xsl:template>

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

<xsl:template match="d:link[@linkend]">
    <xsl:variable name="linkend" select="@linkend" />
    <a href="#{$linkend}">
        <xsl:if test="@xml:id">
            <xsl:attribute name="name">
                <xsl:value-of select="@xml:id" />
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="d:remark">
            <xsl:attribute name="title">
                <xsl:value-of select="d:remark" />
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="count(//*[@xml:id=$linkend])=0">
            <xsl:attribute name="style">
                <xsl:value-of select="'color:red'" />
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="@*[name()!='linkend' and name()!='xml:id']" />
        <xsl:apply-templates />
    </a>
</xsl:template>

<xsl:template name="id">
    <xsl:param name="target" />
    <xsl:variable name="id" select="$target/@xml:id" />
    <xsl:variable name="title" select="replace(normalize-space($target/d:title|$target/d:info/d:title),'\W','_')" />
    <xsl:variable name="precedingTitle" select="count($target/preceding::d:title[replace(normalize-space(),'\W','_')=$title])" />

    <xsl:choose>
        <xsl:when test="$id">
            <xsl:value-of select="$id" />
        </xsl:when>
        <xsl:when test="$title and 0!=$precedingTitle">
            <xsl:value-of select="concat($title, $precedingTitle)" />
        </xsl:when>
        <xsl:when test="$title">
            <xsl:value-of select="$title" />
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="generate-id($target)" />
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

</xsl:stylesheet>
