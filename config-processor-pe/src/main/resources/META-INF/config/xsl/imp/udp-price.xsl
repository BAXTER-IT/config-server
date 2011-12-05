<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:udpprice="http://baxter-it.com/config/pe/udpprice"
    xmlns:c="http://baxter-it.com/config/component" exclude-result-prefixes="xs udpprice c"
    xmlns="http://baxter-it.com/config/pe/properties" version="2.0">

    <xsl:import href="baxterxsl:repo-base.xsl"/>
    <xsl:import href="../imp/merge-redundant-groups.xsl"/>

    <xsl:param name="configurationComponentId"/>

    <xsl:template name="udp-price-connections">
        <xsl:variable name="udpPrice">
            <xsl:call-template name="load-merged-repo-document">
                <xsl:with-param name="prefix" select="'udp-price'"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:apply-templates
            select="$udpPrice/udpprice:configuration/udpprice:connection[c:component[@id=$configurationComponentId]]"
        />
    </xsl:template>

    <xsl:template match="udpprice:connection">
        <xsl:param name="path" select="@id"/>
        <xsl:choose>
            <xsl:when test="contains($path,'.')">
                <xsl:call-template name="in-container">
                    <xsl:with-param name="path" select="$path"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <group>
                    <xsl:attribute name="key">
                        <xsl:value-of select="$path"/>
                    </xsl:attribute>
                    <entry key="ServerHost">
                        <xsl:value-of select="@serverHost"/>
                    </entry>
                    <entry key="ServerPort">
                        <xsl:value-of select="@serverPort"/>
                    </entry>
                    <entry key="ClientPort">
                        <xsl:value-of select="@clientPort"/>
                    </entry>
                </group>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
