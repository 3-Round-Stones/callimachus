<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	<xsl:include href="rdfxml.xsl" />
	<xsl:output method="xml" encoding="UTF-8"/>
	<xsl:param name="this" />
	<xsl:param name="xslt" select="'/callimachus/graph.xsl'" />
	<xsl:variable name="callimachus">
		<xsl:call-template name="substring-before-last">
			<xsl:with-param name="string" select="$xslt"/>
			<xsl:with-param name="delimiter" select="'/'"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="profile" select="concat($callimachus, '/profile')" />
	<xsl:template match="/">
		<html>
			<head>
				<base href="{$this}" />
				<title>
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="$this"/>
					</xsl:call-template>
				</title>
				<style>
					.uri, .bnode, .literal { font-size: large; }
					.bnode, .literal { font-family: monospace; white-space: pre-wrap; }
					.predicate { font-weight: bold; }
				</style>
			</head>
			<body>
				<h1>
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="$this"/>
					</xsl:call-template>
					<xsl:text> Graph</xsl:text>
				</h1>
				<xsl:apply-templates />
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
