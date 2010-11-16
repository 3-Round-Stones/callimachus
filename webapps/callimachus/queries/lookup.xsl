<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sparql="http://www.w3.org/2005/sparql-results#">
	<xsl:import href="../iriref.xsl" />
	<xsl:output method="xml" encoding="UTF-8"/>
	<xsl:template match="/">
		<html>
			<head>
				<title>Search Results</title>
				<script>
				// <![CDATA[
				function parameter(name) {
					var regex = new RegExp("[\\?&]"+name+"=([^&#]*)")
					var m = regex.exec(location.href)
					return m ? decodeURIComponent(m[1].replace('+', ' ')) : null
				}
				function init() {
					if (parameter("q")) {
						document.getElementById("q").value = parameter("q")
					}
				}
				// ]]>
				</script>
			</head>
			<body onload="init()">
				<h1>Search Results</h1>
				<form method="GET" class="search">
					<input type="text" id="q" name="q" size="40" />
					<button type="submit">Search</button>
				</form>
				<hr />
				<xsl:if test="not(/sparql:sparql/sparql:results/sparql:result)">
					<p>No resources with this label found.</p>
				</xsl:if>
				<xsl:if test="/sparql:sparql/sparql:results/sparql:result">
					<xsl:apply-templates />
				</xsl:if>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="sparql:sparql">
		<xsl:apply-templates select="sparql:results" />
	</xsl:template>
	<xsl:template match="sparql:results">
		<ul id="results">
			<xsl:apply-templates select="sparql:result" />
		</ul>
	</xsl:template>
	<xsl:template match="sparql:result">
		<li class="result">
			<xsl:if test="sparql:binding[@name='icon']">
				<img src="{sparql:binding[@name='icon']/*}" class="icon" />
			</xsl:if>
			<xsl:if test="not(sparql:binding[@name='icon'])">
				<img src="/callimachus/rdf-icon.png" class="icon" />
			</xsl:if>
			<a>
				<xsl:if test="sparql:binding[@name='url']">
					<xsl:attribute name="href">
						<xsl:value-of select="sparql:binding[@name='url']/*" />
					</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="sparql:binding[@name='label']/*" />
			</a>
			<xsl:if test="sparql:binding[@name='comment']">
				<pre class="wiki summary">
					<xsl:value-of select="sparql:binding[@name='comment']/*" />
				</pre>
			</xsl:if>
			<xsl:if test="sparql:binding[@name='url']">
				<div class="cite">
					<span class="url" title="{sparql:binding[@name='url']/*}">
						<xsl:variable name="ref">
							<xsl:call-template name="iriref">
								<xsl:with-param name="iri" select="sparql:binding[@name='url']/*" />
							</xsl:call-template>
						</xsl:variable>
						<xsl:choose>
							<xsl:when test="string-length($ref) &gt; 63">
								<xsl:attribute name="title"><xsl:value-of select="$ref" /></xsl:attribute>
								<xsl:value-of select="substring($ref, 0, 40)" />
								<span>...</span>
								<xsl:value-of select="substring($ref, string-length($ref) - 20, string-length($ref))" />
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$ref" />
							</xsl:otherwise>
						</xsl:choose>
					</span>
					<xsl:if test="sparql:binding[@name='modified']">
						<span> - </span>
						<span class="abbreviated datetime-locale">
							<xsl:value-of select="sparql:binding[@name='modified']/*" />
						</span>
					</xsl:if>
				</div>
			</xsl:if>
		</li>
	</xsl:template>
</xsl:stylesheet>
