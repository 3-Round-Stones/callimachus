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
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    exclude-result-prefixes="xhtml">
    <xsl:import href="iriref.xsl" />
    <xsl:output indent="no" method="xml" />
    <xsl:param name="target" />
    <xsl:param name="systemId" />
    <xsl:template name="resource">
        <xsl:param name="iri" />
        <xsl:call-template name="iriref">
            <xsl:with-param name="iri" select="$iri" />
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="rdf:RDF">
        <div class="graph">
            <xsl:apply-templates select="*" />
        </div>
        <script type="text/javascript">
            if ($('.literal[property="http://callimachusproject.org/rdf/2009/framework#hasResultLimit"]').length) {
                if (window.location.search.indexOf("?query=") === 0) {
                    var qs = window.location.search.substring(1);
                    window.location.replace(window.location.pathname + '#' + qs + '&amp;error=Too+Many+Results');
                }
            }
            $('ul.properties').each(function(){
                $(this).children('li').sort(calli.compareElementsBy('.predicate')).appendTo(this);
            });
        </script>
    </xsl:template>
    <xsl:template match="/rdf:RDF/rdf:Description/*[not(@rdf:nodeID) and not(@rdf:resource) and not(@rdf:datatype) and not(@xml:lang) and not(@rdf:parseType)]">
        <li class="triple">
            <span class="predicate">
                <xsl:call-template name="resource">
                    <xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
                </xsl:call-template>
            </span>
            <xsl:text> </xsl:text>
            <span class="plain literal" property="{concat(namespace-uri(),local-name())}">
                <xsl:apply-templates />
            </span>
        </li>
    </xsl:template>
    <xsl:template match="/rdf:RDF/rdf:Description/*[@rdf:nodeID]">
        <li class="triple">
            <span class="predicate">
                <xsl:call-template name="resource">
                    <xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
                </xsl:call-template>
            </span>
            <xsl:text> </xsl:text>
            <a href="#{@rdf:nodeID}" class="bnode" rel="{concat(namespace-uri(),local-name())}">
                <xsl:text>_:</xsl:text>
                <xsl:value-of select="@rdf:nodeID" />
            </a>
        </li>
    </xsl:template>
    <xsl:template match="/rdf:RDF/rdf:Description/*[@rdf:resource]">
        <li class="triple">
            <span class="predicate">
                <xsl:call-template name="resource">
                    <xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
                </xsl:call-template>
            </span>
            <xsl:text> </xsl:text>
            <a href="{@rdf:resource}" class="uri" rel="{concat(namespace-uri(),local-name())}">
                <xsl:call-template name="resource">
                    <xsl:with-param name="iri" select="@rdf:resource"/>
                </xsl:call-template>
            </a>
            <xsl:apply-templates mode="describe" select="." />
        </li>
    </xsl:template>
    <xsl:template mode="describe" match="node()" />
    <xsl:template match="/rdf:RDF/rdf:Description/*[@rdf:datatype]">
        <li class="triple">
            <span class="predicate">
                <xsl:call-template name="resource">
                    <xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
                </xsl:call-template>
            </span>
            <xsl:text> </xsl:text>
            <span class="typed literal" property="{concat(namespace-uri(),local-name())}" datatype="{@rdf:datatype}">
                <xsl:apply-templates />
                <span class="datatype">
                    <span>^^</span>
                    <xsl:call-template name="resource">
                        <xsl:with-param name="iri" select="@rdf:datatype" />
                    </xsl:call-template>
                </span>
            </span>
        </li>
    </xsl:template>
    <xsl:template match="/rdf:RDF/rdf:Description/*[@xml:lang]">
        <li class="triple">
            <span class="predicate">
                <xsl:call-template name="resource">
                    <xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
                </xsl:call-template>
            </span>
            <xsl:text> </xsl:text>
            <span class="plain literal" property="{concat(namespace-uri(),local-name())}">
                <xsl:attribute name="xml:lang">
                    <xsl:value-of select="@xml:lang" />
                </xsl:attribute>
                <xsl:apply-templates />
                <span class="language">
                    <span>@</span>
                    <xsl:value-of select="@xml:lang" />
                </span>
            </span>
        </li>
    </xsl:template>
    <xsl:template match="/rdf:RDF/rdf:Description/*[@rdf:parseType='Literal']">
        <li class="triple">
            <span class="predicate">
                <xsl:call-template name="resource">
                    <xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
                </xsl:call-template>
            </span>
            <xsl:text> </xsl:text>
            <span class="typed literal" property="{concat(namespace-uri(),local-name())}">
                <xsl:copy-of select="node()" />
            </span>
        </li>
    </xsl:template>
    <xsl:template match="/rdf:RDF/rdf:Description/*[@rdf:parseType='Resource']">
        <li class="triple">
            <span class="predicate">
                <xsl:call-template name="resource">
                    <xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
                </xsl:call-template>
            </span>
            <ul class="properties" rel="{concat(namespace-uri(),local-name())}">
                <xsl:apply-templates />
            </ul>
        </li>
    </xsl:template>
    <xsl:template match="/rdf:RDF/rdf:Description/*[@rdf:parseType='Collection']">
        <li class="triple">
            <span class="predicate">
                <xsl:call-template name="resource">
                    <xsl:with-param name="iri" select="concat(namespace-uri(),local-name())" />
                </xsl:call-template>
            </span>
            <ol class="collection">
                <xsl:apply-templates />
            </ol>
        </li>
    </xsl:template>
    <xsl:template match="/rdf:RDF/rdf:Description[@rdf:about]">
        <div resource="{@rdf:about}">
            <a href="{@rdf:about}" class="uri">
                <xsl:if test="substring-before(@rdf:about, '#')=$systemId">
                    <xsl:attribute name="name"><xsl:value-of select="substring-after(@rdf:about, '#')" /></xsl:attribute>
                </xsl:if>
                <xsl:call-template name="resource">
                    <xsl:with-param name="iri" select="@rdf:about"/>
                </xsl:call-template>
            </a>
            <xsl:apply-templates mode="describe" select="." />
            <xsl:apply-templates mode="properties" select="." />
        </div>
    </xsl:template>
    <xsl:template match="/rdf:RDF/rdf:Description[@rdf:ID]">
        <div resource="#{@rdf:ID}">
            <a href="#{@rdf:ID}" class="uri" name="{@rdf:ID}">
                <xsl:value-of select="@rdf:ID"/>
            </a>
            <xsl:apply-templates mode="properties" select="." />
        </div>
    </xsl:template>
    <xsl:template match="/rdf:RDF/rdf:Description[@rdf:nodeID]">
        <div resource="_:{@rdf:nodeID}">
            <a name="{@rdf:nodeID}" id="{@rdf:nodeID}" class="bnode">
                <xsl:text>_:</xsl:text>
                <xsl:value-of select="@rdf:nodeID" />
            </a>
            <xsl:apply-templates mode="properties" select="." />
        </div>
    </xsl:template>
    <xsl:template mode="properties" match="rdf:Description">
        <xsl:if test="rdf:type[@rdf:resource]">
            <xsl:text> </xsl:text>
            <span class="predicate">a</span>
            <xsl:for-each select="rdf:type[@rdf:resource]">
                <xsl:sort select="@rdf:resource" />
                <xsl:text> </xsl:text>
                <a href="{@rdf:resource}" class="uri" rel="{concat(namespace-uri(),local-name())}">
                    <xsl:call-template name="resource">
                        <xsl:with-param name="iri" select="@rdf:resource"/>
                    </xsl:call-template>
                </a>
                <xsl:apply-templates mode="describe" select="." />
            </xsl:for-each>
        </xsl:if>
        <ul class="properties">
            <xsl:apply-templates select="*[not(self::rdf:type[@rdf:resource])]" />
        </ul>
    </xsl:template>
</xsl:stylesheet>
