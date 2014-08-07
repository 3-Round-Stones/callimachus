<?xml version="1.0" encoding="UTF-8"?>
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
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml">

    <xsl:variable name="systemId" select="base-uri()" />
    <xsl:variable name="origin" select="resolve-uri('/')" />
    <xsl:variable name="callback" select="$systemId" />

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

    <!-- form -->
    <xsl:template match="form|xhtml:form">
        <xsl:apply-templates mode="form" select="." />
    </xsl:template>

    <!-- Form -->
    <xsl:template mode="form" match="*">
        <xsl:copy>
            <xsl:call-template name="data-attributes" />
            <xsl:apply-templates mode="form" select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template mode="form" match="@*|comment()|text()">
        <xsl:copy />
    </xsl:template>

    <xsl:template name="data-attributes">
        <xsl:if test="$callback and ancestor-or-self::*[@id and not(self::xhtml:html)]">
            <xsl:if test="*[@about or @resource] and not(@data-construct)">
                <!-- Called when a resource URI is dropped to construct its label -->
                <xsl:attribute name="data-construct">
                    <xsl:value-of select="$callback" />
                    <xsl:text>?options&amp;element=</xsl:text>
                    <xsl:apply-templates mode="xptr-element" select="." />
                    <xsl:text>&amp;resource={resource}</xsl:text>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="*[@about or @typeof or @resource or @property] and not(@data-add)">
                <!-- Called to insert another property value or node -->
                <xsl:attribute name="data-add">
                    <xsl:value-of select="$callback" />
                    <xsl:text>?element=</xsl:text>
                    <xsl:apply-templates mode="xptr-element" select="." />
                </xsl:attribute>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <xsl:template name="replace-string">
        <xsl:param name="text"/>
        <xsl:param name="replace"/>
        <xsl:param name="with"/>
        <xsl:choose>
            <xsl:when test="contains($text,$replace)">
                <xsl:value-of select="substring-before($text,$replace)"/>
                <xsl:value-of select="$with"/>
                <xsl:call-template name="replace-string">
                    <xsl:with-param name="text" select="substring-after($text,$replace)"/>
                    <xsl:with-param name="replace" select="$replace"/>
                    <xsl:with-param name="with" select="$with"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- xptr-element -->
    <xsl:template mode="xptr-element" match="*[@id]">
        <xsl:value-of select="@id" />
    </xsl:template>
    <xsl:template mode="xptr-element" match="*">
        <xsl:apply-templates mode="xptr-element" select=".." />
        <xsl:text>/</xsl:text>
        <xsl:value-of select="count(preceding-sibling::*) + 1" />
    </xsl:template>

</xsl:stylesheet>
