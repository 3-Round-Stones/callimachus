<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    exclude-result-prefixes="xhtml sparql">
    <xsl:import href="../transforms/iriref.xsl" />
    <xsl:template match="/">
        <html>
            <head>
                <title>Search Results</title>
                <script>
                // <![CDATA[
                function parameter(name) {
                    var regex = new RegExp("[\\?&]"+name+"=([^&#]*)")
                    var m = regex.exec(location.href)
                    return m ? decodeURIComponent(m[1].replace(/\+/g, ' ')) : null
                }
                function init() {
                    if (parameter("q")) {
                        document.getElementById("q").value = parameter("q")
                    }
                }
                // ]]>
                </script>
            </head>
            <body onload="init()">
                <h1>Search Results</h1>
                <form method="GET" class="search">
                    <input type="text" id="q" name="q" size="40" />
                    <button type="submit">Search</button>
                </form>
                <hr />
                <xsl:if test="not(/sparql:sparql/sparql:results/sparql:result[sparql:binding[@name='type']/*='entry'])">
                    <p>No resources with this label found.</p>
                </xsl:if>
                <xsl:if test="/sparql:sparql/sparql:results/sparql:result[sparql:binding[@name='type']/*='entry']">
                    <ul id="results">
                        <xsl:apply-templates select="/sparql:sparql/sparql:results/sparql:result[sparql:binding[@name='type']/*='entry']" />
                    </ul>
                </xsl:if>
            </body>
        </html>
    </xsl:template>
    <xsl:template match="sparql:result">
        <li class="result">
            <a>
                <xsl:if test="sparql:binding[@name='id']">
                    <xsl:attribute name="href">
                        <xsl:value-of select="sparql:binding[@name='id']/*" />
                    </xsl:attribute>
                    <xsl:attribute name="class">
                        <xsl:value-of select="'view'" />
                    </xsl:attribute>
                </xsl:if>
                <xsl:if test="sparql:binding[@name='icon']">
                    <img src="{sparql:binding[@name='icon']/*}" class="icon" />
                </xsl:if>
                <xsl:if test="not(sparql:binding[@name='icon'])">
                    <img src="/callimachus/images/rdf-icon.png" class="icon" />
                </xsl:if>
            </a>
            <a>
                <xsl:if test="sparql:binding[@name='id']">
                    <xsl:attribute name="href">
                        <xsl:value-of select="sparql:binding[@name='id']/*" />
                    </xsl:attribute>
                    <xsl:attribute name="class">
                        <xsl:value-of select="'view'" />
                    </xsl:attribute>
                </xsl:if>
                <xsl:value-of select="sparql:binding[@name='title']/*" />
            </a>
            <xsl:if test="sparql:binding[@name='summary']">
                <pre class="wiki summary">
                    <xsl:value-of select="sparql:binding[@name='summary']/*" />
                </pre>
            </xsl:if>
            <xsl:if test="sparql:binding[@name='id']">
                <div class="cite">
                    <span class="url" title="{sparql:binding[@name='id']/*}">
                        <xsl:variable name="ref">
                            <xsl:call-template name="iriref">
                                <xsl:with-param name="iri" select="sparql:binding[@name='id']/*" />
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:choose>
                            <xsl:when test="string-length($ref) &gt; 63">
                                <xsl:attribute name="title"><xsl:value-of select="$ref" /></xsl:attribute>
                                <xsl:value-of select="substring($ref, 0, 40)" />
                                <span>...</span>
                                <xsl:value-of select="substring($ref, string-length($ref) - 20, string-length($ref))" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$ref" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                    <xsl:if test="sparql:binding[@name='updated']">
                        <span> - </span>
                        <time class="abbreviated">
                            <xsl:value-of select="sparql:binding[@name='updated']/*" />
                        </time>
                    </xsl:if>
                </div>
            </xsl:if>
        </li>
    </xsl:template>
</xsl:stylesheet>
