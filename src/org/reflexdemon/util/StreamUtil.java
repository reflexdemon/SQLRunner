package org.reflexdemon.util;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 *
 */
public final class StreamUtil 
{
   static final int EOS=-1;

   /** 
    * Pumps one stream to another, with a buffer - both streams are
    * closed when the pump is complete.
    */
   public static final int pump(InputStream is, OutputStream os)
      throws IOException {
      int totalBytes;
      try
      {
         byte buffer[]=new byte[8192];
         int bytes=is.read(buffer);
         totalBytes = 0;
         while (bytes > 0) {
            totalBytes+=bytes;
            os.write(buffer, 0, bytes);
            // System.out.println ("last read: "+bytes+" reading...");
            // SOLVED! -- See jet_net ChunkedInputStream, and 
            // Transfer-Encoding: chunked.  !!
            // pab, 24/7/2003
            bytes=is.read(buffer);
         }
      } 
      finally
      {
         if (os != null) 
         {
            os.flush(); 
            os.close();
         }
         if (is != null)
         {
            is.close();
         }
      }
      return totalBytes;
   }
   
   /** 
    * Pumps one stream to another, with a buffer - both streams are
    * closed when the pump is complete.
    */
   public static final int pump(Reader is, Writer os)
         throws IOException {
      char buffer[]=new char[8192];
      int bytes=is.read(buffer);
      int totalBytes=0;
      try
      {
         while (bytes > 0) {
            totalBytes+=bytes;
            os.write(buffer, 0, bytes);
            // System.out.println ("last read: "+bytes+" reading...");
            // SOLVED! -- See jet_net ChunkedInputStream, and 
            // Transfer-Encoding: chunked.  !!
            // pab, 24/7/2003
            bytes=is.read(buffer);
         }
      } 
      finally
      {
         if (os != null) 
         {
            os.flush(); 
            os.close();
         }
         if (is != null)
         {
            is.close();
         }
      }
      return totalBytes;
   }
   
   public static final void unbufferedPump(InputStream is, OutputStream os) 
      throws IOException {
      try
      {
         int b=is.read();
         while (b != EOS) {
            os.write(b);
            b=is.read();
         }
      } 
      finally
      {
         if (os != null) 
         {
            os.flush(); 
            os.close();
         }
         if (is != null)
         {
            is.close();
         }
      }
   }

  /** */
  public static final void pumpExactly(InputStream is, OutputStream os, 
                                       int bytes) 
    throws IOException {
     try
     {
        for (int i=0; i<bytes; i++) {
           os.write(is.read());
        }
     } 
     finally
     {
        if (os != null) 
        {
           os.flush(); 
           os.close();
        }
        if (is != null)
        {
           is.close();
        }
     }
  }

   /** 
    * Reads all remaining bytes from a stream and returns it 
    * as a string. 
    */
   public static String readToString(InputStream is) 
      throws IOException {
      ByteArrayOutputStream baos=new ByteArrayOutputStream();
      pump(is, baos);
      
      return baos.toString();
   }
   
   /** 
    * Reads all remaining bytes from a stream and returns it 
    * as a string. 
    */
   public static String readToString(InputStream is, String encoding) 
      throws IOException {
      ByteArrayOutputStream baos=new ByteArrayOutputStream();
      pump(is, baos);
      
      return baos.toString(encoding);
   }

   public static byte[] readFully(InputStream is) 
      throws IOException {
      
      try {
         
         ByteArrayOutputStream baos=new ByteArrayOutputStream();
         pump(is, baos);
//         int b=is.read();
//         while (b != EOS) {
//            baos.write(b);
//            b=is.read();
//         }
//         is.close();
         return baos.toByteArray();
      } finally {

      }
   }
   
   public static String readToString(File f) throws IOException {
      return readToString(new FileInputStream(f));
   }
   
   public static byte[] readFully(File f) 
   throws IOException {
      return readFully(new FileInputStream(f));
   }
   
   public static final void write(String s, Writer w) throws IOException {
      w.write(s);
      w.flush();
   }
   
   public static final void write(String s, File f) throws IOException {
      FileWriter fw=null;
      try
      {
         fw = new FileWriter(f);
         write(s, fw);
      }
      finally
      {
         if (fw != null) fw.close();
      }
   }
   
   public static final int write(byte b[], File f) throws IOException {
      FileOutputStream fos=null;
      try
      {
         fos = new FileOutputStream(f, true);
         fos.write(b);
      }
      finally
      {
         if (fos != null)
         {
            fos.flush();
            fos.close();
         }
      }
      return b.length;
   }
   
   public static final int write(InputStream is, File f) throws IOException {
      return pump(is, new FileOutputStream(f));
   }
   
   public static final void close(InputStream is) {
      if (is != null) {
         try {
            is.close();
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
   }
   
   public static final void close(Reader is) {
      if (is != null) {
         try {
            is.close();
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
   }
}
