<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    exclude-result-prefixes="xhtml sparql">
    <xsl:import href="../transforms/iriref.xsl" />
    <xsl:template match="/">
        <html>
            <head>
                <title>Recent Changes</title>
                <link rel="help" href="../../callimachus-for-web-developers#System_menu" target="_blank" title="Help" />
            </head>
            <body>
                <div class="container">
                    <hgroup class="page-header">
                        <h1>Recent Changes</h1>
                    </hgroup>
                    <xsl:apply-templates />
                </div>
            </body>
        </html>
    </xsl:template>
    <xsl:template match="sparql:results">
        <xsl:if test="not(sparql:result[sparql:binding[@name='type']/*='entry'])">
            <p>No changes have been made recently.</p>
        </xsl:if>
        <xsl:if test="sparql:result[sparql:binding[@name='type']/*='entry']">
            <xsl:for-each-group select="sparql:result[sparql:binding[@name='type']/*='entry']"
                    group-by="substring-before(sparql:binding[@name='updated']/*, 'T')">
                <h2><time class="abbreviated date"><xsl:value-of select="sparql:binding[@name='updated']/*" /></time></h2>
                <ul>
                    <xsl:apply-templates select="current-group()" />
                </ul>
            </xsl:for-each-group>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sparql:result">
        <li class="result">
            <xsl:if test="sparql:binding[@name='icon']/*">
                <img src="{sparql:binding[@name='icon']/*}" class="icon" />
            </xsl:if>
            <xsl:if test="not(sparql:binding[@name='icon']/*)">
                <img src="{resolve-uri('../images/rdf_flyer.png')}" class="icon" />
            </xsl:if>
            <a>
                <xsl:if test="sparql:binding[@name='id']/*">
                    <xsl:attribute name="href">
                        <xsl:value-of select="concat(sparql:binding[@name='id']/*,'?view')" />
                    </xsl:attribute>
                </xsl:if>
                <xsl:apply-templates select="sparql:binding[@name='title']/*" />
                <xsl:if test="not(sparql:binding[@name='title']/*)">
                    <xsl:call-template name="iriref">
                        <xsl:with-param name="iri" select="sparql:binding[@name='id']/*"/>
                    </xsl:call-template>
                </xsl:if>
            </a>
            <xsl:text>; </xsl:text>
            <a href="{sparql:binding[@name='link_href']/*}"><time class="abbreviated time"><xsl:value-of select="sparql:binding[@name='updated']/*" /></time></a>
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
            <xsl:if test="sparql:binding[@name='summary']/*">
                <xsl:text> (</xsl:text>
                <xsl:value-of select="sparql:binding[@name='summary']/*" />
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
