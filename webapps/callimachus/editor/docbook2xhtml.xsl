<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml" 
    xmlns:d="http://docbook.org/ns/docbook" 
    xmlns:xl   ="http://www.w3.org/1999/xlink"
    exclude-result-prefixes="xsl d">
<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

<xsl:template match="text()|comment()">
    <xsl:copy />
</xsl:template>

<xsl:template match="@*">
    <xsl:attribute name="{local-name()}">
        <xsl:value-of select="." />
    </xsl:attribute>
</xsl:template>

<xsl:template match="*">
    <div class="{name()}">
        <xsl:apply-templates />
    </div>
</xsl:template>

<xsl:template match="d:remark|d:alt">
    <xsl:comment>
        <xsl:value-of select="." />
    </xsl:comment>
</xsl:template>

<xsl:template match="d:info" />
<xsl:template match="d:title" />
<xsl:template match="d:titleabbrev" />
<xsl:template match="d:subtitle" />

<xsl:template match="d:article[d:title]">
    <html>
        <head>
            <xsl:apply-templates select="d:title/preceding-sibling::node()" />
            <title><xsl:value-of select="d:title[1]" /></title>
        </head>
        <body>
            <xsl:apply-templates select="d:title/following-sibling::node()" />
        </body>
    </html>
</xsl:template>

<xsl:template match="d:article[not(d:title)]">
    <html>
        <head>
            <title />
        </head>
        <body>
            <xsl:apply-templates />
        </body>
    </html>
</xsl:template>

<xsl:template match="d:section">
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="d:section/d:title">
    <xsl:choose>
        <xsl:when test="ancestor::d:section[6]">
            <h6><xsl:apply-templates /></h6>
        </xsl:when>
        <xsl:when test="ancestor::d:section[5]">
            <h5><xsl:apply-templates /></h5>
        </xsl:when>
        <xsl:when test="ancestor::d:section[4]">
            <h4><xsl:apply-templates /></h4>
        </xsl:when>
        <xsl:when test="ancestor::d:section[3]">
            <h3><xsl:apply-templates /></h3>
        </xsl:when>
        <xsl:when test="ancestor::d:section[2]">
            <h2><xsl:apply-templates /></h2>
        </xsl:when>
        <xsl:when test="ancestor::d:section[1]">
            <h1><xsl:apply-templates /></h1>
        </xsl:when>
        <xsl:otherwise>
            <h1><xsl:apply-templates /></h1>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template match="d:para">
    <p>
        <xsl:apply-templates />
    </p>
</xsl:template>

<xsl:template match="d:blockquote">
    <xsl:apply-templates select="d:para" />
</xsl:template>

<xsl:template match="d:blockquote/d:para">
    <blockquote>
        <xsl:apply-templates />
    </blockquote>
</xsl:template>

<xsl:template match="d:programlisting">
    <pre>
        <xsl:apply-templates />
    </pre>
</xsl:template>

<!-- Hyperlinks -->
<xsl:template match="d:ulink | d:link[not(@linkend)]">
    <a href="{@url | @xl:href}">
        <xsl:if test="d:remark | @xl:title">
            <xsl:attribute name="title">
                <xsl:value-of select="d:remark | @xl:title" />
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="node()" />
    </a>
</xsl:template>

<xsl:template match="d:link">
    <a href="#{linkend}">
        <xsl:if test="d:remark and not(@xl:title)">
            <xsl:attribute name="title">
                <xsl:value-of select="d:remark" />
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates />
    </a>
</xsl:template>

<!-- Images -->
<xsl:template match="d:inlinemediaobject">
    <xsl:apply-templates select="d:imageobject" />
</xsl:template>

<xsl:template match="d:figure">
    <xsl:apply-templates select="d:mediaobject" />
</xsl:template>

<xsl:template match="d:mediaobject">
    <xsl:apply-templates select="d:imageobject" />
</xsl:template>

