<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	<xsl:output method="xml" encoding="UTF-8"/>
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
			<xsl:value-of select="$delimiter" />
		</xsl:if>
		<xsl:if test="contains($string,$delimiter)">
			<xsl:call-template name="substring-before-last">
				<xsl:with-param name="string" select="substring-after($string,$delimiter)"/>
				<xsl:with-param name="delimiter" select="$delimiter"/>
			</xsl:call-template>
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
				</xsl:when>
				<xsl:when test="contains($iri, '/')">
					<xsl:call-template name="substring-before-last">
						<xsl:with-param name="string" select="$iri"/>
						<xsl:with-param name="delimiter" select="'/'"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:when test="contains($iri, ':')">
					<xsl:call-template name="substring-before-last">
						<xsl:with-param name="string" select="$iri"/>
						<xsl:with-param name="delimiter" select="':'"/>
					</xsl:call-template>
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
		<xsl:variable name="profile" select="document('/callimachus/profile')//*[@property='rdfa:uri' and @content=$namespace]" />
		<xsl:if test="$namespace and $profile">
			<xsl:value-of select="$profile/../*[@property='rdfa:prefix']" />
			<xsl:text>:</xsl:text>
		</xsl:if>
		<xsl:value-of select="$local" />
	</xsl:template>
	<xsl:template match="/">
		<html>
			<head>
				<xsl:if test="/sparql:sparql">
					<title>SPARQL Results</title>
				</xsl:if>
				<xsl:if test="/rdf:RDF">
					<title>RDF Graph</title>
				</xsl:if>
				<style>
					.uri, .bnode, .literal { font-size: large; }
					.bnode, .literal { font-family: monospace; white-space: pre-wrap; }
					.predicate { font-weight: bold; }
				</style>
			</head>
			<body>
				<xsl:if test="/sparql:sparql">
					<h1>SPARQL Results</h1>
				</xsl:if>
				<xsl:if test="/rdf:RDF">
					<h1>RDF Graph</h1>
				</xsl:if>
				<xsl:choose>
					<xsl:when test="/sparql:sparql/sparql:boolean">
						<xsl:apply-templates />
					</xsl:when>
					<xsl:when test="/sparql:sparql/sparql:results/sparql:result">
						<xsl:apply-templates />
					</xsl:when>
					<xsl:when test="/rdf:RDF">
						<xsl:apply-templates />
					</xsl:when>
					<xsl:otherwise>
						<p>No results.</p>
					</xsl:otherwise>
				</xsl:choose>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="sparql:sparql">
		<table id="sparql">
			<xsl:apply-templates select="*" />
		</table>
	</xsl:template>
	<xsl:template match="sparql:head">
		<thead id="head">
			<xsl:apply-templates select="sparql:variable" />
		</thead>
	</xsl:template>
	<xsl:template match="sparql:variable">
		<th>
			<xsl:value-of select="@name" />
		</th>
	</xsl:template>
	<xsl:template match="sparql:boolean">
		<tbody id="boolean">
			<tr>
				<td>
					<xsl:value-of select="text()" />
				</td>
			</tr>
		</tbody>
	</xsl:template>
	<xsl:template match="sparql:results">
		<tbody id="results">
			<xsl:apply-templates select="sparql:result" />
		</tbody>
	</xsl:template>
	<xsl:template match="sparql:result">
		<xsl:variable name="current" select="."/> 
		<tr class="result">
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
		<a href="{text()}" class="diverted describe uri">
			<xsl:call-template name="curie">
				<xsl:with-param name="iri" select="text()"/>
			</xsl:call-template>
		</a>
	</xsl:template>
	<xsl:template match="sparql:bnode">
		<a class="bnode" about="_:{text()}" name="{text()}">
			<xsl:value-of select="text()" />
		</a>
	</xsl:template>
	<xsl:template match="sparql:literal">
		<span class="literal" title="@datatype" datatype="@datatype">
			<xsl:value-of select="text()" />
		</span>
	</xsl:template>
	<xsl:template
		match="sparql:literal[@datatype='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral']">
		<span class="literal" datatype="rdf:XMLLiteral">
			<xsl:value-of disable-output-escaping="yes" select="text()" />
		</span>
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
			<a href="{@rdf:resource}" class="diverted describe uri">
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
			<span datatype="{@rdf:datatype}" title="{@rdf:datatype}" class="literal">
				<xsl:attribute name="property">
					<xsl:call-template name="curie">
						<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
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
			<a href="{@rdf:about}" class="diverted describe uri">
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
			<a name="{@rdf:nodeID}" class="bnode">
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
			<a href="{@rdf:about}" class="diverted describe uri">
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
			<a name="{@rdf:nodeID}" class="bnode">
				<xsl:value-of select="@rdf:nodeID" />
			</a>
			<ul>
				<xsl:apply-templates select="*" />
			</ul>
		</div>
	</xsl:template>
</xsl:stylesheet>
