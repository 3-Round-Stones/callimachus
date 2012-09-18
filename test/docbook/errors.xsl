<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version                 = "1.0"
                xmlns                   = "http://www.w3.org/1999/xhtml"
                xmlns:doc               = "http://docbook.org/ns/docbook"
                xmlns:c                 = "http://www.w3.org/ns/xproc-step"
                xmlns:xsl               = "http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes = "xsl doc c">
	<xsl:output encoding = "UTF-8" 
	            indent   = "yes" 
	            method   = "xml" />
	<xsl:template match="/">
        <details>
            <summary>Errors</summary>
    	    <dl>
    	       <xsl:apply-templates select="/c:errors/c:error"/>
    	    </dl>
        </details>	    
	</xsl:template>
	<xsl:template match="c:error">
	    <dt>
	       <a href="{@href}">
	           <xsl:value-of select="@name | @code"/>
	           <xsl:text> ( line number: </xsl:text>
	           <xsl:value-of select="@line"/>
	           <xsl:text> )</xsl:text>
	       </a>
	    </dt>
	    <dd>
	        <xsl:value-of select="."/>
	    </dd>
	</xsl:template>
</xsl:stylesheet>