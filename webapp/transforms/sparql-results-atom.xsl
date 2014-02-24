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
    xmlns="http://www.w3.org/2005/Atom"
    xmlns:app="http://www.w3.org/2007/app"
    xmlns:openSearch="http://a9.com/-/spec/opensearch/1.1/"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
    exclude-result-prefixes="sparql">

<xsl:template match="sparql:head" />

<xsl:template match="sparql:variable" />

<xsl:template match="sparql:boolean" />

<xsl:template match="sparql:results">
    <feed>
        <xsl:apply-templates select="sparql:result" />
    </feed>
</xsl:template>

<xsl:template match="sparql:result">
    <xsl:variable name="id" select="sparql:binding[@name='id']/*/text()" />
    <xsl:if test="not(preceding::sparql:result[sparql:binding[@name='id']/*=$id])">
    <entry>
        <xsl:for-each select="../sparql:result[sparql:binding[@name='id']/*=$id]/sparql:binding">
            <xsl:variable name="name" select="@name" />
            <xsl:variable name="value" select="*" />
            <xsl:if test="not(preceding::sparql:result[sparql:binding[@name='id']/*=$id]/sparql:binding[@name=$name]/*=$value)">
                <xsl:apply-templates select="." />
            </xsl:if>
        </xsl:for-each>
    </entry>
    </xsl:if>
</xsl:template>

<xsl:template match="sparql:result[sparql:binding[@name='type']/*='feed']">
    <xsl:variable name="id" select="sparql:binding[@name='id']/*/text()" />
    <xsl:if test="not(preceding::sparql:result[sparql:binding[@name='id']/*=$id])">
    <xsl:for-each select="../sparql:result[sparql:binding[@name='id']/*=$id]/sparql:binding">
        <xsl:variable name="name" select="@name" />
        <xsl:variable name="value" select="*" />
        <xsl:if test="not(preceding::sparql:result[sparql:binding[@name='id']/*=$id]/sparql:binding[@name=$name]/*=$value)">
            <xsl:apply-templates select="." />
        </xsl:if>
    </xsl:for-each>
    </xsl:if>
</xsl:template>

<xsl:template match="sparql:binding[@name='id']">
    <id>
        <xsl:value-of select="*" />
    </id>
</xsl:template>

<xsl:template match="sparql:binding[@name='totalResults']">
    <openSearch:totalResults>
        <xsl:value-of select="*" />
    </openSearch:totalResults>
</xsl:template>

<xsl:template match="sparql:binding[@name='title']">
    <title>
        <xsl:value-of select="*" />
    </title>
</xsl:template>

<xsl:template match="sparql:binding[@name='subtitle']">
    <subtitle>
        <xsl:value-of select="*" />
    </subtitle>
</xsl:template>

<xsl:template match="sparql:binding[@name='icon']">
    <icon>
        <xsl:value-of select="*" />
    </icon>
</xsl:template>

<xsl:template match="sparql:binding[@name='logo']">
    <logo>
        <xsl:value-of select="*" />
    </logo>
</xsl:template>

<xsl:template match="sparql:binding[@name='summary']">
    <summary>
        <xsl:value-of select="*" />
    </summary>
</xsl:template>

<xsl:template match="sparql:binding[@name='published']">
    <published>
        <xsl:value-of select="*" />
    </published>
</xsl:template>

<xsl:template match="sparql:binding[@name='updated']">
    <updated>
        <xsl:value-of select="*" />
    </updated>
</xsl:template>

<xsl:template match="sparql:binding[@name='rights']">
    <rights>
        <xsl:value-of select="*" />
    </rights>
</xsl:template>

<xsl:template match="sparql:binding[@name='link_href']">
    <link href="{*}">
        <xsl:if test="../sparql:binding[@name='link_rel']">
            <xsl:attribute name="rel"><xsl:value-of select="../sparql:binding[@name='link_rel']/*" /></xsl:attribute>
        </xsl:if>
        <xsl:if test="../sparql:binding[@name='link_type']">
            <xsl:attribute name="type"><xsl:value-of select="../sparql:binding[@name='link_type']/*" /></xsl:attribute>
        </xsl:if>
    </link>
</xsl:template>

<xsl:template match="sparql:binding[@name='link_view_href']">
    <link rel="alternate" type="text/html" href="{*}" />
</xsl:template>

<xsl:template match="sparql:binding[@name='link_edit_media_href']">
    <link rel="edit-media" href="{*}" />
</xsl:template>

<xsl:template match="sparql:binding[@name='link_history_href']">
    <link rel="version-history" href="{*}" />
</xsl:template>

<xsl:template match="sparql:binding[@name='link_describedby_href']">
    <link rel="describedby" href="{*}" />
</xsl:template>

<xsl:template match="sparql:binding[@name='content_src']">
    <content src="{*}">
        <xsl:if test="../sparql:binding[@name='content_type']">
            <xsl:attribute name="type"><xsl:value-of select="../sparql:binding[@name='content_type']/*" /></xsl:attribute>
        </xsl:if>
    </content>
</xsl:template>

<xsl:template match="sparql:binding[@name='content']">
    <content>
        <xsl:if test="../sparql:binding[@name='content_type']">
            <xsl:attribute name="type"><xsl:value-of select="../sparql:binding[@name='content_type']/*" /></xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="*" />
    </content>
</xsl:template>

