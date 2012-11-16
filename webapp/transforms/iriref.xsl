<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:rdfa="http://www.w3.org/ns/rdfa#">
    <xsl:param name="systemId" />
    <xsl:template name="substring-after-last">
        <xsl:param name="string"/>
        <xsl:param name="delimiter"/>
        <xsl:if test="not(contains(substring($string, 0, string-length($string) - 1),$delimiter))">
            <xsl:value-of select="$string"/>
        </xsl:if>
        <xsl:if test="contains(substring($string, 0, string-length($string) - 1),$delimiter)">
            <xsl:call-template name="substring-after-last">
                <xsl:with-param name="string" select="substring-after($string,$delimiter)"/>
                <xsl:with-param name="delimiter" select="$delimiter"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
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
    <xsl:template name="iriref">
        <xsl:param name="iri"/>
        <xsl:variable name="namespace">
            <xsl:choose>
                <xsl:when test="contains($iri, '#')">
                    <xsl:call-template name="substring-before-last">
                        <xsl:with-param name="string" select="$iri"/>
                        <xsl:with-param name="delimiter" select="'#'"/>
                    </xsl:call-template>
                    <xsl:text>#</xsl:text>
                </xsl:when>
                <xsl:when test="contains($iri, '/')">
                    <xsl:call-template name="substring-before-last">
                        <xsl:with-param name="string" select="$iri"/>
                        <xsl:with-param name="delimiter" select="'/'"/>
                    </xsl:call-template>
                    <xsl:text>/</xsl:text>
                </xsl:when>
                <xsl:when test="contains($iri, ':')">
                    <xsl:call-template name="substring-before-last">
                        <xsl:with-param name="string" select="$iri"/>
                        <xsl:with-param name="delimiter" select="':'"/>
                    </xsl:call-template>
                    <xsl:text>:</xsl:text>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="local">
            <xsl:choose>
                <xsl:when test="contains($iri, '#')">
                    <xsl:call-template name="substring-after-last">
                        <xsl:with-param name="string" select="$iri"/>
                        <xsl:with-param name="delimiter" select="'#'"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="contains($iri, '/')">
                    <xsl:call-template name="substring-after-last">
                        <xsl:with-param name="string" select="$iri"/>
                        <xsl:with-param name="delimiter" select="'/'"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="contains($iri, ':')">
                    <xsl:call-template name="substring-after-last">
                        <xsl:with-param name="string" select="$iri"/>
                        <xsl:with-param name="delimiter" select="':'"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$iri" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="wd">
            <xsl:if test="contains($systemId, '/')">
                <xsl:call-template name="substring-before-last">
                    <xsl:with-param name="string" select="$systemId" />
                    <xsl:with-param name="delimiter" select="'/'" />
                </xsl:call-template>
                <xsl:text>/</xsl:text>
            </xsl:if>
        </xsl:variable>
        <xsl:variable name="ns" select="document('../profile')//rdfa:uri[text()=$namespace]" />
        <xsl:choose>
            <xsl:when test="$namespace and $ns">
                <xsl:value-of select="$ns/../rdfa:prefix" />
                <xsl:value-of select="':'" />
                <xsl:value-of select="$local" />
            </xsl:when>
            <xsl:when test="not(contains($iri, '://')) or substring-before($systemId, '://') != substring-before($iri, '://')">
                <xsl:value-of select="$iri" />
            </xsl:when>
            <xsl:when test="$wd and string-length($wd) &lt; string-length($iri) and $wd=substring($iri, 1, string-length($wd))">
                <xsl:value-of select="substring($iri, string-length($wd) + 1, string-length($iri))" />
            </xsl:when>
            <xsl:when test="substring-before(substring-after($systemId, '://'), '/') = substring-before(substring-after($iri, '://'), '/')">
                <xsl:value-of select="'/'" />
                <xsl:value-of select="substring-after(substring-after($iri, '://'), '/')" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="'//'" />
                <xsl:value-of select="substring-after($iri, '://')" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
