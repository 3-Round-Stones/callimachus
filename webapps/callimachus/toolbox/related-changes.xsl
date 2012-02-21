<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="xhtml sparql">
	<xsl:import href="recent-changes.xsl" />
	<xsl:template match="/">
		<html>
			<head>
				<title>Related Changes</title>
			</head>
			<body>
				<h1>Related Changes</h1>
				<xsl:if test="not(/sparql:sparql/sparql:results/sparql:result)">
					<p>No changes have been made recently.</p>
				</xsl:if>
				<xsl:if test="/sparql:sparql/sparql:results/sparql:result">
					<xsl:apply-templates />
				</xsl:if>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
