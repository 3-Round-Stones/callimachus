<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0" type="calli:docbook-inclusion"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:d="http://docbook.org/ns/docbook"
        xmlns:xl="http://www.w3.org/1999/xlink"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="application/docbook+xml" />

    <p:xinclude fixup-xml-base="true" fixup-xml-lang="true" />

    <p:make-absolute-uris match="@fileref|@xl:href" />

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/docbook-inclusion.xsl" />
        </p:input>
    </p:xslt>

</p:pipeline>
