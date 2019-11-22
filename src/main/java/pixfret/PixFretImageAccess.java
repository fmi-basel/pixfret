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

import ij.*;
import ij.process.*;
import java.awt.image.*;

/**
* PixFRET
* Pixel by Pixel analysis of FRET with ImageJ
* 
* Full information: http://www.unil.ch/cig/page16989.html
*
* Description:
* The plugin PixFRET allows to visualize the FRET between two partners in a cell
* or in a cell population by computing pixel by pixel the images of a sample 
* acquired in a three channel setting.
*
* Authors:
* Jerome Feige 1, Daniel Sage 2, Walter Wahli 1, Beatrice Desvergne 1, Laurent Gelman 1.
* 1 - Center for Integrative Genomics, NCCR frontiers in Genetics, University of Lausanne, Switzerland.
* 2 - Biomedical Imaging Group (BIG), Swiss Federal Institute of Technology Lausanne (EPFL), Switzerland.
*
*/
	
public class PixFretImageAccess {

	public float pixels[] = null;		// store the pixel data
	private int nx = 0;					// size in X axis
	private int ny = 0;					// size in Y axis
	private int size = 0;				// size = nx*ny

	private float tolerance = 1e-6f;	// Tolerance for the initial value in the convolveIIR
	private double logTolerance = Math.log(tolerance);

	/**
	* Creates a new object of the class PixFretImageAccess from an 
	* ImageProcessor object.
	*
	* ImageProcessor object contains the image data, the size and 
	* the type of the image. The ImageProcessor is provided by ImageJ,
	* it should by a 8-bit, 16-bit. 
	*
	* @param ip    an ImageProcessor object provided by ImageJ
	*/
	public PixFretImageAccess(ImageProcessor ip) {
		if (ip == null) 
			throw new 
				ArrayStoreException("Constructor: ImageProcessor == null.");
		nx = ip.getWidth();
		ny = ip.getHeight();
		size = nx*ny;
		pixels = new float[size];
		if (ip.getPixels() instanceof byte[]) {
			byte[] bsrc = (byte[])ip.getPixels();
			for (int k=0; k<size; k++)
				pixels[k] = (float)(bsrc[k] & 0xFF);
		
		}	
		else if (ip.getPixels() instanceof short[]) {
			 short[] ssrc = (short[])ip.getPixels();
			 for (int k=0; k<size; k++)
				pixels[k] = (float)(ssrc[k] & 0xFFFF);
		}	
		else if (ip.getPixels() instanceof float[]) {
			 float[] fsrc = (float[])ip.getPixels();
			 for (int k=0; k<size; k++)
				pixels[k] = (float)fsrc[k];
		}
		else  {
			throw new 
				ArrayStoreException("Constructor: Unexpected image type.");
		}
	}


	/**
	* Creates a new object of the class PixFretImageAccess.
	*
	* The size of the image are given as parameter.
	* The data pixels are empty and are not initialized.
	*
	* @param nx       	the size of the image along the X-axis
	* @param ny       	the size of the image along the Y-axis
	*/
	public PixFretImageAccess(int nx, int ny) {
		if (nx < 1)
			throw new 
				ArrayStoreException("Constructor: nx < 1.");
		if (ny < 1)
			throw new 
				ArrayStoreException("Constructor: ny < 1.");
		this.nx = nx;
		this.ny = ny;
		size = nx*ny;
		pixels = new float[size];
	}

	/**
	* Return the width of the image.
	*
	* @return     	the image width
	*/
	public int getWidth() {
		return nx;
	}

	/**
	* Return the height of the image.
	*
	* @return     	the image height
	*/
	public int getHeight() {
		return ny;
	}

	/**
	* Return the maximum value of PixFretImageAccess.
	*
	* @return     	the minimum and maximum value
	*/
	public float[] getMinMax() {
		
		float maxi = pixels[0];
		float mini = pixels[0];
		for (int i=1; i<size; i++) {
			if (pixels[i] > maxi) 
				maxi = pixels[i];
			if (pixels[i] < mini) 
				mini = pixels[i];
		}
		float[] minmax = {mini, maxi};
		return minmax;
	}

	/**
	* Create a FloatProcessor from the pixel data.
	*
	* @return     	the FloatProcessor
	*/
	public FloatProcessor createFloatProcessor() {
		FloatProcessor fp = new  FloatProcessor(nx, ny) ;
		float[] fsrc = new float[size];
		for (int k=0; k<size; k++)
				fsrc[k] = (float)(pixels[k]);
	 	fp.setPixels(fsrc);
	 	return fp;
	}

