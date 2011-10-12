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

	<xsl:output method="xml" />

	<xsl:template match="@*|comment()|text()">
		<xsl:copy />
	</xsl:template>

	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="xhtml:form">
		<xsl:apply-templates mode="form" select="." />
	</xsl:template>

	<xsl:template mode="form" match="@*|comment()|text()">
		<xsl:copy />
	</xsl:template>

	<xsl:template mode="form" match="*">
		<xsl:copy>
			<xsl:call-template name="data-attributes" />
			<xsl:apply-templates mode="form" select="@*|node()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template mode="form" match="xhtml:button[contains(@class, 'add') and not(@data-dialog)]">
		<xsl:copy>
			<xsl:call-template name="data-attributes" />
			<xsl:apply-templates mode="form" select="@*|node()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template name="data-attributes">
		<xsl:apply-templates mode="data-var" select="@*"/>
		<xsl:apply-templates mode="data-expression" select="@*"/>
		<xsl:if test="text() and not(*|comment())">
			<xsl:call-template name="data-text-expression">
				<xsl:with-param name="text" select="string(.)"/>
			</xsl:call-template>
		</xsl:if>
		<xsl:if test="xhtml:option[@about or @resource] or xhtml:label[@about or @resource]">
			<!-- Called to populate select/radio/checkbox -->
			<xsl:attribute name="data-options">
				<xsl:value-of select="$this" />
				<xsl:text>?options&amp;query=</xsl:text>
				<xsl:value-of select="$query" />
				<xsl:text>&amp;element=</xsl:text>
				<xsl:apply-templates mode="xptr-element" select="." />
			</xsl:attribute>
		</xsl:if>
		<xsl:if test="*[@about or @resource] and not(@data-construct)">
			<!-- Called when a resource URI is dropped to construct its label -->
			<xsl:attribute name="data-construct">
				<xsl:value-of select="$this" />
				<xsl:text>?construct&amp;query=</xsl:text>
				<xsl:value-of select="$query" />
				<xsl:text>&amp;element=</xsl:text>
				<xsl:apply-templates mode="xptr-element" select="." />
				<xsl:text>&amp;about={about}</xsl:text>
			</xsl:attribute>
		</xsl:if>
		<xsl:if test="*[@about or @resource]//*[contains(' rdfs:label skos:prefLabel skos:altLabel skosxl:literalForm ', concat(' ', @property, ' ')) or contains(text(), '{rdfs:label}') or contains(text(), '{skos:prefLabel}') or contains(text(), '{skos:altLabel}') or contains(text(), '{skosxl:literalForm}')] and not(@data-search)">
			<!-- Lookup possible members by label -->
			<xsl:attribute name="data-search">
				<xsl:value-of select="$this" />
				<xsl:text>?search&amp;query=</xsl:text>
				<xsl:value-of select="$query" />
				<xsl:text>&amp;element=</xsl:text>
				<xsl:apply-templates mode="xptr-element" select="." />
				<xsl:text>&amp;q={searchTerms}</xsl:text>
			</xsl:attribute>
		</xsl:if>
		<xsl:if test="*[@about or @typeof or @resource or @property] and not(@data-add)">
			<!-- Called to insert another property value or node -->
			<xsl:attribute name="data-add">
				<xsl:value-of select="$this" />
				<xsl:text>?template&amp;query=</xsl:text>
				<xsl:value-of select="$query" />
				<xsl:text>&amp;element=</xsl:text>
				<xsl:apply-templates mode="xptr-element" select="." />
			</xsl:attribute>
		</xsl:if>
	</xsl:template>

	<!-- variable expressions -->
	<xsl:template mode="data-var" match="@about|@resource|@content|@href|@src">
		<xsl:if test="starts-with(., '?')">
			<xsl:attribute name="data-var-{name()}">
				<xsl:value-of select="." />
			</xsl:attribute>
		</xsl:if>
	</xsl:template>
	<xsl:template mode="data-var" match="@*" />
	<xsl:template mode="data-expression" match="@*">
		<xsl:variable name="expression">
			<xsl:value-of select="substring-before(substring-after(., '{'), '}')"/>
		</xsl:variable>
		<xsl:if test="string(.) = concat('{', $expression, '}')">
			<xsl:attribute name="data-expression-{name()}">
				<xsl:value-of select="$expression" />
			</xsl:attribute>
		</xsl:if>
	</xsl:template>
	<xsl:template name="data-text-expression">
		<xsl:param name="text" />
		<xsl:variable name="expression">
			<xsl:value-of select="substring-before(substring-after($text, '{'), '}')"/>
		</xsl:variable>
		<xsl:if test="$text = concat('{', $expression, '}')">
			<xsl:attribute name="data-text-expression">
				<xsl:value-of select="$expression" />
			</xsl:attribute>
		</xsl:if>
	</xsl:template>

	<!-- xptr-element -->
	<xsl:template mode="xptr-element" match="/*">
		<xsl:value-of select="'/1'" />
	</xsl:template>

	<xsl:template mode="xptr-element" match="*">
		<xsl:apply-templates mode="xptr-element" select=".." />
		<xsl:text>/</xsl:text>
		<xsl:value-of select="count(preceding-sibling::*) + 1" />
	</xsl:template>

</xsl:stylesheet>
