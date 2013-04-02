<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://docbook.org/ns/docbook" xmlns:d="http://docbook.org/ns/docbook"
    xmlns:xl="http://www.w3.org/1999/xlink" exclude-result-prefixes="xsl d">
<xsl:output method="xml" indent="yes" />

<xsl:template match="@*|node()">
    <xsl:copy>
        <xsl:if test="self::* and base-uri(.)!=base-uri(..) and not(@xml:id)">
            <xsl:attribute name="xml:id">
                <xsl:call-template name="id">
                    <xsl:with-param name="target" select="." />
                </xsl:call-template>
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
</xsl:template>

<xsl:template match="d:preface/d:article|d:partintro/d:article|d:article/d:article|d:chapter/d:article|d:appendix/d:article|d:section/d:article|d:topic/d:article">
    <section>
        <xsl:if test="base-uri(.)!=base-uri(..) and not(@xml:id)">
            <xsl:attribute name="xml:id">
                <xsl:call-template name="id">
                    <xsl:with-param name="target" select="." />
                </xsl:call-template>
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="@*|node()" />
    </section>
</xsl:template>

<xsl:template match="@xml:id">
    <xsl:attribute name="xml:id">
        <xsl:call-template name="id">
            <xsl:with-param name="target" select=".." />
        </xsl:call-template>
    </xsl:attribute>
</xsl:template>

<xsl:template match="d:link/@linkend">
    <xsl:variable name="id" select="." />
    <xsl:variable name="base" select="base-uri(.)" />
    <xsl:variable name="article" select="ancestor::*[base-uri()=$base][last()]" />
    <xsl:variable name="target" select="$article//*[@xml:id=$id]" />
    <xsl:choose>
        <xsl:when test="count($target)=1">
            <xsl:attribute name="linkend">
                <xsl:call-template name="id">
                    <xsl:with-param name="target" select="$target" />
                </xsl:call-template>
            </xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
            <xsl:copy />
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template match="d:link/@xl:href">
    <xsl:variable name="url" select="." />
    <xsl:variable name="uri">
        <xsl:choose>
            <xsl:when test="contains($url, '?')">
                <xsl:value-of select="substring-before($url, '?')" />
            </xsl:when>
            <xsl:when test="contains($url, '#')">
                <xsl:value-of select="substring-before($url, '#')" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="string($url)" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="frag" select="substring-after($url, '#')" />
    <xsl:choose>
        <xsl:when test="$uri=base-uri(/)">
            <xsl:attribute name="linkend">
                <xsl:value-of select="$frag" />
            </xsl:attribute>
        </xsl:when>
        <xsl:when test="//*[@xml:id=$frag and $uri=base-uri()]">
            <xsl:attribute name="linkend">
                <xsl:call-template name="id">
                    <xsl:with-param name="target" select="(//*[@xml:id=$frag and $uri=base-uri()])[1]" />
                </xsl:call-template>
            </xsl:attribute>
        </xsl:when>
        <xsl:when test="string-length($frag)=0 and //*[$uri=base-uri()]">
            <xsl:attribute name="linkend">
                <xsl:call-template name="id">
                    <xsl:with-param name="target" select="(//*[$uri=base-uri()])[1]" />
                </xsl:call-template>
            </xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
            <xsl:copy />
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template name="id">
    <xsl:param name="target" />
    <xsl:variable name="id" select="$target/@xml:id" />
    <xsl:variable name="preceding" select="count($target/preceding::*[@xml:id = $id])" />
    <xsl:variable name="title" select="replace(normalize-space($target/d:title|$target/d:info/d:title),'\W','_')" />
    <xsl:variable name="precedingTitle" select="count($target/preceding::d:title[replace(normalize-space(),'\W','_')=$title])" />
    
    <xsl:choose>
        <xsl:when test="$id and 0!=$preceding">
            <xsl:value-of select="concat($id, $preceding)" />
        </xsl:when>
        <xsl:when test="$id">
            <xsl:value-of select="$id" />
        </xsl:when>
        <xsl:when test="$title and 0!=$precedingTitle">
            <xsl:value-of select="concat($title, $precedingTitle)" />
        </xsl:when>
        <xsl:when test="$title">
            <xsl:value-of select="$title" />
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="generate-id($target)" />
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

</xsl:stylesheet>
