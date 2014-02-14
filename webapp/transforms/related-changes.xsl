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
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    exclude-result-prefixes="xhtml sparql">
    <xsl:import href="folder-changes.xsl" />
    <xsl:template match="/">
        <html>
            <head>
                <title>Related Changes</title>
                <link rel="help" href="../../callimachus-for-web-developers#System_menu" target="_blank" title="Help" />
            </head>
            <body>
                <div class="container">
                    <hgroup class="page-header">
                        <h1>Related Changes</h1>
                    </hgroup>
                    <xsl:apply-templates />
                </div>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
