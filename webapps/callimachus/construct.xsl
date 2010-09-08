<?xml version="1.0" encoding="UTF-8" ?>
<!--
   Copyright (c) 2009-2010 Zepheira LLC, Some rights reserved

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
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:calli="http://callimachusproject.org/rdf/2009/framework#">
	<xsl:param name="this" />
	<xsl:param name="mode" select="''" />
	<xsl:param name="element" select="'/1'" />
	<xsl:output omit-xml-declaration="yes" standalone="no" />
	<xsl:variable name="profile" select="concat($this, '?xslt&amp;mode=', $mode, '&amp;element=', $element)" />
	<xsl:variable name="data" select="/" />
	<xsl:variable name="variables"
		select="'http://callimachusproject.org/rdf/2009/framework/variables/?'" />
	<xsl:variable name="target"
		select="$data/rdf:RDF/rdf:Description[1]/@rdf:about" />
	<xsl:variable name="etag" select="$data/rdf:RDF/rdf:Description[@rdf:about=$target]/calli:etag/text()" />
	<xsl:template match="/">
		<xsl:variable name="html" select="document($profile)/*" />
		<xsl:variable name="about" select="$target" />
		<xsl:variable name="scope">
			<xsl:text></xsl:text>
		</xsl:variable>
		<xsl:variable name="newscope">
			<xsl:apply-templates mode="scope-about" select="$html">
				<xsl:with-param name="about" select="$about" />
				<xsl:with-param name="scope" select="$scope" />
			</xsl:apply-templates>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$data/rdf:RDF">
				<xsl:apply-templates select="$html">
					<xsl:with-param name="about" select="$about" />
					<xsl:with-param name="top" select="true()" />
					<xsl:with-param name="scope" select="$newscope" />
				</xsl:apply-templates>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="{local-name($html)}" namespace="{namespace-uri($html)}">
					<xsl:apply-templates
						select="$html/@*|$html/*|$html/text()|$html/comment()">
						<xsl:with-param name="about" select="$about" />
						<xsl:with-param name="top" select="true()" />
						<xsl:with-param name="scope" select="$newscope" />
					</xsl:apply-templates>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="head">
		<!-- <xsl:comment>head</xsl:comment> -->
		<xsl:param name="about" />
		<xsl:param name="nodeID" />
		<xsl:param name="top" />
		<xsl:param name="scope" />
		<xsl:variable name="newscope">
			<xsl:apply-templates mode="scope-about" select=".">
				<xsl:with-param name="about" select="$about" />
				<xsl:with-param name="nodeID" select="$nodeID" />
				<xsl:with-param name="scope" select="$scope" />
			</xsl:apply-templates>
		</xsl:variable>
		<xsl:copy>
			<xsl:apply-templates select="@*">
				<xsl:with-param name="about" select="$about" />
				<xsl:with-param name="nodeID" select="$nodeID" />
				<xsl:with-param name="scope" select="$newscope" />
			</xsl:apply-templates>
			<xsl:if test="$target and not(./*[local-name()='base'])">
				<xsl:element name="base">
					<xsl:attribute name="href"><xsl:value-of
						select="$target" /></xsl:attribute>
				</xsl:element>
			</xsl:if>
			<xsl:apply-templates select="*|text()|comment()">
				<xsl:with-param name="about" select="$about" />
				<xsl:with-param name="nodeID" select="$nodeID" />
				<xsl:with-param name="top" select="$top" />
				<xsl:with-param name="scope" select="$newscope" />
			</xsl:apply-templates>
			<xsl:if test="$etag">
				<meta http-equiv="etag" content="{$etag}" />
			</xsl:if>
		</xsl:copy>
	</xsl:template>
	<!-- <span property="skos:prefLabel"/> -->
	<xsl:template match="*[@property]">
		<!-- <xsl:comment>*[@property]</xsl:comment> -->
		<xsl:param name="about" />
		<xsl:param name="nodeID" />
		<xsl:param name="top" />
		<xsl:param name="scope" />
		<xsl:variable name="newscope">
			<xsl:apply-templates mode="scope-about" select=".">
				<xsl:with-param name="about" select="$about" />
				<xsl:with-param name="nodeID" select="$nodeID" />
				<xsl:with-param name="scope" select="$scope" />
			</xsl:apply-templates>
		</xsl:variable>
		<xsl:variable name="tmode">
			<xsl:choose>
				<xsl:when test="local-name(..)='option'">
					<xsl:value-of select="'value'" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="'property'" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:apply-templates mode="properties" select=".">
			<xsl:with-param name="about" select="$about" />
			<xsl:with-param name="nodeID" select="$nodeID" />
			<xsl:with-param name="top" select="$top" />
			<xsl:with-param name="curie" select="@property" />
			<xsl:with-param name="scope" select="$newscope" />
			<xsl:with-param name="tmode" select="$tmode" />
		</xsl:apply-templates>
	</xsl:template>
	<!--
		<a rel="skos:inScheme" href="?scheme">View Scheme</a>
	-->
	<xsl:template
		match="*[(@rel and contains(@rel, ':') or @rev) and (starts-with(@resource,'?') or starts-with(@href,'?'))]">
		<!-- <xsl:comment>*[(@rel and contains(@rel, ':') or @rev) and (starts-with(@resource,'?') or starts-with(@href,'?'))]</xsl:comment> -->
		<xsl:param name="about" />
		<xsl:param name="nodeID" />
		<xsl:param name="scope" />
		<xsl:variable name="variable">
			<xsl:choose>
				<xsl:when test="@resource">
					<xsl:value-of select="@resource" />
				</xsl:when>
				<xsl:when test="@href">
					<xsl:value-of select="@href" />
				</xsl:when>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="newscope">
			<xsl:apply-templates mode="scope-about" select=".">
				<xsl:with-param name="about" select="$about" />
				<xsl:with-param name="nodeID" select="$nodeID" />
				<xsl:with-param name="scope" select="$scope" />
			</xsl:apply-templates>
		</xsl:variable>
		<xsl:apply-templates mode="properties" select=".">
			<xsl:with-param name="about" select="$about" />
			<xsl:with-param name="nodeID" select="$nodeID" />
			<xsl:with-param name="curie" select="$variable" />
			<xsl:with-param name="scope" select="$newscope" />
			<xsl:with-param name="tmode" select="'resource'" />
		</xsl:apply-templates>
	</xsl:template>
	<!--
		<div rel="skos:hasTopConcept"> <div about="?concept"
		typeof="skos:Concept"> <span property="skos:prefLabel"/> </div> </div>
	-->
	<xsl:template match="*[(@rel and contains(@rel, ':') or @rev) and not(@resource or @href)]">
		<!-- <xsl:comment>*[(@rel and contains(@rel, ':') or @rev) and not(@resource or @href)]</xsl:comment> -->
		<xsl:param name="about" />
		<xsl:param name="nodeID" />
		<xsl:param name="top" />
		<xsl:param name="scope" />
		<xsl:variable name="rel">
			<xsl:choose>
				<xsl:when test="@rel and contains(@rel, ':')">
					<xsl:value-of  select="@rel" />
				</xsl:when>
				<xsl:otherwise>
				<xsl:value-of  select="@rev" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="newscope">
			<xsl:apply-templates mode="scope-about" select=".">
				<xsl:with-param name="about" select="$about" />
				<xsl:with-param name="nodeID" select="$nodeID" />
				<xsl:with-param name="scope" select="$scope" />
			</xsl:apply-templates>
		</xsl:variable>
		<xsl:copy>
			<xsl:if test="$top">
				<xsl:if test="1=count(*[@about or @src or @href])">
					<xsl:attribute name="data-rel"><xsl:value-of
						select="@rel" /></xsl:attribute>
					<xsl:attribute name="data-search">
						<xsl:value-of select="$this" />
						<xsl:text>?search&amp;mode=</xsl:text>
						<xsl:value-of select="$mode" />
						<xsl:text>&amp;element=</xsl:text>
						<xsl:apply-templates mode="xptr-element" select="." />
						<xsl:text>&amp;q={searchTerms}</xsl:text>
					</xsl:attribute>
					<xsl:attribute name="data-add">
						<xsl:value-of select="$this" />
						<xsl:text>?construct&amp;mode=</xsl:text>
						<xsl:value-of select="$mode" />
						<xsl:text>&amp;element=</xsl:text>
						<xsl:apply-templates mode="xptr-element" select="*[@about or @src or @href]" />
						<xsl:text>&amp;about={about}</xsl:text>
					</xsl:attribute>
				</xsl:if>
				<xsl:if test="not(*[@about or @src or @href]) and 1=count(*)">
					<xsl:attribute name="data-more">
						<xsl:value-of select="$this" />
						<xsl:text>?template&amp;mode=</xsl:text>
						<xsl:value-of select="$mode" />
						<xsl:text>&amp;element=</xsl:text>
						<xsl:apply-templates mode="xptr-element" select="*" />
					</xsl:attribute>
				</xsl:if>
			</xsl:if>
			<xsl:apply-templates select="@*">
				<xsl:with-param name="about" select="$about" />
				<xsl:with-param name="nodeID" select="$nodeID" />
				<xsl:with-param name="scope" select="$newscope" />
			</xsl:apply-templates>
			<xsl:for-each select="*|text()|comment()">
				<xsl:choose>
					<xsl:when test="starts-with(@about, '?')">
						<xsl:apply-templates mode="properties" select=".">
							<xsl:with-param name="about" select="$about" />
							<xsl:with-param name="nodeID" select="$nodeID" />
							<xsl:with-param name="curie" select="@about" />
							<xsl:with-param name="scope" select="$newscope" />
							<xsl:with-param name="tmode" select="'hanging'" />
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="not(@about) and starts-with(@src, '?')">
						<xsl:apply-templates mode="properties" select=".">
							<xsl:with-param name="about" select="$about" />
							<xsl:with-param name="nodeID" select="$nodeID" />
							<xsl:with-param name="curie" select="@src" />
							<xsl:with-param name="scope" select="$newscope" />
							<xsl:with-param name="tmode" select="'hanging'" />
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="not(@about) and not(@src) and starts-with(@href, '?')">
						<xsl:apply-templates mode="properties" select=".">
							<xsl:with-param name="about" select="$about" />
							<xsl:with-param name="nodeID" select="$nodeID" />
							<xsl:with-param name="curie" select="@href" />
							<xsl:with-param name="scope" select="$newscope" />
							<xsl:with-param name="tmode" select="'hanging'" />
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test=".//@rel and contains(.//@rel, ':') or .//@rev or .//@property or .//@typeof">
						<xsl:apply-templates mode="properties" select=".">
							<xsl:with-param name="about" select="$about" />
							<xsl:with-param name="nodeID" select="$nodeID" />
							<xsl:with-param name="curie" select="$rel" />
							<xsl:with-param name="scope" select="$newscope" />
							<xsl:with-param name="tmode" select="'hanging'" />
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<xsl:apply-templates mode="copy" select="." />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
		</xsl:copy>
	</xsl:template>
	<!-- <div> <span property="rdfs:label"/> </div> -->
	<xsl:template match="*">
		<xsl:param name="about" />
		<xsl:param name="nodeID" />
		<xsl:param name="top" />
		<xsl:param name="scope" />
		<xsl:choose>
			<xsl:when test="$top and starts-with(@about, '?') and not(contains($scope, concat(@about, '=')))">
				<!-- <xsl:comment>$top and starts-with(@about, '?') and not(contains($scope, concat(@about, '=')))</xsl:comment> -->
				<xsl:apply-templates mode="properties" select=".">
					<xsl:with-param name="about" select="$about" />
					<xsl:with-param name="top" select="$top" />
					<xsl:with-param name="curie" select="@about" />
					<xsl:with-param name="scope" select="$scope" />
					<xsl:with-param name="tmode" select="'hanging'" />
				</xsl:apply-templates>
			</xsl:when>
			<xsl:when test="$top and starts-with(@src, '?') and not(@rel and contains(@rel, ':') or @rev) and not(contains($scope, concat(@src, '=')))">
				<!-- <xsl:comment>$top and starts-with(@src, '?') and not(@rel and contains(@rel, ':') or @rev) and not(contains($scope, concat(@src, '=')))</xsl:comment> -->
				<xsl:apply-templates mode="properties" select=".">
					<xsl:with-param name="about" select="$about" />
					<xsl:with-param name="top" select="$top" />
					<xsl:with-param name="curie" select="@src" />
					<xsl:with-param name="scope" select="$scope" />
					<xsl:with-param name="tmode" select="'hanging'" />
				</xsl:apply-templates>
			</xsl:when>
			<xsl:when test="$top">
				<xsl:copy>
					<xsl:if test="1=count(*/@property)">
						<xsl:attribute name="data-property">
							<xsl:value-of select="*/@property" />
						</xsl:attribute>
						<xsl:attribute name="data-more">
							<xsl:value-of select="$this" />
							<xsl:text>?template&amp;mode=</xsl:text>
							<xsl:value-of select="$mode" />
							<xsl:text>&amp;element=</xsl:text>
							<xsl:apply-templates mode="xptr-element" select="*[@property]" />
						</xsl:attribute>
					</xsl:if>
					<xsl:if test="1=count(*[@rel and contains(@rel, ':') or @rev]) and *[(@rel and contains(@rel, ':') or @rev) and (starts-with(@resource,'?') or starts-with(@href,'?'))]">
						<xsl:attribute name="data-rel"><xsl:value-of select="*/@rel"/><xsl:value-of select="*/@rev"/></xsl:attribute>
						<xsl:attribute name="data-options">
							<xsl:value-of select="$this" />
							<xsl:text>?options&amp;mode=</xsl:text>
							<xsl:value-of select="$mode" />
							<xsl:text>&amp;element=</xsl:text>
							<xsl:apply-templates mode="xptr-element" select="." />
						</xsl:attribute>
					</xsl:if>
					<xsl:apply-templates select="@*|*|comment()|text()">
						<xsl:with-param name="about" select="$about" />
						<xsl:with-param name="nodeID" select="$nodeID" />
						<xsl:with-param name="top" select="$top" />
						<xsl:with-param name="scope" select="$scope" />
					</xsl:apply-templates>
				</xsl:copy>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:apply-templates select="@*|*|comment()|text()">
						<xsl:with-param name="about" select="$about" />
						<xsl:with-param name="nodeID" select="$nodeID" />
						<xsl:with-param name="top" select="$top" />
						<xsl:with-param name="scope" select="$scope" />
					</xsl:apply-templates>
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="@*[starts-with(., '?')]">
		<xsl:param name="about" />
		<xsl:param name="nodeID" />
		<xsl:param name="scope" />
		<xsl:choose>
		<xsl:when test="contains($scope, concat(., '=')) and not(contains($scope, concat(., '=&#xA;')))">
			<xsl:attribute name="{name()}" namespace="{namespace-uri()}">
				<xsl:value-of select="substring-before(substring-after($scope, concat(., '=')), '&#xA;')" />
			</xsl:attribute>
		</xsl:when>
		<xsl:when test="not(contains($scope, concat(., '=')))">
			<xsl:attribute name="{name()}" namespace="{namespace-uri()}">
				<xsl:value-of select="." />
			</xsl:attribute>
		</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- <a href="">This Entity</a> -->
	<xsl:template match="@href[.='']">
		<xsl:param name="about" />
		<xsl:attribute name="{name()}" namespace="{namespace-uri()}">
			<xsl:value-of select="$about" />
		</xsl:attribute>
	</xsl:template>
	<xsl:template match="@*">
		<xsl:attribute name="{name()}" namespace="{namespace-uri()}">
			<xsl:value-of select="." />
		</xsl:attribute>
	</xsl:template>
	<xsl:template match="comment()">
		<xsl:copy />
	</xsl:template>
	<!-- scope -->
	<xsl:template mode="scope-about" match="*">
		<!-- <xsl:comment>scope-about</xsl:comment> -->
		<xsl:param name="about" />
		<xsl:param name="nodeID" />
		<xsl:param name="scope" />
		<xsl:value-of select="$scope" />
		<xsl:if test="starts-with(@about, '?') and not(contains($scope, concat(@about, '=')))">
			<xsl:value-of select="@about" />
			<xsl:text>=</xsl:text>
			<xsl:value-of select="$about" />
			<xsl:if test="$nodeID">
				<xsl:value-of select="concat('[_:', $nodeID, ']')" />
			</xsl:if>
			<xsl:text>&#xA;</xsl:text>
		</xsl:if>
		<xsl:if test="starts-with(@src, '?') and not(contains($scope, concat(@src, '=')))">
			<xsl:value-of select="@src" />
			<xsl:text>=</xsl:text>
			<xsl:value-of select="$about" />
			<xsl:text>&#xA;</xsl:text>
		</xsl:if>
		<xsl:if test="starts-with(@href, '?') and @property and not(contains($scope, concat(@href, '=')))">
			<xsl:value-of select="@href" />
			<xsl:text>=</xsl:text>
			<xsl:value-of select="$about" />
			<xsl:text>&#xA;</xsl:text>
		</xsl:if>
	</xsl:template>
	<xsl:template mode="scope-resource" match="*">
		<!-- <xsl:comment>scope-resource</xsl:comment> -->
		<xsl:param name="about" />
		<xsl:param name="nodeID" />
		<xsl:param name="scope" />
		<xsl:value-of select="$scope" />
		<xsl:if test="starts-with(@resource, '?') and not(contains($scope, concat(@resource, '=')))">
			<xsl:value-of select="@resource" />
			<xsl:text>=</xsl:text>
			<xsl:value-of select="$about" />
			<xsl:if test="$nodeID">
				<xsl:value-of select="concat('[_:', $nodeID, ']')" />
			</xsl:if>
			<xsl:text>&#xA;</xsl:text>
		</xsl:if>
		<xsl:if test="starts-with(@href, '?') and not(contains($scope, concat(@href, '=')))">
			<xsl:value-of select="@href" />
			<xsl:text>=</xsl:text>
			<xsl:value-of select="$about" />
			<xsl:text>&#xA;</xsl:text>
		</xsl:if>
	</xsl:template>
	<!-- properties -->
	<xsl:template mode="properties" match="*">
		<!-- <xsl:comment>properties</xsl:comment> -->
		<xsl:param name="about" />
		<xsl:param name="nodeID" />
		<xsl:param name="top" />
		<xsl:param name="curie" />
		<xsl:param name="scope" />
		<xsl:param name="tmode" />
		<xsl:variable name="localname">
			<xsl:choose>
				<xsl:when test="starts-with($curie, '?')">
					<xsl:value-of select="substring-after($curie, '?')" />
				</xsl:when>
				<xsl:when test="contains($curie, ':')">
					<xsl:value-of select="substring-after($curie, ':')" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$curie" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="ns">
			<xsl:choose>
				<xsl:when test="starts-with($curie, '?')">
					<xsl:value-of select="$variables" />
				</xsl:when>
				<xsl:when test="contains($curie, ':')">
					<xsl:value-of select="ancestor-or-self::*/namespace::*[name()=substring-before($curie,':')]" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="'http://www.w3.org/1999/xhtml/vocab#'" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="tag" select="." />
		<xsl:for-each select="$data">
			<xsl:for-each select="/rdf:RDF/rdf:Description[@rdf:about=$about or @rdf:nodeID=$nodeID]/*[local-name()=$localname and namespace-uri()=$ns]">
				<xsl:sort select="@rdf:resource" />
				<xsl:sort select="." />
				<xsl:apply-templates mode="rdf" select=".">
					<xsl:with-param name="tag" select="$tag" />
					<xsl:with-param name="top" select="$top" />
					<xsl:with-param name="curie" select="$curie" />
					<xsl:with-param name="scope" select="$scope" />
					<xsl:with-param name="tmode" select="$tmode" />
				</xsl:apply-templates>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>
	<!-- <skos:prefLabel xml:lang="en">My Label</skos:prefLabel> -->
	<xsl:template mode="rdf" match="*">
		<!-- <xsl:comment>rdf</xsl:comment> -->
		<xsl:param name="tag" />
		<xsl:param name="scope" />
		<xsl:param name="tmode" />
		<xsl:choose>
			<xsl:when test="$tmode='value'">
				<xsl:value-of select="*|text()|comment()" />
			</xsl:when>
			<xsl:when test="$tmode='property'">
				<xsl:element name="{local-name($tag)}" namespace="{namespace-uri($tag)}">
					<xsl:apply-templates select="$tag/@*">
						<xsl:with-param name="about" select="../@rdf:about" />
						<xsl:with-param name="nodeID" select="../@rdf:nodeID" />
						<xsl:with-param name="scope" select="$scope" />
					</xsl:apply-templates>
					<xsl:if test="@xml:lang">
						<xsl:attribute name="lang"><xsl:value-of select="@xml:lang" /></xsl:attribute>
					</xsl:if>
					<xsl:if test="@rdf:parseType='Literal'">
						<xsl:attribute name="datatype"><xsl:text>rdf:XMLLiteral</xsl:text></xsl:attribute>
					</xsl:if>
					<xsl:if test="@rdf:datatype">
						<xsl:variable name="dt" select="@rdf:datatype" />
						<xsl:variable name="namespace"
							select="$tag/ancestor-or-self::*/namespace::*[string-length(.) > 0 and starts-with($dt, .)]" />
						<xsl:variable name="dt_prefix" select="name($namespace)" />
						<xsl:variable name="dt_localname"
							select="substring($dt, string-length($namespace) + 1)" />
						<xsl:if test="$namespace">
							<xsl:attribute name="datatype"><xsl:value-of
								select="concat($dt_prefix, ':', $dt_localname)" /></xsl:attribute>
						</xsl:if>
					</xsl:if>
					<xsl:variable name="value">
						<xsl:choose>
							<xsl:when test="@rdf:parseType='Literal'">
								<xsl:apply-templates
									mode="escape-xml" select="*|text()|comment()" />
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="text()" />
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					<xsl:choose>
						<xsl:when test="local-name($tag)='input'">
							<xsl:attribute name="content"><xsl:value-of select="$value" /></xsl:attribute>
							<xsl:attribute name="value"><xsl:value-of select="$value" /></xsl:attribute>
						</xsl:when>
						<xsl:when test="local-name($tag)='textarea'">
							<xsl:attribute name="content"><xsl:value-of select="$value" /></xsl:attribute>
							<xsl:value-of select="$value" />
						</xsl:when>
						<xsl:when test="local-name($tag)='title'">
							<xsl:attribute name="content"><xsl:value-of select="$value" /></xsl:attribute>
							<xsl:value-of select="*|text()" />
						</xsl:when>
						<xsl:otherwise>
							<!-- copy-of can cause NullPointerException in XSLTC -->
							<xsl:apply-templates mode="copy" select="*|text()|comment()"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- <skos:inScheme rdf:resource="/my-scheme"/> -->
	<xsl:template mode="rdf" match="*[@rdf:resource or @rdf:nodeID]">
		<!-- <xsl:comment>*[@rdf:resource or @rdf:nodeID]</xsl:comment> -->
		<xsl:param name="tag" />
		<xsl:param name="top" />
		<xsl:param name="curie" />
		<xsl:param name="scope" />
		<xsl:param name="tmode" />
		<xsl:choose>
			<xsl:when test="$tmode='hanging'">
				<xsl:variable name="newscope">
					<xsl:apply-templates mode="scope-about" select="$tag">
						<xsl:with-param name="about" select="@rdf:resource" />
						<xsl:with-param name="nodeID" select="@rdf:nodeID" />
						<xsl:with-param name="scope" select="$scope" />
					</xsl:apply-templates>
				</xsl:variable>
				<xsl:apply-templates select="$tag">
					<xsl:with-param name="about" select="@rdf:resource" />
					<xsl:with-param name="nodeID" select="@rdf:nodeID" />
					<xsl:with-param name="top" select="$top" />
					<xsl:with-param name="scope" select="$newscope" />
				</xsl:apply-templates>
			</xsl:when>
			<xsl:when test="$tmode='resource'">
				<xsl:element name="{local-name($tag)}" namespace="{namespace-uri($tag)}">
					<xsl:variable name="newscope">
						<xsl:apply-templates mode="scope-resource" select="$tag">
							<xsl:with-param name="about" select="@rdf:resource" />
							<xsl:with-param name="nodeID" select="@rdf:nodeID" />
							<xsl:with-param name="scope" select="$scope" />
						</xsl:apply-templates>
					</xsl:variable>
					<xsl:apply-templates
						select="$tag/@*[local-name()!='resource' and local-name()!='href']">
						<xsl:with-param name="about" select="../@rdf:about" />
						<xsl:with-param name="nodeID" select="../@rdf:nodeID" />
						<xsl:with-param name="scope" select="$newscope" />
					</xsl:apply-templates>
					<xsl:if test="$tag/@resource and @rdf:resource">
						<xsl:attribute name="resource"><xsl:value-of
							select="@rdf:resource" /></xsl:attribute>
					</xsl:if>
					<xsl:if test="$tag/@resource and @rdf:nodeID">
						<xsl:attribute name="resource"><xsl:value-of
							select="concat('[_:', @rdf:nodeID, ']')" /></xsl:attribute>
					</xsl:if>
					<xsl:if test="$tag/@href">
						<xsl:attribute name="href"><xsl:value-of
							select="@rdf:resource" /></xsl:attribute>
					</xsl:if>
					<xsl:if test="1=count($tag/*[@rel='rdf:first']/*)">
						<xsl:attribute name="data-member">
							<xsl:value-of select="$this" />
							<xsl:text>?construct&amp;mode=</xsl:text>
							<xsl:value-of select="$mode" />
							<xsl:text>&amp;element=</xsl:text>
							<xsl:apply-templates mode="xptr-element" select="$tag/*[@rel='rdf:first']/*" />
							<xsl:text>&amp;about={about}</xsl:text>
						</xsl:attribute>
					</xsl:if>
					<xsl:choose>
						<xsl:when test="@rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'">
						</xsl:when>
						<xsl:when test="$tag/@rel='rdf:rest'">
							<xsl:apply-templates select="$tag/../*|$tag/../comment()|$tag/../text()">
								<xsl:with-param name="about" select="@rdf:resource" />
								<xsl:with-param name="nodeID" select="@rdf:nodeID" />
								<xsl:with-param name="scope" select="$newscope" />
							</xsl:apply-templates>
						</xsl:when>
						<xsl:otherwise>
							<xsl:apply-templates select="$tag/*|$tag/comment()|$tag/text()">
								<xsl:with-param name="about" select="@rdf:resource" />
								<xsl:with-param name="nodeID" select="@rdf:nodeID" />
								<xsl:with-param name="scope" select="$newscope" />
							</xsl:apply-templates>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- copy -->
	<xsl:template mode="copy" match="*">
		<xsl:element name="{local-name()}" namespace="{namespace-uri()}">
			<xsl:apply-templates mode="copy" select="@*|*|text()|comment()" />
		</xsl:element>
	</xsl:template>
	<xsl:template mode="copy" match="@*">
		<xsl:attribute name="{name()}" namespace="{namespace-uri()}">
			<xsl:value-of select="." />
		</xsl:attribute>
	</xsl:template>
	<xsl:template mode="copy" match="comment()">
		<xsl:comment><xsl:value-of select="." /></xsl:comment>
	</xsl:template>
	<xsl:template mode="copy" match="text()">
		<xsl:value-of select="."/>
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
	<!-- escape-xml -->
	<xsl:template mode="escape-xml" match="*">
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()" />
		<xsl:apply-templates mode="escape-xml" select="@*" />
		<xsl:choose>
			<xsl:when test="*|comment()|text()">
				<xsl:text>&gt;</xsl:text>
				<xsl:apply-templates mode="escape-xml" select="*|comment()|text()" />
				<xsl:text>&lt;/</xsl:text><xsl:value-of select="local-name()" /><xsl:text>&gt;</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text> /&gt;</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template mode="escape-xml" match="@*">
		<xsl:text> </xsl:text><xsl:value-of select="name()" /><xsl:text>="</xsl:text>
		<xsl:call-template name="escape-xml-attribute">
			<xsl:with-param name="s" select="." />
		</xsl:call-template>
		<xsl:text>"</xsl:text>
	</xsl:template>
	<xsl:template mode="escape-xml" match="comment()">
		<xsl:text>&lt;--</xsl:text>
		<xsl:call-template name="escape-xml-text">
			<xsl:with-param name="s" select="." />
		</xsl:call-template>
		<xsl:text>--&gt;</xsl:text>
	</xsl:template>
	<xsl:template mode="escape-xml" match="text()">
		<xsl:call-template name="escape-xml-text">
			<xsl:with-param name="s" select="." />
		</xsl:call-template>
	</xsl:template>
	<xsl:template name="escape-xml-attribute">
		<xsl:param name="s" />
		<xsl:choose>
			<xsl:when test="contains($s,'&amp;')">
				<xsl:call-template name="encode-xml-attribute-strng">
					<xsl:with-param name="s"
						select="concat(substring-before($s,'&amp;'),'&amp;amp;')" />
				</xsl:call-template>
				<xsl:call-template name="escape-xml-attribute">
					<xsl:with-param name="s" select="substring-after($s,'&amp;')" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="encode-xml-attribute-strng">
					<xsl:with-param name="s" select="$s" />
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="encode-xml-attribute-strng">
		<xsl:param name="s" />
		<xsl:choose>
			<!-- quote -->
			<xsl:when test="contains($s,'&quot;')">
				<xsl:call-template name="encode-xml-attribute-strng">
					<xsl:with-param name="s"
						select="concat(substring-before($s,'&quot;'),'&amp;quot;',substring-after($s,'&quot;'))" />
				</xsl:call-template>
			</xsl:when>
			<!-- line feed -->
			<xsl:when test="contains($s,'&#xA;')">
				<xsl:call-template name="encode-xml-attribute-strng">
					<xsl:with-param name="s"
						select="concat(substring-before($s,'&#xA;'),'&amp;#xA;',substring-after($s,'&#xA;'))" />
				</xsl:call-template>
			</xsl:when>
			<!-- carriage return -->
			<xsl:when test="contains($s,'&#xD;')">
				<xsl:call-template name="encode-xml-attribute-strng">
					<xsl:with-param name="s"
						select="concat(substring-before($s,'&#xD;'),'&amp;#xD;',substring-after($s,'&#xD;'))" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="encode-xml-strng">
					<xsl:with-param name="s" select="$s" />
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="escape-xml-text">
		<xsl:param name="s" />
		<xsl:choose>
			<xsl:when test="contains($s,'&amp;')">
				<xsl:call-template name="encode-xml-strng">
					<xsl:with-param name="s"
						select="concat(substring-before($s,'&amp;'),'&amp;amp;')" />
				</xsl:call-template>
				<xsl:call-template name="escape-xml-text">
					<xsl:with-param name="s" select="substring-after($s,'&amp;')" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="encode-xml-strng">
					<xsl:with-param name="s" select="$s" />
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="encode-xml-strng">
		<xsl:param name="s" />
		<xsl:choose>
			<!-- less than -->
			<xsl:when test="contains($s,'&lt;')">
				<xsl:call-template name="encode-xml-strng">
					<xsl:with-param name="s"
						select="concat(substring-before($s,'&lt;'),'&amp;lt;',substring-after($s,'&lt;'))" />
				</xsl:call-template>
			</xsl:when>
			<!-- greater than -->
			<xsl:when test="contains($s,'&gt;')">
				<xsl:call-template name="encode-xml-strng">
					<xsl:with-param name="s"
						select="concat(substring-before($s,'&gt;'),'&amp;gt;',substring-after($s,'&gt;'))" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$s" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
