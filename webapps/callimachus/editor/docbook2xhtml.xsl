<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns="http://www.w3.org/1999/xhtml" xmlns:d="http://docbook.org/ns/docbook" exclude-result-prefixes="xsl d">
<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

<xsl:template match="comment()">
    <xsl:copy />
</xsl:template>

<xsl:template match="@*">
    <xsl:comment>
        <xsl:value-of select="name()" />
        <xsl:text>="</xsl:text>
        <xsl:value-of select="." />
        <xsl:text>"</xsl:text>
    </xsl:comment>
</xsl:template>

<xsl:template match="@id">
    <xsl:attribute name="id">
        <xsl:value-of select="." />
    </xsl:attribute>
</xsl:template>

<xsl:template match="@lang|@xml:lang">
    <xsl:attribute name="lang">
        <xsl:value-of select="." />
    </xsl:attribute>
</xsl:template>

<xsl:template match="*">
    <div class="{name()}">
        <xsl:apply-templates />
    </div>
</xsl:template>

<xsl:template match="d:remark">
    <xsl:comment>
        <xsl:value-of select="." />
    </xsl:comment>
</xsl:template>

<xsl:template match="d:article">
    <html>
        <head>
            <title><xsl:value-of select="//d:title[1]" /></title>
        </head>
        <body>
            <xsl:apply-templates />
        </body>
    </html>
</xsl:template>

<xsl:template match="d:section">
    <xsl:apply-templates select="node()" />
</xsl:template>

<xsl:template match="d:title">
    <xsl:choose>
        <xsl:when test="ancestor::d:section[6]">
            <h6><xsl:apply-templates select="node()" /></h6>
        </xsl:when>
        <xsl:when test="ancestor::d:section[5]">
            <h5><xsl:apply-templates select="node()" /></h5>
        </xsl:when>
        <xsl:when test="ancestor::d:section[4]">
            <h4><xsl:apply-templates select="node()" /></h4>
        </xsl:when>
        <xsl:when test="ancestor::d:section[3]">
            <h3><xsl:apply-templates select="node()" /></h3>
        </xsl:when>
        <xsl:when test="ancestor::d:section[2]">
            <h2><xsl:apply-templates select="node()" /></h2>
        </xsl:when>
        <xsl:when test="ancestor::d:section[1]">
            <h1><xsl:apply-templates select="node()" /></h1>
        </xsl:when>
        <xsl:otherwise>
            <h1><xsl:apply-templates select="node()" /></h1>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template match="d:para">
    <p>
        <xsl:apply-templates select="node()" />
    </p>
</xsl:template>

<xsl:template match="d:blockquote">
    <blockquote>
        <xsl:apply-templates select="d:para" />
    </blockquote>
</xsl:template>

<xsl:template match="d:programlisting">
    <pre>
        <xsl:apply-templates select="node()" />
    </pre>
</xsl:template>

<!-- Hyperlinks -->
<xsl:template match="d:ulink">
    <a href="{@url}">
        <xsl:if test="d:remark">
            <xsl:attribute name="title">
                <xsl:value-of select="d:remark" />
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="node()" />
    </a>
</xsl:template>

<xsl:template match="d:link">
    <a href="#{linkend}">
        <xsl:if test="d:remark">
            <xsl:attribute name="title">
                <xsl:value-of select="d:remark" />
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="node()" />
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
        <xsl:apply-templates select="node()" />
    </ul>
</xsl:template>

<xsl:template match="d:orderedlist">
    <ol>
        <xsl:apply-templates select="node()" />
    </ol>
</xsl:template>

<!-- This template makes a DocBook variablelist out of an HTML definition list -->
<xsl:template match="d:variablelist">
    <dl>
        <xsl:for-each select="d:varlistentry">
            <dt>
                <xsl:apply-templates select="d:term" />
            </dt>
            <dd>
                <xsl:apply-templates select="d:listitem/node()"/>
            </dd>
        </xsl:for-each>
    </dl>
</xsl:template>

<xsl:template match="d:listitem">
    <li>
        <xsl:apply-templates select="node()" />
    </li>
</xsl:template>

<!-- tables -->
<xsl:template match="d:informaltable">
    <table>
        <xsl:apply-templates select="node()" />
    </table>
</xsl:template>

<xsl:template match="d:caption">
    <caption>
        <xsl:apply-templates select="node()" />
    </caption>
</xsl:template>

<xsl:template match="d:colgroup">
    <colgroup>
        <xsl:apply-templates select="node()" />
    </colgroup>
</xsl:template>

<xsl:template match="d:col">
    <col>
        <xsl:apply-templates select="node()" />
    </col>
</xsl:template>

<xsl:template match="d:thead">
    <thead>
        <xsl:apply-templates select="node()" />
    </thead>
</xsl:template>

<xsl:template match="d:tr|d:row">
    <tr>
        <xsl:apply-templates select="node()" />
    </tr>
</xsl:template>

<xsl:template match="d:th">
    <th>
        <xsl:apply-templates select="node()" />
    </th>
</xsl:template>

<xsl:template match="d:tbody">
    <tbody>
        <xsl:apply-templates select="node()" />
    </tbody>
</xsl:template>

<xsl:template match="d:td">
    <td>
        <xsl:apply-templates select="node()" />
    </td>
</xsl:template>

<xsl:template match="d:tfoot">
    <tfoot>
        <xsl:apply-templates select="node()" />
    </tfoot>
</xsl:template>

<!-- inline formatting -->
<xsl:template match="d:emphasis[@role='bold']">
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
        <xsl:apply-templates select="node()" />
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
        <xsl:apply-templates select="node()" />
    </code>
</xsl:template>

</xsl:stylesheet>
