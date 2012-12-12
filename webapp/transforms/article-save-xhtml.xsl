<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xhtml="http://www.w3.org/1999/xhtml" 
    xmlns="http://docbook.org/ns/docbook" 
    xmlns:xl   ="http://www.w3.org/1999/xlink"
    version="1.0" 
    exclude-result-prefixes="xsl xhtml">
<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

<xsl:key name="section" match="node()[not(name()='h1' or name()='h2' or name()='h3' or name()='h4' or name()='h5' or name()='h6')]"
use="generate-id(preceding-sibling::*[name()='h1' or name()='h2' or name()='h3' or name()='h4' or name()='h5' or name()='h6'][1])"/>

<xsl:key name="h1" match="xhtml:h1" use="generate-id(preceding::xhtml:head[1])"/>
<xsl:key name="h2" match="xhtml:h2" use="generate-id(preceding::*[name()='head' or name()='h1'][1])"/>
<xsl:key name="h3" match="xhtml:h3" use="generate-id(preceding::*[name()='head' or name()='h1' or name()='h2'][1])"/>
<xsl:key name="h4" match="xhtml:h4" use="generate-id(preceding::*[name()='head' or name()='h1' or name()='h2' or name()='h3'][1])"/>
<xsl:key name="h5" match="xhtml:h5" use="generate-id(preceding::*[name()='head' or name()='h1' or name()='h2' or name()='h3' or name()='h4'][1])"/>
<xsl:key name="h6" match="xhtml:h6" use="generate-id(preceding::*[name()='head' or name()='h1' or name()='h2' or name()='h3' or name()='h4' or name()='h5'][1])"/>

<xsl:template match="text()|comment()">
    <xsl:copy />
</xsl:template>

<xsl:template match="@*">
    <xsl:attribute name="{local-name()}">
        <xsl:value-of select="." />
    </xsl:attribute>
</xsl:template>

<xsl:template match="@id|@xml:id">
    <xsl:attribute name="xml:id">
        <xsl:value-of select="." />
    </xsl:attribute>
</xsl:template>

<xsl:template match="@lang|@xml:lang">
    <xsl:attribute name="xml:lang">
        <xsl:value-of select="." />
    </xsl:attribute>
</xsl:template>

<xsl:template match="@title">
    <xsl:attribute name="xl:title">
        <xsl:value-of select="." />
    </xsl:attribute>
</xsl:template>

<xsl:template match="*">
    <xsl:element name="{local-name()}">
        <xsl:apply-templates select="@*|node()" />
    </xsl:element>
</xsl:template>

<xsl:template match="xhtml:html">
    <article version="5.0">
        <xsl:apply-templates select="xhtml:body/xhtml:h1/@id" />
        <xsl:if test="not(xhtml:body/xhtml:h1)">
            <title />
        </xsl:if>
        <xsl:apply-templates select="xhtml:body"/>
        <xsl:if test="not(xhtml:body/*)">
            <para />
        </xsl:if>
    </article>
</xsl:template>

<xsl:template match="xhtml:body">
    <xsl:apply-templates select="@*" />
    <xsl:choose>
        <xsl:when test="not(*[name()='h1' or name()='h2' or name()='h3' or name()='h4' or name()='h5' or name()='h6'])">
            <!-- No sections if there are no heading elements (article without title nor sections)-->
            <xsl:apply-templates />
        </xsl:when>
        <xsl:otherwise>
            <xsl:apply-templates select="*[name()='h1' or name()='h2' or name()='h3' or name()='h4' or name()='h5' or name()='h6'][1]/preceding-sibling::node()" />
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
    <title><xsl:apply-templates /></title>
    <xsl:apply-templates select="key('section', generate-id())" />
    <xsl:apply-templates select="key('h6', generate-id())" />
    <xsl:apply-templates select="key('h5', generate-id())" />
    <xsl:apply-templates select="key('h4', generate-id())" />
    <xsl:apply-templates select="key('h3', generate-id())" />
    <xsl:apply-templates select="key('h2', generate-id())" />
    <xsl:if test="not(following-sibling::*)">
        <para />
    </xsl:if>
