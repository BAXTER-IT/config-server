/*
 * Yurconf Processor Fundamental
 * This software is distributed as is.
 * 
 * We do not care about any damages that could be caused
 * by this software directly or indirectly.
 * 
 * Join our team to help make it better.
 */
package org.yurconf.processor.repo.file;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yurconf.processor.AbstractProcessor;
import org.yurconf.processor.ConfigurationRepository;
import org.yurconf.processor.ProcessorException;
import org.yurconf.processor.ProcessorFactory;
import org.yurconf.processor.desc.Descriptor;
import org.yurconf.processor.desc.Parameter;
import org.yurconf.processor.desc.Processor;
import org.yurconf.processor.desc.Upgrade;
import org.yurconf.processor.impl.XSLTProcessor;
import org.yurconf.processor.repo.RepositoryException;

import org.yurconf.om.ConfigID;
import org.yurconf.om.Version;

/**
 * Processor Factory that returns a target processor for specified input.
 *
 * @TODO extract the package introspection into the new class
 *
 * @author ykryshchuk
 * @since 1.5
 */
public final class ProcessorFactoryImpl implements ProcessorFactory
{

  /**
   * Logger instance.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorFactoryImpl.class);

  /**
   * BeanUtils instance.
   */
  private static final BeanUtilsBean BUB = BeanUtilsBean.getInstance();

  /**
   * Processors cache.
   */
  private final ProcessorsCache processorsCache = new ProcessorsCache();

  /**
   * Configuration repository.
   */
  private final ConfigurationRepository repository;

  /**
   * Hidden constructor.
   * ProcessorFactory
   * @param repositoryRoot
   *          configuration repository root
   * @throws ProcessorException
   *           if cannot create factory
   */
  private ProcessorFactoryImpl(final File repositoryRoot, final ClassLoader processorsCL) throws ProcessorException
  {
	LOGGER.trace("Creating factory with repository at {}", repositoryRoot.getAbsolutePath());
	this.repository = new RepositoryImpl(repositoryRoot, processorsCL);
	loadProcessors(processorsCL);
  }

  /**
   * Returns the factory instance for specified repository.
   *
   * @param repository
   *          root of configuration repository
   * @return processor factory instance
   * @throws ProcessorException
   *           if cannot get factory
   */
  public static ProcessorFactory getInstance(final File repository, final ClassLoader processorsCL) throws ProcessorException
  {
	return new ProcessorFactoryImpl(repository, processorsCL);
  }

  /* (non-Javadoc)
   * @see com.baxter.config.processor.ProcessorFactory#getProcessor(com.baxter.config.om.ConfigID, com.baxter.config.om.Version)
   */
  @Override
  public AbstractProcessor getProcessor(final ConfigID configId, final Version version) throws ProcessorException
  {
	final AbstractProcessor processor = this.processorsCache.findProcessor(configId);
	if (processor == null)
	{
	  throw new ProcessorException("Unsupported configuration");
	}
	else
	{
	  if (processor.isVersionSupported(version))
	  {
		return processor;
	  }
	  else
	  {
		throw new ProcessorException("Unsupported version");
	  }
	}
  }

  /* (non-Javadoc)
   * @see com.baxter.config.processor.ProcessorFactory#getRepository()
   */
  @Override
  public ConfigurationRepository getRepository()
  {
	return this.repository;
  }

