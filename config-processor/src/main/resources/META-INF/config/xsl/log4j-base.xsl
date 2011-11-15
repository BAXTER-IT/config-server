<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:log4j="http://jakarta.apache.org/log4j/" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  exclude-result-prefixes="xs" version="2.0">

  <xsl:output encoding="UTF-8" method="xml" doctype-system="log4j.dtd"/>


  <xsl:template match="configuration">
    <log4j:configuration debug="true">
      <xsl:apply-templates select="*"/>
    </log4j:configuration>
  </xsl:template>

  <xsl:template match="console-appender">
    <appender class="org.apache.log4j.ConsoleAppender">
      <xsl:attribute name="name">
        <xsl:value-of select="@name"/>
      </xsl:attribute>
      <param name="Target" value="System.out"/>
      <xsl:apply-templates select="layout"/>
    </appender>
  </xsl:template>

  <xsl:template name="appender-pseudo-name">
    <xsl:param name="name"/>
    <xsl:attribute name="{$name}">
      <xsl:text>PSEUDO_</xsl:text>
      <xsl:value-of select="@name"/>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="async-appender">
    <appender class="org.apache.log4j.AsyncAppender">
      <xsl:attribute name="name">
        <xsl:value-of select="@name"/>
      </xsl:attribute>
      <appender-ref>
        <xsl:call-template name="appender-pseudo-name">
          <xsl:with-param name="name">
            <xsl:text>ref</xsl:text>
          </xsl:with-param>
        </xsl:call-template>
      </appender-ref>
    </appender>
  </xsl:template>

  <xsl:template match="layout">
    <layout class="org.apache.log4j.PatternLayout">
      <param name="ConversionPattern">
        <xsl:attribute name="value">
          <xsl:value-of select="."/>
        </xsl:attribute>
      </param>
    </layout>
  </xsl:template>


  <xsl:template name="build-log-directory-name">
    <xsl:text>--NOT IMPLEMENTED--</xsl:text>
  </xsl:template>

  <xsl:template match="fileName">
    <param name="File">
      <xsl:attribute name="value">
        <xsl:call-template name="build-log-directory-name"/>
        <xsl:value-of select="."/>
      </xsl:attribute>
    </param>
  </xsl:template>

  <xsl:template match="rolling-file-appender">
    <xsl:call-template name="async-appender"/>
    <appender class="org.apache.log4j.RollingFileAppender">
      <xsl:call-template name="appender-pseudo-name">
        <xsl:with-param name="name">
          <xsl:text>name</xsl:text>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:apply-templates select="fileName"/>
      <param name="MaxFileSize">
        <xsl:attribute name="value">
          <xsl:apply-templates select="maxSize"/>
        </xsl:attribute>
      </param>
      <param name="MaxBackupIndex">
        <xsl:attribute name="value">
          <xsl:apply-templates select="backupIndex"/>
        </xsl:attribute>
      </param>
      <xsl:apply-templates select="layout"/>
    </appender>
  </xsl:template>

  <xsl:template match="file-appender">
    <xsl:call-template name="async-appender"/>
    <appender class="org.apache.log4j.FileAppender">
      <xsl:call-template name="appender-pseudo-name">
        <xsl:with-param name="name">
          <xsl:text>name</xsl:text>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:apply-templates select="fileName"/>
      <xsl:apply-templates select="layout"/>
    </appender>
  </xsl:template>

  <xsl:template match="logger[@name='ROOT']">
    <root>
      <xsl:apply-templates select="appender-ref"/>
      <xsl:apply-templates select="@level"/>
    </root>
  </xsl:template>

  <xsl:template match="logger">
    <category>
      <xsl:apply-templates select="@name"/>
      <xsl:apply-templates select="@additivity"/>
      <xsl:apply-templates select="appender-ref"/>
      <xsl:apply-templates select="@level"/>
    </category>
  </xsl:template>

  <xsl:template match="logger/@name">
    <xsl:attribute name="name">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="logger/@additivity">
    <xsl:attribute name="additivity">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="logger/@level">
    <level>
      <xsl:attribute name="value">
        <xsl:value-of select="."/>
      </xsl:attribute>
    </level>
  </xsl:template>

  <xsl:template match="appender-ref">
    <appender-ref>
      <xsl:attribute name="ref">
        <xsl:value-of select="@ref"/>
      </xsl:attribute>
    </appender-ref>
  </xsl:template>

</xsl:stylesheet>
