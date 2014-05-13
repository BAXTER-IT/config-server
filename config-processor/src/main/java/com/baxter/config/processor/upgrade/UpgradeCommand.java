/*
 * Configuration Processors
 * Copyright (C) 2012-2013  BAXTER Technologies
 * 
 * This software is a property of BAXTER Technologies
 * and should remain that way. If you got this source
 * code from elsewhere please immediately inform Franck.
 */
package com.baxter.config.processor.upgrade;

/**
 * @author xpdev
 * @since 1.5
 */
public interface UpgradeCommand
{

  void upgrade(UpgradeContext context) throws UpgradeException;

}
