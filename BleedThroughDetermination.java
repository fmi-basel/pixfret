package pixfret;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;

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


public class BleedThroughDetermination  extends JDialog implements ActionListener, WindowListener {

	private JButton bn;
	private JComboBox choiceModel;
	private JPanel  pnParameters[];
	private JPanel  pnFormula[];
	private int channel;
	private JTextField txtLocal[][] 		= new JTextField[3][3];
	private JTextField txtParams[][][] 		= new JTextField[3][2][3];

	private JTextField txtBackFret 			= new JTextField("0", 5);
	private JTextField txtBackChannel		= new JTextField("0", 5);
	
	private GridBagLayout 		layout		= new GridBagLayout();
	private GridBagConstraints 	constraint	= new GridBagConstraints();
	
	private JButton	bnCancel	 			= new JButton("Cancel");
	private JButton	bnSetModel				= new JButton("Set");
	private JButton	bnGetModel				= new JButton("Get");
	private JButton	bnResetModel			= new JButton("Reset");
	private JButton	bnGetBack				= new JButton("Get");
	private JButton	bnSetBack				= new JButton("Set");
	private JButton	bnResetBack				= new JButton("Reset");
	private JButton	bnAccept				= new JButton("Accept");
	private float params[][] 				= new float[3][3];

	private	int bleed[][];

	private String[] paramName				= {"a", "b", "e"};
	private String[] modelName				= {"Constant", "Linear", "Expo."};
	
	private Plot plot;
	private	int nbins = 256;
	
	private int count = 0;
	private int min[]	= new int[2];
	private int max[] 	= new int[2];
	
//	private JCheckBox chkModel[]			= new JCheckBox[3];
//	private JCheckBoxGroup grpModel			= new JCheckBoxGroup();
	
	private float resRatio	 = 1.0f;
	private float resChannel = 1.0f;
	private boolean data = false;
	private boolean fit[] = {false, false, false};

	private int countBack = 0;
	
	private float backgroundFret=0, backgroundChannel=0;
	
