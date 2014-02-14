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
        xmlns:l="http://xproc.org/library">

    <p:serialization port="result" media-type="text/javascript" method="text" encoding="UTF-8" />

    <p:option name="handler" required="true" />
    <p:option name="reqId" required="true" />

    <p:xslt>
        <p:with-param name="handler" select="$handler" />
        <p:with-param name="reqId" select="$reqId" />
        <p:input port="stylesheet">
            <p:document href="../transforms/sparql-results-wire.xsl" />
        </p:input>
    </p:xslt>

</p:pipeline>
