<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	<xsl:output method="xml" encoding="UTF-8"/>
	<xsl:param name="this" />
	<xsl:param name="xslt" select="'/sparql/sparql.xsl'" />
	<xsl:variable name="sparql">
		<xsl:call-template name="substring-before-last">
			<xsl:with-param name="string" select="$xslt"/>
			<xsl:with-param name="delimiter" select="'/'"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="origin">
		<xsl:call-template name="substring-before-last">
			<xsl:with-param name="string" select="$sparql" />
			<xsl:with-param name="delimiter" select="'/'"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="profile" select="concat($origin, '/callimachus/profile')" />
	<xsl:include href="/callimachus/rdfxml.xsl" />
	<xsl:template match="/">
		<html>
			<head>
				<base href="{$this}" />
				<title>SPARQL Results</title>
				<style>
					.uri, .bnode, .literal { font-size: large; }
					.bnode, .literal { font-family: monospace; white-space: pre-wrap; }
					.predicate { font-weight: bold; }
				</style>
			</head>
			<body>
				<h1>SPARQL Results</h1>
				<xsl:apply-templates />
			</body>
		</html>
	</xsl:template>
	<xsl:template match="sparql:sparql">
		<table id="sparql">
			<xsl:apply-templates select="*" />
		</table>
	</xsl:template>
	<xsl:template match="sparql:head">
		<thead id="head">
			<xsl:apply-templates select="sparql:variable" />
		</thead>
	</xsl:template>
	<xsl:template match="sparql:variable">
		<th>
			<xsl:value-of select="@name" />
		</th>
	</xsl:template>
	<xsl:template match="sparql:boolean">
		<tbody id="boolean">
			<tr>
				<td>
					<xsl:value-of select="text()" />
				</td>
			</tr>
		</tbody>
	</xsl:template>
	<xsl:template match="sparql:results">
		<tbody id="results">
			<xsl:apply-templates select="sparql:result" />
		</tbody>
	</xsl:template>
	<xsl:template match="sparql:result">
		<xsl:variable name="current" select="."/> 
		<tr class="result">
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
	<xsl:template match="sparql:uri">
		<a href="{text()}" class="uri">
			<xsl:call-template name="curie">
				<xsl:with-param name="iri" select="text()"/>
			</xsl:call-template>
		</a>
	</xsl:template>
	<xsl:template match="sparql:bnode">
		<a class="bnode" about="_:{text()}" name="{text()}">
			<xsl:value-of select="text()" />
		</a>
	</xsl:template>
	<xsl:template match="sparql:literal">
		<span class="literal">
			<xsl:attribute name="datatype">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="@datatype" />
				</xsl:call-template>
			</xsl:attribute>
			<xsl:attribute name="title">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="@datatype" />
				</xsl:call-template>
			</xsl:attribute>
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