	/**
	* Constructor
	*/
	public BleedThroughDetermination(JComboBox choiceModel, JPanel pnParameters[], JPanel  pnFormula[], JButton bn, int channel, int model, JTextField[][][] txtParams) {
		super(new Frame(), "Bleed-through Determination [" + (channel == 0 ? "Donor" : "Acceptor") + "]");
		bn.setEnabled(false);
		this.choiceModel 	= choiceModel;
		this.channel 		= channel;
		this.pnParameters 	= pnParameters;
		this.pnFormula 		= pnFormula;
		this.txtParams		= txtParams;
		this.bn 		= bn;
		bleed 			= new int[nbins][nbins];
		max[0] 			= 255;
		max[1]			= 255;
		
		// JPanel Back
		JPanel pnBackParam = new JPanel();
		pnBackParam.setLayout(layout);
		addComponent(pnBackParam, 0, 0, 1, 1, 4, new JLabel("FRET", JLabel.RIGHT));
		addComponent(pnBackParam, 0, 1, 1, 1, 4, txtBackFret);
		addComponent(pnBackParam, 0, 2, 1, 1, 4, new JLabel(getChannel(), JLabel.RIGHT));
		addComponent(pnBackParam, 0, 3, 1, 1, 4, txtBackChannel);

		// JPanel Back
		JPanel pnBack = new JPanel();
		pnBack.setLayout(layout);
		addComponent(pnBack, 0, 0, 1, 1, 4, new JLabel("Background Values"));
		addComponent(pnBack, 0, 1, 1, 1, 4, bnResetBack);
		addComponent(pnBack, 0, 2, 1, 1, 4, bnSetBack);
		addComponent(pnBack, 0, 3, 1, 1, 4, bnGetBack);
		
		// JPanel Parameters
		JPanel pnParams = new JPanel();
		pnParams.setLayout(layout);
		for (int m=0; m<3; m++) {
//			chkModel[m] = new Checkbox(modelName[m], grpModel, (m==model));
//			addComponent(pnParams, m, 0, 1, 1, 4, chkModel[m]);
			for (int p=0; p<3; p++) {
				txtLocal[p][m] = new JTextField("---", 7);
				//txtLocal[p][m].setBackground(Color.lightGray);
				//txtLocal[p][m].setEditable(false);
			}
			for (int p=0; p<m+1; p++) {
				addComponent(pnParams, m, 1+p*2, 1, 1, 4, new JLabel(paramName[p], JLabel.RIGHT));
				addComponent(pnParams, m, 2+p*2, 1, 1, 4, txtLocal[p][m]);
			}
		}
		// JPanel Model
		JPanel pnModel = new JPanel();
		pnModel.setLayout(layout);
		addComponent(pnModel, 0, 1, 1, 1, 4, new JLabel("Model Parameters"));
		addComponent(pnModel, 0, 2, 1, 1, 4, bnResetModel);
		//addComponent(pnModel, 0, 3, 1, 1, 4, bnSetModel);
		addComponent(pnModel, 0, 4, 1, 1, 4, bnGetModel);
		
		// JPanel
		JPanel panels = new JPanel();
		panels.setLayout(layout);
		addComponent(panels, 0, 1, 1, 1, 4, pnBack);
		addComponent(panels, 1, 1, 1, 1, 4, pnBackParam);
		
		addComponent(panels, 3, 1, 1, 1, 4, pnModel);
		addComponent(panels, 4, 1, 1, 1, 4, pnParams);

		// Plot
		//plot = new Plot(nbins, this);
			
		// JPanel Buttons
		JPanel pnButtons = new JPanel();
		pnButtons.setLayout(layout);
		addComponent(pnButtons, 0, 0, 1, 1, 8, bnCancel);
		addComponent(pnButtons, 0, 4, 1, 1, 8, bnAccept);

		// JPanel Main
		JPanel pnMain = new JPanel();
		pnMain.setLayout(new BorderLayout());
		pnMain.add(plot, BorderLayout.NORTH);
		pnMain.add(panels, BorderLayout.CENTER);
		pnMain.add(pnButtons, BorderLayout.SOUTH);
		
		// Add Listeners
		bnCancel.addActionListener(this);
		bnGetBack.addActionListener(this);
		bnSetBack.addActionListener(this);
		bnResetBack.addActionListener(this);
		bnGetModel.addActionListener(this);
		bnSetModel.addActionListener(this);
		bnResetModel.addActionListener(this);
		bnAccept.addActionListener(this);
		addWindowListener(this);
		
		// Building the main JPanel
		this.getContentPane().add(pnMain);
		pack();
		GUI.center(this);
		
		setVisible(true);
		IJ.wait(250); 	// work around for Sun/WinNT bug
		
	}

	public String getChannel() {
		if (channel == 0)
			return "Donor";
		else
			return "Acceptor";
	}
	
	/**
	 * Add a component in a JPanel in the northeast of the cell.
	 */
	private void addComponent(JPanel pn, int row, int col, int width, int height, int space, JComponent comp) {
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = width;
		constraint.gridheight = height;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(space, space, space, space);
		constraint.weightx = IJ.isMacintosh()?90:100;
		constraint.fill = constraint.HORIZONTAL;
		layout.setConstraints(comp, constraint);
		pn.add(comp);
	}

