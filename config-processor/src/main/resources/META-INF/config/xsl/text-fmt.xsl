<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs" version="2.0">

    <xsl:template name="CR">
        <xsl:text disable-output-escaping="yes">&#13;</xsl:text>
    </xsl:template>

    <xsl:template name="LF">
        <xsl:text disable-output-escaping="yes">&#10;</xsl:text>
    </xsl:template>
    
    <xsl:template name="CRLF">
        <xsl:call-template name="CR"/>
        <xsl:call-template name="LF"/>
    </xsl:template>

</xsl:stylesheet>
