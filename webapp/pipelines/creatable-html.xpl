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
 <p:pipeline type="calli:page-layout-html" version="1.0"
        xmlns:p     ="http://www.w3.org/ns/xproc"
        xmlns:c     ="http://www.w3.org/ns/xproc-step"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:l     ="http://xproc.org/library">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="target" select="''" />
    <p:option name="query" select="''" />

    <p:import href="page-layout.xpl" />

    <p:variable name="targetOrSystem" select="if($target)then $target else base-uri()" />
    <p:variable name="find-realm-uri" select="concat('../queries/find-realm.rq?results&amp;target=', encode-for-uri($targetOrSystem))" />
    <p:variable name="realm" select="doc($find-realm-uri)//sparql:binding[@name='realm']/sparql:uri" />

    <calli:page-layout>
        <p:with-option name="realm" select="$realm" />
    </calli:page-layout>

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/flatten.xsl" />
        </p:input>
    </p:xslt>

    <p:xslt>
        <p:with-param name="target" select="$target"/>
        <p:with-param name="query" select="$query"/>
        <p:input port="stylesheet">
            <p:document href="../transforms/page-info.xsl" />
        </p:input>
    </p:xslt>

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/xhtml-to-html.xsl" />
        </p:input>
    </p:xslt>
</p:pipeline>
