<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns="http://docbook.org/ns/docbook" version="1.0" exclude-result-prefixes="xsl xhtml">
<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

<xsl:key name="section" match="*[not(name()='h1' or name()='h2' or name()='h3' or name()='h4' or name()='h5' or name()='h6')]"
use="generate-id(preceding-sibling::*[name()='h1' or name()='h2' or name()='h3' or name()='h4' or name()='h5' or name()='h6'][1])"/>

<xsl:key name="h1" match="xhtml:h1" use="generate-id(preceding::xhtml:head[1])"/>
<xsl:key name="h2" match="xhtml:h2" use="generate-id(preceding::*[name()='head' or name()='h1'][1])"/>
<xsl:key name="h3" match="xhtml:h3" use="generate-id(preceding::*[name()='head' or name()='h1' or name()='h2'][1])"/>
<xsl:key name="h4" match="xhtml:h4" use="generate-id(preceding::*[name()='head' or name()='h1' or name()='h2' or name()='h3'][1])"/>
<xsl:key name="h5" match="xhtml:h5" use="generate-id(preceding::*[name()='head' or name()='h1' or name()='h2' or name()='h3' or name()='h4'][1])"/>
<xsl:key name="h6" match="xhtml:h6" use="generate-id(preceding::*[name()='head' or name()='h1' or name()='h2' or name()='h3' or name()='h4' or name()='h5'][1])"/>

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
	<xsl:comment><xsl:text>&lt;</xsl:text><xsl:value-of select="name()"/><xsl:text>&gt;</xsl:text></xsl:comment>
	<xsl:apply-templates select="*|text()|comment()" />
	<xsl:comment><xsl:text>&lt;/</xsl:text><xsl:value-of select="name()"/><xsl:text>&gt;</xsl:text></xsl:comment>
</xsl:template>

<xsl:template match="xhtml:html">
	<article>
		<xsl:apply-templates select="xhtml:body"/>
	</article>
</xsl:template>

<xsl:template match="xhtml:body">
	<xsl:variable name="before" select="*[name()='h1' or name()='h2' or name()='h3' or name()='h4' or name()='h5' or name()='h6'][1]/preceding-sibling::*" />
	<xsl:choose>
		<xsl:when test="not(*[name()='h1' or name()='h2' or name()='h3' or name()='h4' or name()='h5' or name()='h6'])">
			<section>
				<xsl:apply-templates select="node()" />
			</section>
		</xsl:when>
		<xsl:when test="$before">
			<section>
				<xsl:apply-templates select="@*" />
				<xsl:apply-templates select="$before" />
				<xsl:apply-templates select="key('h6', generate-id(/xhtml:html/xhtml:head))" />
				<xsl:apply-templates select="key('h5', generate-id(/xhtml:html/xhtml:head))" />
				<xsl:apply-templates select="key('h4', generate-id(/xhtml:html/xhtml:head))" />
				<xsl:apply-templates select="key('h3', generate-id(/xhtml:html/xhtml:head))" />
				<xsl:apply-templates select="key('h2', generate-id(/xhtml:html/xhtml:head))" />
				<xsl:apply-templates select="key('h1', generate-id(/xhtml:html/xhtml:head))" />
			</section>
		</xsl:when>
		<xsl:otherwise>
			<xsl:apply-templates select="key('h6', generate-id(/xhtml:html/xhtml:head))" />
			<xsl:apply-templates select="key('h5', generate-id(/xhtml:html/xhtml:head))" />
			<xsl:apply-templates select="key('h4', generate-id(/xhtml:html/xhtml:head))" />
			<xsl:apply-templates select="key('h3', generate-id(/xhtml:html/xhtml:head))" />
			<xsl:apply-templates select="key('h2', generate-id(/xhtml:html/xhtml:head))" />
			<xsl:apply-templates select="key('h1', generate-id(/xhtml:html/xhtml:head))" />
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="xhtml:h1">
	<section>
		<title><xsl:apply-templates select="@*|node()" /></title>
		<xsl:apply-templates select="key('section', generate-id())" />
		<xsl:apply-templates select="key('h6', generate-id())" />
		<xsl:apply-templates select="key('h5', generate-id())" />
		<xsl:apply-templates select="key('h4', generate-id())" />
		<xsl:apply-templates select="key('h3', generate-id())" />
		<xsl:apply-templates select="key('h2', generate-id())" />
	</section>
