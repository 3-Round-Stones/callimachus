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
<xsl:stylesheet version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">
    <xsl:output media-type="text/csv" encoding="UTF-8" method="text" />
    <xsl:template match="sparql:sparql">
        <xsl:apply-templates select="sparql:head" />
        <xsl:apply-templates select="sparql:results" />
    </xsl:template>
    <xsl:template match="sparql:head">
        <xsl:apply-templates select="sparql:variable" />
        <xsl:text>&#xD;&#xA;</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:variable">
        <xsl:value-of select="@name" />
        <xsl:if test="position() != last()">
            <xsl:text>,</xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sparql:results">
        <xsl:apply-templates select="sparql:result" />
    </xsl:template>
    <xsl:template match="sparql:result">
        <xsl:variable name="current" select="."/> 
        <xsl:for-each select="../../sparql:head/sparql:variable">
            <xsl:variable name="name" select="@name"/> 
            <xsl:apply-templates select="$current/sparql:binding[@name=$name]" /> 
            <xsl:if test="position() != last()">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:text>&#xD;&#xA;</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:binding">
        <xsl:apply-templates select="*" />
    </xsl:template>
    <xsl:template match="sparql:uri">
        <xsl:text>"</xsl:text>
        <xsl:apply-templates select="text()" />
        <xsl:text>"</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:bnode">
        <xsl:text>"</xsl:text>
        <xsl:apply-templates select="text()" />
        <xsl:text>"</xsl:text>
    </xsl:template>
    <xsl:template match="sparql:literal">
        <xsl:text>"</xsl:text>
        <xsl:apply-templates select="text()" />
        <xsl:text>"</xsl:text>
    </xsl:template>
</xsl:stylesheet>
