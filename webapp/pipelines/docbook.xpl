<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0" type="calli:docbook"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:d="http://docbook.org/ns/docbook"
        xmlns:xl="http://www.w3.org/1999/xlink"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="application/docbook+xml" />

    <p:xinclude fixup-xml-base="true" fixup-xml-lang="true" />

    <p:make-absolute-uris match="@fileref|@xl:href" />

    <p:rename new-name="d:section" match="d:preface/d:article|d:partintro/d:article|d:article/d:article|d:chapter/d:article|d:appendix/d:article|d:section/d:article|d:topic/d:article" />

</p:pipeline>
