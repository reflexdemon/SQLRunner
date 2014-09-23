package org.reflexdemon.util;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

/**
 * NOTE: This configuration is used only in the process of reading 
 * the properties (either from a Database or from a properties file).
 * After the properties are loaded, this simple log configuration is 
 * overridden by a properties based long configuration.
   
 * This is only used during the bootstrap process so that we have a 
 * valid Log4J confguration in the process of startup before we have
 * access to the properties necessary to fully configure Log4J.
 *
 * @author Paul Bemowski
 */
public final class Log4JLogConfig {
   /** Used to init log4j before the properties file is actually read
    * so we have a valid log4j configuration at startup.
    */
   public static final void log4jBootstrap(String level) {
      System.out.println("Bootstrapping Log4J w/ Default level '"+level+"'");
      
      Properties p=new Properties();
      p.put("log4j.rootLogger", level+", stdout");
      p.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
      p.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
      p.put("log4j.appender.stdout.layout.ConversionPattern", "%c{1} %-5p: %m%n");
      PropertyConfigurator.configure(p);
   }
   
   public static final void log4jBootstrap() {
      log4jBootstrap("DEBUG");
   }
}
