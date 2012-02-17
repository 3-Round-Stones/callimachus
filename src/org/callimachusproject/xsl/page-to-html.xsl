<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml"
		version="1.0" exclude-result-prefixes="xhtml">
	<xsl:output indent="no" method="html" />

	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|comment()|text()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@*|comment()">
		<xsl:copy />
	</xsl:template>

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

	<!-- strip xml:space attributes -->
	<xsl:template match = '@xml:space' />

	<!-- strip all processing instructions -->
	<xsl:template match = 'processing-instruction()' />

	<!-- strip meta charset -->
	<xsl:template match = 'xhtml:meta[translate(@http-equiv,"ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz") = "content-type" ]' />

	<xsl:template match="xhtml:*">
		<xsl:element name="{local-name()}">
			<xsl:if test="@xml:lang and not(@lang)">
				<xsl:attribute name="lang">
					<xsl:value-of select="@xml:lang" />
				</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates select="@*[name() != 'xml:lang']|*|comment()|text()"/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="*[@id='access']" />

	<xsl:template match="*[@id='resource-lastmod']" />

	<xsl:template match="*[@id='breadcrumbs']" />

	<xsl:template match="title|script|style|iframe|noembed|noframes">
		<!-- Some XSLT engines may not output HTML and this can help an HTML parser parse XML. -->
		<xsl:element name="{local-name()}">
			<xsl:apply-templates select="@*|*|comment()|text()"/>
			<xsl:if test="not(text())">
				<xsl:text> </xsl:text>
			</xsl:if>
		</xsl:element>
	</xsl:template>

	<xsl:template match="xhtml:title|xhtml:script|xhtml:style|xhtml:iframe|xhtml:noembed|xhtml:noframes">
		<!-- Some XSLT engines may not output HTML and this can help an HTML parser parse XML. -->
		<xsl:element name="{local-name()}">
			<xsl:apply-templates select="@*|*|comment()|text()"/>
			<xsl:if test="not(text())">
				<xsl:text> </xsl:text>
			</xsl:if>
		</xsl:element>
	</xsl:template>

</xsl:transform>
