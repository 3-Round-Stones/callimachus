<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://www.w3.org/2005/Atom"
	xmlns:atom="http://www.w3.org/2005/Atom">

<xsl:param name="xsltId" />

<xsl:variable name="origin">
	<xsl:if test="$xsltId">
		<xsl:value-of select="concat(substring-before($xsltId,'://'),'://',substring-before(substring-after($xsltId,'://'),'/'),'/')" />
	</xsl:if>
</xsl:variable>
<xsl:variable name="diverted" select="concat($origin,'diverted;')" />

<xsl:template match="@*|node()">
	<xsl:copy>
		<xsl:apply-templates select="@*|node()"/>
	</xsl:copy>
</xsl:template>

<xsl:template match="@href[not(starts-with(.,$origin))]">
	<xsl:attribute name="href">
		<xsl:call-template name="divert">
			<xsl:with-param name="url" select="." />
		</xsl:call-template>
	</xsl:attribute>
</xsl:template>

<xsl:template match="@src[not(starts-with(.,$origin))]">
	<xsl:attribute name="src">
		<xsl:call-template name="divert">
			<xsl:with-param name="url" select="." />
		</xsl:call-template>
	</xsl:attribute>
</xsl:template>

<xsl:template match="atom:icon[not(starts-with(.,$origin))]">
	<xsl:copy>
		<xsl:apply-templates select="@*" />
		<xsl:call-template name="divert">
			<xsl:with-param name="url" select="." />
		</xsl:call-template>
	</xsl:copy>
</xsl:template>

<xsl:template match="atom:logo[not(starts-with(.,$origin))]">
	<xsl:copy>
		<xsl:apply-templates select="@*" />
		<xsl:call-template name="divert">
			<xsl:with-param name="url" select="." />
		</xsl:call-template>
	</xsl:copy>
</xsl:template>

<xsl:template name="divert">
	<xsl:param name="url" />
	<xsl:variable name="uri">
		<xsl:choose>
			<xsl:when test="contains($url,'?')">
				<xsl:value-of select="substring-before($url,'?')" />
			</xsl:when>
			<xsl:when test="contains($url,'#')">
				<xsl:value-of select="substring-before($url,'#')" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$url" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:value-of select="$diverted" />
	<xsl:call-template name="replace-string">
		<xsl:with-param name="text">
			<xsl:call-template name="replace-string">
				<xsl:with-param name="text" select="$uri"/>
				<xsl:with-param name="replace" select="'%'"/>
				<xsl:with-param name="with" select="'%25'"/>
			</xsl:call-template>
		</xsl:with-param>
		<xsl:with-param name="replace" select="'+'"/>
		<xsl:with-param name="with" select="'%2B'"/>
	</xsl:call-template>
	<xsl:value-of select="substring-after($url,$uri)" />
</xsl:template>

<xsl:template name="replace-string">
	<xsl:param name="text"/>
	<xsl:param name="replace"/>
	<xsl:param name="with"/>
	<xsl:choose>
		<xsl:when test="contains($text,$replace)">
			<xsl:value-of select="substring-before($text,$replace)"/>
			<xsl:value-of select="$with"/>
			<xsl:call-template name="replace-string">
				<xsl:with-param name="text" select="substring-after($text,$replace)"/>
				<xsl:with-param name="replace" select="$replace"/>
				<xsl:with-param name="with" select="$with"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:otherwise>
			<xsl:value-of select="$text"/>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

</xsl:stylesheet>
