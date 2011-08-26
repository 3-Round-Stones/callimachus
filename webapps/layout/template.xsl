<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="xhtml sparql">
	<xsl:output method="xml" />
	<xsl:param name="xslt" select="'/layout/template.xsl'" />
	<xsl:param name="this" />
	<xsl:param name="query" />
	<xsl:param name="template" select="false()" />
	<xsl:variable name="layout">
		<xsl:call-template name="substring-before-last">
			<xsl:with-param name="string" select="$xslt"/>
			<xsl:with-param name="delimiter" select="'/'"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="origin">
		<xsl:call-template name="substring-before-last">
			<xsl:with-param name="string" select="$layout" />
			<xsl:with-param name="delimiter" select="'/'"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="scheme" select="substring-before($xslt, '://')" />
	<xsl:variable name="host" select="substring-before(substring-after($xslt, '://'), '/')" />
	<xsl:variable name="callimachus">
		<xsl:if test="$scheme and $host">
			<xsl:value-of select="concat($scheme, '://', $host, '/callimachus')" />
		</xsl:if>
		<xsl:if test="not($scheme) or not($host)">
			<xsl:value-of select="'/callimachus'" />
		</xsl:if>
	</xsl:variable>
	<xsl:variable name="layout_head" select="document(concat($layout, '/layout.xhtml'))/xhtml:html/xhtml:head" />
	<xsl:variable name="layout_body" select="document(concat($layout, '/layout.xhtml'))/xhtml:html/xhtml:body" />
	<xsl:variable name="template_body" select="/xhtml:html/xhtml:body|/html/body" />

	<xsl:template name="substring-before-last">
		<xsl:param name="string"/>
		<xsl:param name="delimiter"/>
		<xsl:if test="contains($string,$delimiter)">
			<xsl:value-of select="substring-before($string,$delimiter)"/>
			<xsl:if test="contains(substring-after($string,$delimiter),$delimiter)">
				<xsl:value-of select="$delimiter"/>
				<xsl:call-template name="substring-before-last">
					<xsl:with-param name="string" select="substring-after($string,$delimiter)"/>
					<xsl:with-param name="delimiter" select="$delimiter"/>
				</xsl:call-template>
			</xsl:if>
		</xsl:if>
	</xsl:template>

	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|comment()|text()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@*|comment()">
		<xsl:copy />
	</xsl:template>

	<xsl:template name="merge-attributes">
		<xsl:param name="one" />
		<xsl:param name="two" />
		<xsl:if test="$one/@class and $two/@class">
			<xsl:attribute name="class">
				<xsl:value-of select="$one/@class" />
				<xsl:text> </xsl:text>
				<xsl:value-of select="$two/@class" />
			</xsl:attribute>
		</xsl:if>
		<xsl:apply-templates select="@*[name() != 'class']" />
		<xsl:for-each select="$two/@*">
			<xsl:variable name="name" select="name()" />
			<xsl:if test="not($one/@*[name()=$name])">
				<xsl:apply-templates select="." />
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

	<!-- head -->
	<xsl:template match="head|xhtml:head">
		<xsl:copy>
			<xsl:call-template name="merge-attributes">
				<xsl:with-param name="one" select="." />
				<xsl:with-param name="two" select="$layout_head" />
			</xsl:call-template>
			<xsl:if test="//form|//xhtml:form">
				<link type="text/css" href="{$layout}/jquery-ui.css" rel="stylesheet" />
			</xsl:if>
			<xsl:if test="//*[contains(@class,'aside')]">
				<link rel="stylesheet" href="{$layout}/aside.css" />
			</xsl:if>
			<xsl:apply-templates select="$layout_head/*[local-name()!='script']|comment()" />
			<xsl:apply-templates select="*[local-name()!='script']|comment()" />

			<script type="text/javascript" src="{$callimachus}/scripts/web_bundle?source">&#160;</script>
			<xsl:if test="//form|//xhtml:form">
				<script type="text/javascript" src="{$callimachus}/scripts/form_bundle?source">&#160;</script>
			</xsl:if>
			<xsl:if test="$query='create'">
				<script type="text/javascript" src="{$callimachus}/operations/create.js">&#160;</script>
			</xsl:if>
			<xsl:if test="$query='edit'">
				<script type="text/javascript" src="{$callimachus}/operations/edit.js">&#160;</script>
			</xsl:if>
			<xsl:apply-templates select="$layout_head/*[local-name()='script']" />
			<xsl:apply-templates select="*[local-name()='script']" />
		</xsl:copy>
	</xsl:template>

	<!-- body -->
	<xsl:template match="body|xhtml:body">
		<xsl:copy>
			<xsl:call-template name="merge-attributes">
				<xsl:with-param name="one" select="." />
				<xsl:with-param name="two" select="$layout_body" />
			</xsl:call-template>
			<xsl:apply-templates select="$layout_body/*|$layout_body/text()|$layout_body/comment()" />
			<xsl:if test="not($layout_body)">
				<xsl:apply-templates select="*|text()|comment()" />
			</xsl:if>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="xhtml:ul[@id='tabs']/xhtml:li/xhtml:a[@href]">
		<xsl:copy>
			<xsl:if test="@href=concat('?',$query)">
				<xsl:apply-templates select="@*[name()!='href' and name()!='onclick']" />
			</xsl:if>
			<xsl:if test="not(@href=concat('?',$query))">
				<xsl:apply-templates select="@*" />
			</xsl:if>
			<xsl:apply-templates select="*|text()|comment()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="xhtml:div[@id='content']">
		<xsl:copy>
			<xsl:apply-templates select="@*" />
			<xsl:apply-templates select="$template_body/*|$template_body/comment()|$template_body/text()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="xhtml:ul[@id='nav']">
		<xsl:copy-of select="document(concat($callimachus, '/menu?items'))/xhtml:html/xhtml:body/node()" />
	</xsl:template>

	<xsl:template match="xhtml:p[@id='resource-lastmod']">
		<xsl:if test="$template and ($query='view' or $query='edit')">
			<p id="resource-lastmod" about="?this" rel="audit:revision" resource="?revision">This resource was last modified at 
				<time pubdate="pubdate" property="audit:committedOn" class="abbreviated" />
			</p>
		</xsl:if>
	</xsl:template>

	<xsl:template match="xhtml:p[@id='manifest-rights']">
		<xsl:copy>
			<xsl:apply-templates select="@*" />
			<xsl:copy-of select="document(concat($callimachus, '/manifest?rights'))/xhtml:html/xhtml:body/node()" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
