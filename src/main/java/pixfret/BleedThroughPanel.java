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

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;

import ij.text.*;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.swing.table.*;
import java.io.*;

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


public class BleedThroughPanel  extends JPanel implements ActionListener {

	private final int 	DONOR	= 0;
	private final int	ACCEP	= 1;
	
	private final int	CST		= 0;		// Constant Model
	private final int	LIN		= 1;		// Linear Model
	private final int 	EXP 	= 2;		// Exponential Model

	private final int	A		= 0;		// Constant Model
	private final int	B		= 1;		// Linear Model
	private final int 	E 		= 2;		// Exponential Model

	private JPanel  pnParameters[];
	private JPanel  pnFormula[];
	private int channel;
	private JTextField txtParams[][] 		= new JTextField[3][3];
	private JLabel	   lblParams[][] 		= new JLabel[3][3];

	private JTextField txtBackFret 			= new JTextField("0", 5);
	private JTextField txtBackChannel		= new JTextField("0", 5);
	private JTextField txtSmooth			= new JTextField("2.0", 5);

	private JLabel lblGaussianBlur 			= new JLabel("Gaussian Blur");
	private JLabel lblRecommended			= new JLabel("(2.0 recommended)");
	
	private GridBagLayout 		layout		= new GridBagLayout();
	private GridBagConstraints 	constraint	= new GridBagConstraints();
	
	private JButton	bnGetModel				= new JButton("Get");
	private JButton	bnResetModel			= new JButton("Reset");
	private JButton	bnAcceptModel			= new JButton("Accept");
	private JButton	bnGetBack				= new JButton("Get");
	private JButton	bnResetBack				= new JButton("Reset");
	private JButton	bnAcceptBack			= new JButton("Accept");
	private float params[][] 				= new float[3][3];

	private String[] paramName				= {"a", "b", "e"};
	private String[] modelName				= {"Constant", "Linear", "Expo."};
	
	private Plot plot;
	private	int nbins = 256;
	
	private JRadioButton chkModel[]			= new JRadioButton[3];
	
	private boolean data = false;
	private boolean fit[] = {false, false, false};

	private int countBack = 0;
	
	private float backgroundFret=0, backgroundChannel=0;
	
	private Properties props;
	private String filename;

	private JLabel lblFormula[];
	private JTabbedPane tabbedPane;

	private	float valueChannel[];
	private	float valueRatio[];
	
