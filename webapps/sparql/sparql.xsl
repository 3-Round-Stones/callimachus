<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sparql="http://www.w3.org/2005/sparql-results#">
	<xsl:output method="xml" encoding="UTF-8"/>
	<xsl:param name="xslt" />
	<xsl:template name="substring-after-last">
		<xsl:param name="string"/>
		<xsl:param name="delimiter"/>
		<xsl:if test="not(contains($string,$delimiter))">
			<xsl:value-of select="$string"/>
		</xsl:if>
		<xsl:if test="contains($string,$delimiter)">
			<xsl:call-template name="substring-after-last">
				<xsl:with-param name="string" select="substring-after($string,$delimiter)"/>
				<xsl:with-param name="delimiter" select="$delimiter"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	<xsl:template match="/">
		<html>
			<head>
				<title>SPARQL Query Results</title>
			</head>
			<body>
				<h1>SPARQL Query Results</h1>
				<xsl:if test="not(/sparql:sparql/sparql:results/sparql:result)">
					<p>No results.</p>
				</xsl:if>
				<xsl:if test="/sparql:sparql/sparql:results/sparql:result">
					<xsl:apply-templates />
				</xsl:if>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="sparql:sparql">
		<table>
			<xsl:apply-templates select="sparql:head" />
			<xsl:apply-templates select="sparql:results" />
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
	<xsl:template match="sparql:results">
		<tbody id="results">
			<xsl:apply-templates select="sparql:result" />
		</tbody>
	</xsl:template>
	<xsl:template match="sparql:result">
		<xsl:variable name="current" select="."/> 
		<tr class="result">
			<xsl:for-each select="../../sparql:head/sparql:variable">
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
	<xsl:template match="sparql:uri">
		<a href="{text()}">
			<xsl:choose>
			<xsl:when test="contains(text(), '#')">
				<xsl:call-template name="substring-after-last">
					<xsl:with-param name="string" select="text()"/>
					<xsl:with-param name="delimiter" select="'#'"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="contains(text(), '/')">
				<xsl:call-template name="substring-after-last">
					<xsl:with-param name="string" select="text()"/>
					<xsl:with-param name="delimiter" select="'/'"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="contains(text(), ':')">
				<xsl:call-template name="substring-after-last">
					<xsl:with-param name="string" select="text()"/>
					<xsl:with-param name="delimiter" select="':'"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="text()" />
			</xsl:otherwise>
			</xsl:choose>
		</a>
	</xsl:template>
	<xsl:template match="sparql:bnode">
		<span class="bnode">
			<xsl:value-of select="text()" />
		</span>
	</xsl:template>
	<xsl:template match="sparql:literal">
		<span class="literal" title="@datatype" datatype="@datatype">
			<xsl:value-of select="text()" />
		</span>
	</xsl:template>
	<xsl:template
		match="sparql:literal[@datatype='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral']">
		<span class="literal" datatype="rdf:XMLLiteral">
			<xsl:value-of disable-output-escaping="yes" select="text()" />
		</span>
	</xsl:template>
</xsl:stylesheet>
