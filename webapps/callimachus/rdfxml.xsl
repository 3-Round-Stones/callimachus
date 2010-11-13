<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	<xsl:template name="substring-after-last">
		<xsl:param name="string"/>
		<xsl:param name="delimiter"/>
		<xsl:if test="not(contains(substring($string, 0, string-length($string) - 1),$delimiter))">
			<xsl:value-of select="$string"/>
		</xsl:if>
		<xsl:if test="contains(substring($string, 0, string-length($string) - 1),$delimiter)">
			<xsl:call-template name="substring-after-last">
				<xsl:with-param name="string" select="substring-after($string,$delimiter)"/>
				<xsl:with-param name="delimiter" select="$delimiter"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	<xsl:template name="substring-before-last">
		<xsl:param name="string"/>
		<xsl:param name="delimiter"/>
		<xsl:if test="contains($string,$delimiter)">
			<xsl:value-of select="substring-before($string,$delimiter)"/>
			<xsl:if test="contains(substring-after($string,$delimiter),$delimiter)">
				<xsl:value-of select="$delimiter"/>
				<xsl:call-template name="substring-before-last">
					<xsl:with-param name="string" select="substring-after($string,$delimiter)"/>
					<xsl:with-param name="delimiter" select="$delimiter"/>
				</xsl:call-template>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	<xsl:template name="curie">
		<xsl:param name="iri"/>
		<xsl:variable name="namespace">
			<xsl:choose>
				<xsl:when test="contains($iri, '#')">
					<xsl:call-template name="substring-before-last">
						<xsl:with-param name="string" select="$iri"/>
						<xsl:with-param name="delimiter" select="'#'"/>
					</xsl:call-template>
					<xsl:text>#</xsl:text>
				</xsl:when>
				<xsl:when test="contains($iri, '/')">
					<xsl:call-template name="substring-before-last">
						<xsl:with-param name="string" select="$iri"/>
						<xsl:with-param name="delimiter" select="'/'"/>
					</xsl:call-template>
					<xsl:text>/</xsl:text>
				</xsl:when>
				<xsl:when test="contains($iri, ':')">
					<xsl:call-template name="substring-before-last">
						<xsl:with-param name="string" select="$iri"/>
						<xsl:with-param name="delimiter" select="':'"/>
					</xsl:call-template>
					<xsl:text>:</xsl:text>
				</xsl:when>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="local">
			<xsl:choose>
				<xsl:when test="contains($iri, '#')">
					<xsl:call-template name="substring-after-last">
						<xsl:with-param name="string" select="$iri"/>
						<xsl:with-param name="delimiter" select="'#'"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:when test="contains($iri, '/')">
					<xsl:call-template name="substring-after-last">
						<xsl:with-param name="string" select="$iri"/>
						<xsl:with-param name="delimiter" select="'/'"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:when test="contains($iri, ':')">
					<xsl:call-template name="substring-after-last">
						<xsl:with-param name="string" select="$iri"/>
						<xsl:with-param name="delimiter" select="':'"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$iri" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="ns" select="document($profile)//*[@property='rdfa:uri' and @content=$namespace]" />
		<xsl:if test="$namespace and $ns">
			<xsl:value-of select="$ns/../*[@property='rdfa:prefix']" />
			<xsl:text>:</xsl:text>
		</xsl:if>
		<xsl:value-of select="$local" />
	</xsl:template>
	<xsl:template match="rdf:RDF">
		<div class="graph">
			<xsl:apply-templates select="*" />
		</div>
	</xsl:template>
	<xsl:template match="*">
		<li>
			<label class="predicate">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<span class="literal">
				<xsl:attribute name="property">
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:apply-templates />
			</span>
		</li>
	</xsl:template>
	<xsl:template match="*[@rdf:nodeID]">
		<li>
			<label class="predicate">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<a href="#{@rdf:nodeID}" class="uri">
				<xsl:attribute name="rel">
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:value-of select="@rdf:nodeID" />
			</a>
		</li>
	</xsl:template>
	<xsl:template match="*[@rdf:resource]">
		<li>
			<label class="predicate">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<a href="{@rdf:resource}" class="uri">
				<xsl:attribute name="rel">
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="@rdf:resource"/>
				</xsl:call-template>
			</a>
		</li>
	</xsl:template>
	<xsl:template match="*[@rdf:datatype]">
		<li>
			<label class="predicate">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<span class="literal">
				<xsl:attribute name="property">
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:attribute name="datatype">
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="@rdf:datatype" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:attribute name="title">
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="@rdf:datatype" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:apply-templates />
			</span>
		</li>
	</xsl:template>
	<xsl:template match="*[@rdf:parseType='Literal']">
		<li>
			<label class="predicate">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<span class="literal">
				<xsl:attribute name="property">
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:copy-of select="node()" />
			</span>
		</li>
	</xsl:template>
	<xsl:template match="*[@rdf:parseType='Resource']">
		<li>
			<label class="predicate">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<ul>
				<xsl:attribute name="rel">
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:apply-templates select="*" />
			</ul>
		</li>
	</xsl:template>
	<xsl:template match="*[@rdf:parseType='Collection']">
		<li>
			<label class="predicate">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<ol>
				<xsl:apply-templates select="*" />
			</ol>
		</li>
	</xsl:template>
	<xsl:template match="*[@rdf:about]">
		<div about="{@rdf:about}">
			<xsl:attribute name="typeof">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</xsl:attribute>
			<a href="{@rdf:about}" class="uri">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="@rdf:about"/>
				</xsl:call-template>
			</a>
			<span> a </span>
			<span>
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</span>
			<ul>
				<xsl:apply-templates select="*" />
			</ul>
		</div>
	</xsl:template>
	<xsl:template match="*[@rdf:ID]">
		<div about="#{@rdf:ID}">
			<xsl:attribute name="typeof">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</xsl:attribute>
			<a href="#{@rdf:ID}" class="uri">
				<xsl:value-of select="@rdf:ID"/>
			</a>
			<span> a </span>
			<span>
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</span>
			<ul>
				<xsl:apply-templates select="*" />
			</ul>
		</div>
	</xsl:template>
	<xsl:template match="*[@rdf:nodeID][*]">
		<div about="_:{@rdf:nodeID}">
			<xsl:attribute name="typeof">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</xsl:attribute>
			<a name="{@rdf:nodeID}" id="{@rdf:nodeID}" class="bnode">
				<xsl:value-of select="@rdf:nodeID" />
			</a>
			<span> a </span>
			<span>
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</span>
			<ul>
				<xsl:apply-templates select="*" />
			</ul>
		</div>
	</xsl:template>
	<xsl:template match="rdf:Description[@rdf:about]">
		<div about="{@rdf:about}">
			<a href="{@rdf:about}" class="uri">
				<xsl:call-template name="curie">
					<xsl:with-param name="iri" select="@rdf:about"/>
				</xsl:call-template>
			</a>
			<ul>
				<xsl:apply-templates select="*" />
			</ul>
		</div>
	</xsl:template>
	<xsl:template match="rdf:Description[@rdf:ID]">
		<div about="#{@rdf:ID}">
			<a href="#{@rdf:ID}" class="uri">
				<xsl:value-of select="@rdf:ID"/>
			</a>
			<ul>
				<xsl:apply-templates select="*" />
			</ul>
		</div>
	</xsl:template>
	<xsl:template match="rdf:Description[@rdf:nodeID]">
		<div about="_:{@rdf:nodeID}">
			<a name="{@rdf:nodeID}" id="{@rdf:nodeID}" class="bnode">
				<xsl:value-of select="@rdf:nodeID" />
			</a>
			<ul>
				<xsl:apply-templates select="*" />
			</ul>
		</div>
	</xsl:template>
</xsl:stylesheet>
