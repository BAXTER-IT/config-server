/*
 * Baxter Configuration Object Model
 * Copyright (C) 2012-2013  BAXTER Technologies
 * 
 * This software is a property of BAXTER Technologies
 * and should remain that way. If you got this source
 * code from elsewhere please immediately inform Franck.
 */
package org.yurconf.om;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.yurconf.om.Version;

/**
 * Unit test for {@link Version} class.
 *
 * @author ykryshchuk
 * @since 1.5
 */
public class TestVersion
{

  @Test
  public void testValueOf_100()
  {
	final Version v = Version.valueOf("100");
	assertEquals(1, v.getParts().length);
	assertEquals(100, v.getParts()[0]);
  }

  @Test
  public void testValueOf_1_0()
  {
	final Version v = Version.valueOf("1.0");
	assertEquals(2, v.getParts().length);
	assertEquals(1, v.getParts()[0]);
	assertEquals(0, v.getParts()[1]);
  }

  @Test
  public void testValueOf_1_5_3()
  {
	final Version v = Version.valueOf("1.5.3");
	assertEquals(3, v.getParts().length);
	assertEquals(1, v.getParts()[0]);
	assertEquals(5, v.getParts()[1]);
	assertEquals(3, v.getParts()[2]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValueOf_1_a()
  {
	Version.valueOf("1.a");
  }

  @Test
  public void testValueOfToString_1_1_0()
  {
	final Version v =  Version.valueOf("1.1.0");
	assertEquals("1.1.0", v.toString());
  }

  @Test
  public void testCompare_1_5_and_1_5()
  {
	assertTrue(Version.valueOf("1.5").compareTo(Version.valueOf("1.5")) == 0);
  }

  @Test
  public void testCompare_1_5_and_1_6()
  {
	assertTrue(Version.valueOf("1.5").compareTo(Version.valueOf("1.6")) < 0);
  }

  @Test
  public void testCompare_1_5_and_1_0()
  {
	assertTrue(Version.valueOf("1.5").compareTo(Version.valueOf("1.0")) > 0);
  }

  @Test
  public void testValueOf_null()
  {
	final Version v = Version.valueOf(null);
	assertNull(v);
  }

}
