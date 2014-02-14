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
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="template" required="true" />
    <p:option name="target" required="true" />
    <p:option name="query" required="true" />
    <p:option name="element" select="'/1'" />

    <p:import href="library.xpl" />

    <p:load>
        <p:with-option name="href" select="$template" />
    </p:load>

    <p:xslt name="template-element">
        <p:with-param name="element" select="$element" />
        <p:input port="stylesheet">
            <p:document href="../transforms/element.xsl" />
        </p:input>
    </p:xslt>

    <calli:render>
        <p:input port="template">
            <p:pipe step="template-element" port="result" />
        </p:input>
        <p:input port="source">
            <p:pipe step="pipeline" port="source" />
        </p:input>
    </calli:render>

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
