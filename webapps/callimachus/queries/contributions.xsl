<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sparql="http://www.w3.org/2005/sparql-results#">
	<xsl:import href="recent-changes.xsl" />
	<xsl:output method="xml" encoding="UTF-8"/>
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
</xsl:stylesheet>