</xsl:template>

<xsl:template match="xhtml:h2">
	<section>
		<title><xsl:apply-templates select="@*|node()" /></title>
		<xsl:apply-templates select="key('section', generate-id())" />
		<xsl:apply-templates select="key('h6', generate-id())" />
		<xsl:apply-templates select="key('h5', generate-id())" />
		<xsl:apply-templates select="key('h4', generate-id())" />
		<xsl:apply-templates select="key('h3', generate-id())" />
	</section>
</xsl:template>

<xsl:template match="xhtml:h3">
	<section>
		<title><xsl:apply-templates select="@*|node()" /></title>
		<xsl:apply-templates select="key('section', generate-id())" />
		<xsl:apply-templates select="key('h6', generate-id())" />
		<xsl:apply-templates select="key('h5', generate-id())" />
		<xsl:apply-templates select="key('h4', generate-id())" />
	</section>
</xsl:template>

<xsl:template match="xhtml:h4">
	<section>
		<title><xsl:apply-templates select="@*|node()" /></title>
		<xsl:apply-templates select="key('section', generate-id())" />
		<xsl:apply-templates select="key('h6', generate-id())" />
		<xsl:apply-templates select="key('h5', generate-id())" />
	</section>
</xsl:template>

<xsl:template match="xhtml:h5">
	<section>
		<title><xsl:apply-templates select="@*|node()" /></title>
		<xsl:apply-templates select="key('section', generate-id())" />
		<xsl:apply-templates select="key('h6', generate-id())" />
	</section>
</xsl:template>

<xsl:template match="xhtml:h6">
	<section>
		<title><xsl:apply-templates select="@*|node()" /></title>
		<xsl:apply-templates select="key('section', generate-id())" />
	</section>
</xsl:template>

<!-- para elements -->
<xsl:template match="xhtml:p">
	<para>
		<xsl:apply-templates select="node()" />
	</para>
</xsl:template>

<xsl:template match="xhtml:blockquote">
	<blockquote>
		<para>
			<xsl:apply-templates select="node()" />
		</para>
	</blockquote>
</xsl:template>

<xsl:template match="xhtml:pre">
	<programlisting>
		<xsl:apply-templates select="node()" />
	</programlisting>
</xsl:template>

<xsl:template match="xhtml:code">
	<programlisting>
		<xsl:apply-templates select="node()" />
	</programlisting>
</xsl:template>

<!-- Hyperlinks -->
<xsl:template match="xhtml:a">
	<ulink url="{@href}">
		<xsl:apply-templates select="node()" />
		<xsl:if test="@title">
			<remark><xsl:value-of select="@title" /></remark>
		</xsl:if>
	</ulink>
</xsl:template>

<xsl:template match="xhtml:a[starts-with(@href,'#')]">
	<link linkend="{substring-after(@href,'#')}">
		<xsl:apply-templates select="node()" />
		<xsl:if test="@title">
			<remark><xsl:value-of select="@title" /></remark>
		</xsl:if>
	</link>
</xsl:template>

<!-- Images -->
<xsl:template match="xhtml:img">
	<xsl:choose>
		<xsl:when test="boolean(parent::xhtml:p)">
			<inlinemediaobject>
				<xsl:call-template name="imageobject" />
			</inlinemediaobject>
		</xsl:when>
		<xsl:otherwise>
			<figure>
				<mediaobject>
					<xsl:call-template name="imageobject" />
				</mediaobject>
			</figure>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template name="imageobject">
	<imageobject>
		<imagedata fileref="{@src}">
			<xsl:if test="@height != ''">
				<xsl:attribute name="depth">
					<xsl:value-of select="concat(@height,'px')"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:if test="@width != ''">
				<xsl:attribute name="width">
					<xsl:value-of select="concat(@width,'px')"/>
				</xsl:attribute>
			</xsl:if>
		</imagedata>
	</imageobject>
	<xsl:if test="@alt">
		<alt>
			<xsl:value-of select="@alt" />
		</alt>
	</xsl:if>
	<xsl:if test="@title">
		<caption>
			<para><xsl:value-of select="@title" /></para>
		</caption>
	</xsl:if>
