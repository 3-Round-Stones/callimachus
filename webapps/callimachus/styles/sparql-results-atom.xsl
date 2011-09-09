<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/2005/Atom"
	xmlns:openSearch="http://a9.com/-/spec/opensearch/1.1/"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
	exclude-result-prefixes="sparql">

<xsl:param name="this" />

<xsl:key name="results" match="sparql:result" use="sparql:binding[@name='id']/*"/>
<xsl:key name="bindings" match="sparql:binding" use="../sparql:binding[@name='id']/*"/>

<xsl:template match="sparql:head" />

<xsl:template match="sparql:variable" />

<xsl:template match="sparql:boolean" />

<xsl:template match="sparql:results">
	<feed>
		<xsl:apply-templates select="sparql:result[generate-id()=generate-id(key('results',sparql:binding[@name='id']/*)[1])]" />
	</feed>
</xsl:template>

<xsl:template match="sparql:result">
	<xsl:variable name="id" select="sparql:binding[@name='id']/*/text()" />
	<entry>
		<xsl:for-each select="key('bindings',$id)">
			<xsl:variable name="name" select="@name" />
			<xsl:variable name="value" select="*" />
			<xsl:if test="not(preceding::sparql:result[sparql:binding[@name='id']/*=$id]/sparql:binding[@name=$name]/*=$value)">
				<xsl:apply-templates select="." />
			</xsl:if>
		</xsl:for-each>
	</entry>
</xsl:template>

<xsl:template match="sparql:result[sparql:binding/@name='totalResults']">
	<xsl:variable name="id" select="sparql:binding[@name='id']/*/text()" />
	<xsl:for-each select="key('bindings',$id)">
		<xsl:variable name="name" select="@name" />
		<xsl:variable name="value" select="*" />
		<xsl:if test="not(preceding::sparql:result[sparql:binding[@name='id']/*=$id]/sparql:binding[@name=$name]/*=$value)">
			<xsl:apply-templates select="." />
		</xsl:if>
	</xsl:for-each>
</xsl:template>

<xsl:template match="sparql:binding[@name='id']">
	<id>
		<xsl:value-of select="*" />
	</id>
</xsl:template>

<xsl:template match="sparql:binding[@name='totalResults']">
	<openSearch:totalResults>
		<xsl:value-of select="*" />
	</openSearch:totalResults>
</xsl:template>

<xsl:template match="sparql:binding[@name='title']">
	<title>
		<xsl:value-of select="*" />
	</title>
</xsl:template>

<xsl:template match="sparql:binding[@name='subtitle']">
	<subtitle>
		<xsl:value-of select="*" />
	</subtitle>
</xsl:template>

<xsl:template match="sparql:binding[@name='icon']">
	<icon>
		<xsl:value-of select="*" />
	</icon>
</xsl:template>

<xsl:template match="sparql:binding[@name='logo']">
	<logo>
		<xsl:value-of select="*" />
	</logo>
</xsl:template>

<xsl:template match="sparql:binding[@name='summary']">
	<summary>
		<xsl:value-of select="*" />
	</summary>
</xsl:template>

<xsl:template match="sparql:binding[@name='published']">
	<published>
		<xsl:value-of select="*" />
	</published>
</xsl:template>

<xsl:template match="sparql:binding[@name='updated']">
	<updated>
		<xsl:value-of select="*" />
	</updated>
</xsl:template>

<xsl:template match="sparql:binding[@name='rights']">
	<rights>
		<xsl:value-of select="*" />
	</rights>
</xsl:template>

<xsl:template match="sparql:binding[@name='link']">
	<link href="{*}" />
</xsl:template>

<xsl:template match="sparql:binding[@name='link_feed']">
	<link rel="alternate" type="application/atom+xml" href="{*}" />
</xsl:template>

<xsl:template match="sparql:binding[@name='content_src']">
	<content src="{*}">
		<xsl:if test="../sparql:binding[@name='content_type']">
			<xsl:attribute name="type"><xsl:value-of select="../sparql:binding[@name='content_type']/*" /></xsl:attribute>
		</xsl:if>
	</content>
</xsl:template>

<xsl:template match="sparql:binding[@name='content']">
	<content>
		<xsl:if test="../sparql:binding[@name='content_type']">
			<xsl:attribute name="type"><xsl:value-of select="../sparql:binding[@name='content_type']/*" /></xsl:attribute>
		</xsl:if>
		<xsl:apply-templates select="*" />
	</content>
