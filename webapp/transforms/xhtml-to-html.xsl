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
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml"
        version="1.0" exclude-result-prefixes="xhtml">
    <xsl:output indent="no" method="html" doctype-system="about:legacy-compat" />

    <!-- normal elements -->
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- strip meta charset -->
    <xsl:template match = 'xhtml:meta[translate(@http-equiv,"ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz") = "content-type" ]' />

    <!-- strip xmlns from head -->
    <xsl:template match="xhtml:head|xhtml:head/*">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
    </xsl:template>

    <!-- void elements -->
    <xsl:template match="xhtml:area|xhtml:base|xhtml:br|xhtml:col|xhtml:command|xhtml:embed|xhtml:hr|xhtml:img|xhtml:input|xhtml:keygen|xhtml:link|xhtml:meta|xhtml:param|xhtml:source|xhtml:track|xhtml:wbr">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@*"/>
        </xsl:element>
    </xsl:template>

    <!-- Raw text elements -->
    <xsl:template match="xhtml:script|xhtml:style">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@*" />
            <xsl:apply-templates select="node()" mode="serialize" />
        </xsl:element>
    </xsl:template>

    <!-- End tag optional elements -->
    <xsl:template match="xhtml:li|xhtml:dt|xhtml:dd|xhtml:p|xhtml:rt|xhtml:rp|xhtml:optgroup|xhtml:option|xhtml:colgroup|xhtml:thead|xhtml:tbody|xhtml:tfoot|xhtml:tr|xhtml:td|xhtml:th">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|comment()">
        <xsl:copy />
    </xsl:template>

    <!-- strip RDFa version attribute -->
    <xsl:template match = 'xhtml:html/@version' />

    <!-- strip xml:space attributes -->
    <xsl:template match = '@xml:space' />

    <!-- convert lang attributes -->
    <xsl:template match="@xml:lang">
        <xsl:if test="not(../@lang)">
            <xsl:attribute name="lang">
                <xsl:value-of select="." />
            </xsl:attribute>
        </xsl:if>
    </xsl:template>

    <!-- strip all processing instructions -->
    <xsl:template match = 'processing-instruction()' />

    <!-- serialize void elements -->
    <xsl:template mode="serialize" match="xhtml:area|xhtml:base|xhtml:br|xhtml:col|xhtml:command|xhtml:embed|xhtml:hr|xhtml:img|xhtml:input|xhtml:keygen|xhtml:link|xhtml:meta|xhtml:param|xhtml:source|xhtml:track|xhtml:wbr">
        <xsl:text>&lt;</xsl:text>
        <xsl:value-of select="local-name()"/>
        <xsl:apply-templates select="@*" mode="serialize" />
        <xsl:text> /&gt;</xsl:text>
    </xsl:template>

    <xsl:template match="*" mode="serialize">
        <xsl:text>&lt;</xsl:text>
        <xsl:value-of select="local-name()"/>
        <xsl:apply-templates select="@*" mode="serialize" />
        <xsl:text>&gt;</xsl:text>
        <xsl:apply-templates mode="serialize" />
        <xsl:text>&lt;/</xsl:text>
        <xsl:value-of select="local-name()"/>
        <xsl:text>&gt;</xsl:text>
    </xsl:template>

    <xsl:template match="@*" mode="serialize">
        <xsl:text> </xsl:text>
        <xsl:value-of select="local-name()"/>
        <xsl:text>="</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>"</xsl:text>
    </xsl:template>

    <xsl:template match="text()" mode="serialize">
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="comment()" mode="serialize">
        <xsl:text>&lt;!--</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>--&gt;</xsl:text>
    </xsl:template>

    <!-- strip all processing instructions -->
    <xsl:template match="processing-instruction()" mode="serialize" />

</xsl:transform>