</xsl:template>

<xsl:template match="xhtml:h2">
    <section>
        <xsl:apply-templates select="@id" />
        <title><xsl:apply-templates /></title>
        <xsl:apply-templates select="key('section', generate-id())" />
        <xsl:apply-templates select="key('h6', generate-id())" />
        <xsl:apply-templates select="key('h5', generate-id())" />
        <xsl:apply-templates select="key('h4', generate-id())" />
        <xsl:apply-templates select="key('h3', generate-id())" />
        <xsl:if test="not(following-sibling::*)">
            <para />
        </xsl:if>
    </section>
</xsl:template>

<xsl:template match="xhtml:h3">
    <section>
        <xsl:apply-templates select="@id" />
        <title><xsl:apply-templates /></title>
        <xsl:apply-templates select="key('section', generate-id())" />
        <xsl:apply-templates select="key('h6', generate-id())" />
        <xsl:apply-templates select="key('h5', generate-id())" />
        <xsl:apply-templates select="key('h4', generate-id())" />
        <xsl:if test="not(following-sibling::*)">
            <para />
        </xsl:if>
    </section>
</xsl:template>

<xsl:template match="xhtml:h4">
    <section>
        <xsl:apply-templates select="@id" />
        <title><xsl:apply-templates /></title>
        <xsl:apply-templates select="key('section', generate-id())" />
        <xsl:apply-templates select="key('h6', generate-id())" />
        <xsl:apply-templates select="key('h5', generate-id())" />
        <xsl:if test="not(following-sibling::*)">
            <para />
        </xsl:if>
    </section>
</xsl:template>

<xsl:template match="xhtml:h5">
    <section>
        <xsl:apply-templates select="@id" />
        <title><xsl:apply-templates /></title>
        <xsl:apply-templates select="key('section', generate-id())" />
        <xsl:apply-templates select="key('h6', generate-id())" />
        <xsl:if test="not(following-sibling::*)">
            <para />
        </xsl:if>
    </section>
</xsl:template>

<xsl:template match="xhtml:h6">
    <section>
        <xsl:apply-templates select="@id" />
        <title><xsl:apply-templates /></title>
        <xsl:apply-templates select="key('section', generate-id())" />
        <xsl:if test="not(following-sibling::*)">
            <para />
        </xsl:if>
    </section>
</xsl:template>

<!-- para elements -->
<xsl:template match="xhtml:p">
    <para>
        <xsl:apply-templates select="@id|@lang" />
        <xsl:apply-templates />
    </para>
</xsl:template>

<xsl:template match="xhtml:blockquote">
    <blockquote>
        <xsl:apply-templates />
    </blockquote>
</xsl:template>

<xsl:template match="xhtml:pre[@class='programlisting']">
    <programlisting>
        <xsl:apply-templates select="@id|@lang" />
        <xsl:apply-templates />
    </programlisting>
</xsl:template>

<xsl:template match="xhtml:pre[not(@class='programlisting')]">
    <screen>
        <xsl:apply-templates select="@id|@lang" />
        <xsl:apply-templates />
    </screen>
</xsl:template>

<!-- Hyperlinks -->
<!-- 
    link - A hypertext link
     - @xlink:href  - Identifies a link target with a URI.
     - @xlink:title - Identifies the XLink title of the link.
     - @linkend     - Points to an internal link target by identifying the value of its xml:id attribute.
  -->
<xsl:template match="xhtml:a">
    <link xl:href="{@href}">
        <xsl:if test="@title">
            <xsl:attribute name="xl:title" namespace="http://www.w3.org/1999/xlink">
                <xsl:value-of select="@title"/>
            </xsl:attribute>
        </xsl:if>                
        <xsl:apply-templates select="node()" />
    </link>
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
<xsl:template match="xhtml:figure | xhtml:p[@class='figure']">
    <informalfigure>
        <xsl:apply-templates select="@id|@lang" />
        <xsl:apply-templates />
    </informalfigure>
