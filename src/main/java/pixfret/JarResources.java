/*-
 * #%L
 * PixFRET
 * %%
 * Copyright (C) 2005 - 2019 University of Lausanne and
 * 			Swiss Federal Institute of Technology Lausanne (EPFL),
 * 			Switzerland
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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
