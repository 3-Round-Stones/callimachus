<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="sparql">
	<xsl:output indent="no" method="html" encoding="UTF-8"/>
	<xsl:template match="/">
		<html>
			<head>
				<title></title>
			</head>
			<body>
				<xsl:apply-templates />
			</body>
		</html>
	</xsl:template>
	<xsl:template match="sparql:sparql">
		<table>
			<xsl:apply-templates select="*" />
		</table>
	</xsl:template>
	<xsl:template match="sparql:head">
		<thead>
			<xsl:apply-templates select="sparql:variable" />
		</thead>
	</xsl:template>
	<xsl:template match="sparql:variable">
		<th>
			<xsl:value-of select="@name" />
		</th>
	</xsl:template>
	<xsl:template match="sparql:boolean">
		<tbody>
			<tr>
				<td>
					<xsl:value-of select="text()" />
				</td>
			</tr>
		</tbody>
	</xsl:template>
	<xsl:template match="sparql:results">
		<tbody>
			<xsl:apply-templates select="sparql:result" />
		</tbody>
	</xsl:template>
	<xsl:template match="sparql:result">
		<xsl:variable name="current" select="."/> 
		<tr>
			<xsl:for-each select="../../sparql:head[1]/sparql:variable">
				<xsl:variable name="name" select="@name"/> 
				<td>
					<xsl:apply-templates select="$current/sparql:binding[@name=$name]" /> 
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>
	<xsl:template match="sparql:binding">
		<xsl:apply-templates select="*" />
	</xsl:template>
</xsl:stylesheet>
