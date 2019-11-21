package pixfret;

import javax.swing.*;
import java.text.DecimalFormat;
import java.awt.*;
import java.awt.event.*;
import ij.process.ColorProcessor;
import ij.*;

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

public class Plot extends JPanel implements MouseListener, MouseMotionListener {

	private final int	CST		= 0;		// Constant Model
	private final int	LIN		= 1;		// Linear Model
	private final int 	EXP 	= 2;		// Exponential Model

	private final int	CHANNEL = 0;		// X axis
	private final int	RATIO	= 1;		// Y axis

	private final int LEFT 	= 60;
	private final int TOP 	= 15;
	private final int BOTTOM= 45;

	final private	int bx = 256;					// size of the graph
	final private	int by = 256;					// size of the graph
	
	private Image image;					// contains an image of the graph

	private float[][] params	= new float[3][3];
	
	private boolean drag[] = {false, false, false, false};
	private BleedThroughPanel dlg;
	
	private Point hotspot[] = new Point[4];
	
	private float scale[] 	= {1.0f, 1.0f};
	private float off[] 	= {0.0f, 0.0f};

	private Font font = new Font("SansSerif",  Font.PLAIN, 11);
	private boolean fit[] = {false, false, false};
	
	private Color colorArea 	= new Color(164, 164, 32);
	private Color colorSelected = new Color(255, 255, 64);
	private DecimalFormat format3 = new DecimalFormat("000");
	
	private float y1linear=1.0f;
	private float y2linear=1.0f;
	JTextField[][] txtParams;
	
	private boolean drawScatteredPlot=false;

	private	float valueChannel[];
	private	float valueRatio[];