	/**
	* Create a new PixFretImageAccess object by duplication of the current the 
	* PixFretImageAccess object.
	*
	* @return   a new PixFretImageAccess object
	**/
	public PixFretImageAccess duplicate() {
		PixFretImageAccess ia = new PixFretImageAccess(nx, ny);
		System.arraycopy(pixels, 0, ia.pixels, 0, size);
		return ia;
	}

	/**
	* An PixFretImageAccess object calls this method for getting
	* the gray level of a selected pixel.
	*
	* Mirror border conditions are applied.
	*
	* @param x		input, the integer x-coordinate of a pixel
	* @param y		input, the integer y-coordinate of a pixel
	* @return     	the gray level of the pixel (float) 
	*/
	public float getPixel(int x, int y) {
/*		int periodx = 2*nx-2;
	 	int periody = 2*ny-2;
		if (x<0) {			
			while (x<0) x += periodx;		// Periodize	
			if (x >= nx) x = periodx - x;	// Symmetrize
		}
		else if (x>=nx) {			
			while (x>=nx) x -= periodx;		// Periodize	
			if (x < 0) x = -x;				// Symmetrize
		}
		if (y<0) {			
			while (y<0) y += periody;		// Periodize	
			if (y>=ny)  y = periody - y;	// Symmetrize	
		}
		else if (y>=ny) {			
			while (y>=ny) y -= periody;		// Periodize	
			if (y < 0) y = -y;				// Symmetrize
	 	}
*/
		return pixels[x+y*nx];
	}

	/**
	* An PixFretImageAccess object calls this method for getting a 
	* whole column of the image.
	*
	* The column should already created with the correct size [ny].
	*
	* @param x       	input, the integer x-coordinate of a column
	* @param column     output, an array of the type float
	*/
	public void getColumn(int x, float[] column) {
		if (x < 0) 
			throw new IndexOutOfBoundsException("getColumn: x < 0.");
		if (x >= nx)
			throw new IndexOutOfBoundsException("getColumn: x >= nx.");
		if (column == null)
			throw new ArrayStoreException("getColumn: column == null.");
		if (column.length != ny)
			throw new ArrayStoreException("getColumn: column.length != ny.");
		for (int i=0; i<ny; i++) {
			column[i] = pixels[x];
			x += nx;
		}
	}

	/**
	* An PixFretImageAccess object calls this method for getting a 
	* whole row of the image.
	*
	* The row should already created with the correct size [nx].
	*
	* @param y       	input, the integer y-coordinate of a row
	* @param row        output, an array of the type float
	*/
	public void getRow(int y, float[] row)	{
		if ( y < 0)
	    	throw new IndexOutOfBoundsException("getRow: y < 0.");
		if (y >= ny)
	    	throw new IndexOutOfBoundsException("getRow: y >= ny.");
		if (row == null)
			throw new ArrayStoreException("getColumn: row == null.");
		if (row.length != nx)
			throw new ArrayStoreException("getColumn: row.length != nx.");
		y *= nx;
		System.arraycopy(pixels, y, row, 0, row.length);
	}