	/**
	* Constructor
	*/
	public BleedThroughPanel(int channel, Properties props, String filename) {
		super();
		this.channel 	= channel;
		this.props		= props;
		this.filename	= filename;
		
		// JPanel Back
		JPanel pnBackParam = new JPanel();
		pnBackParam.setLayout(layout);
		addComponent(pnBackParam, 0, 0, 1, 1, 2, new JLabel("FRET", JLabel.RIGHT));
		addComponent(pnBackParam, 0, 1, 1, 1, 2, txtBackFret);
		addComponent(pnBackParam, 0, 2, 1, 1, 2, new JLabel(getChannelName(), JLabel.RIGHT));
		addComponent(pnBackParam, 0, 3, 1, 1, 2, txtBackChannel);

		JPanel pnBackButtons = new JPanel();
		pnBackButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		pnBackButtons.add(bnResetBack);
		pnBackButtons.add(bnGetBack);
		pnBackButtons.add(bnAcceptBack);
		
		JPanel pnBack = new JPanel();
		pnBack.setLayout(layout);
		addComponent(pnBack, 0, 0, 1, 1, 4, pnBackParam);
		addComponent(pnBack, 1, 0, 1, 1, 4, pnBackButtons);
		pnBack.setBorder(BorderFactory.createTitledBorder("Background " + getChannelName() ));
		
		// JPanel Model
		JPanel pnModelParams = new JPanel();
		ButtonGroup group = new ButtonGroup();
		pnModelParams.setLayout(layout);
		addComponent(pnModelParams, 0, 0, 2, 1, 1, lblGaussianBlur);
		addComponent(pnModelParams, 0, 2, 1, 1, 1, txtSmooth);
		addComponent(pnModelParams, 0, 4, 3, 1, 1, lblRecommended);
		for (int m=0; m<3; m++) {
			chkModel[m] = new JRadioButton(modelName[m]);
			group.add(chkModel[m]);
			addComponent(pnModelParams, m+1, 0, 1, 1, 1, chkModel[m]);
			for (int p=0; p<3; p++) {
				txtParams[p][m] = new JTextField("---", 7);
				lblParams[p][m] = new JLabel(paramName[p], JLabel.RIGHT);
			}
			for (int p=0; p<m+1; p++) {
				addComponent(pnModelParams, m+1, 1+p*2, 1, 1, 1, lblParams[p][m]);
				addComponent(pnModelParams, m+1, 2+p*2, 1, 1, 1, txtParams[p][m]);
			}
		}
		
		JPanel pnModelButtons = new JPanel();
		pnModelButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		pnModelButtons.add(bnResetModel);
		pnModelButtons.add(bnGetModel);
		pnModelButtons.add(bnAcceptModel);

		JPanel pnModel = new JPanel();
		pnModel.setLayout(layout);
		addComponent(pnModel, 0, 1, 1, 1, 4, pnModelParams);
		addComponent(pnModel, 1, 1, 1, 1, 4, pnModelButtons);
		pnModel.setBorder(BorderFactory.createTitledBorder("Model " + getChannelName()));
		
		// Plot
		JPanel pnPlot = new JPanel();
		
		pnPlot.setLayout(layout);
		plot = new Plot(nbins, txtParams, this);
		addComponent(pnPlot, 0, 0, 1, 1, 4, plot);
		pnPlot.setBorder(BorderFactory.createTitledBorder(getChannelName()+" SBT Model"));
			
		// JPanel Main
		setLayout(new BorderLayout());
		add(pnPlot, BorderLayout.NORTH);
		add(pnBack, BorderLayout.CENTER);
		add(pnModel, BorderLayout.SOUTH);
		
		// Add Listeners
		bnGetBack.addActionListener(this);
		bnResetBack.addActionListener(this);
		bnAcceptBack.addActionListener(this);
		bnGetModel.addActionListener(this);
		bnResetModel.addActionListener(this);
		bnAcceptModel.addActionListener(this);
		
		setEnabledModel(false);
	}

	/**
	*/
	private void setEnabledModel(boolean state) {
		bnResetModel.setEnabled(state);
		bnGetModel.setEnabled(state);
		bnAcceptModel.setEnabled(state);
		txtSmooth.setEnabled(state);
		lblGaussianBlur.setEnabled(state);
		lblRecommended.setEnabled(state);
		for (int m=0; m<3; m++) {
			chkModel[m].setEnabled(state);
			for (int p=0; p<3; p++) {
				txtParams[p][m].setEnabled(state);
				lblParams[p][m].setEnabled(state);
			}
		}
	}
	
	/**
	* Get the model parameters of this channel.
	*/
	public float[][] getParams() {
		float[][] para = new float[3][3];
		for(int m=0; m<3; m++) 
		for(int p=0; p<3; p++)
			para[p][m] = getFloatValue(txtParams[p][m]);
		return para;
	}
	
	/**
	*/
	public int getModel() {
		for(int m=0; m<3; m++)
			if (chkModel[m].isSelected())
				return m;
		return 0;
	}
	
