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
<p:pipeline version="1.0" name="pipeline"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<p:serialization port="result" media-type="text/plain" method="text" />

<p:option name="schema" required="true" />

<p:load name="given-schema">
    <p:with-option name="href" select="$schema"/>
</p:load>

<p:try>
    <p:group>
        <p:validate-with-relax-ng>
            <p:input port="source">
                <p:pipe step="pipeline" port="source" />
            </p:input>
            <p:input port="schema">
                <p:pipe step="given-schema" port="result" />
            </p:input>
        </p:validate-with-relax-ng>

        <p:sink />

        <p:identity>
            <p:input port="source">
                <p:inline>
                    <c:data content-type="text/plain" />
                </p:inline>
            </p:input>
        </p:identity>
    </p:group>
    <p:catch name="error-state">
        <p:xslt>
            <p:input port="source">
                <p:pipe step="error-state" port="error" />
            </p:input>
            <p:input port="stylesheet">
                <p:inline>
                    <xsl:stylesheet version="2.0">
                        <xsl:template match="c:errors">
                            <c:data content-type="text/plain">
                                <xsl:text>Document is invalid</xsl:text>
                                <xsl:apply-templates />
                            </c:data>
                        </xsl:template>
                    	<xsl:template match="c:error">
                            <xsl:text>&#xA;</xsl:text>
                            <xsl:choose>
                                <xsl:when test="contains(.,'Exception; ')">
                                    <xsl:value-of select="substring-after(.,'Exception; ')" />
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                    	</xsl:template>
                    </xsl:stylesheet>
                </p:inline>
            </p:input>
        </p:xslt>
    </p:catch>
</p:try>

</p:pipeline>
