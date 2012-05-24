<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    exclude-result-prefixes="xhtml sparql">
    <xsl:import href="../transforms/iriref.xsl" />
    <xsl:param name="this" />
    <xsl:template match="/">
        <html>
            <head>
                <title><xsl:value-of select="sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='title']/*" /></title>
            </head>
            <body>
                <h1><xsl:value-of select="sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='title']/*" /></h1>
                <xsl:apply-templates />
            </body>
        </html>
    </xsl:template>
    <xsl:template match="sparql:results">
        <xsl:if test="not(sparql:result[sparql:binding[@name='type']/*='entry'])">
            <p>No changes have been made recently.</p>
        </xsl:if>
        <xsl:if test="sparql:result[sparql:binding[@name='type']/*='entry']">
            <ul>
                <xsl:for-each select="sparql:result[sparql:binding[@name='type']/*='entry']">
                    <xsl:variable name="id" select="sparql:binding[@name='id']/*" />
                    <xsl:if test="not(preceding::sparql:result[sparql:binding[@name='id']/*=$id])">
                        <xsl:apply-templates select="." />
                    </xsl:if>
                </xsl:for-each>
            </ul>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sparql:result">
        <xsl:variable name="id" select="sparql:binding[@name='id']/*" />
        <xsl:variable name="results" select="../sparql:result[sparql:binding[@name='id']/*=$id]" />
        <xsl:if test="not(substring-before(sparql:binding[@name='updated']/*, 'T')=substring-before(preceding-sibling::*[1]/sparql:binding[@name='updated']/*, 'T'))">
            <xsl:text disable-output-escaping="yes">&lt;/ul&gt;</xsl:text>
            <h2><time class="abbreviated date"><xsl:value-of select="sparql:binding[@name='updated']/*" /></time></h2>
            <xsl:text disable-output-escaping="yes">&lt;ul&gt;</xsl:text>
        </xsl:if>
        <li class="result">
            <xsl:if test="$results/sparql:binding[@name='icon']/*">
                <img src="{$results/sparql:binding[@name='icon']/*}" class="icon" />
            </xsl:if>
            <xsl:if test="not($results/sparql:binding[@name='icon']/*)">
                <img src="/callimachus/images/rdf-icon.png" class="icon" />
            </xsl:if>
            <a href="{$this}" class="view">
                <xsl:apply-templates select="$results/sparql:binding[@name='title']/*" />
                <xsl:if test="not($results/sparql:binding[@name='title']/*)">
                    <xsl:call-template name="iriref">
                        <xsl:with-param name="iri" select="sparql:binding[@name='id']/*"/>
                    </xsl:call-template>
                </xsl:if>
            </a>
            <xsl:text>; </xsl:text>
            <a href="{sparql:binding[@name='id']/*}"><time class="abbreviated time"><xsl:value-of select="sparql:binding[@name='updated']/*" /></time></a>
            <xsl:text>..</xsl:text>
            <a>
                <xsl:if test="sparql:binding[@name='contributor_uri']">
                    <xsl:attribute name="href">
                        <xsl:value-of select="sparql:binding[@name='contributor_uri']/*" />
                    </xsl:attribute>
                </xsl:if>
                <xsl:apply-templates select="sparql:binding[@name='contributor_name']/*" />
                <xsl:if test="not(sparql:binding[@name='contributor_name'])">
                    <xsl:value-of select="sparql:binding[@name='contributor_uri']/*" />
                </xsl:if>
            </a>
            <xsl:if test="$results/sparql:binding[@name='summary']/*">
                <xsl:text> (</xsl:text>
                <xsl:value-of select="$results/sparql:binding[@name='summary']/*" />
                <xsl:text>)</xsl:text>
            </xsl:if>
        </li>
    </xsl:template>
    <xsl:template match="sparql:binding">
        <xsl:apply-templates select="*" />
    </xsl:template>
    <xsl:template match="sparql:uri">
        <span class="uri">
            <xsl:value-of select="text()" />
        </span>
    </xsl:template>
    <xsl:template match="sparql:bnode">
        <span class="bnode">
            <xsl:value-of select="text()" />
        </span>
    </xsl:template>
    <xsl:template match="sparql:literal">
        <span class="literal">
            <xsl:value-of select="text()" />
        </span>
    </xsl:template>
    <xsl:template
        match="sparql:literal[@datatype='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral']">
        <span class="literal" datatype="rdf:XMLLiteral">
            <xsl:value-of disable-output-escaping="yes" select="text()" />
        </span>
    </xsl:template>
</xsl:stylesheet>
