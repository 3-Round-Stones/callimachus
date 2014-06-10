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
 <p:pipeline version="1.0"
        xmlns:p     ="http://www.w3.org/ns/xproc"
        xmlns:c     ="http://www.w3.org/ns/xproc-step"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:l     ="http://xproc.org/library"
        xmlns:xhtml ="http://www.w3.org/1999/xhtml">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="target" select="''" />
    <p:option name="html" required="true" />

    <p:import href="page-layout-html.xpl" />

    <p:string-replace name="template" match="xhtml:title/text()">
        <p:with-option name="replace" select="concat('&quot;', replace($target,'^.*/([^/]+)\.md(\?.*)?', '$1'), '&quot;')" />
        <p:input port="source">
            <p:inline>
                <html xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                        <title>{title}</title>
                    </head>
                    <body>
                        <div class="container">
                            <c:body />
                        </div>
                    </body>
                </html>
            </p:inline>
        </p:input>
    </p:string-replace>

    <p:add-attribute attribute-name="href" match="/c:request">
        <p:with-option name="attribute-value" select="$html" />
        <p:input port="source">
            <p:inline>
                <c:request method="GET" detailed="true" />
            </p:inline>
        </p:input>
    </p:add-attribute>

    <p:http-request name="http-get" />

    <p:choose name="http-choose">
        <p:when test="/c:response[@status='200' or @status='203']">
            <p:identity>
                <p:input port="source" select="/c:response/c:body">
                    <p:pipe step="http-get" port="result" />
                </p:input>
            </p:identity>
        </p:when>
        <p:otherwise>
            <p:error name="could-not-get" code="http-get">
                <p:input port="source" select="/c:response/c:body">
                    <p:pipe step="http-get" port="result" />
                </p:input>
            </p:error>
        </p:otherwise>
    </p:choose>

    <p:identity name="http-response" />

    <p:unescape-markup name="html" content-type="text/html" />

    <p:replace match="c:body">
        <p:input port="source">
            <p:pipe step="template" port="result" />
        </p:input>
        <p:input port="replacement" select="/c:body/*">
            <p:pipe step="html" port="result" />
        </p:input>
    </p:replace>

    <calli:page-layout-html query="view">
        <p:with-option name="target" select="$target" />
    </calli:page-layout-html>
</p:pipeline>
