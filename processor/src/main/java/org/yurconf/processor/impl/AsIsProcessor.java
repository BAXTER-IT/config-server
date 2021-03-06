/*
 * Yurconf Processor Fundamental
 * This software is distributed as is.
 * 
 * We do not care about any damages that could be caused
 * by this software directly or indirectly.
 * 
 * Join our team to help make it better.
 */
package org.yurconf.processor.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import org.apache.commons.io.IOUtils;
import org.yurconf.processor.AbstractProcessor;
import org.yurconf.processor.ProcessorContext;
import org.yurconf.processor.ProcessorException;
import org.yurconf.processor.ProcessorFactory;
import org.yurconf.processor.desc.Descriptor;

/**
 * Default implementation of As Is Configuration Processor.
 *
 * @author ykryshchuk
 * @since 1.5
 */
public class AsIsProcessor extends AbstractProcessor
{

  private static final String PARAM_FILE = "file";

  /**
   * Creates processor for specified descriptor.
   *
   * @param descriptor
   *          processor descriptor
   */
  public AsIsProcessor(final Descriptor descriptor, final ProcessorFactory processorFactory)
  {
	super(descriptor, processorFactory);
  }

  private File getRequestedFile(final ProcessorContext context)
  {
	final File productDir = getFactory().getRepository().getProductDirectory(getDescriptor().getProductId());
	final String filename = getParameterByName(context.getParameters(), PARAM_FILE);
	final Stack<String> variants = new Stack<String>();
	variants.addAll(context.getConfigID().getVariants());
	if (!variants.isEmpty())
	{
	  while (!variants.isEmpty())
	  {
		final String variant = variants.pop();
		final File variantFile = new VariantFile(productDir, filename, variant);
		if (variantFile.isFile())
		{
		  return variantFile;
		}
		else
		{
		  logger.warn("Requested variant ({}) not found for file {}", variant, filename);
		}
	  }
	  logger.warn("Variants do not exist for {}. Falling back to orifinal file", filename);
	}
	return new File(productDir, filename);
  }

  @Override
  public void process(final ProcessorContext context) throws ProcessorException
  {
	// 1. Determine the source file location
	final File file = getRequestedFile(context);
	if (!file.isFile())
	{
	  logger.error("Could not find source file " + file.getAbsolutePath());
	  throw new ProcessorException("Source file not found");
	}

	// 2. Determine the content type
	// TODO CFG-26
	// context.setContentType(null, null);

	// 3. Write the source file to stream
	try
	{
	  final InputStream stream = new FileInputStream(file);
	  try
	  {
		IOUtils.copy(stream, context.getOutputStream());
	  }
	  finally
	  {
		stream.close();
	  }
	}
	catch (final IOException e)
	{
	  logger.error("Failed to process", e);
	  throw new ProcessorException(e);
	}
  }
}
