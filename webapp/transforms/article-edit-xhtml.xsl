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
<xsl:stylesheet 
    version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml" 
    xmlns:d="http://docbook.org/ns/docbook" 
    xmlns:xl   ="http://www.w3.org/1999/xlink"
    exclude-result-prefixes="xsl d xl">
<xsl:output media-type="application/xhtml+xml" method="xml" indent="yes" omit-xml-declaration="yes"/>

<xsl:template match="text()|comment()">
    <xsl:copy />
</xsl:template>

<xsl:template match="@*">
    <xsl:attribute name="{local-name()}">
        <xsl:value-of select="." />
    </xsl:attribute>
</xsl:template>

<!-- must use anchor mode -->
<xsl:template match="@xml:id" />

<xsl:template mode="anchor" match="@xml:id">
    <a name="{.}" />
</xsl:template>

<xsl:template match="*">
    <div class="{name()}">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </div>
</xsl:template>

<xsl:template match="d:remark|d:alt">
    <xsl:comment>
        <xsl:value-of select="." />
    </xsl:comment>
</xsl:template>

<xsl:template match="/">
    <html>
        <head>
            <title>
                <xsl:value-of select="/*/d:title|/*/d:info/d:title" />
            </title>
        </head>
        <body>
            <xsl:apply-templates />
        </body>
    </html>
</xsl:template>

<xsl:template match="d:article">
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="d:section">
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="d:info">
    <xsl:apply-templates select="d:title" />
</xsl:template>

<xsl:template match="d:info/d:title">
    <xsl:choose>
        <xsl:when test="ancestor::*[7]">
            <h6>
                <xsl:apply-templates mode="anchor" select="../../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h6>
        </xsl:when>
        <xsl:when test="ancestor::*[6]">
            <h5>
                <xsl:apply-templates mode="anchor" select="../../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h5>
        </xsl:when>
        <xsl:when test="ancestor::*[5]">
            <h4>
                <xsl:apply-templates mode="anchor" select="../../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h4>
        </xsl:when>
        <xsl:when test="ancestor::*[4]">
            <h3>
                <xsl:apply-templates mode="anchor" select="../../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h3>
        </xsl:when>
        <xsl:when test="ancestor::*[3]">
            <h2>
                <xsl:apply-templates mode="anchor" select="../../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h2>
        </xsl:when>
        <xsl:otherwise>
            <h1>
                <xsl:apply-templates mode="anchor" select="../../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h1>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template match="d:title">
    <xsl:choose>
        <xsl:when test="ancestor::*[6]">
            <h6>
                <xsl:apply-templates mode="anchor" select="../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h6>
        </xsl:when>
        <xsl:when test="ancestor::*[5]">
            <h5>
                <xsl:apply-templates mode="anchor" select="../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h5>
        </xsl:when>
        <xsl:when test="ancestor::*[4]">
            <h4>
                <xsl:apply-templates mode="anchor" select="../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h4>
        </xsl:when>
        <xsl:when test="ancestor::*[3]">
            <h3>
                <xsl:apply-templates mode="anchor" select="../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h3>
        </xsl:when>
        <xsl:when test="ancestor::*[2]">
            <h2>
                <xsl:apply-templates mode="anchor" select="../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h2>
        </xsl:when>
        <xsl:otherwise>
            <h1>
                <xsl:apply-templates mode="anchor" select="../@xml:id" />
                <xsl:apply-templates mode="heading" select="." />
            </h1>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template mode="heading" match="node()">
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="d:para">
    <p>
        <xsl:apply-templates select="@xml:lang" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </p>
</xsl:template>

<xsl:template match="d:simpara">
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="d:blockquote">
    <blockquote>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </blockquote>
</xsl:template>

<xsl:template match="d:programlisting">
    <pre>
        <code>
            <xsl:apply-templates select="@*" />
            <xsl:apply-templates mode="anchor" select="@xml:id" />
            <xsl:apply-templates />
        </code>
    </pre>
</xsl:template>

<xsl:template match="d:programlisting/@language">
    <xsl:attribute name="class">
        <xsl:text>language-</xsl:text>
        <xsl:value-of select="." />
    </xsl:attribute>
</xsl:template>

<xsl:template match="d:screen">
    <pre>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </pre>
</xsl:template>

<!-- Hyperlinks -->
<xsl:template match="d:anchor[@xml:id]">
    <a name="{@xml:id}" />
</xsl:template>

