<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        name="test-docbook"
        exclude-inline-prefixes="calli xhtml c xsl l"
        xmlns:doc   = "http://docbook.org/ns/docbook"
        xmlns:f     = "http://www.w3.org/2005/xpath-functions"
        xmlns:calli = "http://callimachusproject.org/rdf/2009/framework#"
        xmlns:xhtml = "http://www.w3.org/1999/xhtml"
        xmlns:xsl   = "http://www.w3.org/1999/XSL/Transform"
        xmlns:p     = "http://www.w3.org/ns/xproc"
        xmlns:c     = "http://www.w3.org/ns/xproc-step"
        xmlns:l     = "http://xproc.org/library">
        
    <p:option name    = "target"                   />
    <p:import href    = "wrap-and-escape.xpl"      />
    <p:import href    = "wrap-two-documents.xpl"   />    
    <p:viewport match = "xhtml:body/xhtml:details" name="transform-detail" >
        <p:group name = "docbook-test">
            <p:variable name = "file" 
                        select="//xhtml:a[@rel='source']/@href"/>
            <p:variable name="stripped-file" 
                        select="concat(//xhtml:a[@rel='source']/@href,'.stripped')"/>
                        
            <!--
              Expected docbook
              -->
            <p:load name="test-file">
                <p:with-option name="href" select="p:resolve-uri($file, base-uri(.))"/>
            </p:load>
            <p:validate-with-relax-ng>
                <p:input port="schema">
                    <p:document href="../../webapps/callimachus/schemas/docbookxi.rng" />
                </p:input>                
            </p:validate-with-relax-ng>            
            <p:xslt name="stripped-original">
                <p:input port="stylesheet">
                    <p:document href="strip-whitespace.xsl" />
                </p:input>
            </p:xslt>
                        
            <!-- Escape / serialize the original docbook -->
            <calli:wrap-and-escape name   = "escape-original">
                <p:input port="source">
                    <p:pipe step="test-file" port="result"/>
                </p:input>                                
            </calli:wrap-and-escape>

            <!--
              Round trip docbook, capturing error (and results) if there is one 
              -->
            <p:try>
                <p:group>
                    <p:xslt>
                        <p:input port="source">
                            <p:pipe step="test-file" port="result"/>
                        </p:input>                
                        <p:input port="stylesheet">
                            <p:document href="../../webapps/callimachus/editor/docbook2xhtml.xsl" />
                        </p:input>
                    </p:xslt>                        
                    <p:xslt name="roundtrip">
                        <p:input port="stylesheet">
                            <p:document href="../../webapps/callimachus/editor/xhtml2docbook.xsl" />
                        </p:input>
                    </p:xslt>    
                    <p:xslt name="stripped-roundtrip">
                        <p:input port="stylesheet">
                            <p:document href="strip-whitespace.xsl" />
                        </p:input>
                    </p:xslt>

                    <!-- Escape / serialize the round-tripped docbook -->
                    <calli:wrap-and-escape name="escape-roundtrip">
                        <p:input port="source">
                            <p:pipe step="roundtrip" port="result" />
                        </p:input>
                    </calli:wrap-and-escape>
                    
                    <calli:wrap-two-documents name   = "wrapped-and-escaped-docs">
                        <p:input port="source">
                            <p:pipe step="stripped-original"  port="result"/>
                        </p:input>                                
                        <p:input port="other">
                            <p:pipe step="stripped-roundtrip" port="result"/>
                        </p:input>                                                        
                    </calli:wrap-two-documents>
                                    
                    <!-- Fill in expected/actual and other  modifications to xhtml:detail -->
                    <p:xslt>
                        <p:input port="source">
                            <p:pipe step="transform-detail" port="current" />
                        </p:input>
                        <p:input port="stylesheet">
                            <p:document href="annotate-test-suite.xsl" />
                        </p:input>
                        <p:with-param name="escaped-original"         select="string(.)" >
                            <p:pipe     step="escape-original" port="result"     />
                        </p:with-param>
                        <p:with-param name="escaped-roundtrip"  select="string(.)" >
                            <p:pipe     step="escape-roundtrip" port="result"     />
                        </p:with-param>
                        <p:with-param name="success"            select="f:deep-equal(/root/doc:article[1],/root/doc:article[2])">
                            <p:pipe step="wrapped-and-escaped-docs" port="result" />
                        </p:with-param>
                        <p:with-param name="error"              select="false()" />
                    </p:xslt>
                </p:group>
                <p:catch name="error-state">
                    <!-- Fill in expected/actual/error modifications to xhtml:detail -->
                    
                    <!-- Extract error summarization -->
                    <p:xslt name="error-summary">
                        <p:input port="source">
                            <p:pipe step="error-state" port="error" />
                        </p:input>                        
                        <p:input port="stylesheet">
                            <p:document href="errors.xsl"     />
                        </p:input>
                    </p:xslt>
                    
                    <!-- Annotate the existing details with test run information -->
                    <p:xslt name="annotated-detail">
                        <p:input port="source">
                            <p:pipe step="transform-detail" port="current" />
                        </p:input>
                        <p:input port="stylesheet">
                            <p:document href="annotate-test-suite.xsl"     />
                        </p:input>
                        <p:with-param name="escaped-original"         select="string(.)"  >
                            <p:pipe     step="escape-original" port="result"     />
                        </p:with-param>
                        <p:with-param name="escaped-roundtrip"  select="''"      />
                        <p:with-param name="success"            select="false()" />
                        <p:with-param name="error"              select="true()"  />
                    </p:xslt>
                    
                    <!-- Insert error summary -->
                    <p:viewport match="xhtml:details">
                        <p:viewport-source>
                            <p:pipe step="annotated-detail" port="result" />
                        </p:viewport-source>
                        <p:insert position="first-child">
                            <p:input    port="insertion">
                                <p:pipe step="error-summary" port="result"/>
                            </p:input>
                        </p:insert>   
                    </p:viewport>
                </p:catch>
            </p:try>
        </p:group>
    </p:viewport>
</p:pipeline>
