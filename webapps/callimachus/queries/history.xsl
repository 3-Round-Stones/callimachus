<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sparql="http://www.w3.org/2005/sparql-results#">
	<xsl:output method="xml" encoding="UTF-8"/>
	<xsl:param name="xslt" />
	<xsl:variable name="host" select="substring-before(substring-after($xslt, '://'), '/')" />
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
				<title><xsl:value-of select="sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='label']/*" /></title>
			</head>
			<body>
				<h1><xsl:value-of select="sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='label']/*" /></h1>
				<xsl:if test="not(/sparql:sparql/sparql:results/sparql:result)">
					<p>No changes have ever been made.</p>
				</xsl:if>
				<xsl:if test="/sparql:sparql/sparql:results/sparql:result">
					<xsl:apply-templates />
				</xsl:if>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="sparql:sparql">
		<xsl:apply-templates select="sparql:results" />
	</xsl:template>
	<xsl:template match="sparql:results">
		<div id="results">
			<xsl:apply-templates select="sparql:result" />
		</div>
	</xsl:template>
	<xsl:template match="sparql:result">
		<xsl:if test="not(substring-before(sparql:binding[@name='modified']/*, 'T')=substring-before(preceding-sibling::*[1]/sparql:binding[@name='modified']/*, 'T'))">
			<xsl:if test="preceding-sibling::*[1]/sparql:binding[@name='modified']/*">
				<xsl:text disable-output-escaping="yes">&lt;/ul&gt;</xsl:text>
			</xsl:if>
			<h2 class="date-locale"><xsl:value-of select="substring-before(sparql:binding[@name='modified']/*, 'T')" /></h2>
			<xsl:text disable-output-escaping="yes">&lt;ul&gt;</xsl:text>
		</xsl:if>
		<li class="result">
			<span class="date-locale"><xsl:value-of select="substring-after(sparql:binding[@name='modified']/*, 'T')" /></span>
			<xsl:text>..</xsl:text>
			<a>
				<xsl:if test="sparql:binding[@name='user']">
					<xsl:attribute name="href">
						<xsl:value-of select="sparql:binding[@name='user']/*" />
					</xsl:attribute>
				</xsl:if>
				<xsl:apply-templates select="sparql:binding[@name='name']/*" />
				<xsl:if test="not(sparql:binding[@name='name'])">
					<xsl:value-of select="sparql:binding[@name='user']/*" />
				</xsl:if>
			</a>
			<xsl:if test="sparql:binding[@name='note']">
				<xsl:text> (</xsl:text>
				<xsl:value-of select="sparql:binding[@name='note']/*" />
				<xsl:text>)</xsl:text>
			</xsl:if>
		</li>
		<xsl:if test="not(following-sibling::*[1]/sparql:binding[@name='modified']/*)">
			<xsl:text disable-output-escaping="yes">&lt;/ul&gt;</xsl:text>
		</xsl:if>
	</xsl:template>
	<xsl:template match="sparql:binding">
		<xsl:apply-templates select="*" />
	</xsl:template>
	<xsl:template match="sparql:uri">
		<span class="uri">
			<xsl:value-of select="text()" />
		</span>
	</xsl:template>
	<xsl:template match="sparql:bnode">
		<span class="bnode">
			<xsl:value-of select="text()" />
		</span>
	</xsl:template>
	<xsl:template match="sparql:literal">
		<span class="literal">
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