	/**
	* An PixFretImageAccess object calls this method for getting a neighborhood
	* arround a pixel position.
	*
	* The neigh parameter should already created. The size of the array 
	* determines the neighborhood block.
	*
	* <br>Mirror border conditions are applied.
	* <br>
	* <br>The pixel value of (x-n/2, y-n/2) is put into neigh[0][0]
	* <br>...
	* <br>The pixel value of (x+n/2, y+n/2) is put into neigh[n-1][n-1]
	* <br>
	* <br>For example if neigh is a float[4][4]:
	* <br>The pixel value of (x-1, y-1) is put into neigh[0][0]
	* <br>The pixel value of (x  , y  ) is put into neigh[1][1]
	* <br>The pixel value of (x+1, y+1) is put into neigh[2][2]
	* <br>The pixel value of (x+2, y+2) is put into neigh[3][3]
	* <br>...
	* <br>For example if neigh is a float[5][5]:
	* <br>The pixel value of (x-2, y-2) is put into neigh[0][0]
	* <br>The pixel value of (x-1, y-1) is put into neigh[1][1]
	* <br>The pixel value of (x  , y  ) is put into neigh[2][2]
	* <br>The pixel value of (x+1, y+1) is put into neigh[3][3]
	* <br>The pixel value of (x+2, y+2) is put into neigh[4][4]

	* @param x		the integer x-coordinate of a selected central pixel
	* @param y		the integer y-coordinate of a selected central pixel
	* @param neigh	output, a 2D array s
	*/
	public void getNeighborhood(int x, int y, float neigh[][]) {
		int bx=neigh.length;
		int by=neigh[0].length;
		
		int bx2 = (bx-1)/2;
		int by2 = (by-1)/2;
		
	 	if (x >= bx2)
	 	if (y >= by2)
	 	if (x < nx-bx2-1)
	 	if (y < ny-by2-1) { 
			int index = (y-by2)*nx + (x-bx2);
			for (int j = 0; j < by; j++) {
	 			for (int i = 0; i < bx; i++) {
					neigh[i][j] = pixels[index++];			
				}
				index += (nx - bx);
			}	
			return;
		}

		int xt[] = new int[bx];
		for (int k = 0; k < bx; k++) {
			int xa = x + k - bx2;
	    	int periodx = 2*nx - 2;				
			while (xa < 0) 
				xa += periodx;				// Periodize
			while (xa >= nx) {
				xa = periodx - xa;			// Symmetrize
				if (xa < 0)  xa = - xa;
			}
			xt[k] = xa;
		}

		int yt[] = new int[by];
		for (int k = 0; k < by; k++) {
			int ya = y + k - by2;
	    	int periody = 2*ny - 2;			
			while (ya < 0) ya += periody;	// Periodize
			while (ya >= ny)  {
				ya = periody - ya;			// Symmetrize
				if (ya < 0)  ya = - ya;
			}
			yt[k] = ya;
		}
		
	 	int somme=0;
	 	for (int j = 0; j < by; j++) {
			int index = yt[j]*nx;
	 		for (int i = 0; i < bx; i++) {
	 	        somme =index+xt[i];
				neigh[i][j] = pixels[somme];
			}
		}	
	}

	/**
	* An PixFretImageAccess object calls this method in order a value
	* of the gray level to be put to a position inside it
	* given by the coordinates.
	*
	* @param x		input, the integer x-coordinate of a pixel
	* @param y		input, the integer y-coordinate of a pixel
	* @param value	input, a value of the gray level of the type float
	*/
	public void putPixel(int x, int y, float value) {
	   	if (x < 0)
	    	return;
		if (x >= nx)
	    	return;
		if (y < 0)
	   		return;
		if (y >= ny)
	   		return;
		pixels[x+y*nx] = value;
	}

	/**
	* An PixFretImageAccess object calls this method to put a whole 
	* column in a specified position into the image.
	*
	* @param x       	input, the integer x-coordinate of a column
	* @param column     input, an array of the type float
	*/
	public void putColumn (int x, float[] column) {
		if (x < 0) 
			throw new IndexOutOfBoundsException("putColumn: x < 0.");
		if (x >= nx)
			throw new IndexOutOfBoundsException("putColumn: x >= nx.");
		if (column == null)
			throw new ArrayStoreException("putColumn: column == null.");
		if (column.length != ny)
			throw new ArrayStoreException("putColumn: column.length != ny.");
		for (int i=0; i<ny; i++) {
			pixels[x] = column[i];
			x += nx;
		}
	}

	/**
	* An PixFretImageAccess object calls this method to put a whole 
	* row in a specified position into the image.
	*
	* @param y       input, the integer x-coordinate of a column
	* @param row     input, an array of the type float
	*/
	public void putRow(int y, float[] row) {
		if (y < 0) 
			throw new IndexOutOfBoundsException("putRow: y < 0.");
		if (y >= ny)
			throw new IndexOutOfBoundsException("putRow: y >= ny.");
		if (row == null)
			throw new ArrayStoreException("putRow: row == null.");
		if (row.length != nx)
			throw new ArrayStoreException("putRow: row.length != nx.");
		y *= nx;
		System.arraycopy(row, 0, pixels, y, nx);
	}

	/**
	* Display an image.
	*
	* @param title   a string for the title of the window
	*/
	public void show(String title) {
		FloatProcessor fp = createFloatProcessor();
		fp.resetMinAndMax();
		ImagePlus impResult = new ImagePlus(title, fp);
		impResult.show();
	}

