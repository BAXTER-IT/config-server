/*
 * Yurconf Processor Fundamental
 * This software is distributed as is.
 * 
 * We do not care about any damages that could be caused
 * by this software directly or indirectly.
 * 
 * Join our team to help make it better.
 */
package org.yurconf.processor.upgrade;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.io.output.WriterOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.yurconf.om.ConfigID;
import org.yurconf.om.ConfigParameter;
import org.yurconf.processor.ProcessorContext;
import org.yurconf.processor.ProcessorException;
import org.yurconf.processor.desc.Descriptor;
import org.yurconf.processor.desc.FilenameProvider;
import org.yurconf.processor.impl.AbstractXSLTProcessor;
import org.yurconf.processor.impl.XSLTProcessor;

/**
 * @author xpdev
 * @since 1.5
 */
public final class TransformCommand extends AbstractFileCommand implements UpgradeCommand
{
  private final String stylesheet;

  protected TransformCommand(final FilenameProvider filenameProvider, final String stylesheet)
  {
	super(filenameProvider);
	this.stylesheet = stylesheet;
  }

  @Override
  public void upgrade(final UpgradeContext context) throws UpgradeException
  {
	final UpgradeXSLTProcessor processor = new UpgradeXSLTProcessor(context.getDescriptor(), context);
	final StringWriter xsltWriter = new StringWriter();
	try
	{
	  try
	  {
		processor.process(new UpgradeProcessorContext(xsltWriter));
		xsltWriter.flush();
		logger.debug("Upgrade XSLT output: {}", xsltWriter);
	  }
	  finally
	  {
		xsltWriter.close();
	  }
	}
	catch (final ProcessorException e)
	{
	  if (UpgradeException.class.isInstance(e))
	  {
		throw UpgradeException.class.cast(e);
	  }
	  else
	  {
		throw new UpgradeException(e);
	  }
	}
	catch (final IOException e)
	{
	  throw new UpgradeException(e);
	}
  }

  private final class UpgradeProcessorContext implements ProcessorContext
  {

	private final OutputStream outputstream;

	private UpgradeProcessorContext(final Writer writer)
	{
	  this.outputstream = new WriterOutputStream(writer);
	}

	@Override
	public void setContentType(final String contentType, final String encoding)
	{
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
	  return this.outputstream;
	}

	@Override
	public ConfigID getConfigID()
	{
	  // no config request - no config id
	  return null;
	}

	@Override
	public List<ConfigParameter> getParameters()
	{
	  // not appicable for upgrade
	  return null;
	}

	@Override
	public URL getConfigurationBaseUrl()
	{
	  // not appicable for upgrade
	  return null;
	}
  }

  private class UpgradeXSLTProcessor extends XSLTProcessor
  {

	private final UpgradeContext upgradeContext;

	protected UpgradeXSLTProcessor(final Descriptor descriptor, final UpgradeContext upgradeContext)
	{
	  super(descriptor, upgradeContext.getProcessorFactory());
	  this.upgradeContext = upgradeContext;
	  setStylesheet(TransformCommand.this.stylesheet);

	}

	@Override
	protected Source getXmlSource(final ProcessorContext context) throws ParserConfigurationException, ProcessorException
	{
	  final Source originalSource = super.getXmlSource(context);
	  if (DOMSource.class.isInstance(originalSource))
	  {
		final DOMSource domSource = DOMSource.class.cast(originalSource);
		final Document doc = Document.class.cast(domSource.getNode());
		final Element rootElement = doc.getDocumentElement();
		try
		{
		  final Element sourcesElement = doc.createElementNS(AbstractXSLTProcessor.XML_NS_CONF, "sources");
		  // iterate source files and add subelements
		  // Processor's repository root
		  final File repoDir = this.upgradeContext.getProcessorFactory().getRepository()
			  .getProductDirectory(this.upgradeContext.getDescriptor().getProductId());
		  final URI baseURI = repoDir.toURI();
		  sourcesElement.setAttribute("repo", baseURI.toString());
		  final List<String> filenames = listFilenames(baseURI);
		  for (final String filename : filenames)
		  {
			final Element sourceElement = doc.createElementNS(AbstractXSLTProcessor.XML_NS_CONF, "source");
			final File sourceFile = new File(repoDir, filename);
			final URL sourceURL = sourceFile.toURI().toURL();
			sourceElement.setTextContent(sourceURL.toString());
			sourcesElement.appendChild(sourceElement);
		  }

		  rootElement.appendChild(sourcesElement);
		  return domSource;
		}
		catch (final IOException | URISyntaxException e)
		{
		  throw new UpgradeException(e);
		}
	  }
	  else
	  {
		logger.warn("The original source is not a DOM - {}", originalSource);
		return originalSource;
	  }
	}

  }

}
