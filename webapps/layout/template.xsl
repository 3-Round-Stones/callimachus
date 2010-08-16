<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#">
	<xsl:param name="xslt" select="'/template.xsl'" />
	<xsl:param name="mode" />
	<xsl:variable name="layout" select="substring-before($xslt, '/template.xsl')" />
	<xsl:variable name="origin" select="substring-before($xslt, '/' + substring-after(substring-after($xslt, '://'), '/'))" />
	<xsl:variable name="callimachus" select="concat($origin, '/callimachus')" />
	<xsl:variable name="realm" select="concat($origin, '/realm')" />
	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|comment()|text()" />
		</xsl:copy>
	</xsl:template>
	<xsl:template match="@*">
		<xsl:attribute name="{name()}">
			<xsl:choose>
				<xsl:when test="starts-with(., '/layout/')">
					<xsl:value-of select="$layout"/>
					<xsl:value-of select="substring-after(., '/layout')" />
				</xsl:when>
				<xsl:when test="starts-with(., '/')">
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
			<link rel="icon" href="/layout/favicon.ico" />
			<link rel="stylesheet" href="{$layout}/template.css" />
			<script type="text/javascript" src="{$callimachus}/diverted.js"> </script>
			<script type="text/javascript" src="{$layout}/prompt.js"> </script>
			<xsl:if test="contains($mode, 'copy') or contains($mode, 'edit') or contains($mode, 'delete')">
			<script type="text/javascript" src="{$callimachus}/jquery.js"> </script>
			<script type="text/javascript" src="{$callimachus}/jquery-ui.js"> </script>
			<script type="text/javascript" src="{$callimachus}/jquery.qtip.js"> </script>
			<script type="text/javascript" src="{$callimachus}/jquery.rdfquery.rdfa.js"> </script>
			<script type="text/javascript" src="{$callimachus}/status.js"> </script>
			<script type="text/javascript" src="{$callimachus}/elements.js"> </script>
			<xsl:if test="contains($mode, 'copy')">
			<script type="text/javascript" src="{$callimachus}/copy.js"> </script>
			</xsl:if>
			<xsl:if test="contains($mode, 'edit')">
			<script type="text/javascript" src="{$callimachus}/edit.js"> </script>
			</xsl:if>
			<xsl:if test="contains($mode, 'delete')">
			<script type="text/javascript" src="{$callimachus}/delete.js"> </script>
			</xsl:if>
			</xsl:if>
			<xsl:apply-templates select="*|text()|comment()" />
		</xsl:copy>
	</xsl:template>
	<xsl:template match="body">
		<xsl:copy>
			<xsl:apply-templates select="@*" />
			<div id="header">
				<div id="credentials">
					<a id="credential" href="{$realm}/authority?credential"></a>
					<a id="login" href="{$realm}/authority?login">Login</a>
					<span class="logout"> | </span>
					<a class="logout" href="{$realm}/authority?logout">Logout</a>
				</div>
				<div id="logo">
					<a href="{$origin}/">
						<img src="{$layout}/logo.png" />
					</a>
				</div>
				<ul id="tabs" class="tabs">
					<xsl:for-each select="/html/head/link[@target='_self']">
						<xsl:choose>
							<xsl:when test="@href=''">
								<li>
									<span><xsl:value-of select="@title" /></span>
								</li>
							</xsl:when>
							<xsl:when test="@href">
								<li>
									<a class="diverted" target="_self">
										<xsl:apply-templates select="@href"/>
										<xsl:value-of select="@title" />
									</a>
								</li>
							</xsl:when>
						</xsl:choose>
					</xsl:for-each>
				</ul>
			</div>

			<xsl:apply-templates mode="menu" select="document(concat($layout, '/menu'))" />

			<div id="content">
				<xsl:apply-templates select="*|comment()|text()" />
			</div>

			<div id="footer">
				<a href="http://callimachusproject.org/" title="Callimachus">
					<img src="{$callimachus}/callimachus-powered.png" alt="Callimachus" />
				</a>
			</div>
		</xsl:copy>
	</xsl:template>
	<xsl:template mode="menu" match="sparql:sparql">
		<ul id="nav">
			<xsl:apply-templates mode="menu" select="sparql:results/sparql:result[not(sparql:binding/@name='parent')]" />
		</ul>
	</xsl:template>
	<xsl:template mode="menu" match="sparql:result[not(sparql:binding/@name='link')]">
		<li>
			<span>
				<xsl:value-of select="sparql:binding[@name='label']/*" />
			</span>
			<xsl:variable name="node" select="sparql:binding[@name='item']/*/text()" />
			<ul>
				<xsl:apply-templates mode="menu" select="../sparql:result[sparql:binding[@name='parent']/*/text()=$node]" />
			</ul>
		</li>
	</xsl:template>
	<xsl:template mode="menu" match="sparql:result[sparql:binding/@name='link']">
		<li>
			<a href="{sparql:binding[@name='link']/*}">
				<xsl:value-of select="sparql:binding[@name='label']/*" />
			</a>
			<xsl:variable name="node" select="sparql:binding[@name='item']/*/text()" />
			<ul>
				<xsl:apply-templates mode="menu" select="../sparql:result[sparql:binding[@name='parent']/*/text()=$node]" />
			</ul>
		</li>
	</xsl:template>
</xsl:stylesheet>