</xsl:template>

<xsl:template match="sparql:binding[@name='category_term']">
	<category term="{*}">
		<xsl:if test="../sparql:binding[@name='category_scheme']">
			<xsl:attribute name="scheme"><xsl:value-of select="../sparql:binding[@name='content_scheme']/*" /></xsl:attribute>
		</xsl:if>
		<xsl:if test="../sparql:binding[@name='category_label']">
			<xsl:attribute name="label"><xsl:value-of select="../sparql:binding[@name='content_label']/*" /></xsl:attribute>
		</xsl:if>
	</category>
</xsl:template>

<xsl:template match="sparql:binding[@name='generator']">
	<generator>
		<xsl:if test="../sparql:binding[@name='generator_uri']">
			<xsl:attribute name="uri"><xsl:value-of select="../sparql:binding[@name='generator_uri']/*" /></xsl:attribute>
		</xsl:if>
		<xsl:if test="../sparql:binding[@name='generator_version']">
			<xsl:attribute name="version"><xsl:value-of select="../sparql:binding[@name='generator_version']/*" /></xsl:attribute>
		</xsl:if>
		<xsl:value-of select="*" />
	</generator>
</xsl:template>

<xsl:template match="sparql:binding[@name='author_uri']">
	<author>
		<xsl:if test="../sparql:binding[@name='author_name']">
			<name><xsl:value-of select="../sparql:binding[@name='author_name']/*" /></name>
		</xsl:if>
		<uri><xsl:value-of select="*" /></uri>
		<xsl:if test="../sparql:binding[@name='author_email']">
			<email><xsl:value-of select="../sparql:binding[@name='author_email']/*" /></email>
		</xsl:if>
	</author>
</xsl:template>

<xsl:template match="sparql:binding[@name='contributor_uri']">
	<contributor>
		<xsl:if test="../sparql:binding[@name='contributor_name']">
			<name><xsl:value-of select="../sparql:binding[@name='contributor_name']/*" /></name>
		</xsl:if>
		<uri><xsl:value-of select="*" /></uri>
		<xsl:if test="../sparql:binding[@name='contributor_email']">
			<email><xsl:value-of select="../sparql:binding[@name='contributor_email']/*" /></email>
		</xsl:if>
	</contributor>
</xsl:template>

<xsl:template match="sparql:binding[@name='reader_uri']">
	<calli:reader>
		<xsl:if test="../sparql:binding[@name='reader_name']">
			<name><xsl:value-of select="../sparql:binding[@name='reader_name']/*" /></name>
		</xsl:if>
		<uri><xsl:value-of select="*" /></uri>
		<xsl:if test="../sparql:binding[@name='reader_email']">
			<email><xsl:value-of select="../sparql:binding[@name='reader_email']/*" /></email>
		</xsl:if>
	</calli:reader>
</xsl:template>

<xsl:template match="sparql:binding[@name='editor_uri']">
	<calli:editor>
		<xsl:if test="../sparql:binding[@name='editor_name']">
			<name><xsl:value-of select="../sparql:binding[@name='editor_name']/*" /></name>
		</xsl:if>
		<uri><xsl:value-of select="*" /></uri>
		<xsl:if test="../sparql:binding[@name='editor_email']">
			<email><xsl:value-of select="../sparql:binding[@name='editor_email']/*" /></email>
		</xsl:if>
	</calli:editor>
</xsl:template>

<xsl:template match="sparql:binding[@name='administrator_uri']">
	<calli:administrator>
		<xsl:if test="../sparql:binding[@name='administrator_name']">
			<name><xsl:value-of select="../sparql:binding[@name='administrator_name']/*" /></name>
		</xsl:if>
		<uri><xsl:value-of select="*" /></uri>
		<xsl:if test="../sparql:binding[@name='administrator_email']">
			<email><xsl:value-of select="../sparql:binding[@name='administrator_email']/*" /></email>
		</xsl:if>
	</calli:administrator>
</xsl:template>

<xsl:template match="sparql:binding" />

<xsl:template match="sparql:uri">
	<xsl:value-of select="text()" />
</xsl:template>

<xsl:template match="sparql:literal">
	<xsl:value-of select="text()" />
</xsl:template>

<xsl:template
	match="sparql:literal[@datatype='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral']">
	<xsl:value-of disable-output-escaping="yes" select="text()" />
</xsl:template>
</xsl:stylesheet>