<xsl:template match="d:imageobject">
    <img src="{d:imagedata/@fileref}">
        <xsl:if test="contains(d:imagedata/@depth, 'px')">
            <xsl:attribute name="height">
                <xsl:value-of select="substring-before(d:imagedata/@depth,'px')"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="contains(d:imagedata/@width,'px')">
            <xsl:attribute name="width">
                <xsl:value-of select="substring-before(d:imagedata/@width,'px')"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="../d:alt">
            <xsl:attribute name="alt">
                <xsl:value-of select="../d:alt"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="../d:caption/d:para">
            <xsl:attribute name="title">
                <xsl:value-of select="../d:caption/d:para"/>
            </xsl:attribute>
        </xsl:if>
    </img>
</xsl:template>

<!-- LIST ELEMENTS -->
<xsl:template match="d:itemizedlist">
    <ul>
        <xsl:apply-templates />
    </ul>
</xsl:template>

<xsl:template match="d:orderedlist">
    <ol>
        <xsl:apply-templates />
    </ol>
</xsl:template>

<!-- This template makes a DocBook variablelist out of an HTML definition list -->
<xsl:template match="d:variablelist">
    <dl>
        <xsl:apply-templates />
    </dl>
</xsl:template>

<xsl:template match="d:varlistentry">
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="d:varlistentry/d:term">
    <dt>
        <xsl:apply-templates />
    </dt>
</xsl:template>

<xsl:template match="d:listitem[d:para]">
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="d:listitem/d:para">
    <li>
        <xsl:apply-templates />
    </li>
</xsl:template>

<xsl:template match="d:listitem[not(d:para)]">
    <li>
        <xsl:apply-templates />
    </li>
</xsl:template>

<xsl:template match="d:varlistentry/d:listitem/d:para">
    <dd>
        <xsl:apply-templates />
    </dd>
</xsl:template>

<xsl:template match="d:varlistentry/d:listitem[not(d:para)]">
    <dd>
        <xsl:apply-templates />
    </dd>
</xsl:template>

<!-- tables -->
<xsl:template match="d:informaltable[not(d:caption)]">
    <table class="table">
        <xsl:apply-templates select="*" />
    </table>
</xsl:template>

<xsl:template match="d:table[not(d:caption)]">
    <table class="table-bordered">
        <caption><xsl:apply-templates select="d:title/node()" /></caption>
        <xsl:apply-templates />
    </table>
</xsl:template>

<xsl:template match="d:table[d:caption] | d:informaltable[d:caption]">
    <table class="table-bordered">
        <xsl:apply-templates />
    </table>
</xsl:template>

<!-- For docbook table elements (which differ from HTML5 elements 
     only by namespace URI), copy them as is but change the namespace -->
<xsl:template match="d:tbody|d:tr|d:thead|d:tfoot|d:caption|d:colgroup|d:td|d:th|d:col">
    <xsl:element name="{local-name()}" 
                 namespace="http://www.w3.org/1999/xhtml">
        <xsl:apply-templates select="@*|node()" />
    </xsl:element>
</xsl:template>

<!-- bypass and process children -->
<xsl:template match="d:tgroup">
    <xsl:apply-templates select="*" />
</xsl:template>

<xsl:template match="d:row">
    <tr>
        <xsl:apply-templates />
    </tr>
</xsl:template>

<xsl:template match="d:entry">
    <td>
        <xsl:apply-templates />
    </td>
</xsl:template>

<!-- inline formatting -->
<xsl:template match="d:emphasis[@role='bold' or @role='strong']">
    <b>
        <xsl:apply-templates select="node()" />
    </b>
</xsl:template>

<xsl:template match="d:emphasis">
    <i>
        <xsl:apply-templates select="node()" />
    </i>
</xsl:template>

<xsl:template match="d:citetitle">
    <cite>
        <xsl:apply-templates />
    </cite>
</xsl:template>

<xsl:template match="d:superscript">
    <sup>
        <xsl:value-of select="."/>
    </sup>
</xsl:template>

<xsl:template match="d:subscript">
    <sub>
        <xsl:value-of select="."/>
    </sub>
</xsl:template>

<xsl:template match="d:literal">
    <code>
        <xsl:apply-templates />
    </code>
</xsl:template>

</xsl:stylesheet>