	/**
	* Constructor.
	*/
	public Plot(int nbins, JTextField[][] txtParams, BleedThroughPanel dlg) {
		this.dlg = dlg;
		this.txtParams = txtParams;
		
		this.setSize(bx, by);
		for (int i=0; i<4; i++) 
			hotspot[i] = new Point(0, 0);
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	/**
	* Overload getPreferredSize.
	*/
	public Dimension getPreferredSize() {
		return new Dimension(bx+6, by+TOP+BOTTOM);
	}

	/**
	* Overload getMaximumSize.
	*/
	public Dimension getMaximumSize() {
		return new Dimension(bx+6, by+TOP+BOTTOM);
	}
	
	/**
	* Overload getMinimumSize.
	*/
	public Dimension getMinimumSize() {
		return new Dimension(bx+6, by+TOP+BOTTOM);
	}

	/**
	* Set the flag drawScatteredPlot.
	*/
	public void setEnabledScatteredPlot(boolean drawScatteredPlot) {
		this.drawScatteredPlot = drawScatteredPlot;
		repaint();
	}
	
	/**
	* Force the parameters to the value get from the GUI. Call by the click on the "Set" button.
	*/
	public void force() {
		
		// Constant
		float value = getFloatValue(txtParams[0][0]);
		float cst = (value - off[RATIO] ) / scale[RATIO];
		params[0][0] = cst;
		fit[0] = true;

		// Linear
		float valueShift = getFloatValue(txtParams[0][1]);
		float valueSlope = getFloatValue(txtParams[1][1]);

		float xr1 = 000 * scale[CHANNEL] + off[CHANNEL];
		float xr2 = 256 * scale[CHANNEL] + off[CHANNEL];
		
		float yr1 = valueSlope*xr1 + valueShift;
		float yr2 = valueSlope*xr2 + valueShift;

		float x1 = (xr1 - off[CHANNEL] ) / scale[CHANNEL];
		float x2 = (xr2 - off[CHANNEL] ) / scale[CHANNEL];
		float y1 = (yr1 - off[RATIO] ) / scale[RATIO];
		float y2 = (yr2 - off[RATIO] ) / scale[RATIO];
		
		float slope = (y2-y1) / (x2-x1);
		float shift = (y1-valueSlope*x1);

		params[1][1] = slope;
		params[0][1] = shift;
		fit[1] = true;
		
		// Expo
		float valueA = getFloatValue(txtParams[0][2]);
		float valueB = getFloatValue(txtParams[1][2]);
		float valueE = getFloatValue(txtParams[2][2]);
		float a = (valueA - off[RATIO] ) / scale[RATIO];
		float b = (valueB - off[RATIO] ) / scale[RATIO];
		float e = (valueE - off[CHANNEL] ) / scale[CHANNEL];
		
		params[0][2] = (float)a;
		params[1][2] = (float)b;
		params[2][2] = (float)e;
		
		repaint();
	}
			
	/**
	* Set a bleed array and repaint it.
	*/
	public void set(/*int datab[][],*/ float[][] params, boolean fit[], float cmin, float cmax, float rmin, float rmax, float[] valueChannel, float[] valueRatio) {
		
		this.fit = fit;
		this.params = params;
		
		hotspot[0].x = 0;
		hotspot[0].y = 0;
		
		hotspot[1].x = 0;
		hotspot[1].y = by;
		
		hotspot[2].x = bx;
		hotspot[2].y = by;
		
		hotspot[3].x = bx;
		hotspot[3].y = 0;

		this.valueChannel = valueChannel;
		this.valueRatio = valueRatio;
		
		scale[CHANNEL] = 256f/(cmax-cmin);			// convert real to [256]
		off[CHANNEL] = -scale[CHANNEL]*cmin;
		scale[RATIO] = 256f/(rmax-rmin);
		off[RATIO] = -scale[RATIO]*rmin;

		if (valueChannel != null) {
			createImage();
			fitConstant();
			fitLinear();
			fitExpo();
		}
	}
	
	/**
	* Reset.
	*/
	public void reset() {
		scale[CHANNEL] = 1.0f;
		scale[RATIO] = 1.0f;
		off[CHANNEL] = 0.0f;
		off[RATIO] = 0.0f;
		
		hotspot[0].x = 0;
		hotspot[0].y = 0;
		
		hotspot[1].x = 0;
		hotspot[1].y = by;
		
		hotspot[2].x = bx;
		hotspot[2].y = by;
		
		hotspot[3].x = bx;
		hotspot[3].y = 0;
	}
	
	/*
	* Create an image containing a pseudo-color representation of the
	* scattered plot.
	*/
	public void createImage() {

		rescale(hotspot[0].x, hotspot[2].x, hotspot[0].y, hotspot[2].y, bx, by);

		ColorProcessor cp2 = new ColorProcessor(bx, by);
		for(int k = 0; k < valueChannel.length; k++) {
			int c = (int)(scale[CHANNEL] * valueChannel[k] + off[CHANNEL]);
			int r = (int)(scale[RATIO] * valueRatio[k] + off[RATIO]);
			cp2.putPixel(c, by-r, 0xFFFFFFFF);
		}
		cp2.invert();

		ImagePlus cimp2 = new ImagePlus("ratio/channel", cp2);
		image = cimp2.getImage();

		image = cimp2.getImage();
		hotspot[0].x = 0;
		hotspot[0].y = 0;
		
		hotspot[1].x = 0;
		hotspot[1].y = bx;
		
		hotspot[2].x = bx;
		hotspot[2].y = by;
		
		hotspot[3].x = by;
		hotspot[3].y = 0;

		repaint();
	}

	/**
	* Rescale the graph in function of the hotspot.
	*/
	private void rescale(int xmin, int xmax, int ymin, int ymax, int bx, int by) {
		int nx = 256; //datab.length;
		int ny = 256; //datab[0].length;

		float hx1 = (xmin-off[CHANNEL]) / scale[CHANNEL];
		float hx2 = (xmax-off[CHANNEL]) / scale[CHANNEL];

		float hy1 = ((by-ymax)-off[RATIO]) / scale[RATIO];
		float hy2 = ((by-ymin)-off[RATIO]) / scale[RATIO];

		scale[CHANNEL] = 256f/(hx2-hx1);			// convert real to [256]
		off[CHANNEL] = -scale[CHANNEL]*hx1;
		scale[RATIO] = 256f/(hy2-hy1);
		off[RATIO] = -scale[RATIO]*hy1;

		int count=0;
		for(int k=0; k<valueChannel.length; k++)
			if (hx1 <= valueChannel[k] && valueChannel[k] < hx2)
			if (hy1 <= valueRatio[k] && valueRatio[k] < hy2)
				count++;
				
		float valueChannelCrop[] = new float[count];
		float valueRatioCrop[] = new float[count];
		count = 0;
		for(int k=0; k<valueChannel.length; k++)
			if (hx1 <= valueChannel[k] && valueChannel[k] < hx2)
			if (hy1 <= valueRatio[k] && valueRatio[k] < hy2) {
				valueChannelCrop[count] = valueChannel[k];
				valueRatioCrop[count] = valueRatio[k];
				count++;
			}
		valueChannel = null;
		valueRatio = null;
		valueChannel = new float[count];
		valueRatio = new float[count];
		System.arraycopy(valueChannelCrop, 0, valueChannel, 0, count);
		System.arraycopy(valueRatioCrop, 0, valueRatio, 0, count);
	}

	/**
	* Fit the constant model.
	*/
	public void fitConstant() {
		float cst = 0f;
		int nb = valueChannel.length;
					
		if (nb > 0) {
			for(int k=0; k<nb; k++)
				cst += valueRatio[k];
			cst = cst/nb;
			txtParams[0][0].setText("" + IJ.d2s(cst,5));
			params[0][0] = cst*scale[RATIO] + off[RATIO];
			fit[0] = true;
		}
		else {
			txtParams[0][0].setText("No fit");
			fit[0] = false;
		}
	}

	/**
	* Fit the linear model.
	*/
	private void fitLinear() {
		float sumx = 0f;
		float sumy = 0f;
		float sumxx = 0f;
		float sumxy = 0f;

		int nb = valueChannel.length;
		for(int k=0; k<nb; k++) {
			sumx  += valueChannel[k];
			sumxx += valueChannel[k]*valueChannel[k];
			sumy  += valueRatio[k];
			sumxy += valueRatio[k]*valueChannel[k];
		}

		if (nb > 0) {
			float slope = (nb*sumxy - sumx*sumy) / (nb * sumxx - sumx * sumx);
			float shift = (sumy - slope * sumx) / nb;
			float x1 = 0;
			float x2 = 256;
			float y1 = slope*x1 + shift;
			float y2 = slope*x2 + shift;
			y1linear = y1;
			y2linear = y2;
			if (Math.abs(slope) > 0.00001) {
				txtParams[0][1].setText("" + IJ.d2s(shift,5));
				txtParams[1][1].setText("" + IJ.d2s(slope,5));
				
				float yr1 = y1 * scale[RATIO] + off[RATIO];
				float yr2 = y2 * scale[RATIO] + off[RATIO];
				float xr1 = x1 * scale[CHANNEL] + off[CHANNEL];
				float xr2 = x2 * scale[CHANNEL] + off[CHANNEL];
		
				float valueSlope = (yr2-yr1) / (xr2-xr1);
				float valueShift = (yr1-valueSlope*xr1);
				params[0][1] = valueShift;
				params[1][1] = valueSlope;

				fit[1] = true;
				return;
			}
		}
		
		for(int i=0; i<2; i++) {
			txtParams[i][1].setText("No fit");
		}
		fit[1] = false;

	}


	/**
	* Fit the Exponential model.
	*/
	private void fitExpo() {
	
		int nb = valueChannel.length;
		
		if (nb < 3) {
			for(int i=0; i<3; i++) {
				txtParams[i][2].setText("No fit");
			}
			fit[2] = false;
			return;
		}

		// Trick to stabilize the exponentiel.
		// The constant a is not fitted. It has always 90% of the constant
		double ac = getFloatValue(txtParams[0][0]);
		double a = ac; //ac*0.9;
		double cmin = (0-off[CHANNEL])/scale[CHANNEL];
		double cmax = (256-off[CHANNEL])/scale[CHANNEL];
		double cmed = (128-off[CHANNEL])/scale[CHANNEL];
		float m = (float)ac; //(0-off[RATIO])/scale[RATIO];
		float b, e;
		int cm = 0;
		m = 0;
		for(int k=0; k<nb; k++) {
			if (valueChannel[k] < cmed) {
				m += valueRatio[k];
				cm++;
			}
		}
		if (cm > 2)
			m = m / cm;
		else
			m = (float)ac;

		int ck = 0;
		for(int k=0; k<nb; k++)
			if (valueChannel[k] > cmed)
			if (valueRatio[k] - m > 0)
				ck++;
				
		double channel[] = new double[ck];
		double ratio[] = new double[ck];
		ck = 0;
		for(int k=0; k<nb; k++) {
			if (valueChannel[k] > cmed)
			if (valueRatio[k] - m > 0) {
				channel[ck] = valueChannel[k];
				ratio[ck] = Math.log(valueRatio[k]-m);
				ck++;
			}
		}
		if (ck < 2) {
			txtParams[0][2].setText("No fit");
			txtParams[1][2].setText("No fit");
			txtParams[2][2].setText("No fit");
			fit[2] = false;
			return;
		}
		//System.out.println(" >>>>>> cmin : " + cmin + " m: " + m + " ck : " + ck);

		/***/
		float sumx = 0f;
		float sumy = 0f;
		float sumxx = 0f;
		float sumxy = 0f;

		for(int k=0; k<ck; k++) {
			sumx  += channel[k];
			sumxx += channel[k]*channel[k];
			sumy  += ratio[k];
			sumxy += ratio[k]*channel[k];
		}
		//System.out.println(" sumxx : " + sumxx);
		//System.out.println(" sumx : " + sumx);
		//System.out.println(" sumy : " + sumy);
		//System.out.println(" sumxy : " + sumxy);

		float et = (float) ((ck*sumxy - sumx*sumy) / (ck * sumxx - sumx * sumx));
		float bt = (float) (Math.exp((sumy - et * sumx) / ck));
		
		a = (float)m;
		b = (float)bt;
		e = (float)et;
		//System.out.println(" REAL a : " + a + " b " + b + " " +" e: " + e);
		
		float u = scale[CHANNEL];
		float v = off[CHANNEL];
		float w = scale[RATIO];
		float z = off[RATIO];
		
		float valueA = w * (float)(m+z/w);
		float valueB = w * (float)(bt*Math.exp(-et*v/u));
		float valueE = et / u;
		params[0][2] = (float)valueA;
		params[1][2] = (float)valueB;
		params[2][2] = (float)valueE;
		//System.out.println(" DISPLAY  a : " + params[0][2] + " b " + params[1][2] + " " +" e: " + params[2][2] );

		//System.out.println( " cmax " + cmax + " Real: " + (a + b * Math.exp(cmax*e)) + " 256: " + (valueA + valueB * Math.exp(2566*valueE)));

		txtParams[0][2].setText("" + IJ.d2s( a ,5));
		txtParams[1][2].setText("" + IJ.d2s( b,5));
		txtParams[2][2].setText("" + IJ.d2s( e,5));
		fit[2] = true;
		
	}
	
	/**
	* Overload the paint method.
	*/
	public void paint(Graphics g) {
		g.setColor(this.getBackground());
		Dimension dimPlot = getSize();
		g.fillRect(0, 0, dimPlot.width, dimPlot.height);
		Dimension dim = this.getSize();

		if (image == null || drawScatteredPlot==false) {
			g.setColor(new Color(0, 0, 128));
			g.drawString("Step 1: Background Determination", 10, 60);
			g.drawString("Define a ROI and click \"Get\"", 20, 85);
			g.drawString("or choose values and click \"Set\",", 20, 110);
			g.drawString("then click \"Accept\" to go to the step 2.", 20, 135);
	
			g.drawString("Step 2: Model Parameters Determination", 10, 170);
			g.drawString("Define a ROI and click \"Get\"", 20, 195);
			g.drawString("or choose values and click \"Set\",", 20, 220);
			g.drawString("then click \"Accept\" to go to the FRET computation.", 20, 245);

			return;
		}
		
		g.drawImage(image, LEFT, TOP, this);
		g.setColor(Color.lightGray);
		g.drawRect(LEFT, TOP, 256, 256);
		if (params != null) {
		
			// Constant
			if (fit[0]) {
				g.setColor(Color.red);
				int ycst = 255-Math.round(params[0][0]);
				g.setColor(Color.red);
				g.drawLine(LEFT, TOP+ycst, LEFT+256, TOP+ycst);
			}
			
			// Linear
			if (fit[1]) {
				float x1 = 0;
				float a = params[0][1];
				float b = params[1][1];
				float y1 = b * x1 + a;
				if (y1 < 0) {
					y1 = 0;
					x1 = (y1 - a)/b;
				}
				else if (y1 > 255) {
					y1 = 255;
					x1 = (y1 - a)/b;
				}
				
				float x2 = 255;
				float y2 = b * x2 + a;
				if (y2 < 0) {
					y2 = 0;
					x2 = (y2 - a)/b;
				}
				else if (y2 > 255) {
					y2 = 255;
					x2 = (y2 - a)/b;
				}
				g.setColor(Color.blue);
				g.drawLine(LEFT+Math.round(x1), TOP+Math.round(255-y1), LEFT+Math.round(x2), TOP+Math.round(255-y2));
			}
			
			// Expo
			if (fit[2]) {
				g.setColor(Color.green);
				float a = params[0][2];
				float b = params[1][2];
				float e = params[2][2];
				int x0 = 0;
				double y0 = b * Math.exp(0.0) + a;
				for(int x=1; x < 255; x++) {
					double ye = b * Math.exp(x*e) + a;
					if (ye < 0)
						break;
					if (ye > 255)
						break;
					g.drawLine(LEFT+x0, TOP+(int)Math.round(255-y0), LEFT+x, TOP+(int)Math.round(255-ye));
					x0 = x;
					y0 = ye;
				}
			}
		}

		g.setFont(font);
		FontMetrics metrics = g.getFontMetrics(font);
		int h = metrics.getHeight()-metrics.getAscent();
		int w = metrics.stringWidth("0.00");
		
		String s;
		g.setColor( ((!drag[0])&&(!drag[1])&&(!drag[2])&&(!drag[3])) ? colorArea: colorSelected);
		g.fillRect(LEFT+hotspot[0].x-3, TOP+hotspot[0].y-3, 6, 6);
		g.fillRect(LEFT+hotspot[1].x-3, TOP+hotspot[1].y-3, 6, 6);
		g.fillRect(LEFT+hotspot[2].x-3, TOP+hotspot[2].y-3, 6, 6);
		g.fillRect(LEFT+hotspot[3].x-3, TOP+hotspot[3].y-3, 6, 6);
					
		g.drawLine(LEFT+hotspot[0].x, TOP+hotspot[0].y, LEFT+hotspot[0].x, TOP+hotspot[2].y);
		g.drawLine(LEFT+hotspot[2].x, TOP+hotspot[0].y, LEFT+hotspot[2].x, TOP+hotspot[2].y);
		g.drawLine(LEFT+hotspot[0].x, TOP+hotspot[0].y, LEFT+hotspot[2].x, TOP+hotspot[0].y);
		g.drawLine(LEFT+hotspot[0].x, TOP+hotspot[2].y, LEFT+hotspot[2].x, TOP+hotspot[2].y);
	
		g.setColor(Color.black);
		
		// Channel Scale

		g.setColor(Color.black);
		for(int i=0; i<=256; i+=64) {
			float v = (i-off[CHANNEL])/scale[CHANNEL]; 		
			s = IJ.d2s(v,0);
			w = metrics.stringWidth(s);
			g.drawString(s, LEFT+i-w/2, TOP+272); 
			g.drawLine(LEFT+i, TOP+256, LEFT+i, TOP+260);
		}
		g.setColor(Color.gray);
		g.setColor(Color.black);
		g.setColor(Color.black);
		
		// Ratio Scale
		for(int i=0; i<=256; i+=64) {
			s = IJ.d2s((i-off[RATIO])/scale[RATIO], 3);
			w = metrics.stringWidth(s);
			g.drawString(s, LEFT-w-6, TOP+(255-i)+h/2+3); 
			g.drawLine(LEFT-1, TOP+(255-i), LEFT-4, TOP+(255-i));
		}
		
		// Channel min/max
		float hx1 = (hotspot[0].x-off[CHANNEL]) / scale[CHANNEL];
		float hx2 = (hotspot[2].x-off[CHANNEL]) /  scale[CHANNEL]; 
		float hy1 = ((by-hotspot[2].y)-off[RATIO]) / scale[RATIO];
		float hy2 = ((by-hotspot[0].y)-off[RATIO]) / scale[RATIO];

		s = IJ.d2s(hx1,2);
		w = metrics.stringWidth(s);
		g.drawString(s, LEFT+hotspot[0].x-w/2, TOP-7); 
		g.drawLine(LEFT+hotspot[0].x, TOP, LEFT+hotspot[0].x, TOP-4);
		
		s = IJ.d2s(hx2,2);
		w = metrics.stringWidth(s);
		g.drawString(s, LEFT+hotspot[2].x-w/2, TOP-7); 
		g.drawLine(LEFT+hotspot[2].x, TOP, LEFT+hotspot[2].x, TOP-4);
		
		// Ratio min/max
		s = IJ.d2s(hy2,3);
		g.drawString(s, LEFT+261, TOP+hotspot[0].y+h/2+3);
		 
		s = IJ.d2s(hy1,3);
		g.drawString(s, LEFT+261, TOP+hotspot[2].y+h/2+3); 
		
		g.drawLine(LEFT+256, TOP+hotspot[0].y, LEFT+259, TOP+hotspot[0].y);
		g.drawLine(LEFT+256, TOP+hotspot[2].y, LEFT+259, TOP+hotspot[2].y);

		// Labels
		s = dlg.getChannelName() + " Intensity";	w = metrics.stringWidth(s);	g.drawString(s, LEFT+128-w/2, TOP+256+30); 
		
		g.setColor(Color.lightGray);
		g.setColor(colorArea);
		d(g, 3,0,5,0); d(g, 5,0,8,3); d(g, 8,3,8,5); d(g, 8,5,7,6); d(g, 7,6,7,7); d(g, 7,7,6,7);
		d(g, 5,8,3,8); d(g, 3,8,0,5); d(g, 0,5,0,3); d(g, 0,3,3,0); /*d(g, 3,0,8,8);*/
		d(g, 9,8,13,12); d(g, 13,12,13,13); d(g, 13,13,12,13); d(g, 12,13,8,9); d(g, 8,9,8,8);
	
	}

	private void d(Graphics g, int x1, int y1, int x2, int y2) {
		g.drawLine(LEFT+240+x1, TOP+4+y1, LEFT+240+x2, TOP+4+y2);
	}

	
	private void update() {
		fitConstant();
		fitLinear();
		fitExpo();
		repaint();
	}
	
	public void mouseClicked(MouseEvent e) 	{}
	public void mouseExited(MouseEvent e) 	{}
	public void mouseEntered(MouseEvent e) 	{}
	public void mouseMoved(MouseEvent e) 	{}
	public void mousePressed(MouseEvent e) {
		int x = e.getX()-LEFT;
		int y = e.getY()-TOP;
		for(int i=0; i<4; i++)
			if ((hotspot[i].x-x)*(hotspot[i].x-x) + (hotspot[i].y-y)*(hotspot[i].y-y) < 20) {
				drag[i] = true;
			}
		
		if ( x < 256 && x > 240)
		if ( y < 16 && y > 0) {
			createImage();
			update();
		}
		
	}

	public void mouseReleased(MouseEvent e) {
		for(int i=0; i<4; i++)
			drag[i] = false;
	}

	public void mouseDragged(MouseEvent e) {
		int x = e.getX()-LEFT;
		int y = e.getY()-TOP;
		if (x < 0)
			x = 0;
		if (y <0)
			y = 0;
		if (y > 255)
			y = 255;
		if (x > 255)
			x = 255;
		if (drag[0] == true) {
			if (x < hotspot[2].x) {
				hotspot[0].x = x;
				hotspot[1].x = x;
			}
			if (y < hotspot[2].y) {
				hotspot[0].y = y;
				hotspot[3].y = y;
			}
			
		}
		else if (drag[1] == true) {
			if (x < hotspot[2].x) {
				hotspot[0].x = x;
				hotspot[1].x = x;
			}
			if (y > hotspot[0].y) {
				hotspot[1].y = y;
				hotspot[2].y = y;
			}
			
		}
		else if (drag[2] == true) {
			if (x > hotspot[0].x) {
				hotspot[2].x = x;
				hotspot[3].x = x;
			}
			if (y > hotspot[0].y) {
				hotspot[2].y = y;	
				hotspot[1].y = y;
			}
		}
		else if (drag[3] == true) {
			if (x > hotspot[0].x) {
				hotspot[2].x = x;
				hotspot[3].x = x;
			}
			if (y < hotspot[2].y) {
				hotspot[3].y = y;	
				hotspot[0].y = y;
			}
		}
		repaint();
	
	}

	/**
	* Get a double value from a JTextField.
	*/
	private float getFloatValue(JTextField text) {
		float d;
		try {
			d = (new Float(text.getText())).floatValue();
			text.setText("" + d);
		}
		
		catch (Exception e) {
			d = 0;
			text.setText("0.0");
		}
		return d;
	}

}