</xsl:template>

<xsl:template match="xhtml:figcaption |xhtml:span[@class='caption']">
    <caption>
        <simpara>
            <xsl:apply-templates />
        </simpara>
    </caption>
</xsl:template>

<xsl:template match="xhtml:figcaption/xhtml:p">
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="xhtml:img">
    <figure>
        <mediaobject>
            <xsl:call-template name="imageobject" />
        </mediaobject>
    </figure>
</xsl:template>

<xsl:template match="xhtml:p/xhtml:img">
    <inlinemediaobject>
        <xsl:call-template name="imageobject" />
    </inlinemediaobject>
</xsl:template>

<xsl:template match="xhtml:figure/xhtml:img|xhtml:p[@class='figure']/xhtml:img">
    <mediaobject>
        <xsl:call-template name="imageobject" />
    </mediaobject>
</xsl:template>

<xsl:template name="imageobject">
    <xsl:if test="@alt">
        <alt>
            <xsl:value-of select="@alt" />
        </alt>
    </xsl:if>
    <imageobject>
        <imagedata>
            <xsl:apply-templates select="@*" />
        </imagedata>
    </imageobject>
    <xsl:if test="@title">
        <caption>
            <simpara><xsl:value-of select="@title" /></simpara>
        </caption>
    </xsl:if>
</xsl:template>

<xsl:template match="xhtml:img/@alt|xhtml:img/@title|xhtml:img/@border|xhtml:img/@style" />

<xsl:template match="xhtml:img/@src">
    <xsl:attribute name="fileref">
        <xsl:value-of select="." />
    </xsl:attribute>
</xsl:template>

<xsl:template match="xhtml:img/@width">
    <xsl:if test="string-length() &gt; 0">
        <xsl:attribute name="contentwidth">
            <xsl:value-of select="concat(.,'px')" />
        </xsl:attribute>
    </xsl:if>
</xsl:template>

<xsl:template match="xhtml:img/@height">
    <xsl:if test="string-length() &gt; 0">
        <xsl:attribute name="contentdepth">
            <xsl:value-of select="concat(.,'px')" />
        </xsl:attribute>
    </xsl:if>
</xsl:template>

<xsl:template match="xhtml:img/@hspace">
    <xsl:if test="string-length() &gt; 0">
        <xsl:attribute name="width">
            <xsl:value-of select="concat(number() + number(../@width),'px')" />
        </xsl:attribute>
    </xsl:if>
</xsl:template>

<xsl:template match="xhtml:img/@vspace">
    <xsl:if test="string-length() &gt; 0">
        <xsl:attribute name="depth">
            <xsl:value-of select="concat(number() + number(../@height),'px')" />
        </xsl:attribute>
    </xsl:if>
</xsl:template>

<!-- LIST ELEMENTS -->
<xsl:template match="xhtml:ul">
    <itemizedlist>
        <xsl:apply-templates />
    </itemizedlist>
</xsl:template>

<xsl:template match="xhtml:ol">
    <orderedlist>
        <xsl:apply-templates />
    </orderedlist>
</xsl:template>

<!-- This template makes a DocBook variablelist out of an HTML definition list -->
<xsl:template match="xhtml:dl">
    <variablelist>
        <xsl:for-each select="xhtml:dt">
            <varlistentry>
                <term>
                    <xsl:apply-templates />
                </term>
                <xsl:apply-templates select="following-sibling::xhtml:dd[1]"/>
            </varlistentry>
        </xsl:for-each>
    </variablelist>
</xsl:template>

<xsl:template match="xhtml:dd">
    <listitem>
        <simpara>
            <xsl:apply-templates />
        </simpara>
    </listitem>