	/**
	*/
	public void setModelAndParams(int model, float[][] params) {
		for(int m=0; m<3; m++)
			chkModel[model].setSelected((m==model));
		for(int m=0; m<3; m++) 
		for(int p=0; p<3; p++)
			txtParams[p][m].setText("" + IJ.d2s(params[p][m], 5));
	}
	
	
	/**
	*/
	public String getChannelName() {
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
	* Change the formula of the current channel in the pane 0.
	* Keep a reference of the lblFormula and tabbedPane.
	*/
	public void setParamsFormula(JLabel lblFormula[], JTabbedPane tabbedPane) {
		this.lblFormula = lblFormula;
		this.tabbedPane = tabbedPane;
		float[][] params = getParams();
		int model = getModel();
		if (channel == DONOR)
			switch(model) {
				case CST:	lblFormula[DONOR].setText("BTdon = " + IJ.d2s(params[A][CST], 5));
							break;
				case LIN:	String sign = (params[B][LIN] < 0 ? "-" : "+");
							lblFormula[DONOR].setText("BTdon = " + IJ.d2s(params[A][LIN], 5) + "* DONORdon " + sign + " " + IJ.d2s(Math.abs(params[B][LIN]), 5));
							break;
				case EXP:	lblFormula[DONOR].setText("BTdon = " + IJ.d2s(params[A][EXP], 5) + " + " + IJ.d2s(params[B][EXP], 5) +  "* exp(" + IJ.d2s(params[E][EXP], 5) + " * DONORdon)");
							break;
			}
		else 
			switch(model) {
				case CST:	lblFormula[ACCEP].setText("BTacc = " + IJ.d2s(params[A][CST], 5));
							break;
				case LIN:	String sign = (params[B][LIN] < 0 ? "-" : "+");
							lblFormula[ACCEP].setText("BTacc = " + IJ.d2s(params[A][LIN], 5) + "* ACCEPTORacc " + sign + " " + IJ.d2s(Math.abs(params[B][LIN]), 5));
							break;
				case EXP:	lblFormula[ACCEP].setText("BTacc = " + IJ.d2s(params[A][EXP], 5) + " + " + IJ.d2s(params[B][EXP], 5) +  "* exp(" + IJ.d2s(params[E][EXP], 5) + " * ACCEPTORacc)");
							break;
			}

		tabbedPane.setSelectedIndex(0);
	}

	/**
	* Change the formula of the current channel in the pane 0.
	*/
	public void setParamsFormula() {
	
		float[][] params = getParams();
		int model = getModel();
		if (channel == DONOR)
			switch(model) {
				case CST:	lblFormula[DONOR].setText("BTdon = " + IJ.d2s(params[A][CST], 5));
							break;
				case LIN:	String sign = (params[B][LIN] < 0 ? "-" : "+");
							lblFormula[DONOR].setText("BTdon = " + IJ.d2s(params[A][LIN], 5) + "* DONORdon " + sign + " " + IJ.d2s(Math.abs(params[B][LIN]), 5));
							break;
				case EXP:	lblFormula[DONOR].setText("BTdon = " + IJ.d2s(params[A][EXP], 5) + " + " + IJ.d2s(params[B][EXP], 5) +  "* exp(" + IJ.d2s(params[E][EXP], 5) + " * DONORdon)");
							break;
			}
		else 
			switch(model) {
				case CST:	lblFormula[ACCEP].setText("BTacc = " + IJ.d2s(params[A][CST], 5));
							break;
				case LIN:	String sign = (params[B][LIN] < 0 ? "-" : "+");
							lblFormula[ACCEP].setText("BTacc = " + IJ.d2s(params[A][LIN], 5) + "* ACCEPTORacc " + sign + " " + IJ.d2s(Math.abs(params[B][LIN]), 5));
							break;
				case EXP:	lblFormula[ACCEP].setText("BTacc = " + IJ.d2s(params[A][EXP], 5) + " + " + IJ.d2s(params[B][EXP], 5) +  "* exp(" + IJ.d2s(params[E][EXP], 5) + " * ACCEPTORacc)");
							break;
			}

		tabbedPane.setSelectedIndex(0);
	}

	/**
	 * Implements the actionPerformed for the ActionListener.
	 */
	public synchronized  void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnResetModel) {
			bnGetModel.setEnabled(true);
			resetModel();	
			plot.setEnabledScatteredPlot(true);
		}
		else if (e.getSource() == bnGetModel) {
			plot.setEnabledScatteredPlot(true);
			measure();
		}
		else if (e.getSource() == bnAcceptModel) {
			plot.setEnabledScatteredPlot(true);
			plot.force();
			setParamsFormula();
		}
		else if (e.getSource() == bnResetBack) {
			txtBackChannel.setText("0");
			txtBackFret.setText("0");
			countBack = 0;
			backgroundChannel = getFloatValue(txtBackChannel);
			backgroundFret    = getFloatValue(txtBackFret);
			setEnabledModel(false);
			bnGetBack.setText("Get");
			resetModel();
			plot.setEnabledScatteredPlot(false);
		}
		else if (e.getSource() == bnGetBack) {
			bnGetBack.setText("Add");
			plot.setEnabledScatteredPlot(false);
			setEnabledModel(false);
			background();
		}
		else if (e.getSource() == bnAcceptBack) {
//			bnGetBack.setEnabled(false);
			bnResetModel.setEnabled(true);
			bnGetModel.setEnabled(true);
			bnAcceptModel.setEnabled(true);
			backgroundChannel = getFloatValue(txtBackChannel);
			backgroundFret    = getFloatValue(txtBackFret);
			resetModel();	
			setEnabledModel(true);
			plot.setEnabledScatteredPlot(true);
		}
	}

	/**
	* Reset the model parameters.
	*/
	private void resetModel() {
		for(int i = 0; i < 3; i++)
		for(int m = 0; m < 3; m++) {
			txtParams[i][m].setText("---");
		}
		fit[0] = false;
		fit[1] = false;
		fit[2] = false;
		plot.reset();
		
		plot.set(params, fit, 0, 255, 0, 255, null, null);
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
		Rectangle rect = roi.getBoundingRect();
		
		Cursor cursor = this.getCursor();
		this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		IJ.showStatus("PixFRET: Starting measure...");
		PixFretImageAccess sbt     = new PixFretImageAccess(imp.getStack().getProcessor(1));
		PixFretImageAccess channel = new PixFretImageAccess(imp.getStack().getProcessor(2));
		float sigma = getFloatValue(txtSmooth);
		
		
		IJ.showStatus("PixFRET: Smoothing ...");
		sbt.smoothGaussian(sigma);
		channel.smoothGaussian(sigma);
	
		float ratioMax = 2f;
		float rscale = 512;
		
		IJ.showStatus("PixFRET: Computing ...");
		int nr = FMath.ceil(ratioMax*rscale)+1;
		
		int nx = FMath.ceil(nbins);
		int ny = FMath.ceil(nbins);
		int nix = channel.getWidth();
		
		byte[][] dataByte = null; //new byte[n][n];
				
		float cmin = Float.MAX_VALUE;
		float cmax = -Float.MAX_VALUE;
		float bmin = Float.MAX_VALUE;
		float bmax = -Float.MAX_VALUE;
		
		float rmin = Float.MAX_VALUE;
		float rmax = -Float.MAX_VALUE;
		
		int index = 0;
		
		if (maskRoi != null) {
			/***/
			PixFretImageAccess mask = new PixFretImageAccess(maskRoi);
			int mx = mask.getWidth();
			int my = mask.getHeight();
		
			for(int y = 0; y < my; y++) 
			for(int x = 0; x < mx; x++) {
				float chan = channel.pixels[rect.x+x + nix*(rect.y+y)]-backgroundChannel;
				if (chan > 0) {
					if (chan < cmin)
						cmin = chan;
					else if (chan > cmax)
						cmax = chan;
				}
			}		

			float cscale = 1f;
			float coff = -cmin;
			int nc = FMath.ceil(cmax-cmin);
			dataByte = new byte[nc][nr];

			int count = 0;
			for(int y = 0; y < my; y++)
			for(int x = 0; x < mx; x++) {
				if (maskRoi.getPixel(x, y) != 0.0) {
					index = rect.x+x + nix*(rect.y+y);
					float chan = channel.pixels[index]-backgroundChannel;
					if (chan > 0) {
						float ratio = (sbt.pixels[index]-backgroundFret)/chan;
						if (ratio > 0 && ratio < ratioMax) {
							int c = FMath.round(chan*cscale+coff);
							int r = FMath.round(ratio*rscale);
							if (c >= 0)
							if (c < nc)
					//		if (dataByte[c][r] == 0) {
								dataByte[c][r]++;
								count++;
					//		}
						}
					}
				}
			}	
			valueChannel = new float[count];
			valueRatio = new float[count];
			count = 0;			
			for(int c = 0; c < nc; c++)
			for(int r = 0; r < nr; r++) {
				if (dataByte[c][r] == 1) {
					valueChannel[count] = (c-coff)/cscale; 
					valueRatio[count] = (r/rscale);
					if (valueRatio[count] < rmin)
						rmin = valueRatio[count];
					else if (valueRatio[count] > rmax)
						rmax = valueRatio[count];
					count++;
				}
			}
		}
		else {
			for(int y = rect.y; y < rect.y+rect.height; y++)
			for(int x = rect.x; x < rect.x+rect.width;  x++) {
				float chan = channel.pixels[x + nix*y]-backgroundChannel;
				if (chan > 0) {
					if (chan < cmin)
						cmin = chan;
					else if (chan > cmax)
						cmax = chan;
				}
			}		
			float cscale = 1f;
			float coff = -cmin;
			int nc = FMath.ceil(cmax-cmin);
			dataByte = new byte[nc][nr];
			int count = 0;
			for(int y = rect.y; y < rect.y+rect.height; y++)
			for(int x = rect.x; x < rect.x+rect.width;  x++) {
				index = x + nix*y;
				float chan = (channel.pixels[index]-backgroundChannel);
				if (chan > 0) {
					float ratio = (sbt.pixels[index]-backgroundFret) / chan;
					if (ratio > 0 && ratio < ratioMax) {
						int c = FMath.round(chan*cscale+coff);
						int r = FMath.round(ratio*rscale);
						if (c >= 0)
						if (c < nc)
				//		if (dataByte[c][r] == 0) {
							dataByte[c][r]++;
							count++;
				//		}
					}
				}
			}
			valueChannel = new float[count];
			valueRatio = new float[count];
/*			count = 0;			
			for(int c = 0; c < nc; c++)
			for(int r = 0; r < nr; r++) {
				if (dataByte[c][r] > 0) {
					valueChannel[count] = (c-coff)/cscale; 
					valueRatio[count] = (r/rscale);
					if (valueRatio[count] < rmin)
						rmin = valueRatio[count];
					else if (valueRatio[count] > rmax)
						rmax = valueRatio[count];
					count++;
				}
			}
*/
			count = 0;			
			for(int c = 0; c < nc; c++)
			for(int r = 0; r < nr; r++) {
				for(int i=0; i<dataByte[c][r]; i++) {
					valueChannel[count] = (c-coff)/cscale; 
					valueRatio[count] = (r/rscale);
					if (valueRatio[count] < rmin)
						rmin = valueRatio[count];
					else if (valueRatio[count] > rmax)
						rmax = valueRatio[count];
					count++;
				}
			}

		}

		data = true;
		fit[0] = false;
		fit[1] = false;
		fit[2] = false;
		
		this.setCursor(cursor);
		IJ.showStatus("PixFRET");
		plot.set(params, fit,  cmin, cmax, rmin, rmax, valueChannel, valueRatio);
		return true;
	}
	
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
