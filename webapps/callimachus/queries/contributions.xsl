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
				<title>Contributions</title>
			</head>
			<body>
				<h1>Contributions</h1>
				<xsl:if test="not(/sparql:sparql/sparql:results/sparql:result)">
					<p>No contributions have ever been made.</p>
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
			<h2 class="date-locale"><xsl:value-of select="substring-before(sparql:binding[@name='modified']/*, 'T')" /><xsl:text>T00:00:00</xsl:text></h2>
		</xsl:if>
		<ul>
		<li class="result">
			<xsl:if test="sparql:binding[@name='icon']">
				<img src="{sparql:binding[@name='icon']/*}" class="icon" />
			</xsl:if>
			<xsl:if test="not(sparql:binding[@name='icon'])">
				<img src="/callimachus/rdf-icon.png" class="icon" />
			</xsl:if>
			<a>
				<xsl:if test="sparql:binding[@name='url']">
					<xsl:attribute name="href">
						<xsl:value-of select="sparql:binding[@name='url']/*" />
					</xsl:attribute>
				</xsl:if>
				<xsl:apply-templates select="sparql:binding[@name='label']/*" />
				<xsl:if test="not(sparql:binding[@name='label'])">
					<xsl:call-template name="substring-after-last">
						<xsl:with-param name="string" select="sparql:binding[@name='url']/*"/>
						<xsl:with-param name="delimiter" select="'/'"/>
					</xsl:call-template>
				</xsl:if>
			</a>
			<xsl:text>; </xsl:text>
			<span class="date-locale"><xsl:value-of select="substring-after(sparql:binding[@name='modified']/*, 'T')" /></span>
		</li>
		</ul>
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
