<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="xhtml sparql">
	<xsl:output method="xml" />
	<xsl:param name="xsltId" select="'/callimachus/manifest?template'" />
	<xsl:param name="systemId" />
	<xsl:param name="query" />
	<xsl:param name="template" select="false()" />

	<!-- Variables -->
	<xsl:variable name="scheme" select="substring-before($xsltId, '://')" />
	<xsl:variable name="authority" select="substring-before(substring-after($xsltId, '://'), '/')" />
	<xsl:variable name="callimachus">
		<xsl:if test="$scheme and $authority">
			<xsl:value-of select="concat($scheme, '://', $authority, '/callimachus')" />
		</xsl:if>
		<xsl:if test="not($scheme) or not($authority)">
			<xsl:value-of select="'/callimachus'" />
		</xsl:if>
	</xsl:variable>
	<xsl:variable name="manifest">
		<xsl:if test="contains($xsltId,'?')">
			<xsl:value-of select="substring-before($xsltId, '?')" />
		</xsl:if>
		<xsl:if test="not(contains($xsltId,'?'))">
			<xsl:value-of select="concat($callimachus, '/manifest')" />
		</xsl:if>
	</xsl:variable>
	<xsl:variable name="layout_base" select="document(concat($manifest, '?base'))/xhtml:html/xhtml:head/xhtml:base/@href" />
	<xsl:variable name="layout_xhtml" select="document(concat($manifest, '?layout'))" />
	<xsl:variable name="layout_html" select="$layout_xhtml/xhtml:html|$layout_xhtml/html" />
	<xsl:variable name="layout_head" select="$layout_xhtml/xhtml:html/xhtml:head|$layout_xhtml/html/head" />
	<xsl:variable name="layout_body" select="$layout_xhtml/xhtml:html/xhtml:body|$layout_xhtml/html/body" />
	<xsl:variable name="template_body" select="/xhtml:html/xhtml:body|/html/body" />

	<!-- Template -->
	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|text()|comment()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@*|comment()">
		<xsl:copy />
	</xsl:template>

	<xsl:template match="@src|@href|@about|@resource">
		<xsl:attribute name="{name()}">
			<xsl:call-template name="resolve-path">
				<xsl:with-param name="relative" select="." />
				<xsl:with-param name="base" select="$systemId" />
			</xsl:call-template>
		</xsl:attribute>
	</xsl:template>

	<!-- head -->
	<xsl:template match="head|xhtml:head">
		<xsl:copy>
			<xsl:call-template name="merge-attributes">
				<xsl:with-param name="one" select="." />
				<xsl:with-param name="two" select="$layout_head" />
			</xsl:call-template>
			<meta name="viewport" content="width=device-width,height=device-height,initial-scale=1.0,target-densityDpi=device-dpi"/>
			<meta http-equiv="X-UA-Compatible" content="IE=edge;chrome=1" />
			<link rel="icon" href="{$manifest}?favicon" />
			<link rel="stylesheet" href="{$callimachus}/styles/normalize.css" />
			<link rel="stylesheet" href="{$callimachus}/styles/content.css" />
			<xsl:comment>[if gt IE 6]&gt;&lt;!</xsl:comment>
			<xsl:apply-templates mode="layout" select="$layout_head/*[local-name()!='script' and local-name()!='title']|comment()" />
			<xsl:apply-templates select="*[local-name()!='script']|comment()" />
			<xsl:comment>&lt;![endif]</xsl:comment>

			<script type="text/javascript" src="{$callimachus}/scripts/web_bundle?source">&#160;</script>
			<xsl:if test="//form|//xhtml:form">
				<script type="text/javascript" src="{$callimachus}/scripts/form_bundle?source">&#160;</script>
			</xsl:if>
			<xsl:comment>[if lt IE 9]&gt;
				&lt;script src="//html5shim.googlecode.com/svn/trunk/html5.js"&gt;&lt;/script&gt;
				&lt;script src="//ie7-js.googlecode.com/svn/version/2.1(beta4)/IE9.js"&gt;&lt;/script&gt;
				&lt;script src="<xsl:value-of select="concat($callimachus,'/scripts/ie_bundle?source')" />"&gt;&lt;/script&gt;
			&lt;![endif]</xsl:comment>
			<xsl:apply-templates mode="layout" select="$layout_head/*[local-name()='script']" />
			<xsl:apply-templates select="*[local-name()='script']" />
		</xsl:copy>
	</xsl:template>

	<!-- body -->
	<xsl:template match="body|xhtml:body">
		<xsl:copy>
			<xsl:choose>
				<xsl:when test="//@id='sidebar'">
					<xsl:call-template name="merge-attributes">
						<xsl:with-param name="one" select="." />
						<xsl:with-param name="two" select="$layout_body" />
					</xsl:call-template>
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="merge-attributes">
						<xsl:with-param name="one" select="." />
						<xsl:with-param name="two" select="$layout_body" />
						<xsl:with-param name="class" select="'nosidebar'" />
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:apply-templates mode="layout" select="$layout_body/*|$layout_body/text()|$layout_body/comment()" />
			<xsl:if test="not($layout_body)">
				<xsl:apply-templates select="*|text()|comment()" />
			</xsl:if>
		</xsl:copy>
	</xsl:template>

	<!-- form -->
	<xsl:template match="form|xhtml:form">
		<xsl:apply-templates mode="form" select="." />
	</xsl:template>

	<!-- Form -->
	<xsl:template mode="form" match="@*|comment()|text()">
		<xsl:copy />
	</xsl:template>

	<xsl:template mode="form" match="*">
		<xsl:copy>
			<xsl:call-template name="data-attributes" />
			<xsl:apply-templates mode="form" select="@*|node()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template name="data-attributes">
		<xsl:apply-templates mode="data-var" select="@*"/>
		<xsl:apply-templates mode="data-expression" select="@*"/>
		<xsl:if test="text() and not(*|comment())">
			<xsl:call-template name="data-text-expression">
				<xsl:with-param name="text" select="string(.)"/>
			</xsl:call-template>
		</xsl:if>
		<xsl:if test="xhtml:option[@selected='selected'][@about or @resource] or xhtml:label[@about or @resource]/xhtml:input[@checked='checked']">
			<!-- Called to populate select/radio/checkbox -->
			<xsl:attribute name="data-options">
				<xsl:value-of select="$systemId" />
				<xsl:text>?options&amp;query=</xsl:text>
				<xsl:value-of select="$query" />
				<xsl:text>&amp;element=</xsl:text>
				<xsl:apply-templates mode="xptr-element" select="." />
			</xsl:attribute>
		</xsl:if>
		<xsl:if test="*[@about or @resource] and not(@data-construct)">
			<!-- Called when a resource URI is dropped to construct its label -->
			<xsl:attribute name="data-construct">
				<xsl:value-of select="$systemId" />
				<xsl:text>?construct&amp;query=</xsl:text>
				<xsl:value-of select="$query" />
				<xsl:text>&amp;element=</xsl:text>
				<xsl:apply-templates mode="xptr-element" select="." />
				<xsl:text>&amp;about={about}</xsl:text>
			</xsl:attribute>
		</xsl:if>
		<xsl:if test="*[@about or @resource] and .//*[contains(' rdfs:label skos:prefLabel skos:altLabel skosxl:literalForm ', concat(' ', @property, ' ')) or contains(text(), '{rdfs:label}') or contains(text(), '{skos:prefLabel}') or contains(text(), '{skos:altLabel}') or contains(text(), '{skosxl:literalForm}')] and not(@data-search)">
			<!-- Lookup possible members by label -->
			<xsl:attribute name="data-search">
				<xsl:value-of select="$systemId" />
				<xsl:text>?search&amp;query=</xsl:text>
				<xsl:value-of select="$query" />
				<xsl:text>&amp;element={xptr}&amp;q={searchTerms}</xsl:text>
			</xsl:attribute>
		</xsl:if>
		<xsl:if test="*[@about or @typeof or @resource or @property] and not(@data-add)">
			<!-- Called to insert another property value or node -->
			<xsl:attribute name="data-add">
				<xsl:value-of select="$systemId" />
				<xsl:text>?template&amp;query=</xsl:text>
				<xsl:value-of select="$query" />
				<xsl:text>&amp;element=</xsl:text>
				<xsl:apply-templates mode="xptr-element" select="." />
			</xsl:attribute>
		</xsl:if>
	</xsl:template>

	<!-- variable expressions -->
	<xsl:template mode="data-var" match="@*" />
	<xsl:template mode="data-var" match="@about|@resource|@content|@href|@src">
		<xsl:if test="starts-with(., '?')">
			<xsl:attribute name="data-var-{name()}">
				<xsl:value-of select="." />
			</xsl:attribute>
		</xsl:if>
	</xsl:template>
	<xsl:template mode="data-expression" match="@*">
		<xsl:variable name="expression">
			<xsl:value-of select="substring-before(substring-after(., '{'), '}')"/>
		</xsl:variable>
		<xsl:if test="string(.) = concat('{', $expression, '}')">
			<xsl:attribute name="data-expression-{name()}">
				<xsl:value-of select="$expression" />
			</xsl:attribute>
		</xsl:if>
	</xsl:template>
	<xsl:template name="data-text-expression">
		<xsl:param name="text" />
		<xsl:variable name="expression">
			<xsl:value-of select="substring-before(substring-after($text, '{'), '}')"/>
		</xsl:variable>
		<xsl:if test="$text = concat('{', $expression, '}')">
			<xsl:attribute name="data-text-expression">
				<xsl:value-of select="$expression" />
			</xsl:attribute>
		</xsl:if>
	</xsl:template>

	<!-- Layout -->
	<xsl:template mode="layout" match="*">
		<xsl:copy>
			<xsl:apply-templates mode="layout" select="@*|*|text()|comment()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template mode="layout" match="@*|text()|comment()">
		<xsl:copy />
	</xsl:template>

	<xsl:template mode="layout" match="@src|@href|@about|@resource">
		<xsl:attribute name="{name()}">
			<xsl:call-template name="resolve-path">
				<xsl:with-param name="relative" select="." />
				<xsl:with-param name="base" select="$layout_base" />
			</xsl:call-template>
		</xsl:attribute>
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:nav[@id='access']|nav[@id='access']">
		<xsl:if test="xhtml:a[@href=concat('?',$query)]|li/a[@href=concat('?',$query)]">
			<xsl:copy>
				<xsl:apply-templates mode="layout" select="@*|*|text()|comment()" />
			</xsl:copy>
		</xsl:if>
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:nav[@id='access']/xhtml:a[@href]|nav[@id='access']/a[@href]">
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
			<div xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:skos="http://www.w3.org/2004/02/skos/core#"
					xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
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
										<xsl:apply-templates mode="layout" select="$breadcrumb/@*[name()!='href']" />
										<span property="rdfs:label" />
										<span property="skos:prefLabel" />
										<span property="foaf:name" />
									</xsl:element>
									<xsl:value-of select="$separator" />
								</span>
								<xsl:element name="{name($breadcrumb)}">
									<xsl:attribute name="href"><xsl:text>?upupupup</xsl:text></xsl:attribute>
									<xsl:apply-templates mode="layout" select="$breadcrumb/@*[name()!='href']" />
									<span property="rdfs:label" />
									<span property="skos:prefLabel" />
									<span property="foaf:name" />
								</xsl:element>
								<xsl:value-of select="$separator" />
							</span>
							<xsl:element name="{name($breadcrumb)}">
								<xsl:attribute name="href"><xsl:text>?upupup</xsl:text></xsl:attribute>
								<xsl:apply-templates mode="layout" select="$breadcrumb/@*[name()!='href']" />
								<span property="rdfs:label" />
								<span property="skos:prefLabel" />
								<span property="foaf:name" />
							</xsl:element>
							<xsl:value-of select="$separator" />
						</span>
						<xsl:element name="{name($breadcrumb)}">
							<xsl:attribute name="href"><xsl:text>?upup</xsl:text></xsl:attribute>
							<xsl:apply-templates mode="layout" select="$breadcrumb/@*[name()!='href']" />
							<span property="rdfs:label" />
							<span property="skos:prefLabel" />
							<span property="foaf:name" />
						</xsl:element>
						<xsl:value-of select="$separator" />
					</span>
					<xsl:element name="{name($breadcrumb)}">
						<xsl:attribute name="href"><xsl:text>?up</xsl:text></xsl:attribute>
						<xsl:apply-templates mode="layout" select="$breadcrumb/@*[name()!='href']" />
						<span property="rdfs:label" />
						<span property="skos:prefLabel" />
						<span property="foaf:name" />
					</xsl:element>
					<xsl:value-of select="$separator" />
				</span>
				<xsl:element name="{name($here)}">
					<xsl:attribute name="about"><xsl:text>?this</xsl:text></xsl:attribute>
					<xsl:apply-templates mode="layout" select="$here/@*[name()!='about']" />
					<span property="rdfs:label" />
					<span property="skos:prefLabel" />
					<span property="foaf:name" />
				</xsl:element>
				<xsl:value-of select="$close" />
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:nav[@id='menu']|nav[@id='menu']">
		<xsl:copy>
			<xsl:apply-templates mode="layout" select="@*" />
			<xsl:copy-of select="document(concat($callimachus, '/menu?items'))/xhtml:html/xhtml:body/node()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:p[@id='rights']|p[@id='rights']">
		<xsl:copy>
			<xsl:apply-templates mode="layout" select="@*" />
			<xsl:copy-of select="document(concat($manifest, '?rights'))/*/node()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template mode="layout" match="xhtml:p[@id='resource-lastmod']|p[@id='resource-lastmod']">
		<xsl:if test="$template and ($query='view' or $query='edit' or $query='permissions')">
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

	<!-- Helper Functions -->
	<xsl:template name="merge-attributes">
		<xsl:param name="one" />
		<xsl:param name="two" />
		<xsl:param name="class" />
		<xsl:if test="$one/@class or $two/@class or $class">
			<xsl:attribute name="class">
				<xsl:value-of select="$one/@class" />
				<xsl:text> </xsl:text>
				<xsl:value-of select="$two/@class" />
				<xsl:text> </xsl:text>
				<xsl:value-of select="$class" />
			</xsl:attribute>
		</xsl:if>
		<xsl:apply-templates select="$one/@*[name() != 'class']" />
		<xsl:for-each select="$two/@*[name() != 'class']">
			<xsl:variable name="name" select="name()" />
			<xsl:if test="not($one/@*[name()=$name])">
				<xsl:apply-templates select="." />
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="resolve-path">
		<xsl:param name="relative" />
		<xsl:param name="base" />
		<xsl:variable name="scheme" select="substring-before($base, '://')" />
		<xsl:variable name="authority" select="substring-before(substring-after($base, '://'), '/')" />
		<xsl:variable name="path" select="substring-after(substring-after($base, '://'), $authority)" />
		<xsl:choose>
			<xsl:when test="not($scheme) or not($authority)">
				<xsl:value-of select="$relative" />
			</xsl:when>
			<xsl:when test="contains($relative, '{') or contains($relative, ' ') or contains($relative, '&lt;') or contains($relative, '&gt;') or contains($relative, '&quot;') or contains($relative, &quot;'&quot;)">
				<xsl:value-of select="$relative" />
			</xsl:when>
			<xsl:when test="contains($relative, ':') or starts-with($relative,'//')">
				<xsl:value-of select="$relative" />
			</xsl:when>
			<xsl:when test="$relative='' or starts-with($relative,'?') or starts-with($relative,'#')">
				<xsl:value-of select="$relative" />
			</xsl:when>
			<xsl:when test="starts-with($relative, '/')">
				<xsl:value-of select="concat($scheme, '://', $authority)" />
				<xsl:value-of select="$relative" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="substring-before-last">
					<xsl:with-param name="arg" select="$base" />
					<xsl:with-param name="delim" select="'/'" />
				</xsl:call-template>
				<xsl:value-of select="'/'" />
				<xsl:value-of select="$relative" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="substring-before-last">
		<xsl:param name="arg"/>
		<xsl:param name="delim"/>
		<xsl:if test="contains($arg, $delim)">
			<xsl:value-of select="substring-before($arg, $delim)" />
			<xsl:if test="contains(substring-after($arg, $delim), $delim)">
				<xsl:value-of select="$delim" />
				<xsl:call-template name="substring-before-last">
					<xsl:with-param name="arg" select="substring-after($arg, $delim)"/>
					<xsl:with-param name="delim" select="$delim"/>
				</xsl:call-template>
			</xsl:if>
		</xsl:if>
	</xsl:template>

	<!-- xptr-element -->
	<xsl:template mode="xptr-element" match="/xhtml:html/xhtml:body|/html/body">
		<xsl:text>content</xsl:text>
	</xsl:template>
	<xsl:template mode="xptr-element" match="*[@id]">
		<xsl:value-of select="@id" />
	</xsl:template>
	<xsl:template mode="xptr-element" match="*">
		<xsl:apply-templates mode="xptr-element" select=".." />
		<xsl:text>/</xsl:text>
		<xsl:value-of select="count(preceding-sibling::*) + 1" />
	</xsl:template>

</xsl:stylesheet>
