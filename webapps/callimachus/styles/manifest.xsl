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
				<title>Manifest</title>
			</head>
			<body>
				<xsl:apply-templates select="sparql:results/sparql:result/sparql:binding[@name='license']" />
				<xsl:apply-templates select="sparql:results/sparql:result/sparql:binding[@name='rights']" />
			</body>
		</html>
	</xsl:template>
	<xsl:template match="sparql:binding[@name='license']">
		<xsl:variable name="title" select="../sparql:binding[@name='title']/*/text()" />
		<xsl:variable name="creator" select="../sparql:binding[@name='creator']/*/text()" />
		<xsl:text>Except where otherwise noted, content on this site is licensed under the </xsl:text>
		<a rel="license" href="{*/text()}">
			<xsl:value-of select="$creator" />
			<xsl:text> </xsl:text>
			<xsl:value-of select="$title" />
			<xsl:text> License</xsl:text>
		</a>
		<br />
	</xsl:template>
	<xsl:template match="sparql:binding[@name='rights']">
		<span>
			<xsl:value-of select="*/text()" />
		</span>
	</xsl:template>
</xsl:stylesheet>