<xsl:template match="d:ulink | d:link[not(@linkend)]">
    <a href="{@url | @xl:href}">
        <xsl:if test="@xml:id">
            <xsl:attribute name="name">
                <xsl:value-of select="@xml:id" />
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="d:remark | @xl:title">
            <xsl:attribute name="title">
                <xsl:value-of select="d:remark | @xl:title" />
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates />
    </a>
</xsl:template>

<xsl:template match="d:uri[@xl:href]">
    <a href="{@xl:href}">
        <xsl:if test="@xml:id">
            <xsl:attribute name="name">
                <xsl:value-of select="@xml:id" />
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="@xl:title">
            <xsl:attribute name="title">
                <xsl:value-of select="d:remark | @xl:title" />
            </xsl:attribute>
        </xsl:if>
        <code class="uri">
            <xsl:apply-templates />
        </code>
    </a>
</xsl:template>

<xsl:template match="d:uri[not(@xl:href)]">
    <code class="uri">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:link[@linkend]">
    <a href="#{@linkend}">
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
        <xsl:apply-templates />
    </a>
</xsl:template>

<xsl:template match="d:xref[@linkend]">
    <xsl:variable name="linkend">
        <xsl:value-of select="@linkend" />
    </xsl:variable>
    <xsl:variable name="target" select="//*[@xml:id=$linkend]" />
    <xsl:variable name="label">
        <xsl:choose>
            <xsl:when test="$target/d:title">
                <xsl:value-of select="$target/d:title" />
            </xsl:when>
            <xsl:when test="$target/d:info/d:title">
                <xsl:value-of select="$target/d:info/d:title" />
            </xsl:when>
            <xsl:when test="$target/d:caption/d:simpara">
                <xsl:value-of select="$target/d:caption/d:simpara" />
            </xsl:when>
            <xsl:when test="$target/d:caption/d:para">
                <xsl:value-of select="$target/d:caption/d:para" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$linkend" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
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
        <xsl:value-of select="$label" />
    </a>
</xsl:template>

<!-- Images -->
<xsl:template match="d:figure">
    <p class="figure">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates select="d:mediaobject|d:caption" />
        <xsl:if test="not(d:caption)">
            <span class="caption">
                <xsl:apply-templates select="d:info/d:title/node()|d:title/node()" />
            </span>
        </xsl:if>
    </p>
</xsl:template>

<xsl:template match="d:informalfigure">
    <p class="figure">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates select="d:mediaobject|d:caption" />
    </p>
</xsl:template>

<xsl:template match="d:figure/d:caption[d:para or d:simpara]|d:informalfigure/d:caption[d:para or d:simpara]">
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="d:figure/d:caption/d:para|d:informalfigure/d:caption/d:para">
    <span class="caption">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </span>
</xsl:template>

<xsl:template match="d:figure/d:caption/d:simpara|d:informalfigure/d:caption/d:simpara">
    <span class="caption">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </span>
</xsl:template>

<xsl:template match="d:figure/d:caption[not(d:para or d:simpara)]|d:informalfigure/d:caption[not(d:para or d:simpara)]">
    <span class="caption">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </span>
</xsl:template>

<xsl:template match="d:mediaobject">
    <xsl:apply-templates select="d:imageobject[1]" />
</xsl:template>

<xsl:template match="d:inlinemediaobject">
    <xsl:apply-templates select="d:imageobject[1]" />
</xsl:template>

