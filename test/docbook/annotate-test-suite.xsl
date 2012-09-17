<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version                 = "1.0"
                xmlns:xhtml             = "http://www.w3.org/1999/xhtml"
                xmlns:doc               = "http://docbook.org/ns/docbook"
                xmlns:xsl               = "http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes = "xsl xhtml doc">
	<xsl:output encoding = "UTF-8" 
	            indent   = "yes" 
	            method   = "xml" />
	<xsl:param name="escaped-original"  />
	<xsl:param name="escaped-roundtrip" />
	<xsl:param name="success"           />
	<xsl:param name="error"             />
	
	<xsl:template match="/">
	    <xsl:apply-templates select="xhtml:details" mode="root" />
	</xsl:template>

	<xsl:template match="xhtml:details" mode="root">
        <xsl:copy>
            <xsl:if test="$success = 'false'">
                <xsl:attribute name="open">open</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates select="xhtml:summary" mode="root"/>
            <xsl:apply-templates select="*[local-name() != 'summary']" />
            <details xmlns="http://www.w3.org/1999/xhtml">
                <summary>Expected</summary>
                <pre>
                    <xsl:value-of select="$escaped-original"/>
                </pre>
            </details>
            <details xmlns="http://www.w3.org/1999/xhtml">
                <summary>Actual</summary>
                <pre>
                    <xsl:value-of select="$escaped-roundtrip"/>
                </pre>            
            </details>            
        </xsl:copy>
	</xsl:template>
	
	<xsl:template match="xhtml:summary" mode="root">
	    <xsl:copy>
	        <xsl:choose>
    	      <xsl:when test="$error = 'true'">ERROR</xsl:when>
        	      <xsl:when test="$success = 'false'">FAIL</xsl:when>
    	      <xsl:when test="$success = 'true'">PASS</xsl:when>
    	    </xsl:choose>
    	    <xsl:text>&#160;</xsl:text>
    	    <xsl:value-of select="text()"/>
	    </xsl:copy>
	</xsl:template>
	
    <xsl:template match="@*|node()">
        <xsl:copy>
          <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>