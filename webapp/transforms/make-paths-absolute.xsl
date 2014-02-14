<?xml version="1.0" encoding="UTF-8"?>
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
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml">

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@src|@href|@about|@resource|@action|@longdesc|@usemap|@background|@codebase|@cite">
        <xsl:attribute name="{name()}">
            <xsl:call-template name="resolve-path">
                <xsl:with-param name="relative" select="." />
                <xsl:with-param name="base" select="base-uri(.)" />
            </xsl:call-template>
        </xsl:attribute>
    </xsl:template>

    <xsl:template name="resolve-path">
        <xsl:param name="relative" />
        <xsl:param name="base" />
        <xsl:variable name="scheme" select="substring-before($base, '://')" />
        <xsl:variable name="authority" select="substring-before(substring-after($base, '://'), '/')" />
        <xsl:variable name="path" select="substring-after(substring-after($base, '://'), $authority)" />
        <xsl:choose>
            <xsl:when test="not($scheme) or not($authority)">
                <xsl:value-of select="$relative" />
            </xsl:when>
            <xsl:when test="starts-with($relative, '{') or contains($relative, ' ') or contains($relative, '&lt;') or contains($relative, '&gt;') or contains($relative, '&quot;') or contains($relative, &quot;'&quot;)">
                <xsl:value-of select="$relative" />
            </xsl:when>
            <xsl:when test="matches($relative, '^[\w+-.]+:') or starts-with($relative,'//')">
                <xsl:value-of select="$relative" />
            </xsl:when>
            <xsl:when test="$relative='' or starts-with($relative,'?') or starts-with($relative,'#')">
                <xsl:value-of select="$relative" />
            </xsl:when>
            <xsl:when test="starts-with($relative, '/')">
                <xsl:call-template name="normalize-uri-path">
                    <xsl:with-param name="uri">
                        <xsl:value-of select="concat($scheme, '://', $authority)" />
                        <xsl:value-of select="$relative" />
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="normalize-uri-path">
                    <xsl:with-param name="uri">
                        <xsl:call-template name="substring-before-last">
                            <xsl:with-param name="arg" select="$base" />
                            <xsl:with-param name="delim" select="'/'" />
                        </xsl:call-template>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="$relative" />
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="normalize-uri-path">
        <xsl:param name="uri" />
        <xsl:variable name="path" select="concat('/',substring-after(substring-after($uri,'//'),'/'))" />
        <xsl:choose>
            <xsl:when test="(contains($uri,'://') or starts-with($uri,'//')) and contains($path, '?')">
                <xsl:value-of select="substring-before($uri,'//')" />
                <xsl:text>//</xsl:text>
                <xsl:value-of select="substring-before(substring-after($uri,'//'),'/')" />
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path" select="substring-before($path,'?')" />
                </xsl:call-template>
                <xsl:text>?</xsl:text>
                <xsl:value-of select="substring-after($uri,'?')" />
            </xsl:when>
            <xsl:when test="(contains($uri,'://') or starts-with($uri,'//')) and contains($path, '#')">
                <xsl:value-of select="substring-before($uri,'//')" />
                <xsl:text>//</xsl:text>
                <xsl:value-of select="substring-before(substring-after($uri,'//'),'/')" />
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path" select="substring-before($path,'#')" />
                </xsl:call-template>
                <xsl:text>#</xsl:text>
                <xsl:value-of select="substring-after($uri,'#')" />
            </xsl:when>
            <xsl:when test="contains($uri,'://') or starts-with($uri,'//')">
                <xsl:value-of select="substring-before($uri,'//')" />
                <xsl:text>//</xsl:text>
                <xsl:value-of select="substring-before(substring-after($uri,'//'),'/')" />
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path" select="$path" />
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="(starts-with($uri,'/') or starts-with($uri,'.')) and contains($uri, '?')">
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path" select="substring-before($uri,'?')" />
                </xsl:call-template>
                <xsl:text>?</xsl:text>
                <xsl:value-of select="substring-after($uri,'?')" />
            </xsl:when>
            <xsl:when test="(starts-with($uri,'/') or starts-with($uri,'.')) and contains($uri, '#')">
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path" select="substring-before($uri,'#')" />
                </xsl:call-template>
                <xsl:text>#</xsl:text>
                <xsl:value-of select="substring-after($uri,'#')" />
            </xsl:when>
            <xsl:when test="starts-with($uri,'/') or starts-with($uri,'.')">
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path" select="$uri" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$uri" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="normalize-path">
        <xsl:param name="path" />
        <xsl:choose>
            <!-- A. If the input buffer begins with a prefix of "../" or "./",
                then remove that prefix from the input buffer; otherwise, -->
            <xsl:when test="starts-with($path,'../')">
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path" select="substring($path, 4)" />
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="starts-with($path,'./')">
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path" select="substring($path, 3)" />
                </xsl:call-template>
            </xsl:when>
            <!-- B.  if the input buffer begins with a prefix of "/./" or "/.",
                where "." is a complete path segment, then replace that
                prefix with "/" in the input buffer; otherwise, -->
            <xsl:when test="starts-with($path,'/./')">
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path" select="substring($path, 3)" />
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="$path='/.'">
                <xsl:text>/</xsl:text>
            </xsl:when>
            <!-- C.  if the input buffer begins with a prefix of "/../" or "/..",
                where ".." is a complete path segment, then replace that
                prefix with "/" in the input buffer and remove the last
                segment and its preceding "/" (if any) from the output
                buffer; otherwise, -->
            <xsl:when test="contains($path,'/../')">
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path">
                        <xsl:call-template name="substring-before-last">
                            <xsl:with-param name="arg" select="substring-before($path,'/../')" />
                            <xsl:with-param name="delim" select="'/'" />
                        </xsl:call-template>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="substring-after($path,'/../')" />
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="substring($path,string-length($path)-2)='/..'">
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path">
                        <xsl:call-template name="substring-before-last">
                            <xsl:with-param name="arg" select="substring($path,1,string-length($path)-3)" />
                            <xsl:with-param name="delim" select="'/'" />
                        </xsl:call-template>
                        <xsl:text>/</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <!-- D.  if the input buffer consists only of "." or "..", then remove
                 that from the input buffer; otherwise, -->
            <xsl:when test="contains($path,'/./')">
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path">
                        <xsl:value-of select="substring-before($path,'/./')" />
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="substring-after($path,'/./')" />
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="substring($path,string-length($path)-1)='/.'">
                <xsl:call-template name="normalize-path">
                    <xsl:with-param name="path">
                        <xsl:value-of select="substring($path,string-length($path)-1)" />
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <!-- E.  move the first path segment in the input buffer to the end of
                the output buffer, including the initial "/" character (if
                any) and any subsequent characters up to, but not including,
                the next "/" character or the end of the input buffer. -->
            <xsl:otherwise>
                <xsl:value-of select="$path" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="substring-before-last">
        <xsl:param name="arg"/>
        <xsl:param name="delim"/>
        <xsl:if test="contains($arg, $delim)">
            <xsl:value-of select="substring-before($arg, $delim)" />
            <xsl:if test="contains(substring-after($arg, $delim), $delim)">
                <xsl:value-of select="$delim" />
                <xsl:call-template name="substring-before-last">
                    <xsl:with-param name="arg" select="substring-after($arg, $delim)"/>
                    <xsl:with-param name="delim" select="$delim"/>
                </xsl:call-template>
            </xsl:if>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
