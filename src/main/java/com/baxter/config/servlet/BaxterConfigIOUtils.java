package com.baxter.config.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;

import com.baxter.config.model.properties.Import;
import com.baxter.config.model.properties.Properties;

public class BaxterConfigIOUtils
{

  public static void copy(final InputStream inputStream, final OutputStream outputStream, final ConfigurationType type,
	  final Component comp) throws JAXBException, IOException
  {
	switch (type)
	{
	case properties:
	  if (!comp.isServer())
	  {
		copyClientProperies(inputStream, outputStream, comp);
		break;
	  }
	case log4j:
	  copyLog4jXml(inputStream, outputStream);
	  break;
	default:
	  IOUtils.copy(inputStream, outputStream);
	}
  }
  
  private static void copyLog4jXml( final InputStream inputStream, final OutputStream outputStream ) throws IOException {
	final BufferedReader reader = new BufferedReader( new InputStreamReader(inputStream));
	final OutputStreamWriter writer = new OutputStreamWriter(outputStream);
	String line;
	boolean doctypeAdded = false;
	while ( (line=reader.readLine()) != null ) {
	  writer.write(line);
	  writer.write("\n");
	  if ( !doctypeAdded && line.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") != -1 ) {
		writer.write("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">\n");
		doctypeAdded = true;
	  }
	}
	writer.flush();
  }

  private static void copyClientProperies(final InputStream inputStream, final OutputStream outputStream, final Component comp)
	  throws JAXBException, IOException
  {
	final JAXBContext jaxb = JAXBContext.newInstance(Properties.class);
	final Unmarshaller umProps = jaxb.createUnmarshaller();
	final Marshaller mProps = jaxb.createMarshaller();
	mProps.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
	mProps.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	final Properties clientProperties = Properties.class.cast(umProps.unmarshal(inputStream));
	final Import importElem = new Import("./properties-" + comp + "-w.xml", true);
	clientProperties.addImportElem(importElem);
	mProps.marshal(clientProperties, outputStream);
  }
}
