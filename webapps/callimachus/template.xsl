<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:param name="systemId" />
	<xsl:param name="realm" select="'/'" />
	<xsl:variable name="styles"><xsl:value-of select="document(concat($realm,'?layout'))/xsl:stylesheet/xsl:variable[@name='styles']/node()" /></xsl:variable>
	<xsl:variable name="scripts"><xsl:value-of select="document(concat($realm,'?layout'))/xsl:stylesheet/xsl:variable[@name='scripts']/node()" /></xsl:variable>
	<xsl:variable name="layout"><xsl:value-of select="document(concat($realm,'?layout'))/xsl:stylesheet/xsl:variable[@name='layout']/node()" /></xsl:variable>
	<xsl:variable name="favicon"><xsl:value-of select="document(concat($realm,'?layout'))/xsl:stylesheet/xsl:variable[@name='favicon']/node()" /></xsl:variable>
	<xsl:variable name="menu"><xsl:value-of select="document(concat($realm,'?layout'))/xsl:stylesheet/xsl:variable[@name='menu']/node()" /></xsl:variable>
	<xsl:variable name="variation"><xsl:value-of select="document(concat($realm,'?layout'))/xsl:stylesheet/xsl:variable[@name='variation']/node()" /></xsl:variable>
	<xsl:variable name="rights"><xsl:value-of select="document(concat($realm,'?layout'))/xsl:stylesheet/xsl:variable[@name='rights']/node()" /></xsl:variable>
	<xsl:include href="transforms/layout.xsl" />
</xsl:stylesheet>
