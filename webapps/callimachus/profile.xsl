<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdfa="http://www.w3.org/ns/rdfa#"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	<xsl:output method="xml" encoding="UTF-8"/>
	<xsl:template match="/">
		<html>
			<head>
				<title>RDF Profile</title>
			</head>
			<body>
				<h1>RDF Profile</h1>
				<form method="POST" action="" autocomplete="off">
					<xsl:apply-templates />
					<button type="submit">Save</button>
				</form>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="rdf:RDF">
		<table class="graph">
			<thead>
				<tr><th>Prefix</th><th>URI</th></tr>
			</thead>
			<tbody class="sorted">
				<xsl:apply-templates select="*" />
			</tbody>
			<tbody>
				<tr>
					<td>
						<input type="text" size="8" onchange="$('#new-uri').attr('name', value)" />
					</td>
					<td>
						<input type="text" id="new-uri" class="auto-expand" />
						<span class="ui-icon ui-icon-close" style="display:inline-block;vertical-align:middle" onclick="$(this).parents('tr:first').remove()">
							<xsl:text> </xsl:text>
						</span>
					</td>
				</tr>
			</tbody>
		</table>
	</xsl:template>
	<xsl:template match="rdf:Description">
		<tr about="{@rdf:about}">
			<xsl:apply-templates select="rdfa:prefix" />
			<xsl:apply-templates select="rdfa:uri" />
		</tr>
	</xsl:template>
	<xsl:template match="rdfa:prefix">
		<td property="rdfa:prefix" class="asc">
			<xsl:apply-templates />
		</td>
	</xsl:template>
	<xsl:template match="rdfa:uri">
		<td property="rdfa:uri" content="{text()}">
			<input type="text" name="{../rdfa:prefix}" value="{text()}" class="auto-expand" />
			<span class="ui-icon ui-icon-close" style="display:inline-block;vertical-align:middle" onclick="$(this).parents('tr:first').remove()">
				<xsl:text> </xsl:text>
			</span>
		</td>
	</xsl:template>
</xsl:stylesheet>
