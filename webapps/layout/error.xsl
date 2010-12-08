<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml"
		version="1.0" exclude-result-prefixes="xhtml">
	<xsl:import href="/layout/template.xsl" />
	<xsl:output method="html" />

	<xsl:template match="html|xhtml:html">
		<xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html&gt;&#xA;</xsl:text>
		<xsl:copy>
			<xsl:if test="@xml:lang and not(@lang)">
				<xsl:attribute name="lang">
					<xsl:value-of select="@xml:lang" />
				</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates select="@*[name() != 'xml:lang']|*|comment()|text()"/>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