	/**
  	* Display a 32-bits image with a red LUT in [rangeInf, rangeSup].
  	*/
  	public void showLUT(String title, float rangeInf, float rangeSup) {
 		FloatProcessor fp = createFloatProcessor();
		fp.resetMinAndMax();
		ImagePlus out = new ImagePlus(title, fp);
		LookUpTable lut = new LookUpTable(out.getImage());
		float[] minmax = getMinMax();
		if (lut.getMapSize() > 0 && minmax[1] > minmax[0]) {

                            int vinf = (int)Math.round((rangeInf-minmax[0])*255/(minmax[1]-minmax[0]))-1;
                            int vsup = 255-(int)Math.round((minmax[1]-rangeSup)*255/(minmax[1]-minmax[0]));
                    
			byte[] r = lut.getReds();
			byte[] b = lut.getBlues();
			byte[] g = lut.getGreens();
			for(int i=0; i<r.length; i++) {
				if (vinf <= i && i<= vsup) {
					r[i] = (byte)(0);
					b[i] = (byte)(125);
					g[i] = (byte)(65);
				}
				else {
					r[i] = b[i] = g[i] = (byte)(i & 0x00FF);
				}
			}
			ColorModel cm = new IndexColorModel(8, 256, r, g, b);
			FloatProcessor ip = (FloatProcessor)out.getProcessor();
			ip.setColorModel(cm);
			ip.createImage();
			out.updateAndDraw();
		}
		out.show(title);
	} 			

	/**
  	* Gaussian Smoothing.
  	*/
	public void smoothGaussian(float sigma) {
		PixFretImageAccess source = duplicate();
		float N = 3.0f;
		float poles[] = new float[3];
		float s2 = sigma * sigma;
		float a = 1.0f + (N/s2) - (float)(Math.sqrt(N*N+2.0*N*s2)/s2);
		poles[0] = poles[1] = poles[2] = a;
		float row[]  = new float[nx];
		float lambda = 1.0f;
		for (int k = 0; k <poles.length; k++) {
			lambda = lambda * (1.0f - poles[k]) * (1.0f - 1.0f / poles[k]);
		}
		for (int y=0; y<ny; y++) {
			source.getRow(y, row);
			convolveIIR(row, poles, lambda);
			putRow(y, row);
		}
		float col[]  = new float[ny];
		for (int x=0; x<nx; x++) {
			getColumn(x, col);
			convolveIIR(col, poles, lambda);
			putColumn(x, col);
		}
	}

	/**
	* Convolve with with a Infinite Impluse Response filter (IIR)
	*
	* In-place processing.
	*/
	public void convolveIIR(float[] signal, float poles[], float lambda) {
		for (int n=0; n<signal.length; n++) {
			signal[n] = signal[n] * lambda;
		}
		for (int k=0; k<poles.length; k++) {
			signal[0] = getInitialCausalCoefficientMirror(signal, poles[k]);
			for (int n=1; n<signal.length; n++) {
				signal[n] = signal[n] + poles[k] * signal[n - 1];
			}
			signal[signal.length - 1] = getInitialAntiCausalCoefficientMirror(signal, poles[k]);
			for (int n=signal.length-2; 0<=n; n--) {
				signal[n] = poles[k] * (signal[n+1] - signal[n]);
			}
		}
	}

	/**
	 */
	private float getInitialCausalCoefficientMirror(float[] c, float z) {
		float z1 = z;
		float zn = (float)Math.pow(z, c.length - 1);
		float sum = c[0] + zn * c[c.length - 1];
		int horizon = c.length;
		//if (0.0 < tolerance) {
			horizon = 2 + (int)(logTolerance / Math.log(Math.abs(z)));
			horizon = (horizon < c.length) ? (horizon) : (c.length);
		//}
		zn = zn * zn;
		for (int n=1; n<horizon-1; n++) {
			zn = zn / z;
			sum = sum + (z1 + zn) * c[n];
			z1 = z1 * z;
		}
		return(sum / (1.0f - (float)Math.pow(z, 2 * c.length - 2)));
	}

	/**
	 */
	private float getInitialAntiCausalCoefficientMirror(float[] c, float z) {
		return((z * c[c.length - 2] + c[c.length - 1]) * z / (z * z - 1.0f));
	}

} // end of class PixFretImageAccess


