<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="xhtml sparql">
	<xsl:output method="xml" />
	<xsl:param name="xslt" select="'/callimachus/manifest?template'" />
	<xsl:param name="this" />
	<xsl:param name="query" />
	<xsl:param name="template" select="false()" />
	<xsl:variable name="scheme" select="substring-before($xslt, '://')" />
	<xsl:variable name="authority" select="substring-before(substring-after($xslt, '://'), '/')" />
	<xsl:variable name="callimachus">
		<xsl:if test="$scheme and $authority">
			<xsl:value-of select="concat($scheme, '://', $authority, '/callimachus')" />
		</xsl:if>
		<xsl:if test="not($scheme) or not($authority)">
			<xsl:value-of select="'/callimachus'" />
		</xsl:if>
	</xsl:variable>
	<xsl:variable name="manifest">
		<xsl:if test="contains($xslt,'?')">
			<xsl:value-of select="substring-before($xslt, '?')" />
		</xsl:if>
		<xsl:if test="not(contains($xslt,'?'))">
			<xsl:value-of select="concat($callimachus, '/manifest')" />
		</xsl:if>
	</xsl:variable>
	<xsl:variable name="layout_xhtml" select="document(concat($manifest, '?layout'))" />
	<xsl:variable name="layout_head" select="$layout_xhtml/xhtml:html/xhtml:head|$layout_xhtml/html/head" />
	<xsl:variable name="layout_body" select="$layout_xhtml/xhtml:html/xhtml:body|$layout_xhtml/html/body" />
	<xsl:variable name="template_body" select="/xhtml:html/xhtml:body|/html/body" />

	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|text()|comment()" />
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
			<link rel="icon" href="{$manifest}?favicon" />
			<link rel="stylesheet" href="{$callimachus}/styles/content.css" />
			<link rel="stylesheet" href="{$manifest}?style" />
			<link rel="stylesheet" href="{$manifest}?colour" />
			<xsl:comment>[if lt IE 9]&gt;
				&lt;link rel="stylesheet" href="/callimachus/themes/default/ie8.css" /&gt;
				&lt;script src="//html5shim.googlecode.com/svn/trunk/html5.js"&gt;&lt;/script&gt;
			&lt;![endif]</xsl:comment>
			<xsl:if test="//form|//xhtml:form|//*[contains(@class, 'ui-widget')]">
				<link type="text/css" href="{$manifest}?jqueryui" rel="stylesheet" />
			</xsl:if>
			<xsl:if test="//*[contains(@class,'aside')]">
				<link rel="stylesheet" href="{$manifest}?aside" />
			</xsl:if>
			<xsl:apply-templates select="$layout_head/*[local-name()!='script']|comment()" />
			<xsl:apply-templates select="*[local-name()!='script']|comment()" />

			<script type="text/javascript" src="{$callimachus}/scripts/web_bundle?source">&#160;</script>
			<xsl:if test="//form|//xhtml:form">
				<script type="text/javascript" src="{$callimachus}/scripts/form_bundle?source">&#160;</script>
			</xsl:if>
			<xsl:if test="$query='create'">
				<script type="text/javascript" src="{$callimachus}/toolbox/create.js">&#160;</script>
			</xsl:if>
			<xsl:if test="$query='edit'">
				<script type="text/javascript" src="{$callimachus}/toolbox/edit.js">&#160;</script>
			</xsl:if>
			<script type="text/javascript" src="{$manifest}?source"> </script>
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
			<xsl:apply-templates mode="layout" select="$layout_body/*|$layout_body/text()|$layout_body/comment()" />
			<xsl:if test="not($layout_body)">
				<xsl:apply-templates select="*|text()|comment()" />
			</xsl:if>
		</xsl:copy>
	</xsl:template>

	<xsl:template mode="layout" match="*">
		<xsl:copy>
			<xsl:apply-templates mode="layout" select="@*|*|text()|comment()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template mode="layout" match="@*|text()|comment()">
		<xsl:copy />
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:img/@src[starts-with(., '/callimachus/')]|img/@src[starts-with(., '/callimachus/')]">
		<xsl:attribute name="{name()}">
			<xsl:value-of select="$callimachus" />
			<xsl:value-of select="substring-after(.,'/callimachus')" />
		</xsl:attribute>
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:ul[@id='tabs']">
		<xsl:if test="xhtml:li/xhtml:a[@href=concat('?',$query)]|li/a[@href=concat('?',$query)]">
			<xsl:copy>
				<xsl:apply-templates mode="layout" select="@*|*|text()|comment()" />
			</xsl:copy>
		</xsl:if>
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:ul[@id='tabs']/xhtml:li/xhtml:a[@href]|ul[@id='tabs']/li/a[@href]">
		<xsl:copy>
			<xsl:if test="@href=concat('?',$query)">
				<xsl:apply-templates mode="layout" select="@*[name()!='href' and name()!='onclick']" />
			</xsl:if>
			<xsl:if test="not(@href=concat('?',$query))">
				<xsl:apply-templates mode="layout" select="@*" />
			</xsl:if>
			<xsl:apply-templates mode="layout" select="*|text()|comment()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:div[@id='content']|div[@id='content']">
		<xsl:copy>
			<xsl:apply-templates mode="layout" select="@*" />
			<xsl:apply-templates select="$template_body/*|$template_body/comment()|$template_body/text()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:div[@id='breadcrumbs']|div[@id='breadcrumbs']">
		<xsl:if test="$template and $query='view'">
			<xsl:variable name="breadcrumb" select="*[1]" />
			<xsl:variable name="ellipsis" select="$breadcrumb/preceding-sibling::text()[1]" />
			<xsl:variable name="separator" select="$breadcrumb/following-sibling::text()[1]" />
			<xsl:variable name="here" select="*[2]" />
			<xsl:variable name="close" select="$here/following-sibling::text()[1]" />
			<div xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
					 rev="calli:hasComponent" resource="?up">
				<xsl:apply-templates mode="layout" select="@*[name()!='rev' and name()!='rel' and name()!='resource']" />
				<span>
					<span rev="calli:hasComponent" resource="?upup">
						<span rev="calli:hasComponent" resource="?upupup">
							<span rev="calli:hasComponent" resource="?upupupup">
								<span rev="calli:hasComponent" resource="?upupupupup">
									<span rev="calli:hasComponent" resource="?upupupupupup">
										<xsl:value-of select="$ellipsis" />
									</span>
									<xsl:element name="{name($breadcrumb)}">
										<xsl:attribute name="href"><xsl:text>?upupupupup</xsl:text></xsl:attribute>
										<xsl:attribute name="property"><xsl:text>rdfs:label</xsl:text></xsl:attribute>
										<xsl:apply-templates mode="layout" select="$breadcrumb/@*[name()!='href' and name()!='property']" />
									</xsl:element>
									<xsl:value-of select="$separator" />
								</span>
								<xsl:element name="{name($breadcrumb)}">
									<xsl:attribute name="href"><xsl:text>?upupupup</xsl:text></xsl:attribute>
									<xsl:attribute name="property"><xsl:text>rdfs:label</xsl:text></xsl:attribute>
									<xsl:apply-templates mode="layout" select="$breadcrumb/@*[name()!='href' and name()!='property']" />
								</xsl:element>
								<xsl:value-of select="$separator" />
							</span>
							<xsl:element name="{name($breadcrumb)}">
								<xsl:attribute name="href"><xsl:text>?upupup</xsl:text></xsl:attribute>
								<xsl:attribute name="property"><xsl:text>rdfs:label</xsl:text></xsl:attribute>
								<xsl:apply-templates mode="layout" select="$breadcrumb/@*[name()!='href' and name()!='property']" />
							</xsl:element>
							<xsl:value-of select="$separator" />
						</span>
						<xsl:element name="{name($breadcrumb)}">
							<xsl:attribute name="href"><xsl:text>?upup</xsl:text></xsl:attribute>
							<xsl:attribute name="property"><xsl:text>rdfs:label</xsl:text></xsl:attribute>
							<xsl:apply-templates mode="layout" select="$breadcrumb/@*[name()!='href' and name()!='property']" />
						</xsl:element>
						<xsl:value-of select="$separator" />
					</span>
					<xsl:element name="{name($breadcrumb)}">
						<xsl:attribute name="href"><xsl:text>?up</xsl:text></xsl:attribute>
						<xsl:attribute name="property"><xsl:text>rdfs:label</xsl:text></xsl:attribute>
						<xsl:apply-templates mode="layout" select="$breadcrumb/@*[name()!='href' and name()!='property']" />
					</xsl:element>
					<xsl:value-of select="$separator" />
				</span>
				<xsl:element name="{name($here)}">
					<xsl:attribute name="about"><xsl:text>?this</xsl:text></xsl:attribute>
					<xsl:attribute name="property"><xsl:text>rdfs:label</xsl:text></xsl:attribute>
					<xsl:apply-templates mode="layout" select="$here/@*[name()!='property' and name()!='about']" />
				</xsl:element>
				<xsl:value-of select="$close" />
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:ul[@id='nav']|ul[@id='nav']">
		<xsl:copy-of select="document(concat($callimachus, '/menu?items'))/xhtml:html/xhtml:body/node()" />
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:p[@id='resource-lastmod']|p[@id='resource-lastmod']">
		<xsl:if test="$template and ($query='view' or $query='edit')">
			<p xmlns:audit="http://www.openrdf.org/rdf/2009/auditing#" about="?this" rel="audit:revision" resource="?revision">
				<xsl:apply-templates mode="layout" select="@*" />
				<xsl:apply-templates mode="time" select="*|text()|comment()" />
			</p>
		</xsl:if>
	</xsl:template>

	<xsl:template mode="time" match="@*|text()|comment()">
		<xsl:copy />
	</xsl:template>

	<xsl:template mode="time" match="*">
		<xsl:copy>
			<xsl:apply-templates mode="layout" select="@*|*|text()|comment()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template mode="time" match="xhtml:time|time">
		<time pubdate="pubdate" property="audit:committedOn" class="abbreviated" />
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:p[@id='manifest-rights']|p[@id='manifest-rights']">
		<xsl:copy>
			<xsl:apply-templates mode="layout" select="@*" />
			<xsl:copy-of select="document(concat($manifest, '?rights'))/xhtml:html/xhtml:body/node()" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
