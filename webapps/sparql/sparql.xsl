<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="xhtml sparql"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	<xsl:import href="/callimachus/util/iriref.xsl" />
	<xsl:param name="this" />
	<xsl:template match="/">
		<html>
			<head>
				<title>SPARQL Results</title>
				<style>
					.uri, .bnode, .literal { font-size: large; }
					.bnode, .literal { font-family: monospace; white-space: pre-wrap; }
					.predicate { font-weight: bold; }
				</style>
			</head>
			<body>
				<h1>SPARQL Results</h1>
				<xsl:apply-templates />
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
		<a href="{text()}" class="intralink uri">
			<xsl:call-template name="iriref">
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
		<span class="literal">
			<xsl:attribute name="datatype">
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="@datatype" />
				</xsl:call-template>
			</xsl:attribute>
			<xsl:attribute name="title">
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="@datatype" />
				</xsl:call-template>
			</xsl:attribute>
			<xsl:value-of select="text()" />
		</span>
	</xsl:template>
	<xsl:template
		match="sparql:literal[@datatype='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral']">
		<span class="literal" datatype="rdf:XMLLiteral">
			<xsl:value-of disable-output-escaping="yes" select="text()" />
		</span>
	</xsl:template>
	<!-- rdf:RDF -->
	<xsl:template match="rdf:RDF">
		<div class="graph">
			<xsl:apply-templates select="*" />
		</div>
	</xsl:template>
	<xsl:template match="*">
		<li>
			<label class="predicate">
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<span class="literal">
				<xsl:attribute name="property">
					<xsl:call-template name="iriref">
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
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<a href="#{@rdf:nodeID}" class="bnode">
				<xsl:attribute name="rel">
					<xsl:call-template name="iriref">
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
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<a href="{@rdf:resource}" class="intralink uri">
				<xsl:attribute name="rel">
					<xsl:call-template name="iriref">
						<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="@rdf:resource"/>
				</xsl:call-template>
			</a>
		</li>
	</xsl:template>
	<xsl:template match="*[@rdf:datatype]">
		<li>
			<label class="predicate">
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<span class="literal">
				<xsl:attribute name="property">
					<xsl:call-template name="iriref">
						<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:attribute name="datatype">
					<xsl:call-template name="iriref">
						<xsl:with-param name="iri" select="@rdf:datatype" />
					</xsl:call-template>
				</xsl:attribute>
				<xsl:attribute name="title">
					<xsl:call-template name="iriref">
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
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<xsl:text> </xsl:text>
			<span class="literal">
				<xsl:attribute name="property">
					<xsl:call-template name="iriref">
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
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</label>
			<ul>
				<xsl:attribute name="rel">
					<xsl:call-template name="iriref">
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
				<xsl:call-template name="iriref">
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
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</xsl:attribute>
			<a href="{@rdf:about}" class="intralink uri">
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="@rdf:about"/>
				</xsl:call-template>
			</a>
			<span> a </span>
			<span>
				<xsl:call-template name="iriref">
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
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</xsl:attribute>
			<a href="#{@rdf:ID}" class="intralink uri">
				<xsl:value-of select="@rdf:ID"/>
			</a>
			<span> a </span>
			<span>
				<xsl:call-template name="iriref">
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
				<xsl:call-template name="iriref">
					<xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
				</xsl:call-template>
			</xsl:attribute>
			<a name="{@rdf:nodeID}" id="{@rdf:nodeID}" class="bnode">
				<xsl:value-of select="@rdf:nodeID" />
			</a>
			<span> a </span>
			<span>
				<xsl:call-template name="iriref">
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
			<a href="{@rdf:about}" class="intralink uri">
				<xsl:call-template name="iriref">
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
			<a href="#{@rdf:ID}" class="intralink uri">
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
