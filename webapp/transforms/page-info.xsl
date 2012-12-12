<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns="http://www.w3.org/1999/xhtml"
        xmlns:xhtml="http://www.w3.org/1999/xhtml"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <xsl:output indent="no" method="xml" />

    <xsl:param name="target" />
    <xsl:param name="query" />

    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*|*|comment()|text()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|comment()">
        <xsl:copy />
    </xsl:template>

    <xsl:template match="xhtml:nav[@id='calli-access']">
        <xsl:if test="*//@href[.=concat('?',$query)]">
            <xsl:apply-templates mode="access" />
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
            <xsl:apply-templates mode="access" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xhtml:div[@id='calli-lastmod']">
        <xsl:if test="string-length($target) &gt; 0">
            <xsl:apply-templates mode="time" />
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

    <xsl:template match="xhtml:nav[@id='calli-breadcrumb']">
        <xsl:variable name="divider" select="node()" />
        <xsl:variable name="url" select="concat('../queries/page-info.rq?results&amp;target=',encode-for-uri($target))" />
        <xsl:if test="string-length($target) &gt; 0 and count(document($url)//sparql:result[sparql:binding/@name='iri']) > 1">
            <nav class="nav">
                <xsl:for-each select="document($url)//sparql:result[sparql:binding/@name='iri']">
                    <xsl:if test="$target!=sparql:binding[@name='iri']/*">
                        <a href="{sparql:binding[@name='iri']/*}">
                            <xsl:value-of select="sparql:binding[@name='label']/*" />
                            <xsl:if test="not(sparql:binding[@name='label']/*)">
                                <xsl:call-template name="substring-after-last">
                                    <xsl:with-param name="arg" select="sparql:binding[@name='iri']/*" />
                                    <xsl:with-param name="delim" select="'/'" />
                                </xsl:call-template>
                            </xsl:if>
                        </a>
                        <xsl:copy-of select="$divider" />
                    </xsl:if>
                    <xsl:if test="$target=sparql:binding[@name='iri']/*">
                        <span class="active">
                            <xsl:value-of select="sparql:binding[@name='label']/*" />
                            <xsl:if test="not(sparql:binding[@name='label']/*)">
                                <xsl:call-template name="substring-after-last">
                                    <xsl:with-param name="arg" select="sparql:binding[@name='iri']/*" />
                                    <xsl:with-param name="delim" select="'/'" />
                                </xsl:call-template>
                            </xsl:if>
                        </span>
                    </xsl:if>
                </xsl:for-each>
            </nav>
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
