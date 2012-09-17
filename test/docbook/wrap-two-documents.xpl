<?xml version="1.0" encoding="UTF-8" ?>
 <p:pipeline 
        type="calli:wrap-two-documents" 
        version="1.0" name="wrap-two-documents"
        xmlns:p     ="http://www.w3.org/ns/xproc"
        xmlns:c     ="http://www.w3.org/ns/xproc-step"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:xhtml ="http://www.w3.org/1999/xhtml"
        xmlns:l     ="http://xproc.org/library">
        
    <p:input port="other" />
    
    <!-- 
      -->
    <p:viewport match="root">
        <p:viewport-source>
            <p:inline>
                <root />
            </p:inline>            
        </p:viewport-source>
        <p:insert position="first-child">
            <p:input port="insertion">
                <p:pipe step="wrap-two-documents" port="source"/>
            </p:input>
        </p:insert>   
        <p:insert position="first-child">
            <p:input port="insertion">
                <p:pipe step="wrap-two-documents" port="other"/>
            </p:input>
        </p:insert>                                     
    </p:viewport>
</p:pipeline>