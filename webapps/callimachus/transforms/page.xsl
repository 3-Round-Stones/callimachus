<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#">
	<xsl:output indent="no" method="xml" />

	<xsl:param name="this" />
	<xsl:param name="query" />

	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|comment()|text()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@*|comment()">
		<xsl:copy />
	</xsl:template>

	<xsl:template match="*[@id='access']">
		<xsl:if test="*//@href[.=concat('?',$query)]">
			<xsl:copy>
				<xsl:apply-templates mode="access" select="@*|*|text()|comment()" />
			</xsl:copy>
		</xsl:if>
	</xsl:template>

	<xsl:template mode="access" match="*">
		<xsl:copy>
			<xsl:apply-templates mode="access" select="@*|*|text()|comment()" />
		</xsl:copy>
	</xsl:template>
	<xsl:template mode="access" match="@*|text()|comment()">
		<xsl:copy />
	</xsl:template>
	<xsl:template mode="access" match="xhtml:a[@href]|a[@href]">
		<xsl:copy>
			<xsl:if test="@href=concat('?',$query)">
				<xsl:apply-templates mode="access" select="@*[name()!='href' and name()!='onclick']" />
			</xsl:if>
			<xsl:if test="not(@href=concat('?',$query))">
				<xsl:apply-templates mode="access" select="@*" />
			</xsl:if>
			<xsl:apply-templates mode="access" select="*|text()|comment()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="xhtml:p[@id='resource-lastmod']|p[@id='resource-lastmod']">
		<xsl:if test="$query='view' or $query='edit' or $query='discussion' or $query='describe' or $query='history' or $query='permissions'">
			<xsl:copy>
				<xsl:apply-templates select="@*" />
				<xsl:apply-templates mode="time" select="*|text()|comment()" />
			</xsl:copy>
		</xsl:if>
	</xsl:template>

	<xsl:template mode="time" match="@*|text()|comment()">
		<xsl:copy />
	</xsl:template>

	<xsl:template mode="time" match="*">
		<xsl:copy>
			<xsl:apply-templates mode="time" select="@*|node()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template mode="time" match="xhtml:time|time">
		<xsl:variable name="url" select="concat('page-info?',$this)" />
		<xsl:copy>
			<xsl:apply-templates mode="time" select="@*|node()" />
			<xsl:apply-templates mode="time" select="document($url)//sparql:binding[@name='updated']/*/@datatype" />
			<xsl:value-of select="document($url)//sparql:binding[@name='updated']/*" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="xhtml:div[@id='breadcrumbs']|div[@id='breadcrumbs']">
		<xsl:variable name="url" select="concat('page-info?',$this)" />
		<xsl:variable name="breadcrumb" select="*[1]" />
		<xsl:variable name="here" select="*[2]" />
		<xsl:if test="$breadcrumb and $here and count(document($url)//sparql:result[sparql:binding/@name='iri']) > 1">
			<xsl:variable name="ellipsis" select="$breadcrumb/preceding-sibling::text()[1]" />
			<xsl:variable name="separator" select="$breadcrumb/following-sibling::text()[1]" />
			<xsl:variable name="close" select="$here/following-sibling::text()[1]" />
			<xsl:copy>
				<xsl:apply-templates select="@*" />
				<xsl:for-each select="document($url)//sparql:result[sparql:binding/@name='iri']">
					<xsl:if test="$this!=sparql:binding[@name='iri']/*">
						<xsl:element name="{name($breadcrumb)}">
							<xsl:attribute name="href"><xsl:value-of select="sparql:binding[@name='iri']/*" /></xsl:attribute>
							<xsl:apply-templates select="$breadcrumb/@*[name()!='href']" />
							<xsl:value-of select="sparql:binding[@name='label']/*" />
							<xsl:if test="not(sparql:binding[@name='label']/*)">
								<xsl:call-template name="substring-after-last">
									<xsl:with-param name="arg" select="sparql:binding[@name='iri']/*" />
									<xsl:with-param name="delim" select="'/'" />
								</xsl:call-template>
							</xsl:if>
						</xsl:element>
						<xsl:value-of select="$separator" />
					</xsl:if>
					<xsl:if test="$this=sparql:binding[@name='iri']/*">
						<xsl:element name="{name($here)}">
							<xsl:apply-templates select="$here/@*" />
							<xsl:value-of select="sparql:binding[@name='label']/*" />
							<xsl:if test="not(sparql:binding[@name='label']/*)">
								<xsl:call-template name="substring-after-last">
									<xsl:with-param name="arg" select="sparql:binding[@name='iri']/*" />
									<xsl:with-param name="delim" select="'/'" />
								</xsl:call-template>
							</xsl:if>
						</xsl:element>
						<xsl:value-of select="$close" />
					</xsl:if>
				</xsl:for-each>
			</xsl:copy>
		</xsl:if>
	</xsl:template>

	<xsl:template name="substring-after-last">
		<xsl:param name="arg"/>
		<xsl:param name="delim"/>
		<xsl:if test="not(contains($arg, $delim))">
			<xsl:value-of select="$arg" />
		</xsl:if>
		<xsl:if test="contains($arg, $delim)">
			<xsl:call-template name="substring-after-last">
				<xsl:with-param name="arg" select="substring-after($arg, $delim)"/>
				<xsl:with-param name="delim" select="$delim"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