  /**
   * Reads available processor descriptors and loads them.
   */
  private void loadProcessors(final ClassLoader processorsCL)
  {
	try
	{
	  LOGGER.debug("Looking for available processors in classpath...");
	  final DescriptorsIterator descriptors = new DescriptorsIterator(processorsCL);
	  if (!descriptors.hasNext())
	  {
		LOGGER.warn("Could not find any processor in classpath");
	  }
	  while (descriptors.hasNext())
	  {
		final Descriptor descriptor = descriptors.next();
		LOGGER.debug("Found available processor - {}", descriptor);
		try
		{

		  try
		  {
			final Descriptor existingDescriptor = this.repository.getDescriptor(descriptor.getProductId());
			// Now check if this descriptor is newer than the descriptor stored
			// in the repository
			// if this is newer one then apply update
			final Version existingVersion = Version.valueOf(existingDescriptor.getVersion());
			final Version availableVersion = Version.valueOf(descriptor.getVersion());
			if (existingVersion.compareTo(availableVersion) < 0)
			{
			  final Upgrade upgrade = descriptor.getLatestUpgrade(existingDescriptor.getVersion());
			  if (upgrade != null)
			  {
				this.repository.upgradePackage(descriptor, upgrade, this);
			  }
			  else
			  {
				LOGGER.warn("Cannot find upgrade from {} to {}", existingVersion, availableVersion);
			  }
			}
			else
			{
			  LOGGER.debug("Processor in repository is up to date - {}", existingDescriptor);
			}
		  }
		  catch (final RepositoryException e)
		  {
			// if there is no descriptor in repository then just copy entire
			// package
			LOGGER.info("Could not find {} in repository", descriptor);
			this.repository.installPackage(descriptor);
		  }

		  // Finally create processors and register them with cache
		  for (final Processor processorDescriptor : descriptor.getProcessors())
		  {
			final AbstractProcessor processor = createProcessor(descriptor, processorDescriptor);
			this.processorsCache.registerProcessor(descriptor.getProductId(), processorDescriptor.getConfigurationType(),
			    processor);
		  }

		  if (descriptor.getViewerStylesheet() != null)
		  {
			final XSLTProcessor viewer = new XSLTProcessor(descriptor, this);
			viewer.setStylesheet(descriptor.getViewerStylesheet());
			this.processorsCache.registerProcessor(descriptor.getProductId(), "viewer", viewer);
		  }

		}
		catch (final ProcessorException e)
		{
		  LOGGER.error("Could not load descriptor from {}", descriptors.getLastDescriptorResource(), e);
		}
	  }
	}
	catch (final IOException e)
	{
	  LOGGER.error("Failed when looking for descriptors", e);
	}
  }

  /**
   * Instantiates the processor.
   *
   * @param descriptor
   *          package descriptor
   * @param processorDescriptor
   *          process descriptor
   * @return processor instance
   * @throws ProcessorException
   *           if cannot create processor for any reason
   */
  private AbstractProcessor createProcessor(final Descriptor descriptor, final Processor processorDescriptor)
	  throws ProcessorException
  {
	LOGGER.trace("Creating processor - {}", processorDescriptor);
	try
	{
	  final Class<? extends AbstractProcessor> processorClass = Class.forName(processorDescriptor.getClassName()).asSubclass(
		  AbstractProcessor.class);
	  final Constructor<? extends AbstractProcessor> processorConstructor = processorClass.getConstructor(Descriptor.class,
		  ProcessorFactory.class);
	  final AbstractProcessor processor = processorConstructor.newInstance(descriptor, this);
	  for (final Parameter parameter : processorDescriptor.getParameters())
	  {
		LOGGER.trace("Setting {} in {}", parameter, processorDescriptor);
		BUB.setProperty(processor, parameter.getName(), parameter.getValue());
	  }
	  return processor;
	}
	catch (final ClassNotFoundException e)
	{
	  LOGGER.error("Could not find processor class", e);
	  throw new ProcessorException(e);
	}
	catch (final NoSuchMethodException e)
	{
	  LOGGER.error("Could not find constructor in processor class", e);
	  throw new ProcessorException(e);
	}
	catch (final InvocationTargetException e)
	{
	  LOGGER.error("Processor constructor failed", e);
	  throw new ProcessorException(e);
	}
	catch (final IllegalAccessException e)
	{
	  LOGGER.error("Processor constructor not accessible", e);
	  throw new ProcessorException(e);
	}
	catch (final InstantiationException e)
	{
	  LOGGER.error("Could not instantiate processor", e);
	  throw new ProcessorException(e);
	}
  }

}
