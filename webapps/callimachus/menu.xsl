<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="xhtml sparql">
	<xsl:output method="xml" />

	<xsl:template match="sparql:sparql">
		<html>
			<head>
				<title>Menu</title>
			</head>
			<body>
				<ul id="nav">
					<xsl:apply-templates select="sparql:results/sparql:result[not(sparql:binding/@name='heading')]" />
				</ul>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="sparql:result">
		<xsl:variable name="label" select="sparql:binding[@name='label']/*/text()" />
		<li>
			<xsl:if test="sparql:binding[@name='link']">
				<a href="{sparql:binding[@name='link']/*}">
					<xsl:value-of select="$label" />
				</a>
			</xsl:if>
			<xsl:if test="not(sparql:binding[@name='link'])">
				<h3>
					<xsl:value-of select="$label" />
				</h3>
			</xsl:if>
			<xsl:if test="../sparql:result[sparql:binding[@name='heading']/*/text()=$label]">
				<ul>
					<xsl:apply-templates select="../sparql:result[sparql:binding[@name='heading']/*/text()=$label]" />
				</ul>
			</xsl:if>
		</li>
	</xsl:template>
</xsl:stylesheet>