<xsl:template match="d:imageobject">
    <xsl:variable name="contentdepth" select="substring-before(d:imagedata/@contentdepth,'px')" />
    <xsl:variable name="contentwidth" select="substring-before(d:imagedata/@contentwidth,'px')" />
    <xsl:variable name="style">
        <xsl:if test="d:imagedata/@contentwidth">
            <xsl:text>width:</xsl:text>
            <xsl:value-of select="d:imagedata/@contentwidth" />
            <xsl:text>;</xsl:text>
        </xsl:if>
        <xsl:if test="d:imagedata/@contentdepth">
            <xsl:text>height:</xsl:text>
            <xsl:value-of select="d:imagedata/@contentdepth" />
            <xsl:text>;</xsl:text>
        </xsl:if>
        <xsl:if test="$contentdepth and contains(d:imagedata/@depth, 'px') and $contentwidth and contains(d:imagedata/@width,'px')">
            <xsl:text>margin:</xsl:text>
            <xsl:value-of select="(number(substring-before(d:imagedata/@depth,'px')) - number($contentdepth)) div 2"/>
            <xsl:text>px </xsl:text>
            <xsl:value-of select="(number(substring-before(d:imagedata/@width,'px')) - number($contentwidth)) div 2"/>
            <xsl:text>px;</xsl:text>
        </xsl:if>
        <xsl:if test="d:imagedata/@align='left'">
            <xsl:text>float:left;</xsl:text>
        </xsl:if>
        <xsl:if test="d:imagedata/@align='right'">
            <xsl:text>float:right;</xsl:text>
        </xsl:if>
    </xsl:variable>
    <img src="{d:imagedata/@fileref}">
        <xsl:apply-templates select="@xml:id|@xml:lang" />
        <xsl:if test="$style">
            <xsl:attribute name="style">
                <xsl:value-of select="$style" />
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="$contentdepth">
            <xsl:attribute name="height">
                <xsl:value-of select="$contentdepth"/>
            </xsl:attribute>
            <xsl:if test="contains(d:imagedata/@depth, 'px')">
                <xsl:attribute name="vspace">
                    <xsl:value-of select="(number(substring-before(d:imagedata/@depth,'px')) - number($contentdepth)) div 2"/>
                </xsl:attribute>
            </xsl:if>
        </xsl:if>
        <xsl:if test="$contentwidth">
            <xsl:attribute name="width">
                <xsl:value-of select="$contentwidth"/>
            </xsl:attribute>
            <xsl:if test="contains(d:imagedata/@width,'px')">
                <xsl:attribute name="hspace">
                    <xsl:value-of select="(number(substring-before(d:imagedata/@width,'px')) - $contentwidth) div 2"/>
                </xsl:attribute>
            </xsl:if>
        </xsl:if>
        <xsl:if test="d:imagedata/@align">
            <xsl:attribute name="align">
                <xsl:value-of select="d:imagedata/@align"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="../d:alt">
            <xsl:attribute name="alt">
                <xsl:value-of select="../d:alt"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="../d:caption/d:simpara">
            <xsl:attribute name="title">
                <xsl:value-of select="../d:caption/d:simpara"/>
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
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates />
    </ul>
</xsl:template>

<xsl:template match="d:orderedlist">
    <ol>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates />
    </ol>
</xsl:template>

<!-- This template makes a DocBook variablelist out of an HTML definition list -->
<xsl:template match="d:variablelist">
    <dl>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates />
    </dl>
</xsl:template>

<xsl:template match="d:varlistentry">
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="d:varlistentry/d:term">
    <dt>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </dt>
</xsl:template>

<xsl:template match="d:listitem">
    <li>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </li>
</xsl:template>

<xsl:template match="d:varlistentry/d:listitem">
    <dd>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </dd>
</xsl:template>

<!-- tables -->
<xsl:template match="d:informaltable[not(d:caption)]">
    <xsl:variable name="style">
        <xsl:if test="@width">
            <xsl:text>width:</xsl:text>
            <xsl:value-of select="@width" />
            <xsl:if test="not(contains(@width,'%'))">
                <xsl:text>px</xsl:text>
            </xsl:if>
            <xsl:text>;</xsl:text>
        </xsl:if>
    </xsl:variable>
    <table class="table">
        <xsl:apply-templates select="@xml:id|@xml:lang" />
        <xsl:apply-templates select="@summary|@width|@border|@cellspacing|@cellpadding|@frame|@rules" />
        <xsl:if test="$style">
            <xsl:attribute name="style">
                <xsl:value-of select="$style" />
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="*" />
    </table>
</xsl:template>

<xsl:template match="d:table[not(d:caption)]">
    <xsl:variable name="style">
        <xsl:if test="@border">
            <xsl:text>border-style:solid;border-width:</xsl:text>
            <xsl:value-of select="@border" />
            <xsl:text>px;</xsl:text>
        </xsl:if>
        <xsl:if test="@width">
            <xsl:text>width:</xsl:text>
            <xsl:value-of select="@width" />
            <xsl:if test="not(contains(@width,'%'))">
                <xsl:text>px</xsl:text>
            </xsl:if>
            <xsl:text>;</xsl:text>
        </xsl:if>
    </xsl:variable>
    <table class="table table-bordered">
        <xsl:apply-templates select="@xml:id|@xml:lang" />
        <xsl:apply-templates select="@summary|@width|@border|@cellspacing|@cellpadding|@frame|@rules" />
        <xsl:if test="$style">
            <xsl:attribute name="style">
                <xsl:value-of select="$style" />
            </xsl:attribute>
        </xsl:if>
        <caption><xsl:apply-templates select="d:title/node()|d:info/d:title/node()" /></caption>
        <xsl:apply-templates select="d:tgroup" />
    </table>
</xsl:template>

