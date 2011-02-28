<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="xhtml sparql">
	<xsl:output method="xml" />
	<xsl:param name="xslt" select="'/layout/template.xsl'" />
	<xsl:param name="this" />
	<xsl:param name="query" />
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

	<xsl:template match="@*|comment()">
		<xsl:copy />
	</xsl:template>

	<!-- head -->
	<xsl:template match="head|xhtml:head">
		<xsl:copy>
			<xsl:apply-templates select="@*" />
			<link rel="icon" href="{$callimachus}/menu?favicon" />
			<meta name="viewport" content="width=device-width,height=device-height,initial-scale=1,maximum-scale=1"/>
			<link rel="stylesheet" href="{$layout}/template.css" />
			<link type="text/css" href="{$layout}/jquery-ui-1.7.3.custom.css" rel="stylesheet" />
			<xsl:comment>[if lte IE 8]>&lt;link rel="stylesheet" href="<xsl:value-of select="$layout" />/ie8.css" /&gt;&lt;![endif]</xsl:comment>
			<xsl:apply-templates select="*[local-name()='link' or local-name()='style']" />
			<script type="text/javascript" src="{$callimachus}/scripts/web_bundle?source">&#160;</script>
			<xsl:if test="$query='copy' or $query='edit'">
			<script type="text/javascript" src="{$callimachus}/scripts/form_bundle?source">&#160;</script>
			</xsl:if>
			<xsl:if test="$query='copy'">
			<script type="text/javascript" src="{$callimachus}/operations/copy.js">&#160;</script>
			</xsl:if>
			<xsl:if test="$query='edit'">
			<script type="text/javascript" src="{$callimachus}/operations/edit.js">&#160;</script>
			</xsl:if>
			<xsl:apply-templates select="*[local-name()!='link' and local-name()!='style']|text()|comment()" />
		</xsl:copy>
	</xsl:template>

	<!-- body -->
	<xsl:template match="body|xhtml:body">
		<xsl:copy>
			<xsl:attribute name="class">
				<xsl:value-of select="'wait'" />
				<xsl:if test="@class">
					<xsl:value-of select="concat(' ', @class)" />
				</xsl:if>
			</xsl:attribute>
			<xsl:apply-templates select="@*[name() != 'class']" />
			<div id="header">
				<a class="ui-state-default ui-corner-all" id="login-link" href="{$accounts}?login"
						style="display:none;padding: .2em 20px .2em 1em;text-decoration: none;position: relative;">
					Login
					<span class="ui-icon ui-icon-circle-arrow-s"
							style="margin: 0 0 0 5px;position: absolute;right: .2em;top: 50%;margin-top: -8px;">
						<xsl:text> </xsl:text>
					</span>
				</a>
				<span class="authenticated" style="display:none">
					<a id="profile-link" href="{$accounts}?login">Profile</a>
					<span> | </span>
					<a href="{$accounts}?settings">Settings</a>
					<span> | </span>
					<a href="{$accounts}?contributions">Contributions</a>
					<span> | </span>
					<a id="logout-link" href="{$accounts}?logout">Logout</a>
				</span>
				<form method="GET" action="{$callimachus}/go" style="display:inline">
					<span id="search-box">
						<input id="search-box-input" type="text" size="10" name="q" title="Lookup..." />
						<button id="search-box-button" type="button" onclick="form.action='{$callimachus}/lookup';form.submit()">
							<img src="{$layout}/search.png" width="12" height="13" />
						</button>
					</span>
				</form>
			</div>

			<xsl:if test="$query='view' or $query='edit' or $query='discussion' or $query='describe' or $query='history'">
				<ul id="tabs">
					<li id="view-tab" class="authenticated" tabindex="1">
						<xsl:if test="not($query='view')">
							<xsl:attribute name="onclick">location.replace('?view')</xsl:attribute>
						</xsl:if>
						<xsl:text>View</xsl:text>
					</li>
					<li id="edit-tab" class="authenticated" tabindex="2">
						<xsl:if test="not($query='edit')">
							<xsl:attribute name="onclick">location.replace('?edit')</xsl:attribute>
						</xsl:if>
						<xsl:text>Edit</xsl:text>
					</li>
					<li id="discussion-tab" class="authenticated" tabindex="3">
						<xsl:if test="not($query='discussion')">
							<xsl:attribute name="onclick">location.replace('?discussion')</xsl:attribute>
						</xsl:if>
						<xsl:text>Discussion</xsl:text>
					</li>
					<li id="describe-tab" class="authenticated" tabindex="4">
						<xsl:if test="not($query='describe')">
							<xsl:attribute name="onclick">location.replace('?describe')</xsl:attribute>
						</xsl:if>
						<xsl:text>Describe</xsl:text>
					</li>
					<li id="history-tab" class="authenticated" tabindex="5">
						<xsl:if test="not($query='history')">
							<xsl:attribute name="onclick">location.replace('?history')</xsl:attribute>
						</xsl:if>
						<xsl:text>History</xsl:text>
					</li>
				</ul>
			</xsl:if>

			<div id="content">
				<div id="error-widget" class="ui-state-error ui-corner-all" style="padding: 1ex; margin: 1ex; display: none">
					<div><span class="ui-icon ui-icon-alert" style="margin-right: 0.3em; float: left; "></span>
					<strong>Alert:</strong><span id="error-message" style="padding: 0px 0.7em"> Sample ui-state-error style.</span></div>
				</div>
				<xsl:apply-templates select="*|comment()|text()" />
			</div>

			<div id="sidebar">
				<xsl:apply-templates mode="menu" select="document(concat($callimachus, '/menu?evaluate'))" />
			</div>
			<a href="{$origin}/" id="logo">&#160;</a>

			<div id="footer" xmlns:audit="http://www.openrdf.org/rdf/2009/auditing#">
				<xsl:if test="$query='view'">
					<p id="footer-lastmod" rel="audit:revision" resource="?revision">This resource was last modified at 
						<span property="audit:committedOn" class="abbreviated datetime-locale" />.
					</p>
				</xsl:if>
				<a href="http://callimachusproject.org/" title="Callimachus">
					<img src="{$callimachus}/images/callimachus-powered.png" alt="Callimachus" width="98" height="35" />
				</a>
			</div>
		</xsl:copy>
	</xsl:template>

	<!-- menu -->
	<xsl:template mode="menu" match="sparql:sparql">
		<ul id="nav">
			<xsl:apply-templates mode="menu" select="sparql:results/sparql:result[not(sparql:binding/@name='heading')]" />
		</ul>
	</xsl:template>
	<xsl:template mode="menu" match="sparql:result">
		<xsl:variable name="label" select="sparql:binding[@name='label']/*/text()" />
		<li>
			<xsl:if test="sparql:binding/@name='link'">
				<a href="{sparql:binding[@name='link']/*}">
					<xsl:value-of select="$label" />
				</a>
			</xsl:if>
			<xsl:if test="not(sparql:binding/@name='link')">
				<span>
					<xsl:value-of select="$label" />
				</span>
			</xsl:if>
			<xsl:if test="../sparql:result[sparql:binding[@name='heading']/*/text()=$label]">
				<ul>
					<xsl:apply-templates mode="menu" select="../sparql:result[sparql:binding[@name='heading']/*/text()=$label]" />
				</ul>
			</xsl:if>
		</li>
	</xsl:template>
</xsl:stylesheet>
