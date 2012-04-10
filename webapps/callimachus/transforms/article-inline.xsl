<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns="http://www.w3.org/1999/xhtml" xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:d="http://docbook.org/ns/docbook" exclude-result-prefixes="xsl d xhtml">

<xsl:import href="/callimachus/editor/docbook2xhtml.xsl" />

<xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>

<xsl:param name="this" />

<xsl:template match="xhtml:*">
	<xsl:copy>
		<xsl:apply-templates select="@*|*|text()|comment()" />
	</xsl:copy>
</xsl:template>

<xsl:template match="@*|comment()">
	<xsl:copy />
</xsl:template>

<xsl:template match="xhtml:article">
	<article>
		<xsl:apply-templates select="document($this)/d:article/*" />
	</article>
</xsl:template>

</xsl:stylesheet>