</xsl:template>

<xsl:template match="xhtml:li">
    <listitem>
        <simpara>
            <xsl:apply-templates />
        </simpara>
    </listitem>
</xsl:template>

<xsl:template match="xhtml:dd[xhtml:p]">
    <listitem>
        <xsl:apply-templates />
    </listitem>
</xsl:template>

<xsl:template match="xhtml:li[xhtml:p]">
    <listitem>
        <xsl:apply-templates />
    </listitem>
</xsl:template>

<xsl:template match="xhtml:caption|xhtml:tbody|xhtml:tr|xhtml:thead|xhtml:tfoot|xhtml:colgroup|xhtml:col|xhtml:td|xhtml:th">
    <xsl:element name="{local-name()}" 
                 namespace="http://docbook.org/ns/docbook">
        <xsl:apply-templates select="@id|@lang" />
        <xsl:apply-templates select="@align|@colspan|@rowspan|@valign" />
        <xsl:apply-templates />
    </xsl:element>
</xsl:template>


<!-- tables -->
<xsl:template match="xhtml:table[not(xhtml:caption)]">
    <informaltable>
        <xsl:apply-templates select="@id|@lang" />
        <xsl:apply-templates select="@summary|@width|@border|@cellspacing|@cellpadding|@frame|@rules" />
        <xsl:apply-templates />
    </informaltable>
</xsl:template>

<xsl:template match="xhtml:table[xhtml:caption]">
    <table>
        <xsl:apply-templates select="@id|@lang" />
        <xsl:apply-templates select="@summary|@width|@border|@cellspacing|@cellpadding|@frame|@rules" />
        <xsl:apply-templates />
    </table>
</xsl:template>

<!-- inline formatting -->

<xsl:template match="xhtml:code[@class='classname']">
    <classname>
        <xsl:apply-templates />
    </classname>
</xsl:template>

<xsl:template match="xhtml:code[@class='parameter']">
    <parameter>
        <xsl:apply-templates />
    </parameter>
</xsl:template>

<xsl:template match="xhtml:code[@class='filename']">
    <filename>
        <xsl:apply-templates />
    </filename>
</xsl:template>

<xsl:template match="xhtml:code[@class='function']">
    <function>
        <xsl:apply-templates />
    </function>
</xsl:template>

<xsl:template match="xhtml:code[@class='varname']">
    <varname>
        <xsl:apply-templates />
    </varname>
</xsl:template>

<xsl:template match="xhtml:code[@class='uri']">
    <uri>
        <xsl:apply-templates />
    </uri>
</xsl:template>

<xsl:template match="xhtml:code[@class='literal']">
    <literal>
        <xsl:apply-templates />
    </literal>
</xsl:template>

<xsl:template match="xhtml:code[not(@class='function' or @class='varname' or @class='parameter' or @class='filename' or @class='classname' or @class='uri' or @class='literal')]">
    <code>
        <xsl:apply-templates />
    </code>
</xsl:template>

<xsl:template match="xhtml:b">
    <emphasis role="bold">
        <xsl:apply-templates />
    </emphasis>
</xsl:template>

<xsl:template match="xhtml:strong">
    <emphasis role="strong">
        <xsl:apply-templates />
    </emphasis>
</xsl:template>

<xsl:template match="xhtml:i | xhtml:em">
    <emphasis>
        <xsl:apply-templates />
    </emphasis>
</xsl:template>

<xsl:template match="xhtml:u | xhtml:cite">
    <citetitle>
        <xsl:apply-templates />
    </citetitle>
</xsl:template>

<xsl:template match="xhtml:sup">
    <superscript>
        <xsl:apply-templates />
    </superscript>
</xsl:template>

<xsl:template match="xhtml:sub">
    <subscript>
        <xsl:apply-templates />
    </subscript>
</xsl:template>

</xsl:stylesheet>
