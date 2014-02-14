<?xml version="1.0" encoding="UTF-8" ?>
<!--
  - Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -
  -->
<xsl:stylesheet version="1.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml" xmlns:xhtml="http://www.w3.org/1999/xhtml"
    exclude-result-prefixes="xhtml">
<xsl:output method="xml" indent="yes" />

<xsl:param name="element" />

<xsl:template match="/">
    <xsl:call-template name="apply-element">
        <xsl:with-param name="document" select="/" />
        <xsl:with-param name="path" select="$element" />
    </xsl:call-template>
</xsl:template>

<xsl:template name="apply-element">
    <xsl:param name="document" />
    <xsl:param name="path" />
    <xsl:variable name="i">
        <xsl:choose>
            <xsl:when test="starts-with($path,'/') and contains(substring-after($path,'/'),'/')">
                <xsl:value-of select="substring-before(substring-after($path,'/'),'/')" />
            </xsl:when>
            <xsl:when test="starts-with($path,'/')">
                <xsl:value-of select="substring-after($path,'/')" />
            </xsl:when>
            <xsl:when test="contains($path,'/')">
                <xsl:value-of select="substring-before($path,'/')" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$path" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:choose>
        <xsl:when test="string-length($i) = 0">
            <xsl:apply-templates mode="visible" select="$document" />
        </xsl:when>
        <xsl:when test="starts-with($path, '/')">
            <xsl:call-template name="apply-element">
                <xsl:with-param name="document" select="$document/*[position()=$i]" />
                <xsl:with-param name="path" select="substring-after($path, $i)" />
            </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
            <xsl:call-template name="apply-element">
                <xsl:with-param name="document" select="//*[@id=$i]" />
                <xsl:with-param name="path" select="substring-after($path, $i)" />
            </xsl:call-template>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template mode="visible" match="/|@*|node()">
    <xsl:copy>
        <xsl:apply-templates mode="visible" select="@*|node()" />
    </xsl:copy>
</xsl:template>

</xsl:stylesheet>
