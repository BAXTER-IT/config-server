/*
 * Configuration Processors
 * Copyright (C) 2012-2013  BAXTER Technologies
 * 
 * This software is a property of BAXTER Technologies
 * and should remain that way. If you got this source
 * code from elsewhere please immediately inform Franck.
 */
package org.yurconf.processor.upgrade;

import org.yurconf.processor.ProcessorFactory;
import org.yurconf.processor.desc.Descriptor;

/**
 * @author xpdev
 * @since 1.5
 */
public interface UpgradeContext
{

  /**
   * Returns the processor descriptor.
   *
   * @return the descriptor
   */
  Descriptor getDescriptor();

  /**
   * Returns the factory appropriate for upgrade context.
   *
   * @return factory instance
   */
  ProcessorFactory getProcessorFactory();

}
