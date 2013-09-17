/**
 * 
 */
package com.baxter.config.processor.impl;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.FeatureKeys;
import net.sf.saxon.OutputURIResolver;
import net.sf.saxon.event.StandardOutputResolver;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.baxter.config.om.ConfigID;
import com.baxter.config.om.ConfigParameter;
import com.baxter.config.processor.AbstractProcessor;
import com.baxter.config.processor.ProcessorContext;
import com.baxter.config.processor.ProcessorException;
import com.baxter.config.processor.ProcessorFactory;
import com.baxter.config.processor.desc.Descriptor;

/**
 * Abstract implementation of XSLT Processor.
 * 
 * @author ykryshchuk
 * @since ${developmentVersion}
 */
public abstract class AbstractXSLTProcessor extends AbstractProcessor
{

  protected static final String XSLT_PARAM_PRODUCT_ID = "configurationProductId";

  protected static final String XSLT_PARAM_VERSION = "configurationVersion";

  protected static final String XSLT_PARAM_COMPONENT_ID = "configurationComponentId";

  protected static final String XML_NS_CONF = "http://baxter-it.com/config";

  private static final DocumentBuilderFactory DBF = DocumentBuilderFactory
      .newInstance();

  private final TransformerFactory transformerFactory;

  /**
   * Path to a stylesheet.
   */
  private String stylesheet;

  /**
   * Cached Templates instance.
   */
  private Templates templates;

  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  private final ReadLock rLock = rwLock.readLock();

  private final WriteLock wLock = rwLock.writeLock();

  /**
   * Initializes processor.
   * 
   * @param descriptor
   *          configuration processor descriptor
   */
  protected AbstractXSLTProcessor(final Descriptor descriptor,
      final ProcessorFactory processorFactory)
  {
    super(descriptor, processorFactory);
    transformerFactory = TransformerFactory.newInstance(
        "net.sf.saxon.TransformerFactoryImpl", Thread.currentThread()
            .getContextClassLoader());
    transformerFactory.setURIResolver(new BaxterURIResolver());
    transformerFactory.setAttribute(FeatureKeys.OUTPUT_URI_RESOLVER,
        new BaxterOutputURIResolver());
  }

  public String getStylesheet()
  {
    return stylesheet;
  }

  public void setStylesheet(final String stylesheet)
  {
    this.stylesheet = stylesheet;
  }

  /**
   * Returns the default transformer for this XSLT processor.
   * 
   * @return transformer
   * @throws ProcessorException
   *           if cannot create transformer
   */
  protected Transformer getTransformer(final ConfigID configurationIdentifier)
      throws ProcessorException
  {
    try
    {
      final Transformer transformer = getTemplates().newTransformer();
      setupTransformer(transformer, configurationIdentifier);
      return transformer;
    }
    catch (final TransformerConfigurationException e)
    {
      logger.error("Could not create transformer", e);
      throw new ProcessorException(e);
    }
  }

  /**
   * Returns cached templates object that can be used to create transformers.
   * 
   * @return the templates object
   */
  protected Templates getTemplates() throws ProcessorException
  {
    rLock.lock();
    try
    {
      if (templates == null)
      {
        rLock.unlock();
        wLock.lock();
        try
        {
          if (templates == null)
          {
            try
            {
              templates = transformerFactory.newTemplates(getXslSource());
            }
            catch (final TransformerConfigurationException e)
            {
              logger.error("Failed to build Templates", e);
              throw new ProcessorException(e);
            }
            catch (final IOException e)
            {
              logger.error("Failed to read XSL source", e);
              throw new ProcessorException(e);
            }
          }
        }
        finally
        {
          rLock.lock();
          wLock.unlock();
        }
      }
    }
    finally
    {
      rLock.unlock();
    }
    return templates;
  }