	/**
	 * Implements the actionPerformed for the ActionListener.
	 */
	public synchronized  void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnCancel) {
			bn.setEnabled(true);
			dispose();
		}
		else if (e.getSource() == bnResetModel) {
			resetModel();	
		}
		else if (e.getSource() == bnGetModel) {
			measure();
		}
		else if (e.getSource() == bnResetBack) {
			txtBackChannel.setText("0");
			txtBackFret.setText("0");
			countBack = 0;
			backgroundChannel = getFloatValue(txtBackChannel);
			backgroundFret    = getFloatValue(txtBackFret);
			resetModel();	
		}
		else if (e.getSource() == bnSetBack) {
			countBack = 0;
			backgroundChannel = getFloatValue(txtBackChannel);
			backgroundFret    = getFloatValue(txtBackFret);
			resetModel();	
		}
		else if (e.getSource() == bnGetBack) {
			background();
		}
		else if (e.getSource() == bnAccept) {
			for(int m=0; m<3; m++)
			for(int p=0; p<m+1; p++) {
				float value = getFloatValue(txtLocal[p][m]);
				txtParams[p][channel][m].setText("" + value);
			}
			int index = 0;
/*			if (chkModel[0].getState())
				index = 0;
			else if (chkModel[1].getState())
				index = 1;
			else if (chkModel[2].getState())
				index = 2;
			choiceModel.select(index);
			for(int m=0; m<3; m++) {
				pnParameters[m].setVisible(index==m);
				pnFormula[m].setVisible(index==m);
			}
*/			
			bn.setEnabled(true);
			dispose();
		}
	}

	/**
	* Reset the model parameters.
	*/
	private void resetModel() {
		max[0] 			= 255;
		max[1]			= 255;
		min[0] 			= 0;
		min[1]			= 0;
		resRatio		= 1.0f;
		for(int y = 0; y < nbins; y++)
		for(int x = 0; x < nbins; x++) {
			bleed[x][y] = 0;
		}
		for(int i = 0; i < 3; i++)
		for(int m = 0; m < 3; m++) {
			txtLocal[i][m].setBackground(Color.lightGray);
			txtLocal[i][m].setText("---");
		}
		fit[0] = false;
		fit[1] = false;
		fit[2] = false;
		
		plot.set(bleed, min, max, params, resRatio, resChannel, fit);
		update(min, max);
	
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
	
	/**
	* Measure the background level in the ROI.
	*/
	private void background() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Please open a stack of images");
			return;
		}
		if (imp.getStackSize() != 2) {
			IJ.showMessage("The input stack size should be equal to 2.");
			return;
		}
		int type = imp.getType();
		if (type != ImagePlus.GRAY32 && type != ImagePlus.GRAY16 && type != ImagePlus.GRAY8) {
			IJ.showMessage("32-bits or 16-bits or 8-bits image is required.");
			return;
		}
		Roi roi = imp.getRoi();
		if (roi == null) {
			IJ.showMessage("Please draw a ROI in the stack to estimate the background.");
			return;
		}

		ImageProcessor maskRoi = roi.getMask();
		Rectangle rect = roi.getBoundingRect();
		PixFretImageAccess fret 		= new PixFretImageAccess(imp.getStack().getProcessor(1));
		PixFretImageAccess channel 		= new PixFretImageAccess(imp.getStack().getProcessor(2));
		
		float bFret 		= 0.0f;
		float bChannel 		= 0.0f;
		int bCount = 0;
		
		if (maskRoi != null) {
			PixFretImageAccess mask = new PixFretImageAccess(maskRoi);
			int nx = mask.getWidth();
			int ny = mask.getHeight();
		
			for(int y = 0; y < ny; y++)
			for(int x = 0; x < nx; x++) {
				if (maskRoi.getPixel(x, y) != 0.0) {
					bFret 		+= fret.getPixel(rect.x+x, rect.y+y);
					bChannel	+= channel.getPixel(rect.x+x, rect.y+y);
					bCount++;
				}
			}
		}
		else {
			for(int y = 0; y < rect.height; y++)
			for(int x = 0; x < rect.width; x++) {
				bFret 		+= fret.getPixel(rect.x+x, rect.y+y);
				bChannel	+= channel.getPixel(rect.x+x, rect.y+y);
				bCount++;
			}		
		}	
		if (bCount > 0) {
			bFret 		= bFret 	/ bCount;		
			bChannel	= bChannel 	/ bCount;		
		}
		
		backgroundFret		= (backgroundFret*countBack + bFret*bCount)/(countBack+bCount);
		backgroundChannel 	= (backgroundChannel*countBack + bChannel*bCount)/(countBack+bCount);
		countBack += bCount;
		txtBackFret.setText("" + IJ.d2s(backgroundFret));
		txtBackChannel.setText("" + IJ.d2s(backgroundChannel));
		resetModel();
	}

	/**
	* Measure the bleed-through level in the ROI.
	*/
	public boolean measure() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Please open a stack of 2 images ");
			return false;
		}
		if (imp.getStackSize() != 2) {
			IJ.showMessage("The input stack size should be equal to 2.");
			return false;
		}
		int type = imp.getType();
		if (type != ImagePlus.GRAY32 && type != ImagePlus.GRAY16 && type != ImagePlus.GRAY8) {
			IJ.showMessage("32-bits or 16-bits or 8-bits image is required.");
			return false;
		}
		Roi roi = imp.getRoi();
		if (roi == null) {
			IJ.showMessage("Please draw a ROI in the stack to estimate the contamination.");
			return false;
		}
		
		ImageProcessor maskRoi = roi.getMask();
		Rectangle rect 		= roi.getBoundingRect();
		PixFretImageAccess sbt  	= new PixFretImageAccess(imp.getStack().getProcessor(1));
		PixFretImageAccess channel = new PixFretImageAccess(imp.getStack().getProcessor(2));
		
		int abs, ord=0;
		float ratio = 0f;
		float ratioMax   = -Float.MAX_VALUE;
		float channelMax = -Float.MAX_VALUE;
		resRatio = 1.0f;
		if (maskRoi != null) {
			PixFretImageAccess mask = new PixFretImageAccess(maskRoi);
			int nx = mask.getWidth();
			int ny = mask.getHeight();
			for(int y = 0; y < ny; y++)
			for(int x = 0; x < nx; x++) {
				if (maskRoi.getPixel(x, y) != 0.0) {
					abs = Math.round(channel.getPixel(x, y)-backgroundChannel);
					if (abs > channelMax)
						channelMax = abs;
					if (abs > 0) {
						ratio =  (sbt.getPixel(x, y)-backgroundFret) / (channel.getPixel(x, y)-backgroundChannel);
						if (ratio > ratioMax)
							ratioMax = ratio;
					}
				}
			}
			resRatio   = (ratioMax > 0 ? 255f / ratioMax : 1.0f); 
			resChannel = (channelMax > 0 ? 255f / channelMax : 1.0f); 
			for(int y = 0; y < ny; y++)
			for(int x = 0; x < nx; x++) {
				if (maskRoi.getPixel(x, y) != 0.0) {
					abs = Math.round(resChannel *(channel.pixels[x +nx*y]-backgroundChannel));
					if (abs > 0) 
						ord = Math.round(resRatio * (sbt.getPixel(x, y)-backgroundFret) / (channel.getPixel(x, y)-backgroundChannel));
					abs = (abs < 0 ? 0 : (abs > 255 ? 255 : abs));
					ord = (ord < 0 ? 0 : (ord > 255 ? 255 : ord));
					bleed[abs][255-ord]++;
				}
			}
		}
		else {
			for(int y = 0; y < rect.height; y++)
			for(int x = 0; x < rect.width; x++) {
					abs = Math.round(channel.getPixel(x, y)-backgroundChannel);
					if (abs > channelMax)
						channelMax = abs;
					if (abs > 0) {
						ratio =  (sbt.getPixel(x, y)-backgroundFret) / (channel.getPixel(x, y)-backgroundChannel);
						if (ratio > ratioMax)
							ratioMax = ratio;
					}
			}		
			resRatio = (ratioMax > 0 ? 255f / ratioMax : 1.0f); 
			resChannel = (channelMax > 0 ? 255f / channelMax : 1.0f); 
			for(int y = 0; y < rect.height; y++)
			for(int x = 0; x < rect.width; x++) {
					abs = Math.round(resChannel * (channel.getPixel(x, y)-backgroundChannel));
					if (abs > 0) 
						ord = Math.round(resRatio * (sbt.getPixel(x, y)-backgroundFret) / (channel.getPixel(x, y)-backgroundChannel));
					abs = (abs < 0 ? 0 : (abs > 255 ? 255 : abs));
					ord = (ord < 0 ? 0 : (ord > 255 ? 255 : ord));
					bleed[abs][255-ord]++;
			}
		}
		
		min[0] =  Integer.MAX_VALUE;
		max[0] = -Integer.MAX_VALUE;
		min[1] =  Integer.MAX_VALUE;
		max[1] = -Integer.MAX_VALUE;
		for(ord = 0; ord < nbins; ord++)
		for(abs = 0; abs < nbins; abs++) {
			if (bleed[abs][ord] > 0) {
				if (abs < min[0])
					min[0] = abs;
				else if (abs > max[0])
					max[0] = abs;
				if (ord < min[1])
					min[1] = ord;
				else if (ord > max[1])
					max[1] = ord;
			}					
		}
