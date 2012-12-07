<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="target" required="true"  />

    <p:import href="page-layout-html.xpl" />

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/profile.xhtml" />
        </p:input>
    </p:xslt>

    <calli:page-layout-html query="">
        <p:with-option name="target" select="$target" />
    </calli:page-layout-html>
</p:pipeline>