  /**
   * Returns the root XSL source for this transformer.
   * 
   * @return XSL source
   */
  protected Source getXslSource() throws IOException
  {
    if (getStylesheet() == null)
    {
      throw new IllegalStateException("Stylesheet not configured");
    }
    if (BaxterProtocol.XSL.supports(getStylesheet()))
    {
      try
      {
        return BaxterProtocol.XSL.getSource(getStylesheet(),
            AbstractXSLTProcessor.this);
      }
      catch (final TransformerException e)
      {
        throw new IOException("Failed to resolve to baxterxsl", e);
      }
    }
    else
    {
      final URL stylesheetUrl = new URL(getDescriptor().getXslUrl(),
          getStylesheet());
      return new StreamSource(stylesheetUrl.openStream(),
          stylesheetUrl.toString());
    }
  }

  protected Source getXmlSource(final ProcessorContext context)
      throws ParserConfigurationException, ProcessorException
  {
    final DocumentBuilder docBuilder = DBF.newDocumentBuilder();
    final Document doc = docBuilder.newDocument();
    final Element configSrc = doc.createElementNS(XML_NS_CONF,
        "configuration-source");
    final ConfigID configId = context.getConfigID();
    if (configId != null)
    {
      final Element request = doc.createElementNS(XML_NS_CONF, "request");
      request.setAttribute("productId", configId.getProductId());
      request.setAttribute("componentId", configId.getComponentId());
      request.setAttribute("type", configId.getType());
      request
          .setAttribute("base", context.getConfigurationBaseUrl().toString());
      // add config variants
      for (final String cVariant : configId.getVariants())
      {
        final Element variant = doc.createElementNS(XML_NS_CONF, "variant");
        variant.setAttribute("id", cVariant);
        request.appendChild(variant);
      }
      // add config parameters
      for (final ConfigParameter cParam : context.getParameters())
      {
        final Element param = doc.createElementNS(XML_NS_CONF, "parameter");
        param.setAttribute("id", cParam.getName());
        param.setTextContent(cParam.getValue());
        request.appendChild(param);
      }
      configSrc.appendChild(request);
    }
    doc.appendChild(configSrc);
    return new DOMSource(doc);
  }

  /**
   * Performs initialization of transformer.
   * 
   * @param transformer
   *          transformer to setup
   * @param configurationId
   *          actual requestetd configuration, may be null
   */
  protected void setupTransformer(final Transformer transformer,
      final ConfigID configurationId)
  {
    transformer.setParameter(XSLT_PARAM_PRODUCT_ID, getDescriptor()
        .getProductId());
    transformer.setParameter(XSLT_PARAM_VERSION, getDescriptor().getVersion());
    if (configurationId != null)
    {
      transformer.setParameter(XSLT_PARAM_COMPONENT_ID,
          configurationId.getComponentId());
    }
    transformer.setErrorListener(JustLogErrorListener.getInstance());
    transformer.setURIResolver(transformerFactory.getURIResolver());
  }

  private class BaxterOutputURIResolver implements OutputURIResolver
  {

    private final OutputURIResolver standardResolver = StandardOutputResolver
        .getInstance();

    @Override
    public void close(final Result result) throws TransformerException
    {
      standardResolver.close(result);
    }

    @Override
    public Result resolve(final String href, final String base)
        throws TransformerException
    {
      if (BaxterProtocol.REPO.supports(href))
      {
        return BaxterProtocol.REPO.getResult(href, AbstractXSLTProcessor.this);
      }
      else
      {
        return standardResolver.resolve(href, base);
      }
    }

  }

  private class BaxterURIResolver implements URIResolver
  {

    @Override
    public Source resolve(final String href, final String base)
        throws TransformerException
    {
      try
      {
        final BaxterProtocol protocol = BaxterProtocol.protocolFor(href);
        return protocol.getSource(href, AbstractXSLTProcessor.this);
      }
      catch (final IllegalArgumentException e)
      {
        return null;
      }
    }

  }

}
