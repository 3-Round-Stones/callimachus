<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:audit="http://www.openrdf.org/rdf/2009/auditing#"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="xhtml sparql">
	<xsl:import href="iriref.xsl" />
	<xsl:param name="this" />
	<xsl:variable name="name">
		<xsl:choose>
			<xsl:when test="sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='name']/*">
				<xsl:value-of select="sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='name']/*" />
			</xsl:when>
			<xsl:when test="sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='user']/*">
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='user']/*"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="$this"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:template match="/">
		<html>
			<head>
				<title><xsl:value-of select="sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='modified']/*" /> Changeset</title>
				<style>
					.uri, .bnode, .literal { font-size: large; }
					.bnode, .literal { font-family: monospace; white-space: pre-wrap; }
					.predicate { font-weight: bold; }
					.removed { text-decoration: line-through; }
				</style>
			</head>
			<body>
				<p><xsl:text>Revised on </xsl:text>
					<time property="audit:committedOn">
						<xsl:value-of select="sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='modified']/*" />
					</time>
					<xsl:text> by </xsl:text>
					<a href="{sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='user']/*}">
						<xsl:value-of select="$name" />
					</a>
				</p>
				<p>
					<xsl:for-each select="sparql:sparql/sparql:results/sparql:result[sparql:binding/@name='previous']">
						<a href="{sparql:binding[@name='previous']/*}">←Previous revision</a>
						<xsl:text> </xsl:text>
					</xsl:for-each>
					<xsl:if test="sparql:sparql/sparql:results/sparql:result[sparql:binding/@name='subsequent']">
						<xsl:text>|</xsl:text>
					</xsl:if>
					<xsl:for-each select="sparql:sparql/sparql:results/sparql:result[sparql:binding/@name='subsequent']">
						<xsl:text> </xsl:text>
						<a href="{sparql:binding[@name='subsequent']/*}">Subsequent revision→</a>
					</xsl:for-each>
				</p>
				<xsl:apply-templates select="sparql:sparql/sparql:results/sparql:result[sparql:binding/@name='subject']" />
			</body>
		</html>
	</xsl:template>
	<xsl:template match="sparql:result[sparql:binding/@name='subject']">
		<xsl:variable name="subject" select="sparql:binding[@name='subject']/*" />
		<xsl:variable name="predicate" select="sparql:binding[@name='predicate']/*" />
		<xsl:variable name="object" select="sparql:binding[@name='object']/*" />
		<xsl:if test="not($subject=preceding-sibling::*[1]/sparql:binding[@name='subject']/*)">
			<xsl:if test="preceding-sibling::*[1]/sparql:binding[@name='subject']">
				<xsl:text disable-output-escaping="yes">&lt;/ul&gt;</xsl:text>
			</xsl:if>
			<a href="{$subject}" class="view">
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="$subject"/>
				</xsl:call-template>
			</a>
			<xsl:text disable-output-escaping="yes">&lt;ul&gt;</xsl:text>
		</xsl:if>
		<li about="{$subject}">
			<label class="predicate">
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="$predicate"/>
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<span>
				<xsl:if test="string(sparql:binding[@name='added']/*)!=$this">
					<xsl:attribute name="class">
						<xsl:text>removed</xsl:text>
					</xsl:attribute>
				</xsl:if>
				<xsl:apply-templates select="sparql:binding[@name='object']" />
			</span>
		</li>
		<xsl:if test="not(following-sibling::*[1]/sparql:binding[@name='subject'])">
			<xsl:text disable-output-escaping="yes">&lt;/ul&gt;</xsl:text>
		</xsl:if>
	</xsl:template>
	<xsl:template match="sparql:binding">
		<xsl:apply-templates select="*" />
	</xsl:template>
	<xsl:template match="sparql:uri">
		<a href="{text()}" class="view uri">
			<xsl:attribute name="rel">
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="text()" />
				</xsl:call-template>
			</xsl:attribute>
			<xsl:call-template name="iriref">
				<xsl:with-param name="iri" select="text()"/>
			</xsl:call-template>
		</a>
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