<xsl:template match="sparql:binding[@name='category_term']">
    <category term="{*}">
        <xsl:if test="../sparql:binding[@name='category_scheme']">
            <xsl:attribute name="scheme"><xsl:value-of select="../sparql:binding[@name='content_scheme']/*" /></xsl:attribute>
        </xsl:if>
        <xsl:if test="../sparql:binding[@name='category_label']">
            <xsl:attribute name="label"><xsl:value-of select="../sparql:binding[@name='content_label']/*" /></xsl:attribute>
        </xsl:if>
    </category>
</xsl:template>

<xsl:template match="sparql:binding[@name='generator']">
    <generator>
        <xsl:if test="../sparql:binding[@name='generator_uri']">
            <xsl:attribute name="uri"><xsl:value-of select="../sparql:binding[@name='generator_uri']/*" /></xsl:attribute>
        </xsl:if>
        <xsl:if test="../sparql:binding[@name='generator_version']">
            <xsl:attribute name="version"><xsl:value-of select="../sparql:binding[@name='generator_version']/*" /></xsl:attribute>
        </xsl:if>
        <xsl:value-of select="*" />
    </generator>
</xsl:template>

<xsl:template match="sparql:binding[@name='author_uri']">
    <author>
        <xsl:if test="../sparql:binding[@name='author_name']">
            <name><xsl:value-of select="../sparql:binding[@name='author_name']/*" /></name>
        </xsl:if>
        <uri><xsl:value-of select="*" /></uri>
        <xsl:if test="../sparql:binding[@name='author_email']">
            <email><xsl:value-of select="../sparql:binding[@name='author_email']/*" /></email>
        </xsl:if>
    </author>
</xsl:template>

<xsl:template match="sparql:binding[@name='contributor_uri']">
    <contributor>
        <xsl:if test="../sparql:binding[@name='contributor_name']">
            <name><xsl:value-of select="../sparql:binding[@name='contributor_name']/*" /></name>
        </xsl:if>
        <uri><xsl:value-of select="*" /></uri>
        <xsl:if test="../sparql:binding[@name='contributor_email']">
            <email><xsl:value-of select="../sparql:binding[@name='contributor_email']/*" /></email>
        </xsl:if>
    </contributor>
</xsl:template>

<xsl:template match="sparql:binding[@name='collection_href']">
    <app:collection href="{*}">
        <xsl:if test="../sparql:binding[@name='collection_title']">
            <title><xsl:value-of select="../sparql:binding[@name='collection_title']/*" /></title>
        </xsl:if>
        <xsl:variable name="id" select="../sparql:binding[@name='id']/*" />
        <xsl:for-each select="../../sparql:result[sparql:binding[@name='id']/*=$id]/sparql:binding[@name='collection_accept']">
            <app:accept><xsl:value-of select="*" /></app:accept>
        </xsl:for-each>
    </app:collection>
</xsl:template>

<xsl:template match="sparql:binding[@name='link_reader_href']">
    <link rel="http://callimachusproject.org/rdf/2009/framework#reader" href="{*}">
        <xsl:if test="../sparql:binding[@name='link_reader_title']">
            <xsl:attribute name="title"><xsl:value-of select="../sparql:binding[@name='link_reader_title']/*" /></xsl:attribute>
        </xsl:if>
    </link>
</xsl:template>

<xsl:template match="sparql:binding[@name='link_subscriber_href']">
    <link rel="http://callimachusproject.org/rdf/2009/framework#subscriber" href="{*}">
        <xsl:if test="../sparql:binding[@name='link_subscriber_title']">
            <xsl:attribute name="title"><xsl:value-of select="../sparql:binding[@name='link_subscriber_title']/*" /></xsl:attribute>
        </xsl:if>
    </link>
</xsl:template>

<xsl:template match="sparql:binding[@name='link_contributor_href']">
    <link rel="http://callimachusproject.org/rdf/2009/framework#contributor" href="{*}">
        <xsl:if test="../sparql:binding[@name='link_contributor_title']">
            <xsl:attribute name="title"><xsl:value-of select="../sparql:binding[@name='link_contributor_title']/*" /></xsl:attribute>
        </xsl:if>
    </link>
</xsl:template>

<xsl:template match="sparql:binding[@name='link_editor_href']">
    <link rel="http://callimachusproject.org/rdf/2009/framework#editor" href="{*}">
        <xsl:if test="../sparql:binding[@name='link_editor_title']">
            <xsl:attribute name="title"><xsl:value-of select="../sparql:binding[@name='link_editor_title']/*" /></xsl:attribute>
        </xsl:if>
    </link>
</xsl:template>

<xsl:template match="sparql:binding[@name='link_administrator_href']">
    <link rel="http://callimachusproject.org/rdf/2009/framework#administrator" href="{*}">
        <xsl:if test="../sparql:binding[@name='link_administrator_title']">
            <xsl:attribute name="title"><xsl:value-of select="../sparql:binding[@name='link_administrator_title']/*" /></xsl:attribute>
        </xsl:if>
    </link>
</xsl:template>

<xsl:template match="sparql:binding" />

<xsl:template match="sparql:uri">
    <xsl:value-of select="text()" />
</xsl:template>

<xsl:template match="sparql:literal">
    <xsl:value-of select="text()" />
</xsl:template>

<xsl:template
    match="sparql:literal[@datatype='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral']">
    <xsl:value-of disable-output-escaping="yes" select="text()" />
</xsl:template>
</xsl:stylesheet>
