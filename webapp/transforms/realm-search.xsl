<?xml version="1.0" encoding="UTF-8" ?>
<!--
  - Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -
  -->
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
                <title>Search Results</title>
                <link rel="help" href="../../callimachus-for-web-developers" target="_blank" title="Help" />
                <script>
                // <![CDATA[
                jQuery(function($) {
                    if (parameter("q")) {
                        document.getElementById("q").value = parameter("q")
                    }
                    $('.cite time').text(function(){
                        return calli.parseDateTime(this).toLocaleString();
                    });
                    function parameter(name) {
                        var regex = new RegExp("[\\?&]"+name+"=([^&#]*)")
                        var m = regex.exec(window.location.href)
                        return m ? decodeURIComponent(m[1].replace(/\+/g, ' ')) : null
                    }
                });
                // ]]>
                </script>
            </head>
            <body>
                <div class="container">
                    <div class="page-header">
                        <h1>Search Results</h1>
                    </div>
                    <form role="form" method="GET" class="search">
                        <div class="form-group">
                            <input type="text" id="q" name="q" size="40" class="form-control" autofocus="autofocus" required="required" />
                        </div>
                        <div class="form-group">
                            <button type="submit" class="btn btn-primary">Search</button>
                        </div>
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
                </div>
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
                    <img src="{resolve-uri('../images/rdf_flyer.png')}" class="icon" />
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
                <p class="summary">
                    <xsl:value-of select="sparql:binding[@name='summary']/*" />
                </p>
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
                        <time datetime="{sparql:binding[@name='updated']/*}" datatype="{sparql:binding[@name='updated']/*/@datatype}">
                            <xsl:value-of select="sparql:binding[@name='updated']/*" />
                        </time>
                    </xsl:if>
                </div>
            </xsl:if>
        </li>
    </xsl:template>
</xsl:stylesheet>
