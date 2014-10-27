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
        xmlns:xhtml="http://www.w3.org/1999/xhtml"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <xsl:output indent="no" method="xml" />

    <xsl:param name="target" />
    <xsl:param name="query" />

    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*|*|comment()|text()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|comment()">
        <xsl:copy />
    </xsl:template>

    <xsl:template match="xhtml:nav[@id='calli-access']">
        <xsl:if test="*//@href[.=concat('?',$query)]">
            <xsl:apply-templates mode="access" />
        </xsl:if>
    </xsl:template>

    <xsl:template mode="access" match="*">
        <xsl:copy>
            <xsl:apply-templates mode="access" select="@*[name()!='class']" />
            <xsl:attribute name="class">
                <xsl:if test="@class">
                    <xsl:value-of select="@class" />
                    <xsl:text> </xsl:text>
                </xsl:if>
                <xsl:if test="count(*//@href) = 1 and *//@href[.=concat('?',$query)]">
                    <xsl:text>active</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:apply-templates mode="access" select="*|text()|comment()" />
        </xsl:copy>
    </xsl:template>
    <xsl:template mode="access" match="@*|text()|comment()">
        <xsl:copy />
    </xsl:template>
    <xsl:template mode="access" match="xhtml:a[@href]|a[@href]">
        <xsl:copy>
            <xsl:if test="@href=concat('?',$query)">
                <xsl:apply-templates mode="access" select="@*[name()!='href' and name()!='class' and name()!='onclick']" />
                <xsl:attribute name="class">
                    <xsl:if test="@class">
                        <xsl:value-of select="@class" />
                        <xsl:text> </xsl:text>
                    </xsl:if>
                    <xsl:text>active</xsl:text>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="not(@href=concat('?',$query))">
                <xsl:apply-templates mode="access" select="@*" />
            </xsl:if>
            <xsl:apply-templates mode="access" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xhtml:div[@id='calli-lastmod']">
        <xsl:if test="string-length($target) &gt; 0">
            <xsl:apply-templates mode="time" />
        </xsl:if>
    </xsl:template>

    <xsl:template mode="time" match="@*|text()|comment()">
        <xsl:copy />
    </xsl:template>

    <xsl:template mode="time" match="*">
        <xsl:copy>
            <xsl:apply-templates mode="time" select="@*|node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template mode="time" match="xhtml:time|time">
        <xsl:variable name="url" select="concat('../queries/page-info.rq?results&amp;target=',encode-for-uri($target))" />
        <xsl:copy>
            <xsl:apply-templates mode="time" select="@*[name()!='datetime']|node()" />
            <xsl:apply-templates mode="time" select="document($url)//sparql:binding[@name='updated']/*/@datatype" />
            <xsl:attribute name="datetime">
                <xsl:value-of select="document($url)//sparql:binding[@name='updated']/*" />
            </xsl:attribute>
            <xsl:value-of select="document($url)//sparql:binding[@name='updated']/*" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xhtml:nav[@id='calli-if-breadcrumb']">
        <xsl:if test="string-length($target) &gt; 0">
            <xsl:variable name="url" select="concat('../queries/page-info.rq?results&amp;target=',encode-for-uri($target))" />
            <xsl:if test="count(document($url)//sparql:result[sparql:binding/@name='iri']) > 1">
                <xsl:apply-templates />
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <xsl:template match="xhtml:nav[@id='calli-breadcrumb']">
        <xsl:variable name="last" select="*[last()]" />
        <xsl:if test="string-length($target) &gt; 0 and count(*) = 1">
            <xsl:apply-templates mode="breadcrumbs" select="$last" />
        </xsl:if>
        <xsl:if test="string-length($target) &gt; 0 and count(*) &gt; 1">
            <xsl:call-template name="breadcrumbs">
                <xsl:with-param name="breadcrumb" select="$last/preceding-sibling::node()" />
                <xsl:with-param name="active" select="$last" />
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template mode="breadcrumbs" match="*">
        <xsl:variable name="url" select="concat('../queries/page-info.rq?results&amp;target=',encode-for-uri($target))" />
        <xsl:variable name="active" select="*[last()]" />
        <xsl:if test="count(document($url)//sparql:result[sparql:binding/@name='iri']) > 1">
            <xsl:copy>
                <xsl:apply-templates select="@*" />
                <xsl:call-template name="breadcrumbs">
                    <xsl:with-param name="breadcrumb" select="$active/preceding-sibling::node()" />
                    <xsl:with-param name="active" select="$active" />
                </xsl:call-template>
            </xsl:copy>
        </xsl:if>
    </xsl:template>

    <xsl:template name="breadcrumbs">
        <xsl:param name="breadcrumb" />
        <xsl:param name="active" />
        <xsl:variable name="url" select="concat('../queries/page-info.rq?results&amp;target=',encode-for-uri($target))" />
        <xsl:for-each select="document($url)//sparql:result[sparql:binding/@name='iri']">
            <xsl:if test="position()!=last()">
                <xsl:call-template name="breadcrumb">
                    <xsl:with-param name="copy" select="$breadcrumb" />
                    <xsl:with-param name="href" select="sparql:binding[@name='iri']/*" />
                    <xsl:with-param name="label">
                        <xsl:value-of select="sparql:binding[@name='label']/*" />
                        <xsl:if test="not(sparql:binding[@name='label']/*)">
                            <xsl:call-template name="substring-after-last">
                                <xsl:with-param name="arg" select="sparql:binding[@name='iri']/*" />
                                <xsl:with-param name="delim" select="'/'" />
                            </xsl:call-template>
                        </xsl:if>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:if>
            <xsl:if test="position()=last() and position() &gt; 1">
                <xsl:element name="{name($active)}">
                    <xsl:apply-templates select="$active/@*" />
                    <xsl:value-of select="sparql:binding[@name='label']/*" />
                    <xsl:if test="not(sparql:binding[@name='label']/*)">
                        <xsl:call-template name="substring-after-last">
                            <xsl:with-param name="arg" select="sparql:binding[@name='iri']/*" />
                            <xsl:with-param name="delim" select="'/'" />
                        </xsl:call-template>
                    </xsl:if>
                </xsl:element>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="breadcrumb">
        <xsl:param name="copy" />
        <xsl:param name="href" />
        <xsl:param name="label" />
        <xsl:for-each select="$copy">
            <xsl:choose>
                <xsl:when test="local-name()='a'">
                    <xsl:element name="{name()}">
                        <xsl:attribute name="href"><xsl:value-of select="$href" /></xsl:attribute>
                        <xsl:apply-templates select="@*[name()!='href']" />
                        <xsl:value-of select="$label" />
                    </xsl:element>
                </xsl:when>
                <xsl:when test="*">
                    <xsl:element name="{name()}">
                        <xsl:apply-templates select="@*" />
                        <xsl:call-template name="breadcrumb">
                            <xsl:with-param name="copy" select="node()" />
                            <xsl:with-param name="href" select="$href" />
                            <xsl:with-param name="label" select="$label" />
                        </xsl:call-template>
                    </xsl:element>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="." />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="substring-after-last">
        <xsl:param name="arg"/>
        <xsl:param name="delim"/>
        <xsl:if test="not(contains($arg, $delim))">
            <xsl:value-of select="$arg" />
        </xsl:if>
        <xsl:if test="contains($arg, $delim)">
            <xsl:call-template name="substring-after-last">
                <xsl:with-param name="arg" select="substring-after($arg, $delim)"/>
                <xsl:with-param name="delim" select="$delim"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>


