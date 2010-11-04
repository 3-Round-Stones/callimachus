<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sparql="http://www.w3.org/2005/sparql-results#">
	<xsl:output method="xml" encoding="UTF-8"/>
	<xsl:param name="xslt" />
	<xsl:variable name="host" select="substring-before(substring-after($xslt, '://'), '/')" />
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
				<xsl:apply-templates select="sparql:binding[@name='label']" />
			</a>
			<xsl:if test="sparql:binding[@name='comment']">
				<pre class="wiki summary">
					<xsl:apply-templates select="sparql:binding[@name='comment']" />
				</pre>
			</xsl:if>
			<xsl:if test="sparql:binding[@name='url']">
				<div class="cite">
					<span class="url">
						<xsl:variable name="url" select="sparql:binding[@name='url']/*" />
						<xsl:choose>
							<xsl:when test="contains($url, concat('://', $host)) and string-length(substring-after($url, concat('://', $host))) &gt; 63">
								<xsl:attribute name="title"><xsl:value-of
									select="$url" /></xsl:attribute>
								<xsl:value-of select="substring(substring-after($url, concat('://', $host)), 0, 40)" />
								<span>...</span>
								<xsl:value-of
									select="substring(substring-after($url, concat('://', $host)), string-length(substring-after($url, concat('://', $host))) - 20, string-length(substring-after($url, concat('://', $host))))" />
							</xsl:when>
							<xsl:when test="contains($url, concat('://', $host))">
								<xsl:attribute name="title"><xsl:value-of
									select="$url" /></xsl:attribute>
								<xsl:value-of select="substring-after($url, concat('://', $host))" />
							</xsl:when>
							<xsl:when test="contains($url, '://') and string-length(substring-after($url, '://')) &gt; 63">
								<xsl:attribute name="title"><xsl:value-of
									select="$url" /></xsl:attribute>
								<xsl:value-of select="substring(substring-after($url, '://'), 0, 40)" />
								<span>...</span>
								<xsl:value-of
									select="substring(substring-after($url, '://'), string-length(substring-after($url, '://')) - 20, string-length(substring-after($url, '://')))" />
							</xsl:when>
							<xsl:when test="contains($url, '://')">
								<xsl:attribute name="title"><xsl:value-of
									select="$url" /></xsl:attribute>
								<xsl:value-of select="substring-after($url, '://')" />
							</xsl:when>
							<xsl:when test="string-length($url) &gt; 63">
								<xsl:attribute name="title"><xsl:value-of
									select="$url" /></xsl:attribute>
								<xsl:value-of select="substring($url, 0, 40)" />
								<span>...</span>
								<xsl:value-of
									select="substring($url, string-length($url) - 20, string-length($url))" />
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$url" />
							</xsl:otherwise>
						</xsl:choose>
					</span>
					<xsl:if test="sparql:binding[@name='modified']">
						<span> - </span>
						<span class="date-locale">
							<xsl:apply-templates select="sparql:binding[@name='modified']" />
						</span>
					</xsl:if>
				</div>
			</xsl:if>
		</li>
	</xsl:template>
	<xsl:template match="sparql:binding">
		<xsl:apply-templates select="*" />
	</xsl:template>
	<xsl:template match="sparql:uri">
		<span class="uri">
			<xsl:value-of select="text()" />
		</span>
	</xsl:template>
	<xsl:template match="sparql:bnode">
		<span class="bnode">
			<xsl:value-of select="text()" />
		</span>
	</xsl:template>
	<xsl:template match="sparql:literal">
		<span class="literal">
			<xsl:value-of select="text()" />
		</span>
	</xsl:template>
	<xsl:template
		match="sparql:literal[@datatype='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral']">
		<span class="literal" datatype="rdf:XMLLiteral">
			<xsl:value-of disable-output-escaping="yes" select="text()" />
		</span>
	</xsl:template>
</xsl:stylesheet>
