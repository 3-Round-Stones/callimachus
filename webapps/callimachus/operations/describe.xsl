<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	<xsl:include href="../rdfxml.xsl" />
	<xsl:output method="xml" encoding="UTF-8"/>
	<xsl:param name="this" />
	<xsl:template match="/rdf:RDF">
		<html>
			<head>
				<base href="{$this}" />
				<title>
					<xsl:call-template name="iriref">
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
					<xsl:call-template name="iriref">
						<xsl:with-param name="iri" select="$this"/>
					</xsl:call-template>
					<xsl:text> Resource</xsl:text>
				</h1>
				<xsl:apply-imports />
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
