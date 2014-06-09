<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">
    <xsl:output media-type="application/json" method="text" encoding="UTF-8" />
    <xsl:template match="sparql:sparql">
        <xsl:text>{"head":{</xsl:text>
        <xsl:apply-templates select="sparql:head" />
        <xsl:text>},"results":{</xsl:text>
        <xsl:apply-templates select="sparql:results" />
        <xsl:text>}}</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:head">
        <xsl:text>"vars":[</xsl:text>
        <xsl:apply-templates select="sparql:variable" />
        <xsl:text>]</xsl:text>
        <xsl:if test="sparql:link">
            <xsl:text>,"link":[</xsl:text>
            <xsl:apply-templates select="sparql:link" />
            <xsl:text>]</xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sparql:variable">
        <xsl:call-template name="escape-string">
            <xsl:with-param name="s" select="@name"/>
        </xsl:call-template>
        <xsl:if test="position() != last()">
            <xsl:text>,</xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sparql:link">
        <xsl:call-template name="escape-string">
            <xsl:with-param name="s" select="@href"/>
        </xsl:call-template>
        <xsl:if test="position() != last()">
            <xsl:text>,</xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sparql:results">
        <xsl:text>"bindings":[</xsl:text>
        <xsl:apply-templates select="sparql:result" />
        <xsl:text>]</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:result">
        <xsl:text>{</xsl:text>
        <xsl:variable name="current" select="."/> 
        <xsl:apply-templates select="sparql:binding" />
        <xsl:text>}</xsl:text>
        <xsl:if test="position() != last()">
            <xsl:text>,</xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sparql:binding">
        <xsl:call-template name="escape-string">
            <xsl:with-param name="s" select="@name" />
        </xsl:call-template>
        <xsl:text>:</xsl:text>
        <xsl:apply-templates select="sparql:uri|sparql:bnode|sparql:literal" />
        <xsl:if test="position() != last()">
            <xsl:text>,</xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sparql:uri">
        <xsl:text>{"type":"uri","value":</xsl:text>
        <xsl:call-template name="escape-string">
            <xsl:with-param name="s" select="text()"/>
        </xsl:call-template>
        <xsl:text>}</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:bnode">
        <xsl:text>{"type":"bnode","value":</xsl:text>
        <xsl:call-template name="escape-string">
            <xsl:with-param name="s" select="text()"/>
        </xsl:call-template>
        <xsl:text>}</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:literal">
        <xsl:text>{"type":"literal","value":</xsl:text>
        <xsl:call-template name="escape-string">
            <xsl:with-param name="s" select="text()"/>
        </xsl:call-template>
        <xsl:if test="@xml:lang">
            <xsl:text>,"xml:lang":</xsl:text>
            <xsl:call-template name="escape-string">
                <xsl:with-param name="s" select="@xml:lang"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="@datatype">
            <xsl:text>,"datatype":</xsl:text>
            <xsl:call-template name="escape-string">
                <xsl:with-param name="s" select="@datatype"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:text>}</xsl:text>
    </xsl:template>

<!--
  Copyright (c) 2006,2008 Doeke Zanstra
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, 
  are permitted provided that the following conditions are met:

  Redistributions of source code must retain the above copyright notice, this 
  list of conditions and the following disclaimer. Redistributions in binary 
  form must reproduce the above copyright notice, this list of conditions and the 
  following disclaimer in the documentation and/or other materials provided with 
  the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
  THE POSSIBILITY OF SUCH DAMAGE.
-->
  <!-- Main template for escaping strings; used by above template and for object-properties 
       Responsibilities: placed quotes around string, and chain up to next filter, escape-bs-string -->
  <xsl:template name="escape-string">
    <xsl:param name="s"/>
    <xsl:text>"</xsl:text>
    <xsl:call-template name="escape-bs-string">
      <xsl:with-param name="s" select="$s"/>
    </xsl:call-template>
    <xsl:text>"</xsl:text>
  </xsl:template>
  
  <!-- Escape the backslash (\) before everything else. -->
  <xsl:template name="escape-bs-string">
    <xsl:param name="s"/>
    <xsl:choose>
      <xsl:when test="contains($s,'\')">
        <xsl:call-template name="escape-quot-string">
          <xsl:with-param name="s" select="concat(substring-before($s,'\'),'\\')"/>
        </xsl:call-template>
        <xsl:call-template name="escape-bs-string">
          <xsl:with-param name="s" select="substring-after($s,'\')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="escape-quot-string">
          <xsl:with-param name="s" select="$s"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- Escape the double quote ("). -->
  <xsl:template name="escape-quot-string">
    <xsl:param name="s"/>
    <xsl:choose>
      <xsl:when test="contains($s,'&quot;')">
        <xsl:call-template name="encode-string">
          <xsl:with-param name="s" select="concat(substring-before($s,'&quot;'),'\&quot;')"/>
        </xsl:call-template>
        <xsl:call-template name="escape-quot-string">
          <xsl:with-param name="s" select="substring-after($s,'&quot;')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="encode-string">
          <xsl:with-param name="s" select="$s"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- Replace tab, line feed and/or carriage return by its matching escape code. Can't escape backslash
       or double quote here, because they don't replace characters (&#x0; becomes \t), but they prefix 
       characters (\ becomes \\). Besides, backslash should be seperate anyway, because it should be 
       processed first. This function can't do that. -->
  <xsl:template name="encode-string">
    <xsl:param name="s"/>
    <xsl:choose>
      <!-- tab -->
      <xsl:when test="contains($s,'&#x9;')">
        <xsl:call-template name="encode-string">
          <xsl:with-param name="s" select="concat(substring-before($s,'&#x9;'),'\t',substring-after($s,'&#x9;'))"/>
        </xsl:call-template>
      </xsl:when>
      <!-- line feed -->
      <xsl:when test="contains($s,'&#xA;')">
        <xsl:call-template name="encode-string">
          <xsl:with-param name="s" select="concat(substring-before($s,'&#xA;'),'\n',substring-after($s,'&#xA;'))"/>
        </xsl:call-template>
      </xsl:when>
      <!-- carriage return -->
      <xsl:when test="contains($s,'&#xD;')">
        <xsl:call-template name="encode-string">
          <xsl:with-param name="s" select="concat(substring-before($s,'&#xD;'),'\r',substring-after($s,'&#xD;'))"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$s"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>