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
    xmlns:p="http://www.w3.org/ns/xproc"
    xmlns:c="http://www.w3.org/ns/xproc-step"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
    xmlns:l="http://xproc.org/library">
    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="target" required="true"  />
    <p:option name="q" required="true"  />
    <p:import href="page-layout-html.xpl" />

    <p:load>
        <p:with-option 
            name="href" 
            select="concat(
                '../queries/realm-search.rq?results&amp;target=', 
                encode-for-uri($target),
                '&amp;q=',
                encode-for-uri($q)
            )"/>
    </p:load>
    <p:xslt>
        <p:with-param name="systemId" select="$target" />
        <p:input port="stylesheet">
            <p:document href="../transforms/realm-search.xsl" />
        </p:input>
    </p:xslt>
    <calli:page-layout-html query="q">
        <p:with-option name="target" select="$target" />
    </calli:page-layout-html>
</p:pipeline>