System.out.println(" min/max fin de bleed" + min[0] + " " + min[1] + " " + max[0] + " " + max[1]);
System.out.println(" res fin de bleed" + resRatio + " " + resChannel);
		
		data = true;
		fit[0] = false;
		fit[1] = false;
		fit[2] = false;
		
		fitConstant();
		fitLinear();
		fitExpo();
					
		plot.set(bleed, min, max, params, resRatio, resChannel, fit);
		return true;
		
	}
	
	/**
	* Rescale the bleed array.
	*/
	public void rescaleBleed(int[] minNew, int[] maxNew) {
		float stepScaleChannel = maxNew[0] / this.max[0];
		float stepScaleRatio   = maxNew[1] / this.max[1];
System.out.println(" BEFORE max in rescale " + maxNew[0] + " " + maxNew[1]);
System.out.println(" BEFORE res in rescale " + resRatio + " " + resChannel);
		min[0] = 0;
		min[1] = 0;
		this.min = min;
		this.max = maxNew;
		resRatio = (maxNew[1] > 0 ? 255f / maxNew[1] : 1.0f); 
		resChannel = (maxNew[0] > 0 ? 255f / maxNew[0] : 1.0f); 
System.out.println(" max in rescale " + maxNew[0] + " " + maxNew[1]);
System.out.println(" res in rescale " + resRatio + " " + resChannel);
		
		int[][] bleedRescale = new int[nbins][nbins];
		int is[] = new int[nbins];
		int js[] = new int[nbins];
		 
		for (int i=0; i<nbins; i++) {
			int k = Math.round(i*stepScaleChannel);
			is[i] = (k <= 0 ? 0 : (k >nbins ? nbins-1 : k)); 
		}
		for (int j=0; j<nbins; j++) {
			int k = Math.round(j*stepScaleRatio);
			js[j] = (k <= 0 ? 0 : (k >nbins ? nbins-1 : k)); 
		}
		
		for (int i=0; i<nbins; i++)
		for (int j=0; j<nbins; j++)
			 bleedRescale[i][j] = bleed[is[i]][js[j]];
				
		for (int i=0; i<nbins; i++)
		for (int j=0; j<nbins; j++)
			 bleed[i][j] = bleedRescale[i][j];
				
		data = true;
		fit[0] = false;
		fit[1] = false;
		fit[2] = false;
		
		fitConstant();
		fitLinear();
		fitExpo();
					
		plot.set(bleed, min, max, params, resRatio, resChannel, fit);
	}
		
	/**
	* Fit the constant model.
	*/
	private void fitConstant() {
		if (data == false) {
			txtLocal[0][0].setText("No fit");
			return;
		}
		float sumy = 0f;
		float sumb = 0f;
		float fbleed;
		
		min[0] = (min[0] < 0 ? 0 : min[0]);
		min[1] = (min[1] < 0 ? 0 : min[1]);
		max[0] = (max[0] > nbins ? nbins-1 : max[0]);
		max[1] = (max[1] > nbins ? nbins-1 : max[1]);
//PixFretImageAccess pim = new PixFretImageAccess(nbins, nbins);

		for(int y=min[1]; y<max[1]; y++)
		for(int x=min[0]; x<max[0]; x++) {
//pim.putPixel(x, y, 100); 
			if (bleed[x][y] >= 1) {
				sumy += y;
				sumb ++;
//pim.putPixel(x, y, 255); 
			}
		}
//pim.show("pim");
		if (sumb > 0) {
			float cst = (255-sumy/sumb)/resRatio;
			txtLocal[0][0].setText("" + IJ.d2s(cst,5));
			params[0][0] = cst;
			fit[0] = true;
		}
		else {
			txtLocal[0][0].setText("No fit");
			fit[0] = false;
		}
		
	}

	/**
	* Fit the linear model.
	*/
	private void fitLinear() {
		if (data == false || fit[0] == false) {
			txtLocal[0][1].setText("No fit");
			txtLocal[1][1].setText("No fit");
			return;
		}
		float sumx = 0f;
		float sumy = 0f;
		float sumxx = 0f;
		float sumxy = 0f;
		float sumb = 0f;
		float fbleed;
		min[0] = (min[0] < 0 ? 0 : min[0]);
		min[1] = (min[1] < 0 ? 0 : min[1]);
		max[0] = (max[0] > nbins ? nbins-1 : max[0]);
		max[1] = (max[1] > nbins ? nbins-1 : max[1]);
	
		for(int y=min[1]; y<max[1]; y++)
		for(int x=min[0]; x<max[0]; x++) {
			if (bleed[x][y] >= 1) {
				sumx  += x;
				sumy  += y;
				sumxx += x * x;
				sumxy += x * y;
				sumb++;
			}
		}
		if (sumb > 0) {
			float slope = (sumb*sumxy - sumx*sumy) / (sumb * sumxx - sumx * sumx);
			if (Math.abs(slope) > 0.00001) {
				params[1][1] = slope;
				params[0][1] = ((sumy - params[1][1] * sumx) /sumb);
				params[0][1] = (255 - params[0][1])/resRatio;
				params[1][1] = -params[1][1] / resRatio;
				
				for(int i=0; i<2; i++) {
					txtLocal[i][1].setText("" + IJ.d2s(params[i][1],5));
				}
				fit[1] = true;
				return;
			}
		}
		
		for(int i=0; i<2; i++) {
			txtLocal[i][1].setText("No fit");
		}
		fit[1] = false;
		
	}

	/**
	* Fit the Exponential model.
	*/
	private void fitExpo() {
		if (data == false || fit[1] == false) {
			txtLocal[0][2].setText("No fit");
			txtLocal[1][2].setText("No fit");
			txtLocal[2][2].setText("No fit");
			return;
		}
		
		min[0] = (min[0] < 0 ? 0 : min[0]);
		min[1] = (min[1] < 0 ? 0 : min[1]);
		max[0] = (max[0] > nbins ? nbins-1 : max[0]);
		max[1] = (max[1] > nbins ? nbins-1 : max[1]);
		int count = 0;
		for(int y=min[1]; y<max[1]; y++)
		for(int x=min[0]; x<max[0]; x++) {
			if (bleed[x][y] >= 1) 
				count++;
		}

		double xpos[] = new double[count];
		double ypos[] = new double[count];
		double sigma[] = new double[count];
		count = 0;
		for(int y=min[1]; y<max[1]; y++)
		for(int x=min[0]; x<max[0]; x++) {
			if (bleed[x][y] >= 1) { 
				xpos[count] = x;
				ypos[count] = y;
				count++;
			}
		}
		for(int k=0; k<count; k++) {
			sigma[k] = 1.0;
		}
		
		double expoParams[] = new double[3];
		expoParams[0] = min[1];				// constant, a
		expoParams[1] = (max[1]-min[1]);	// amplitude, b
		expoParams[2] = .02;				// constant in expo, e

		boolean expoFit[]	= {true, true, true};
		int itmax = 100;
		double lambda = 0.001;
		Function f = new Function();
		double chi = LevenbergMarquardt.mrqMin(xpos, ypos, sigma, expoParams, expoFit, f, lambda, itmax);

		for(int i=0; i<3; i++)
			params[i][2] = (float)expoParams[i]; 	

		params[0][2] /= resRatio;
		params[1][2] /= resRatio;

		if (chi < 10e6) {
			for(int i=0; i<3; i++) {
				txtLocal[i][2].setText("" + IJ.d2s(params[i][2],5));
			}
			fit[2] = true;
		}
		else {
			for(int i=0; i<3; i++) {
				txtLocal[i][2].setText("No fit" + IJ.d2s(params[i][2],5));
			}
			fit[2] = false;
		}
	
	}
	
	/**
	*/
	public void update(int min[], int max[]) {
		this.min = min;
		this.max = max;
		fitConstant();
		fitLinear();
		fitExpo();
	}
	
	/**
	* Implements the methods for the WindowListener.
	*/
	public void windowActivated(WindowEvent e) 		{}
	public void windowClosing(WindowEvent e) 		{ bn.setEnabled(true); dispose(); }
	public void windowClosed(WindowEvent e) 		{}
	public void windowDeactivated(WindowEvent e) 	{}
	public void windowDeiconified(WindowEvent e)	{}
	public void windowIconified(WindowEvent e)		{}
	public void windowOpened(WindowEvent e)			{}
	
	/**
	* Get a double value from a JTextField.
	*/
	private int getIntegerValue(JTextField text) {
		int d;
		try {
			d = (new Integer(text.getText())).intValue();
			text.setText("" + d);
		}
		
		catch (Exception e) {
			d = 0;
			text.setText("0");
		}
		return d;
	}


}