</xsl:template>

<!-- LIST ELEMENTS -->
<xsl:template match="xhtml:ul">
	<itemizedlist>
		<xsl:apply-templates select="node()" />
	</itemizedlist>
</xsl:template>

<xsl:template match="xhtml:ol">
	<orderedlist>
		<xsl:apply-templates select="node()" />
	</orderedlist>
</xsl:template>

<!-- This template makes a DocBook variablelist out of an HTML definition list -->
<xsl:template match="xhtml:dl">
	<variablelist>
		<xsl:for-each select="xhtml:dt">
			<varlistentry>
				<term>
					<xsl:apply-templates select="node()" />
				</term>
				<listitem>
					<xsl:apply-templates select="following-sibling::xhtml:dd[1]"/>
				</listitem>
			</varlistentry>
		</xsl:for-each>
	</variablelist>
</xsl:template>

<xsl:template match="xhtml:dd">
	<xsl:apply-templates select="node()" />
</xsl:template>

<xsl:template match="xhtml:li">
	<listitem>
		<xsl:apply-templates select="node()" />
	</listitem>
</xsl:template>

<!-- tables -->
<xsl:template match="xhtml:table">
	<informaltable>
		<xsl:apply-templates select="node()" />
	</informaltable>
</xsl:template>

<xsl:template match="xhtml:caption">
	<caption>
		<xsl:apply-templates select="node()" />
	</caption>
</xsl:template>

<xsl:template match="xhtml:colgroup">
	<colgroup>
		<xsl:apply-templates select="node()" />
	</colgroup>
</xsl:template>

<xsl:template match="xhtml:col">
	<col>
		<xsl:apply-templates select="node()" />
	</col>
</xsl:template>

<xsl:template match="xhtml:thead">
	<thead>
		<xsl:apply-templates select="node()" />
	</thead>
</xsl:template>

<xsl:template match="xhtml:tr">
	<tr>
		<xsl:apply-templates select="node()" />
	</tr>
</xsl:template>

<xsl:template match="xhtml:th">
	<th>
		<xsl:apply-templates select="node()" />
	</th>
</xsl:template>

<xsl:template match="xhtml:tbody">
	<tbody>
		<xsl:apply-templates select="node()" />
	</tbody>
</xsl:template>

<xsl:template match="xhtml:td">
	<td>
		<xsl:apply-templates select="node()" />
	</td>
</xsl:template>

<xsl:template match="xhtml:tfoot">
	<tfoot>
		<xsl:apply-templates select="node()" />
	</tfoot>
</xsl:template>

<!-- inline formatting -->
<xsl:template match="xhtml:b | xhtml:strong">
	<emphasis role="bold">
		<xsl:apply-templates select="node()" />
	</emphasis>
</xsl:template>

<xsl:template match="xhtml:i | xhtml:em">
	<emphasis>
		<xsl:apply-templates select="node()" />
	</emphasis>
</xsl:template>

<xsl:template match="xhtml:u | xhtml:cite">
	<citetitle>
		<xsl:apply-templates select="node()" />
	</citetitle>
</xsl:template>

<xsl:template match="xhtml:sup">
	<superscript>
		<xsl:value-of select="."/>
	</superscript>
</xsl:template>

<xsl:template match="xhtml:sub">
	<subscript>
		<xsl:value-of select="."/>
	</subscript>
</xsl:template>

<xsl:template match="xhtml:p/xhtml:code">
	<literal>
		<xsl:apply-templates select="node()" />
	</literal>
</xsl:template>

<xsl:template match="xhtml:a/xhtml:code">
	<literal>
		<xsl:apply-templates select="node()" />
	</literal>
</xsl:template>

<xsl:template match="xhtml:li/xhtml:code">
	<literal>
		<xsl:apply-templates select="node()" />
	</literal>
</xsl:template>

</xsl:stylesheet>
