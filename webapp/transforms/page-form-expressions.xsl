<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml">

    <xsl:variable name="systemId" select="base-uri()" />
    <xsl:variable name="origin" select="resolve-uri('/')" />
    <xsl:variable name="callback">
        <xsl:choose>
            <xsl:when test="starts-with($systemId,$origin)">
                <xsl:value-of select="$systemId" />
            </xsl:when>
            <xsl:when test="$origin and $systemId">
                <xsl:call-template name="divert">
                    <xsl:with-param name="diverted" select="concat($origin,'diverted;')" />
                    <xsl:with-param name="url" select="$systemId" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$systemId" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

    <!-- form -->
    <xsl:template match="form|xhtml:form">
        <xsl:apply-templates mode="form" select="." />
    </xsl:template>

    <!-- Form -->
    <xsl:template mode="form" match="*">
        <xsl:copy>
            <xsl:call-template name="data-attributes" />
            <xsl:apply-templates mode="form" select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template mode="form" match="@*|comment()|text()">
        <xsl:copy />
    </xsl:template>

    <xsl:template name="data-attributes">
        <xsl:if test="$callback and ancestor-or-self::*[@id and not(self::xhtml:html)]">
            <xsl:apply-templates mode="data-var" select="@*"/>
            <xsl:apply-templates mode="data-expression" select="@*"/>
            <xsl:if test="text() and not(*|comment())">
                <xsl:call-template name="data-text-expression">
                    <xsl:with-param name="text" select="string(.)"/>
                </xsl:call-template>
            </xsl:if>
            <xsl:if test="xhtml:option[@selected='selected'][@about or @resource] or xhtml:label[@about or @resource]/xhtml:input[@checked='checked']">
                <xsl:if test=".//*[@typeof or @rel and @resource and not(starts-with(@resource,'?')) or @rev and @resource and not(starts-with(@resource,'?'))]">
                    <xsl:if test="not(@data-options)">
                        <!-- Called to populate select/radio/checkbox -->
                        <xsl:attribute name="data-options">
                            <xsl:value-of select="$callback" />
                            <xsl:text>?options&amp;element=</xsl:text>
                            <xsl:apply-templates mode="xptr-element" select="." />
                        </xsl:attribute>
                    </xsl:if>
                </xsl:if>
            </xsl:if>
            <xsl:if test="*[@about or @resource] and not(@data-construct)">
                <!-- Called when a resource URI is dropped to construct its label -->
                <xsl:attribute name="data-construct">
                    <xsl:value-of select="$callback" />
                    <xsl:text>?options&amp;element=</xsl:text>
                    <xsl:apply-templates mode="xptr-element" select="." />
                    <xsl:text>&amp;resource={resource}</xsl:text>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="*[@about or @resource] and not(@data-search)">
                <!-- Lookup possible members by label -->
                <xsl:attribute name="data-search">
                    <xsl:value-of select="$callback" />
                    <xsl:text>?options&amp;element=</xsl:text>
                    <xsl:apply-templates mode="xptr-element" select="." />
                    <xsl:text>&amp;q={searchTerms}</xsl:text>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="*[@about or @typeof or @resource or @property] and not(@data-add)">
                <!-- Called to insert another property value or node -->
                <xsl:attribute name="data-add">
                    <xsl:value-of select="$callback" />
                    <xsl:text>?element=</xsl:text>
                    <xsl:apply-templates mode="xptr-element" select="." />
                </xsl:attribute>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <!-- variable expressions -->
    <xsl:template mode="data-var" match="@*" />
    <xsl:template mode="data-var" match="@about|@resource|@content|@href|@src">
        <xsl:if test="starts-with(., '?')">
            <xsl:attribute name="data-var-{name()}">
                <xsl:value-of select="." />
            </xsl:attribute>
        </xsl:if>
    </xsl:template>
    <xsl:template mode="data-expression" match="@*">
        <xsl:variable name="expression">
            <xsl:value-of select="substring-before(substring-after(., '{'), '}')"/>
        </xsl:variable>
        <xsl:if test="string(.) = concat('{', $expression, '}')">
            <xsl:attribute name="data-expression-{name()}">
                <xsl:value-of select="$expression" />
            </xsl:attribute>
        </xsl:if>
    </xsl:template>
    <xsl:template name="data-text-expression">
        <xsl:param name="text" />
        <xsl:variable name="expression">
            <xsl:value-of select="substring-before(substring-after($text, '{'), '}')"/>
        </xsl:variable>
        <xsl:if test="$text = concat('{', $expression, '}')">
            <xsl:attribute name="data-text-expression">
                <xsl:value-of select="$expression" />
            </xsl:attribute>
        </xsl:if>
    </xsl:template>

    <xsl:template name="divert">
        <xsl:param name="diverted" />
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

    <!-- xptr-element -->
    <xsl:template mode="xptr-element" match="*[@id]">
        <xsl:value-of select="@id" />
    </xsl:template>
    <xsl:template mode="xptr-element" match="*">
        <xsl:apply-templates mode="xptr-element" select=".." />
        <xsl:text>/</xsl:text>
        <xsl:value-of select="count(preceding-sibling::*) + 1" />
    </xsl:template>

</xsl:stylesheet>
