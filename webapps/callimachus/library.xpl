<?xml version="1.0" encoding="UTF-8" ?>
<p:library version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#">

    <!-- Atomic Extension Steps -->

    <p:declare-step type="calli:render-sparql-query">
        <p:input port="source" sequence="true" primary="true" />
        <p:input port="template" />
        <p:option name="output-base-uri" />
        <p:output port="result" sequence="true" />
    </p:declare-step>

    <p:declare-step type="calli:sparql">
        <p:input port="source" sequence="true" primary="true" />
        <p:input port="query" sequence="true" />
        <p:input port="parameters" kind="parameter" primary="true"/>
        <p:option name="output-base-uri" />
        <p:output port="result" sequence="true" />
    </p:declare-step>

    <p:declare-step type="calli:render">
        <p:input port="source" sequence="true" primary="true" />
        <p:input port="template" />
        <p:option name="output-base-uri" />
        <p:output port="result" sequence="true" />
    </p:declare-step>

    <!-- Subpipelines -->

    <p:import href="page-template.xpl" />

    <p:import href="render-html.xpl" />

</p:library>