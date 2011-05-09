<?xml version="1.0" encoding="UTF-8" ?>
<!--
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="xhtml"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:calli="http://callimachusproject.org/rdf/2009/framework#">

	<xsl:param name="this" />
	<xsl:param name="query" select="''" />
	<xsl:param name="element" select="'/1'" />

	<xsl:output method="xml" />

	<!--
		<div rel="skos:hasTopConcept"> <div about="?concept"
		typeof="skos:Concept"> <span property="skos:prefLabel"/> </div> </div>
	-->
	<xsl:template match="*[(@rel and contains(@rel, ':') or @rev) and not(@resource or @href)]">
		<xsl:copy>
			<xsl:if test="1=count(*[@about or @src or @href])">
				<xsl:attribute name="data-rel"><xsl:value-of select="@rel" /></xsl:attribute>
				<xsl:attribute name="data-search">
					<xsl:value-of select="$this" />
					<xsl:text>?search&amp;query=</xsl:text>
					<xsl:value-of select="$query" />
					<xsl:text>&amp;element=</xsl:text>
					<xsl:apply-templates mode="xptr-element" select="." />
					<xsl:text>&amp;q={searchTerms}</xsl:text>
				</xsl:attribute>
				<xsl:attribute name="data-add">
					<xsl:value-of select="$this" />
					<xsl:text>?construct&amp;query=</xsl:text>
					<xsl:value-of select="$query" />
					<xsl:text>&amp;element=</xsl:text>
					<xsl:apply-templates mode="xptr-element" select="*[@about or @src or @href]" />
					<xsl:text>&amp;about={about}</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:if test="not(*[@about or @src or @href]) and 1=count(*)">
				<xsl:attribute name="data-more">
					<xsl:value-of select="$this" />
					<xsl:text>?template&amp;query=</xsl:text>
					<xsl:value-of select="$query" />
					<xsl:text>&amp;element=</xsl:text>
					<xsl:apply-templates mode="xptr-element" select="*" />
				</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates mode="copy" select="@*|node()"/>
		</xsl:copy>
	</xsl:template>

	<!-- <div> <span property="rdfs:label"/> </div> -->
	<xsl:template match="*">
		<xsl:copy>
			<xsl:if test="1=count(*/@property)">
				<xsl:attribute name="data-property">
					<xsl:value-of select="*/@property" />
				</xsl:attribute>
				<xsl:attribute name="data-more">
					<xsl:value-of select="$this" />
					<xsl:text>?template&amp;query=</xsl:text>
					<xsl:value-of select="$query" />
					<xsl:text>&amp;element=</xsl:text>
					<xsl:apply-templates mode="xptr-element" select="*[@property]" />
				</xsl:attribute>
			</xsl:if>
			<xsl:if test="1=count(*[@rel or @rev]) and *[(@rel and contains(@rel, ':') or @rev) and (starts-with(@resource,'?') or starts-with(@href,'?'))]">
				<xsl:attribute name="data-rel"><xsl:value-of select="*/@rel"/><xsl:value-of select="*/@rev"/></xsl:attribute>
				<xsl:attribute name="data-options">
					<xsl:value-of select="$this" />
					<xsl:text>?options&amp;query=</xsl:text>
					<xsl:value-of select="$query" />
					<xsl:text>&amp;element=</xsl:text>
					<xsl:apply-templates mode="xptr-element" select="." />
				</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@*|comment()|text()">
		<xsl:copy />
	</xsl:template>

	<!-- copy -->
	<xsl:template mode="copy" match="node()">
		<xsl:copy>
			<xsl:apply-templates mode="copy" select="@*|node()" />
		</xsl:copy>
	</xsl:template>
	<xsl:template mode="copy" match="@*">
		<xsl:copy/>
	</xsl:template>

	<!-- xptr-element -->
	<xsl:template mode="xptr-element" match="/*">
		<xsl:value-of select="$element" />
	</xsl:template>
	<xsl:template mode="xptr-element" match="*">
		<xsl:apply-templates mode="xptr-element" select=".." />
		<xsl:text>/</xsl:text>
		<xsl:value-of select="count(preceding-sibling::*) + 1" />
	</xsl:template>

</xsl:stylesheet>
