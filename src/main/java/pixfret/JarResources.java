package pixfret;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.Image;
/**
 * JarResources: JarResources maps all resources included in a
 * Zip or Jar file. Additionaly, it provides a method to extract one
 * as a blob.
 */
public final class JarResources {

   // external debug flag
   public static boolean debugOn=false;

   // jar resource mapping tables
   private static Hashtable htSizes=new Hashtable();  
 
   /**
    * creates a JarResources. It extracts all resources from a Jar
    * into an internal hashtable, keyed by resource names.
    * @param jarFileName a jar or zip file
    */
   public static Image extractImage(String jarFileName, String imageName) {
      try {
          // extracts just sizes only. 
          ZipFile zf=new ZipFile(jarFileName);
          Enumeration e=zf.entries();
          while (e.hasMoreElements()) {
              ZipEntry ze=(ZipEntry)e.nextElement();
              htSizes.put(ze.getName(),new Integer((int)ze.getSize()));
          }
          zf.close();

          // extract resources and put them into the hashtable.
          FileInputStream fis=new FileInputStream(jarFileName);
          BufferedInputStream bis=new BufferedInputStream(fis);
          ZipInputStream zis=new ZipInputStream(bis);
          ZipEntry ze=null;
          while ((ze=zis.getNextEntry())!=null) {
             if (ze.isDirectory()) {
                continue;
             }
             if (debugOn) {
                System.out.println("ze.getName()="+ze.getName()+","+"getSize()="+ze.getSize());
             }
             int size=(int)ze.getSize();
             // -1 means unknown size. 
             if (size==-1) {
                size=((Integer)htSizes.get(ze.getName())).intValue();
             }
             byte[] b=new byte[(int)size];
             int rb=0;
             int chunk=0;
             while (((int)size - rb) > 0) {
                 chunk=zis.read(b,rb,(int)size - rb);
                 if (chunk==-1) {
                    break;
                 }
                 rb+=chunk;
             }
    
		     if (ze.getName().equals(imageName)) {
		    	return java.awt.Toolkit.getDefaultToolkit().createImage (b);
		    
		     } 
          }
       } catch (NullPointerException e) {
          System.out.println("done.");
       } catch (FileNotFoundException e) {
          e.printStackTrace();
       } catch (IOException e) {
          e.printStackTrace();
       }
       return null;
   }

}	// End of JarResources class.
