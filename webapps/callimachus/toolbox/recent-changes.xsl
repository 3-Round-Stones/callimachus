<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="xhtml sparql">
	<xsl:import href="../transforms/iriref.xsl" />
	<xsl:template match="/">
		<html>
			<head>
				<title>Recent Changes</title>
			</head>
			<body>
				<h1>Recent Changes</h1>
				<xsl:if test="not(/sparql:sparql/sparql:results/sparql:result)">
					<p>No changes have been made recently.</p>
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
			<h2><time class="abbreviated date"><xsl:value-of select="sparql:binding[@name='modified']/*" /></time></h2>
			<xsl:text disable-output-escaping="yes">&lt;ul&gt;</xsl:text>
		</xsl:if>
		<li class="result">
			<xsl:if test="sparql:binding[@name='icon']">
				<img src="{sparql:binding[@name='icon']/*}" class="icon" />
			</xsl:if>
			<xsl:if test="not(sparql:binding[@name='icon'])">
				<img src="/callimachus/images/rdf-icon.png" class="icon" />
			</xsl:if>
			<a>
				<xsl:if test="sparql:binding[@name='url']">
					<xsl:attribute name="href">
						<xsl:value-of select="sparql:binding[@name='url']/*" />
					</xsl:attribute>
					<xsl:attribute name="class">
						<xsl:value-of select="'view'" />
					</xsl:attribute>
				</xsl:if>
				<xsl:apply-templates select="sparql:binding[@name='label']/*" />
				<xsl:if test="not(sparql:binding[@name='label'])">
					<xsl:call-template name="iriref">
						<xsl:with-param name="iri" select="sparql:binding[@name='url']/*"/>
					</xsl:call-template>
				</xsl:if>
			</a>
			<xsl:text>; </xsl:text>
			<a href="{sparql:binding[@name='revision']/*}?view"><time class="abbreviated time"><xsl:value-of select="sparql:binding[@name='modified']/*" /></time></a>
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