<xsl:template match="d:table[d:caption] | d:informaltable[d:caption]">
    <xsl:variable name="style">
        <xsl:if test="@border">
            <xsl:text>border-style:solid;border-width:</xsl:text>
            <xsl:value-of select="@border" />
            <xsl:text>px;</xsl:text>
        </xsl:if>
        <xsl:if test="@width">
            <xsl:text>width:</xsl:text>
            <xsl:value-of select="@width" />
            <xsl:if test="not(contains(@width,'%'))">
                <xsl:text>px</xsl:text>
            </xsl:if>
            <xsl:text>;</xsl:text>
        </xsl:if>
    </xsl:variable>
    <table class="table table-bordered">
        <xsl:apply-templates select="@xml:id|@xml:lang" />
        <xsl:apply-templates select="@summary|@width|@border|@cellspacing|@cellpadding|@frame|@rules" />
        <xsl:if test="$style">
            <xsl:attribute name="style">
                <xsl:value-of select="$style" />
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates />
    </table>
</xsl:template>

<!-- For docbook table elements (which differ from HTML5 elements 
     only by namespace URI), copy them as is but change the namespace -->
<xsl:template match="d:tbody|d:tr|d:thead|d:tfoot|d:colgroup|d:col">
    <xsl:element name="{local-name()}" 
                 namespace="http://www.w3.org/1999/xhtml">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates />
    </xsl:element>
</xsl:template>

<xsl:template match="d:caption|d:td|d:th">
    <xsl:variable name="colnum" select="1 + count(preceding-sibling::*)" />
    <xsl:variable name="style">
        <xsl:if test="@align">
            <xsl:text>text-align:</xsl:text>
            <xsl:value-of select="@align" />
            <xsl:text>;</xsl:text>
        </xsl:if>
        <xsl:if test="@valign">
            <xsl:text>vertical-align:</xsl:text>
            <xsl:value-of select="@valign" />
            <xsl:text>;</xsl:text>
        </xsl:if>
        <xsl:if test="../../../d:col[$colnum]/@width and not(@colspan) and not(preceding-sibling::*/@colspan)">
            <xsl:text>width:</xsl:text>
            <xsl:value-of select="../../../d:col[$colnum]/@width" />
            <xsl:if test="not(contains(../../../d:col[$colnum]/@width,'%'))">
                <xsl:text>px</xsl:text>
            </xsl:if>
            <xsl:text>;</xsl:text>
        </xsl:if>
    </xsl:variable>
    <xsl:element name="{local-name()}" 
                 namespace="http://www.w3.org/1999/xhtml">
        <xsl:if test="string-length($style) gt 0 and not(@style)">
            <xsl:attribute name="style">
                <xsl:value-of select="$style" />
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
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
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </td>
</xsl:template>

<!-- inline formatting -->

<xsl:template match="d:code">
    <code>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:classname">
    <code class="classname">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:command">
    <code class="command">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:envar">
    <code class="envar">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:exceptionname">
    <code class="exceptionname">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:filename">
    <code class="filename">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:function">
    <code class="function">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:initializer">
    <code class="initializer">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:interfacename">
    <code class="interfacename">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:literal">
    <code class="literal">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:methodname">
    <code class="methodname">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:modifier">
    <code class="modifier">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:ooclass">
    <code class="ooclass">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:ooexception">
    <code class="ooexception">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:oointerface">
    <code class="oointerface">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:parameter">
    <code class="parameter">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:prompt">
    <code class="prompt">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:property">
    <code class="property">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:returnvalue">
    <code class="returnvalue">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:symbol">
    <code class="symbol">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:token">
    <code class="token">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:type">
    <code class="type">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:uri">
    <code class="uri">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:userinput">
    <code class="userinput">
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="d:varname">
    <var>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </var>
</xsl:template>

<xsl:template match="d:computeroutput">
    <samp>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </samp>
</xsl:template>

<xsl:template match="d:quote">
    <q>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </q>
</xsl:template>

<xsl:template match="d:emphasis[@role='bold']">
    <b>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </b>
</xsl:template>

<xsl:template match="d:emphasis[@role='strong']">
    <strong>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </strong>
</xsl:template>

<xsl:template match="d:emphasis">
    <em>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </em>
</xsl:template>

<xsl:template match="d:citetitle">
    <cite>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </cite>
</xsl:template>

<xsl:template match="d:superscript">
    <sup>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </sup>
</xsl:template>

<xsl:template match="d:subscript">
    <sub>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates mode="anchor" select="@xml:id" />
        <xsl:apply-templates />
    </sub>
</xsl:template>

</xsl:stylesheet>
