package com.baxter.config.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Application Lifecycle Listener implementation class StoreManagerInit
 * 
 */
public class StoreManagerInit implements ServletContextListener
{
  /**
   * Default constructor.
   */
  public StoreManagerInit()
  {
	// TODO Auto-generated constructor stub
  }

  /**
   * @see ServletContextListener#contextInitialized(ServletContextEvent)
   */
  public void contextInitialized(ServletContextEvent sce)
  {
	final StoreManager storeManager = new StoreManager();
	sce.getServletContext().setAttribute("storeManager", storeManager);
	storeManager.copyFileStructure();
  }

  /**
   * @see ServletContextListener#contextDestroyed(ServletContextEvent)
   */
  public void contextDestroyed(ServletContextEvent sce)
  {
	// TODO Auto-generated method stub
  }
}
