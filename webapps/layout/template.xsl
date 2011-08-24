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
	<xsl:param name="template" select="false()" />
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
			<meta name="viewport" content="width=device-width,height=device-height,initial-scale=1.0,target-densityDpi=device-dpi"/>
			<meta http-equiv="X-UA-Compatible" content="IE=edge;chrome=1" />
			<link rel="icon" href="{$callimachus}/manifest?favicon" />
			<link rel="stylesheet" href="{$layout}/layout.css" />
			<link rel="stylesheet" href="{$layout}/colour.css" />
			<xsl:if test="//form|//xhtml:form">
				<link rel="stylesheet" href="{$layout}/form.css" />
				<link type="text/css" href="{$layout}/jquery-ui.css" rel="stylesheet" />
			</xsl:if>
			<xsl:comment>[if lt IE 9]>
				&lt;link rel="stylesheet" href="<xsl:value-of select="$layout" />/ie8.css" /&gt;
			&lt;![endif]</xsl:comment>
			<xsl:apply-templates select="*[local-name()='link' or local-name()='style']" />

			<xsl:comment>[if lt IE 9]>
				&lt;script src="//html5shim.googlecode.com/svn/trunk/html5.js"&gt;&lt;/script&gt;
			&lt;![endif]</xsl:comment>
			<script type="text/javascript" src="{$callimachus}/scripts/web_bundle?source">&#160;</script>
			<xsl:if test="//form|//xhtml:form">
				<script type="text/javascript" src="{$callimachus}/scripts/form_bundle?source">&#160;</script>
			</xsl:if>
			<xsl:if test="$query='create'">
				<script type="text/javascript" src="{$callimachus}/operations/create.js">&#160;</script>
			</xsl:if>
			<xsl:if test="$query='edit'">
				<script type="text/javascript" src="{$callimachus}/operations/edit.js">&#160;</script>
			</xsl:if>
			<xsl:apply-templates select="*[local-name()!='link' and local-name()!='style']|text()|comment()" />
			<noscript><style>body.wait, body.wait * {cursor: auto !important}</style></noscript>
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
				<a id="login-link" href="{$accounts}?login"
						style="display:none;padding: .2em 20px .2em 1em; text-decoration: none;position: relative; border: 1px solid #a8acb8; background: #eeeeee; font-weight: bold; color: #0645ad; outline: none; -moz-border-radius: 2px; -webkit-border-radius: 2px;">
					Login
					<span class="ui-icon ui-icon-circle-arrow-s"
							style="margin: 0 0 0 5px;position: absolute;right: .2em;top: 50%;margin-top: -8px; display: block; text-indent: -99999px; overflow: hidden; background-repeat: no-repeat; width: 16px; height: 16px; background-image: url({$layout}/images/ui-icons_f58735_256x240.png); background-position: -128px -192px;">
						<xsl:text> </xsl:text>
					</span>
				</a>
				<span class="protected" style="display:none">
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
						<input id="search-box-input" type="text" size="10" name="q" placeholder="Lookup..." />
						<button id="search-box-button" type="button" onclick="form.action='{$callimachus}/lookup';form.submit()">
							<img src="{$layout}/search.png" width="12" height="13" />
						</button>
					</span>
				</form>
			</div>

			<xsl:if test="$query='view' or $query='edit' or $query='discussion' or $query='describe' or $query='history'">
				<ul id="tabs" class="protected">
					<li>
						<a id="view-tab" tabindex="1">
							<xsl:if test="not($query='view')">
								<xsl:attribute name="href">?view</xsl:attribute>
								<xsl:attribute name="onclick">location.replace(href);return false</xsl:attribute>
							</xsl:if>
							<xsl:text>View</xsl:text>
						</a>
					</li>
					<li>
						<a id="edit-tab" tabindex="2">
							<xsl:if test="not($query='edit')">
								<xsl:attribute name="href">?edit</xsl:attribute>
								<xsl:attribute name="onclick">location.replace(href);return false</xsl:attribute>
							</xsl:if>
							<xsl:text>Edit</xsl:text>
						</a>
					</li>
					<li>
						<a id="discussion-tab" tabindex="3">
							<xsl:if test="not($query='discussion')">
								<xsl:attribute name="href">?discussion</xsl:attribute>
								<xsl:attribute name="onclick">location.replace(href);return false</xsl:attribute>
							</xsl:if>
							<xsl:text>Discussion</xsl:text>
						</a>
					</li>
					<li>
						<a id="describe-tab" tabindex="4">
							<xsl:if test="not($query='describe')">
								<xsl:attribute name="href">?describe</xsl:attribute>
								<xsl:attribute name="onclick">location.replace(href);return false</xsl:attribute>
							</xsl:if>
							<xsl:text>Describe</xsl:text>
						</a>
					</li>
					<li>
						<a id="history-tab" tabindex="5">
							<xsl:if test="not($query='history')">
								<xsl:attribute name="href">?history</xsl:attribute>
								<xsl:attribute name="onclick">location.replace(href);return false</xsl:attribute>
							</xsl:if>
							<xsl:text>History</xsl:text>
						</a>
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

			<xsl:copy-of select="document(concat($callimachus, '/menu?items'))/xhtml:html/xhtml:body/node()" />
			<a href="{$origin}/" id="logo">&#160;</a>

			<div id="footer" xmlns:audit="http://www.openrdf.org/rdf/2009/auditing#">
				<div id="footer-info">
					<xsl:if test="$template and ($query='view' or $query='edit')">
						<p about="?this" rel="audit:revision" resource="?revision">This resource was last modified at 
							<time pubdate="pubdate" property="audit:committedOn" class="abbreviated" />
						</p>
					</xsl:if>
					<p>
						<xsl:copy-of select="document(concat($callimachus, '/manifest?rights'))/xhtml:html/xhtml:body/node()" />
					</p>
				</div>
				<a href="http://callimachusproject.org/" title="Callimachus">
					<img src="{$callimachus}/images/callimachus-powered.png" alt="Callimachus" width="98" height="35" />
				</a>
			</div>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
