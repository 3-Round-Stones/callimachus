<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sparql="http://www.w3.org/2005/sparql-results#">
	<xsl:output method="text" encoding="UTF-8"/>
	<xsl:param name="handler" select="'google.visualization.Query.setResponse'" />
	<xsl:param name="reqId" select="'0'" />
	<xsl:template match="sparql:sparql">
		<xsl:value-of select="$handler" />
		<xsl:text>({version:'0.6',reqId:'</xsl:text>
		<xsl:value-of select="$reqId" />
		<xsl:text>',status:'ok',table:{</xsl:text>
		<xsl:apply-templates select="sparql:head" />
		<xsl:text>,</xsl:text>
		<xsl:apply-templates select="sparql:results" />
		<xsl:text>}});</xsl:text>
	</xsl:template>
	<xsl:template match="sparql:head">
		<xsl:text>cols:[</xsl:text>
		<xsl:apply-templates select="sparql:variable" />
		<xsl:text>]</xsl:text>
	</xsl:template>
	<xsl:template match="sparql:variable">
		<xsl:variable name="name" select="@name"/>
		<xsl:variable name="datatype" select="/sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name=$name]/sparql:literal/@datatype" />
		<xsl:variable name="local" select="substring-after($datatype, '#')" />
		<xsl:text>{id:'</xsl:text>
		<xsl:value-of select="$name" />
		<xsl:text>',label:'</xsl:text>
		<xsl:value-of select="translate($name,'_',' ')" />
		<xsl:text>',type:'</xsl:text>
		<xsl:choose>
			<xsl:when test="not(starts-with($datatype, 'http://www.w3.org/2001/XMLSchema#'))">
				<xsl:text>string</xsl:text>
			</xsl:when>
			<xsl:when test="$local='boolean'">
				<xsl:text>boolean</xsl:text>
			</xsl:when>
			<xsl:when test="$local='date'">
				<xsl:text>date</xsl:text>
			</xsl:when>
			<xsl:when test="$local='dateTime'">
				<xsl:text>datetime</xsl:text>
			</xsl:when>
			<xsl:when test="$local='time'">
				<xsl:text>timeofday</xsl:text>
			</xsl:when>
			<xsl:when test="$local='float' or $local='decimal' or $local='double' or $local='integer' or $local='long' or $local='int' or $local='short' or $local='byte' or $local='nonPositiveInteger' or $local='negativeInteger' or $local='nonNegativeInteger' or $local='positiveInteger' or $local='unsignedLong' or $local='unsignedInt' or $local='unsignedShort' or $local='unsignedByte'">
					<xsl:text>number</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>string</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:text>'}</xsl:text>
		<xsl:if test="position() != last()">
			<xsl:text>,</xsl:text>
		</xsl:if>
	</xsl:template>
	<xsl:template match="sparql:results">
		<xsl:text>rows:[</xsl:text>
		<xsl:apply-templates select="sparql:result" />
		<xsl:text>]</xsl:text>
	</xsl:template>
	<xsl:template match="sparql:result">
		<xsl:text>{c:[</xsl:text>
		<xsl:variable name="current" select="."/> 
		<xsl:for-each select="../../sparql:head/sparql:variable">
			<xsl:variable name="name" select="@name"/>
			<xsl:if test="not($current/sparql:binding[@name=$name])">
				<xsl:text>{v:''}</xsl:text>
			</xsl:if>
			<xsl:apply-templates select="$current/sparql:binding[@name=$name]" />
			<xsl:if test="position() != last()">
				<xsl:text>,</xsl:text>
			</xsl:if>
		</xsl:for-each>
		<xsl:text>]}</xsl:text>
		<xsl:if test="position() != last()">
			<xsl:text>,</xsl:text>
		</xsl:if>
	</xsl:template>
	<xsl:template match="sparql:binding">
		<xsl:apply-templates select="*" />
	</xsl:template>
	<xsl:template match="sparql:uri">
		<xsl:text>{v:'</xsl:text>
		<xsl:value-of select="text()" />
		<xsl:text>'}</xsl:text>
	</xsl:template>
	<xsl:template match="sparql:bnode">
		<xsl:text>{v:'</xsl:text>
		<xsl:value-of select="text()" />
		<xsl:text>'}</xsl:text>
	</xsl:template>
	<xsl:template match="sparql:literal">
		<xsl:variable name="ns" select="substring-before(@datatype, '#')" />
		<xsl:variable name="local" select="substring-after(@datatype, '#')" />
		<xsl:choose>
			<xsl:when test="not($ns='http://www.w3.org/2001/XMLSchema')">
				<xsl:text>{v:'</xsl:text>
				<xsl:call-template name="replace-string">
					<xsl:with-param name="text" select="text()"/>
					<xsl:with-param name="replace" select="&quot;'&quot;"/>
					<xsl:with-param name="with" select="&quot;\'&quot;"/>
				</xsl:call-template>
				<xsl:text>'}</xsl:text>
			</xsl:when>
			<xsl:when test="$local='float' or $local='decimal' or $local='double' or $local='integer' or $local='long' or $local='int' or $local='short' or $local='byte' or $local='nonPositiveInteger' or $local='negativeInteger' or $local='nonNegativeInteger' or $local='positiveInteger' or $local='unsignedLong' or $local='unsignedInt' or $local='unsignedShort' or $local='unsignedByte'">
				<xsl:text>{v:</xsl:text>
				<xsl:value-of select="text()" />
				<xsl:text>,f:'</xsl:text>
				<xsl:value-of select="text()" />
				<xsl:text>'}</xsl:text>
			</xsl:when>
			<xsl:when test="$local='date'">
				<xsl:variable name="date" select="text()" />
				<xsl:text>{v:new Date(</xsl:text>
				<xsl:value-of select="substring-before($date, '-')" />
				<xsl:text>,</xsl:text>
				<xsl:value-of select="number(substring(substring-after($date, '-'), 1, 2)) - 1" />
				<xsl:text>,</xsl:text>
				<xsl:value-of select="substring(substring-after($date, '-'), 4, 2)" />
				<xsl:text>)}</xsl:text>
			</xsl:when>
			<xsl:when test="$local='dateTime'">
				<xsl:variable name="date" select="substring-before(text(), 'T')" />
				<xsl:variable name="time" select="substring-after(text(), 'T')" />
				<xsl:text>{v:new Date(</xsl:text>
				<xsl:value-of select="substring-before($date, '-')" />
				<xsl:text>,</xsl:text>
				<xsl:value-of select="number(substring(substring-after($date, '-'), 1, 2)) - 1" />
				<xsl:text>,</xsl:text>
				<xsl:value-of select="substring(substring-after($date, '-'), 4, 2)" />
				<xsl:text>,</xsl:text>
				<xsl:value-of select="substring($time,1,2)" />
				<xsl:text>,</xsl:text>
				<xsl:value-of select="substring($time,4,2)" />
				<xsl:text>,</xsl:text>
				<xsl:value-of select="substring($time,7,2)" />
				<xsl:if test="contains($time, '.')">
					<xsl:text>,</xsl:text>
					<xsl:value-of select="substring(substring-after($time, '.'), 1, 3)" />
				</xsl:if>
				<xsl:text>)}</xsl:text>
			</xsl:when>
			<xsl:when test="$local='time'">
				<xsl:variable name="time" select="text()" />
				<xsl:text>{v:[</xsl:text>
				<xsl:value-of select="substring($time,1,2)" />
				<xsl:text>,</xsl:text>
				<xsl:value-of select="substring($time,4,2)" />
				<xsl:text>,</xsl:text>
				<xsl:value-of select="substring($time,7,2)" />
				<xsl:if test="contains($time, '.')">
					<xsl:text>,</xsl:text>
					<xsl:value-of select="substring(substring-after($time, '.'), 1, 3)" />
				</xsl:if>
				<xsl:text>]}</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>{v:'</xsl:text>
				<xsl:call-template name="replace-string">
					<xsl:with-param name="text" select="text()"/>
					<xsl:with-param name="replace" select="&quot;'&quot;"/>
					<xsl:with-param name="with" select="&quot;\'&quot;"/>
				</xsl:call-template>
				<xsl:text>'}</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

  <xsl:template name="replace-string">
    <xsl:param name="text"/>
    <xsl:param name="replace"/>
    <xsl:param name="with"/>
    <xsl:choose>
      <xsl:when test="contains($text,$replace)">
        <xsl:value-of select="substring-before($text,$replace)"/>
        <xsl:value-of select="$with"/>
        <xsl:call-template name="replace-string">
          <xsl:with-param name="text"
select="substring-after($text,$replace)"/>
          <xsl:with-param name="replace" select="$replace"/>
          <xsl:with-param name="with" select="$with"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
