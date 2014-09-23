package org.reflexdemon.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Some simple utils for working with the command line...
 */
public class CLIUtils {
   
   /** 
    * Confirms an option on the command line.
    */
   public static boolean confirm(String message, boolean def) {
      BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
      
      String displayMess=
         "Please answer with [Y,y,N,n], or <return> to accept default.\n"+
         message+" ["+(def?"Y":"y")+"/"+(def?"n":"N")+"]?";
      
      try {
         System.out.println (displayMess);
         String line=br.readLine().trim();
         
         // default
         if (line.length() == 0) {
            line=(def?"Y":"N");
         }
         
         while (line.length() == 0 || 
                (line.length() > 1 && "YyNn".indexOf(line) != -1)) {
            System.out.println ("Invalid input '"+line+"'");
            System.out.println (displayMess);
            line=br.readLine().trim();
         }
         
         if (line.equalsIgnoreCase("Y"))
            return true;
         else 
            return false;
      } catch (Exception ex) {
         // this should never happen.
         ex.printStackTrace();
         return def;
      }
   }
}
