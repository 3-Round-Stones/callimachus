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
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    exclude-result-prefixes="sparql">
    <xsl:output indent="no" media-type="text/html" method="html" encoding="UTF-8" doctype-system="about:legacy-compat"/>
    <xsl:template match="/">
        <html>
            <head>
                <title></title>
            </head>
            <body>
                <div class="container">
                    <xsl:apply-templates />
                </div>
            </body>
        </html>
    </xsl:template>
    <xsl:template match="sparql:sparql">
        <table>
            <xsl:apply-templates select="*" />
        </table>
    </xsl:template>
    <xsl:template match="sparql:head">
        <thead>
            <xsl:apply-templates select="sparql:variable" />
        </thead>
    </xsl:template>
    <xsl:template match="sparql:variable">
        <th>
            <xsl:value-of select="@name" />
        </th>
    </xsl:template>
    <xsl:template match="sparql:boolean">
        <tbody>
            <tr>
                <td>
                    <xsl:value-of select="text()" />
                </td>
            </tr>
        </tbody>
    </xsl:template>
    <xsl:template match="sparql:results">
        <tbody>
            <xsl:apply-templates select="sparql:result" />
        </tbody>
    </xsl:template>
    <xsl:template match="sparql:result">
        <xsl:variable name="current" select="."/> 
        <tr>
            <xsl:for-each select="../../sparql:head[1]/sparql:variable">
                <xsl:variable name="name" select="@name"/> 
                <td>
                    <xsl:apply-templates select="$current/sparql:binding[@name=$name]" /> 
                </td>
            </xsl:for-each>
        </tr>
    </xsl:template>
    <xsl:template match="sparql:binding">
        <xsl:apply-templates select="*" />
    </xsl:template>
    <xsl:template match="sparql:uri">
        <a href="{text()}">
            <xsl:value-of select="replace(text(), 'https?://[^\?#]*/([^\?#/])', '$1')" />
        </a>
    </xsl:template>
</xsl:stylesheet>
