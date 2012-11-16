<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns="http://www.w3.org/1999/xhtml"
        xmlns:xhtml="http://www.w3.org/1999/xhtml"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <xsl:output indent="no" method="xml" />

    <xsl:param name="realm" select="'/'" />
    <xsl:param name="target" />
    <xsl:param name="query" />

    <xsl:variable name="realmNotEmpty">
        <xsl:value-of select="$realm" />
        <xsl:if test="string-length($realm) = 0">
            <xsl:value-of select="'/'" />
        </xsl:if>
    </xsl:variable>

    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*|*|comment()|text()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|comment()">
        <xsl:copy />
    </xsl:template>

    <xsl:template match="*[@id='login-link']/@href">
        <xsl:attribute name="href">
            <xsl:value-of select="$realmNotEmpty" />
            <xsl:text>?login</xsl:text>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="*[@id='profile-link']/@href">
        <xsl:attribute name="href">
            <xsl:value-of select="$realmNotEmpty" />
            <xsl:text>?profile</xsl:text>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="*[@id='logout-link']/@href">
        <xsl:attribute name="href">
            <xsl:value-of select="$realmNotEmpty" />
            <xsl:text>?logout</xsl:text>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="*[@id='access']">
        <xsl:if test="*//@href[.=concat('?',$query)]">
            <xsl:copy>
                <xsl:apply-templates mode="access" select="@*|*|text()|comment()" />
            </xsl:copy>
        </xsl:if>
    </xsl:template>

    <xsl:template mode="access" match="*">
        <xsl:copy>
            <xsl:apply-templates mode="access" select="@*|*|text()|comment()" />
        </xsl:copy>
    </xsl:template>
    <xsl:template mode="access" match="@*|text()|comment()">
        <xsl:copy />
    </xsl:template>
    <xsl:template mode="access" match="xhtml:a[@href]|a[@href]">
        <xsl:copy>
            <xsl:if test="@href=concat('?',$query)">
                <xsl:apply-templates mode="access" select="@*[name()!='href' and name()!='class' and name()!='onclick']" />
                <xsl:attribute name="class">
                    <xsl:if test="@class">
                        <xsl:value-of select="@class" />
                        <xsl:text> </xsl:text>
                    </xsl:if>
                    <xsl:text>active</xsl:text>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="not(@href=concat('?',$query))">
                <xsl:apply-templates mode="access" select="@*" />
            </xsl:if>
            <xsl:apply-templates mode="access" select="*|text()|comment()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xhtml:p[@id='resource-lastmod']|p[@id='resource-lastmod']">
        <xsl:if test="string-length($target) &gt; 0">
            <xsl:copy>
                <xsl:apply-templates select="@*" />
                <xsl:apply-templates mode="time" select="*|text()|comment()" />
            </xsl:copy>
        </xsl:if>
    </xsl:template>

    <xsl:template mode="time" match="@*|text()|comment()">
        <xsl:copy />
    </xsl:template>

    <xsl:template mode="time" match="*">
        <xsl:copy>
            <xsl:apply-templates mode="time" select="@*|node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template mode="time" match="xhtml:time|time">
        <xsl:variable name="url" select="concat('../queries/page-info.rq?results&amp;target=',encode-for-uri($target))" />
        <xsl:copy>
            <xsl:apply-templates mode="time" select="@*|node()" />
            <xsl:apply-templates mode="time" select="document($url)//sparql:binding[@name='updated']/*/@datatype" />
            <xsl:value-of select="document($url)//sparql:binding[@name='updated']/*" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="*[@id='breadcrumbs']">
        <xsl:if test="string-length($target) &gt; 0">
            <xsl:call-template name="breadcrumbs">
                <xsl:with-param name="node" select="." />
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="breadcrumbs">
        <xsl:param name="node" />
        <xsl:variable name="url" select="concat('../queries/page-info.rq?results&amp;target=',encode-for-uri($target))" />
        <xsl:variable name="links" select="$node/xhtml:a|$node/a" />
        <xsl:variable name="breadcrumb" select="$links[1]" />
        <xsl:variable name="active" select="$node/*[2]|$node/*[contains(@class,'active')]" />
        <xsl:variable name="here" select="$active[last()]" />
        <xsl:if test="$breadcrumb and $here and count(document($url)//sparql:result[sparql:binding/@name='iri']) > 1">
            <xsl:variable name="separator" select="$breadcrumb/following-sibling::node()[1]|$breadcrumb/following-sibling::*[contains(@class,'divider')]" />
            <xsl:variable name="close" select="$here/following-sibling::node()" />
            <xsl:copy>
                <xsl:apply-templates select="@*" />
                <xsl:for-each select="document($url)//sparql:result[sparql:binding/@name='iri']">
                    <xsl:if test="$target!=sparql:binding[@name='iri']/*">
                        <xsl:element name="{name($breadcrumb)}">
                            <xsl:attribute name="href"><xsl:value-of select="sparql:binding[@name='iri']/*" /></xsl:attribute>
                            <xsl:apply-templates select="$breadcrumb/@*[name()!='href']" />
                            <xsl:value-of select="sparql:binding[@name='label']/*" />
                            <xsl:if test="not(sparql:binding[@name='label']/*)">
                                <xsl:call-template name="substring-after-last">
                                    <xsl:with-param name="arg" select="sparql:binding[@name='iri']/*" />
                                    <xsl:with-param name="delim" select="'/'" />
                                </xsl:call-template>
                            </xsl:if>
                        </xsl:element>
                        <xsl:copy-of select="$separator" />
                    </xsl:if>
                    <xsl:if test="$target=sparql:binding[@name='iri']/*">
                        <xsl:element name="{name($here)}">
                            <xsl:apply-templates select="$here/@*" />
                            <xsl:value-of select="sparql:binding[@name='label']/*" />
                            <xsl:if test="not(sparql:binding[@name='label']/*)">
                                <xsl:call-template name="substring-after-last">
                                    <xsl:with-param name="arg" select="sparql:binding[@name='iri']/*" />
                                    <xsl:with-param name="delim" select="'/'" />
                                </xsl:call-template>
                            </xsl:if>
                        </xsl:element>
                        <xsl:copy-of select="$close" />
                    </xsl:if>
                </xsl:for-each>
            </xsl:copy>
        </xsl:if>
    </xsl:template>

    <xsl:template name="substring-after-last">
        <xsl:param name="arg"/>
        <xsl:param name="delim"/>
        <xsl:if test="not(contains($arg, $delim))">
            <xsl:value-of select="$arg" />
        </xsl:if>
        <xsl:if test="contains($arg, $delim)">
            <xsl:call-template name="substring-after-last">
                <xsl:with-param name="arg" select="substring-after($arg, $delim)"/>
                <xsl:with-param name="delim" select="$delim"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
