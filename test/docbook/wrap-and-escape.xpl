<?xml version="1.0" encoding="UTF-8" ?>
 <p:pipeline 
        type="calli:wrap-and-escape" 
        version="1.0" name="wrap-and-escape"
        xmlns:p     ="http://www.w3.org/ns/xproc"
        xmlns:c     ="http://www.w3.org/ns/xproc-step"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:xhtml ="http://www.w3.org/1999/xhtml"
        xmlns:l     ="http://xproc.org/library">
    <!-- Wrap the doc-book source in xhtml:p -->
    <p:viewport match="xhtml:pre" name="wrap-docbook-source">
        <p:viewport-source>
            <p:inline>
                <xhtml:pre/>
            </p:inline>            
        </p:viewport-source>
        <p:insert position="first-child">
            <p:input port="insertion">
                <p:pipe step="wrap-and-escape" port="source"/>
            </p:input>
        </p:insert>                
    </p:viewport>
    
    <!-- Escape (the children) of the wrapped doc book source -->
    <p:escape-markup />
</p:pipeline>