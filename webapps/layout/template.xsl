<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#">
	<xsl:param name="xslt" select="'/layout/template.xsl'" />
	<xsl:param name="mode" />
	<xsl:variable name="layout">
		<xsl:call-template name="substring-before-last">
			<xsl:with-param name="string" select="$xslt"/>
			<xsl:with-param name="delimiter" select="'/'"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="origin">
		<xsl:call-template name="substring-before-last">
			<xsl:with-param name="string" select="$layout" />
			<xsl:with-param name="delimiter" select="'/'"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="scheme" select="substring-before($xslt, '://')" />
	<xsl:variable name="host" select="substring-before(substring-after($xslt, '://'), '/')" />
	<xsl:variable name="accounts" select="concat($origin, '/accounts')" />
	<xsl:variable name="callimachus">
		<xsl:if test="$scheme and $host">
			<xsl:value-of select="concat($scheme, '://', $host, '/callimachus')" />
		</xsl:if>
		<xsl:if test="not($scheme) or not($host)">
			<xsl:value-of select="'/callimachus'" />
		</xsl:if>
	</xsl:variable>
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

	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|comment()|text()" />
		</xsl:copy>
	</xsl:template>
	<xsl:template match="@*">
		<xsl:attribute name="{name()}">
			<xsl:choose>
				<xsl:when test="starts-with(., '/callimachus/') and $callimachus">
					<xsl:value-of select="$callimachus"/>
					<xsl:value-of select="substring-after(., '/callimachus')" />
				</xsl:when>
				<xsl:when test="starts-with(., '/layout/') and $layout">
					<xsl:value-of select="$layout"/>
					<xsl:value-of select="substring-after(., '/layout')" />
				</xsl:when>
				<xsl:when test="starts-with(., '//') and $scheme">
					<xsl:value-of select="concat($scheme, ':')"/>
					<xsl:value-of select="." />
				</xsl:when>
				<xsl:when test="starts-with(., '/') and not(starts-with(., '//')) and $origin">
					<xsl:value-of select="$origin"/>
					<xsl:value-of select="." />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="." />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:attribute>
	</xsl:template>
	<xsl:template match="comment()">
		<xsl:copy />
	</xsl:template>
	<xsl:template match="head">
		<xsl:copy>
			<xsl:apply-templates select="@*" />
			<meta http-equiv="X-UA-Compatible" content="IE=8" />
			<link rel="icon" href="{$layout}/favicon.png" />
			<link rel="stylesheet" href="{$layout}/template.css" />
			<link type="text/css" href="{$layout}/jquery-ui-1.7.3.custom.css" rel="stylesheet" />	
			<script type="text/javascript" src="{$callimachus}/scripts/jquery.js"></script>
			<script type="text/javascript" src="{$callimachus}/scripts/jquery-ui.js"></script>
			<script type="text/javascript" src="{$callimachus}/scripts/diverted.js"> </script>
			<script type="text/javascript" src="{$layout}/template.js"> </script>
			<xsl:if test="contains($mode, 'copy') or contains($mode, 'edit') or contains($mode, 'delete')">
			<script type="text/javascript" src="{$callimachus}/scripts/jquery.qtip.js"> </script>
			<script type="text/javascript" src="{$callimachus}/scripts/jquery.rdfquery.rdfa.js"> </script>
			<script type="text/javascript" src="{$callimachus}/scripts/status.js"> </script>
			<script type="text/javascript" src="{$callimachus}/scripts/elements.js"> </script>
			<xsl:if test="contains($mode, 'copy')">
			<script type="text/javascript" src="{$callimachus}/operations/copy.js"> </script>
			</xsl:if>
			<xsl:if test="contains($mode, 'edit')">
			<script type="text/javascript" src="{$callimachus}/operations/edit.js"> </script>
			</xsl:if>
			<xsl:if test="contains($mode, 'delete')">
			<script type="text/javascript" src="{$callimachus}/operations/delete.js"> </script>
			</xsl:if>
			</xsl:if>
			<xsl:if test="//*[@class][contains(@class, 'wiki')]">
			<script type="text/javascript" src="{$callimachus}/scripts/creole.js"> </script>
			</xsl:if>
			<script type="text/javascript" src="{$callimachus}/scripts/enhancements.js"> </script>
			<xsl:apply-templates select="*|text()|comment()" />
		</xsl:copy>
	</xsl:template>
	<xsl:template match="body">
		<xsl:copy>
			<xsl:apply-templates select="@*" />
			<div id="header">
				<form method="GET" action="{$callimachus}/menu">
					<a id="login-link" href="{$accounts}/authority?login" style="display:none">Login</a>
					<span id="authenticated-span" style="display:none">
						<a id="authenticated-link" href="{$accounts}/authority?authenticated"></a>
						<span> | </span>
						<a href="?edit">Settings</a>
						<span> | </span>
						<a href="?contributions">Contributions</a>
						<span> | </span>
						<a href="{$accounts}/authority?logout">Logout</a>
					</span>
					<input type="hidden" name="go" />
					<span id="search-box">
						<input id="search-box-input" type="text" size="10" name="q" title="Lookup..." />
						<button id="search-box-button" type="button" onclick="form.elements['go'].name='lookup';form.submit()">
							<img src="{$layout}/search.png" />
						</button>
					</span>
				</form>
			</div>
			<div id="sidebar">
				<a href="{$origin}/" id="logo"></a>
				<xsl:apply-templates mode="nav" select="document(concat($callimachus, '/menu'))" />
			</div>

			<ul id="tabs" class="tabs">
				<xsl:if test="contains($mode, 'view')">
					<li>
						<span>View</span>
					</li>
					<li>
						<a class="diverted replace" href="?edit">Edit</a>
					</li>
					<li>
						<a class="diverted replace" href="?history">History</a>
					</li>
				</xsl:if>
				<xsl:if test="contains($mode, 'edit')">
					<li>
						<a class="diverted replace" href="?view">View</a>
					</li>
					<li>
						<span>Edit</span>
					</li>
					<li>
						<a class="diverted replace" href="?history">History</a>
					</li>
				</xsl:if>
				<xsl:if test="contains($mode, 'history')">
					<li>
						<a class="diverted replace" href="?view">View</a>
					</li>
					<li>
						<a class="diverted replace" href="?edit">Edit</a>
					</li>
					<li>
						<span>History</span>
					</li>
				</xsl:if>
			</ul>
			<div id="content">
				<xsl:apply-templates select="*|comment()|text()" />
				<div id="content-stop" />
			</div>

			<div id="footer" xmlns:audit="http://www.openrdf.org/rdf/2009/auditing#">
				<p id="footer-lastmod" rel="audit:revision" resource="?revision">This resource was last modified <span property="audit:committedOn" class="date-locale" />.</p>
				<a href="http://callimachusproject.org/" title="Callimachus">
					<img src="{$callimachus}/callimachus-powered.png" alt="Callimachus" />
				</a>
			</div>
		</xsl:copy>
	</xsl:template>
	<xsl:template mode="nav" match="sparql:sparql">
		<ul id="nav">
			<xsl:apply-templates mode="nav" select="sparql:results/sparql:result[not(sparql:binding/@name='parent')]" />
		</ul>
	</xsl:template>
	<xsl:template mode="nav" match="sparql:result[not(sparql:binding/@name='link')]">
		<li>
			<span>
				<xsl:value-of select="sparql:binding[@name='label']/*" />
			</span>
			<xsl:variable name="node" select="sparql:binding[@name='item']/*/text()" />
			<ul>
				<xsl:apply-templates mode="nav" select="../sparql:result[sparql:binding[@name='parent']/*/text()=$node]" />
			</ul>
		</li>
	</xsl:template>
	<xsl:template mode="nav" match="sparql:result[sparql:binding/@name='link']">
		<li>
			<a href="{sparql:binding[@name='link']/*}">
				<xsl:value-of select="sparql:binding[@name='label']/*" />
			</a>
			<xsl:variable name="node" select="sparql:binding[@name='item']/*/text()" />
			<ul>
				<xsl:apply-templates mode="nav" select="../sparql:result[sparql:binding[@name='parent']/*/text()=$node]" />
			</ul>
		</li>
	</xsl:template>
</xsl:stylesheet>
