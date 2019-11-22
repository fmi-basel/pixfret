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

import java.awt.*;
import java.awt.event.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.util.Vector;
import javax.swing.*;
import java.awt.event.*;
import java.net.*;
import ij.text.*;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.swing.table.*;
import javax.swing.event.*;
import java.io.*;

import pixfret.*;

/**
* PixFRET Version 1.4.4
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

public class PixFRET_ extends JDialog implements ActionListener, WindowListener, Runnable {

	// *********************************************************
	// Constant
	// *********************************************************

	private	float limit 	= 10e9f;
	private float rangeInf	= -0.01f;		// Inferior limit for the red colorization in the LUT
	private float rangeSup	=  0.01f;		// Superior limit for the red colorization in the LUT

	private final int 	DONOR	= 0;
	private final int	ACCEP	= 1;
	
	private final int	CST		= 0;		// Constant Model
	private final int	LIN		= 1;		// Linear Model
	private final int 	EXP 	= 2;		// Exponential Model

	private final int	A		= 0;		// Constant Model
	private final int	B		= 1;		// Linear Model
	private final int 	E 		= 2;		// Exponential Model
	
	private Properties props 	= new Properties();
	private	String filename 	= System.getProperty("user.dir") + "/plugins/PixFRETSettings.txt";
	private String jarFile 		= System.getProperty("user.dir") + "/plugins/PixFRET_.jar";

	private Thread thread		= null;

	private float fact				= 1.0f;
	private float blur				= 0.0f;
	private float backgroundFret 	= 0.0f;
	private float backgroundDonor 	= 0.0f;
	private float backgroundAccep	= 0.0f;
	private boolean displayBlurred	= true;
	
	private String outputList[]	= {"FRET/Donor", "FRET/Acceptor", "FRET/(Donor*Acceptor)", "FRET/sqrt(Donor*Acceptor)", "FRET Efficiency"};
	private String outputName 	= outputList[0];	
	private String[] models				= {"Constant Model", "Linear Model", "Exponential Model"};
	private String[] modelsShortName	= {"Cst", "Lin", "Exp"};
	private String[] channelName		= {"Donor", "Acceptor"};
	
	private PanelImage[][]	panelFormula= new PanelImage[3][2];	// index 1: Model, index 2: channel
	private PanelImage[]	panelNorma	= new PanelImage[5];	// index: kind of normalization
	
	private JLabel lblFormula[]	= new JLabel[2];
	
	/**
	* Constructor.
	*/
	public PixFRET_() {

		super(new JFrame(), "PixFRET");
		
		if (IJ.versionLessThan("1.21a"))
			return;
			
		
		doDialog();
		getPreferences();
		
	}

	// *********************************************************
	// GUI Components
	// *********************************************************

	private GridBagLayout 		layout				= new GridBagLayout();
	private GridBagConstraints 	constraint			= new GridBagConstraints();
	private	JComboBox 			choiceOutput;

	private JButton				bnClose	 			= new JButton("Close");
	private JButton				bnSave	 			= new JButton("Save Parameters");
	private JButton				bnRun				= new JButton("Compute FRET");
	private JCheckBox			chkDisplayBlur		= new JCheckBox("Show blurred images", true);
	
	private JTextField			txtBlur 			= new JTextField("3.0", 7);
	private JTextField			txtFact 			= new JTextField("1.0", 7);
	private	JTabbedPane 		tabbedPane	 		= new JTabbedPane();

	private BackgroundPanel 	pnBackground 		= new BackgroundPanel();
	private BleedThroughPanel 	pnBleedThroughDonor = new BleedThroughPanel(DONOR, props, filename);	
	private BleedThroughPanel 	pnBleedThroughAccep = new BleedThroughPanel(ACCEP, props, filename);	
	
	/**
	* Constructor.
	*/
	private void doDialog() {
		
		layout = new GridBagLayout();
		constraint = new GridBagConstraints();
		
		// Panel Normalization
		choiceOutput = new JComboBox(outputList);
		choiceOutput.setSelectedIndex(0);
		
		JPanel pnParams = new JPanel();
		pnParams.setLayout(layout);
		addComponent(pnParams, 4, 0, 1, 1, 4, new JLabel("Gaussian blur"));
		addComponent(pnParams, 4, 1, 1, 1, 4, txtBlur);
		addComponent(pnParams, 4, 2, 1, 1, 4, new JLabel("(0.0 = No blur)"));
		addComponent(pnParams, 6, 0, 1, 1, 4, new JLabel("Threshold"));
		addComponent(pnParams, 6, 1, 1, 1, 4, txtFact);
		addComponent(pnParams, 6, 2, 1, 1, 4, new JLabel("Correction Factor"));
		addComponent(pnParams, 7, 0, 1, 1, 4, new JLabel("Output"));
		addComponent(pnParams, 7, 1, 2, 1, 4, choiceOutput);
		pnParams.setBorder(BorderFactory.createTitledBorder("Parameters"));

		// Formula
		lblFormula[DONOR] = new JLabel("BTdon = b*exp(DONORdon*e) + a");
		lblFormula[ACCEP] = new JLabel("BTacc(x,y) = b*exp(ACCEPTORacc*e) + a");
		lblFormula[DONOR].setFont(new Font("Arial", Font.BOLD, 11));
		lblFormula[ACCEP].setFont(new Font("Arial", Font.BOLD, 11));
		JPanel pnComp = new JPanel();
		pnComp.setLayout(layout);
		//addComponentFree(pnComp, 1, 0, 2, 1, 4, new PanelImage(JarResources.extractImage(jarFile, "formulaFRET.gif"), 398, 23));
		addComponentFree(pnComp, 2, 0, 2, 1, 4, lblFormula[DONOR]);
		addComponentFree(pnComp, 3, 0, 2, 1, 4, lblFormula[ACCEP]);
		addComponentFree(pnComp, 4, 0, 1, 1, 4, chkDisplayBlur);
		addComponentFree(pnComp, 4, 1, 1, 1, 4, bnRun);
		pnComp.setBorder(BorderFactory.createTitledBorder("Computation"));

		JPanel pnButton = new JPanel();
		pnButton.setLayout(new FlowLayout());
		pnButton.add(bnSave);
		pnButton.add(bnClose);

		// TabFRET
        JPanel tabFRET = new JPanel();
        tabFRET.setLayout(layout);
		addComponentFree(tabFRET, 0, 0, 2, 1, 4, pnBackground);
		addComponentFree(tabFRET, 1, 0, 2, 1, 4, pnParams);
		addComponentFree(tabFRET, 2, 0, 2, 1, 4, pnComp);
		addComponentFree(tabFRET, 3, 0, 2, 1, 14, pnButton);
		
	 	// Tabbed Pane
	 	tabbedPane.addTab("FRET", tabFRET);
        tabbedPane.addTab("Donor Model", pnBleedThroughDonor);
        tabbedPane.addTab("Acceptor Model", pnBleedThroughAccep);
		tabbedPane.addTab("About", new About());
 
 		// Listener		
		bnClose.addActionListener(this);
		bnRun.addActionListener(this);
		bnSave.addActionListener(this);
		addWindowListener(this);
		
		// Main
		getPreferences();
		this.setResizable(false);
		this.getContentPane().add(tabbedPane);
		pack();
		GUI.center(this);
		setVisible(true);
		IJ.wait(250); 	// work around for Sun/WinNT bug
		
		pnBleedThroughDonor.setParamsFormula(lblFormula, tabbedPane);		
		pnBleedThroughAccep.setParamsFormula(lblFormula, tabbedPane);		
	}
	
	/**
	* Overload getInsets.
	*/
	public Insets getInsets () {
		return(new Insets(30, 10, 10, 10));
	}

	/**
	* Add a component in a JPanel in the northwest of the cell.
	*/
	private void addComponentFree(JPanel pn, int row, int col, int width, int height, int space, JComponent comp) {
	    constraint.gridx = col;
	    constraint.gridy = row;
	    constraint.gridwidth = width;
	    constraint.gridheight = height;
	    constraint.insets = new Insets(space, space, space, space);
		constraint.weightx = IJ.isMacintosh()?90:100;
	    layout.setConstraints(comp, constraint);
	    pn.add(comp);
	}
	
	/**
	* Add a component in a JPanel in the northwest of the cell.
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
		if (e.getSource() == bnClose) {
			dispose();
		}
		if (e.getSource() == bnSave) {
			setPreferences();
		}
		else if (e.getSource() == bnRun) {
			if (thread == null) {
				thread = new Thread(this);
				thread.setPriority(Thread.MIN_PRIORITY);
				thread.start();
			}
		}
		notify();
	}

	/**
	* Implements the methods for the WindowListener.
	*/
	public void windowActivated(WindowEvent e) 		{}
	public void windowClosing(WindowEvent e) 		{ dispose();}
	public void windowClosed(WindowEvent e) 		{}
	public void windowDeactivated(WindowEvent e) 	{}
	public void windowDeiconified(WindowEvent e)	{}
	public void windowIconified(WindowEvent e)		{}
	public void windowOpened(WindowEvent e)			{}
	
	/**
	* Main routine for the FRET computation.
	*/
	public synchronized void run() {
	
		// *********************************************************
		// Retrieve the on-focus image
		// *********************************************************                
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("The input stack is not open.");
			thread = null;
			return;
		}
		if (imp.getStackSize() != 3) {
			IJ.showMessage("The input stack size should be equal to 3.");
			thread = null;
			return;
		}

		int type = imp.getType();
		if (type != ImagePlus.GRAY32 && type != ImagePlus.GRAY16 && type != ImagePlus.GRAY8) {
			IJ.showMessage("32-bits or 16-bits or 8-bits image is required.");
			thread = null;
			return;
		}

		Cursor cursor = getCursor();
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		
		// ***********************************************************
		// Create the PixFretImageAccess objects
		// ***********************************************************                
		PixFretImageAccess fret 		= new PixFretImageAccess(imp.getStack().getProcessor(1));
		PixFretImageAccess donor  		= new PixFretImageAccess(imp.getStack().getProcessor(2));
		PixFretImageAccess acceptor  	= new PixFretImageAccess(imp.getStack().getProcessor(3));
		
		//*******************************************************
		// Smooth the images
		//*******************************************************
		
		blur = (float)Math.abs(getFloatValue(txtBlur));
		if (blur > 0.0) {
			IJ.showStatus("Blurring the donor image ...");
			donor.smoothGaussian(blur);
			if (chkDisplayBlur.isSelected())
				donor.show("Donor image blurred with value = " + blur);
			IJ.showStatus("Blurring the acceptor image ...");
			acceptor.smoothGaussian(blur);     
			if (chkDisplayBlur.isSelected())
				acceptor.show("Acceptor image blurred with value = " + blur); 
			IJ.showStatus("Blurring the FRET image ...");
			fret.smoothGaussian(blur);
		}

		// ***********************************************************
		// Get the parameters
		// ***********************************************************
		
		int modelDonor = pnBleedThroughDonor.getModel();
		int modelAccep = pnBleedThroughAccep.getModel();
		float[][] paramsDonor = pnBleedThroughDonor.getParams();
		float[][] paramsAccep = pnBleedThroughAccep.getParams();
		displayBlurred = chkDisplayBlur.isSelected();

		float BGFRET 	= pnBackground.getBackgroundFret();
		float BGD 		= pnBackground.getBackgroundDonor();
		float BGA 		= pnBackground.getBackgroundAcceptor();
		fact 			= getFloatValue(txtFact);

		float Nthresh = (float)Math.sqrt(BGD*BGA)*(float)fact;

		int nx = fret.getWidth();
		int ny = fret.getHeight();
		setPreferences();

		long chrono = System.currentTimeMillis();
                
		// ******************************************************
		// expFRET and NexpFRet
		// ******************************************************
		PixFretImageAccess FRET = new PixFretImageAccess(nx, ny);
		PixFretImageAccess NFRET = new PixFretImageAccess(nx, ny);
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		float[][] block = new float[3][3];
		float normTerm = 0.0f;
		
		int outKind = choiceOutput.getSelectedIndex();
		IJ.showStatus("Computing the FRET expression (" + outKind +")...");
		
		for(int y=0;y<ny;y++) {
			for ( int x = 0; x < nx; x++) {

				donor.getNeighborhood(x, y, block);
				float lmdonor = 0.0f;
				for (int i=0; i<3; i++)
				for (int j=0; j<3; j++)
					lmdonor = lmdonor + block[i][j];
				lmdonor = lmdonor / 9.0f;
				float idonor = block[1][1]-BGD;

				if (lmdonor > BGD*(float)fact) {

					acceptor.getNeighborhood(x, y, block);
					float lmacceptor = 0.0f;
					for (int i=0; i<3; i++)
					for (int j=0; j<3; j++)
						lmacceptor = lmacceptor + block[i][j];
					lmacceptor = lmacceptor / 9.0f;
					float iacceptor = block[1][1]-BGA;

					if (lmacceptor > BGA*(float)fact) {

						fret.getNeighborhood(x, y, block);
						float lmfret = 0.0f;
						for (int i=0; i<3; i++)
						for (int j=0; j<3; j++)
							lmfret = lmfret + block[i][j];
						lmfret = lmfret / 9.0f;
						float ifret = block[1][1]-BGFRET;

						if (lmfret > BGFRET*(float)fact) {
							float expfret = ifret;
							switch(modelDonor) {
								case CST: 	expfret -= paramsDonor[0][CST]*idonor;
											break;
								case LIN:	expfret -= (paramsDonor[0][LIN]+paramsDonor[1][LIN]*idonor)*idonor;
											break; 
								case EXP:	expfret -= (paramsDonor[0][EXP]+paramsDonor[1][EXP]*(float)Math.exp(paramsDonor[2][EXP]*idonor))*idonor;
											break;
							}
							switch(modelAccep) {
								case CST: 	expfret -= paramsAccep[0][CST]*iacceptor;
											break;
								case LIN:	expfret -= (paramsAccep[0][LIN]+paramsAccep[1][LIN]*iacceptor)*iacceptor;
											break; 
								case EXP:	expfret -= (paramsAccep[0][EXP]+paramsAccep[1][EXP]*(float)Math.exp(paramsAccep[2][EXP]*iacceptor)*iacceptor);
											break;
							}
							switch(outKind) {
								case 0: 
									normTerm = (float)(Math.abs(idonor));
									break;
								case 1: 
									normTerm = (float)(Math.abs(iacceptor));
									break;
								case 2: 
									normTerm = (float)(Math.abs(idonor*iacceptor));
									break;
								case 3: 
									normTerm = (float)Math.sqrt(Math.abs(idonor*iacceptor));
									break;
								case 4: 
									normTerm = (float)(Math.abs(idonor+expfret));
									break;
							}
							float nexpfret = 0.0f;
							if (normTerm != 0.0f) 
								nexpfret = expfret*100.0f / normTerm;
							float nt = (float)Math.sqrt(Math.abs(idonor*iacceptor));
							
							if (nt>Nthresh) {
								FRET.pixels[x+y*nx]  = expfret;
								NFRET.pixels[x+y*nx] = nexpfret;
							}
						}		
					}                  
				}
			}
		}

		// ******************************************************
		// Display the resulting images	
		// ******************************************************
		if (outKind == 4)
			NFRET.showLUT("FRET Efficiency (%) of " + imp.getTitle(), rangeInf, rangeSup);
		else
			NFRET.showLUT("NFRET (x100) of " + imp.getTitle(), rangeInf, rangeSup);
		FRET.showLUT("FRET of " + imp.getTitle(), rangeInf, rangeSup);  
		IJ.showStatus("PixFRET time:" + (System.currentTimeMillis() - chrono) + " ms");
		setCursor(cursor);
		
		thread = null;
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
	* Get the preferences from the file.
	*/
	private void getPreferences() {
		try {
			FileInputStream in = new FileInputStream(filename);
			props.load(in);
		}
		catch(Exception e) {
		}

		float[][] paramsDonor = new float[3][3];
		float[][] paramsAccep = new float[3][3];
		paramsDonor[A][CST]  = Float.parseFloat(props.getProperty("Constant Donor a", "0.33818"));
		paramsAccep[A][CST]  = Float.parseFloat(props.getProperty("Constant Accep a", "0.01534"));
		
		paramsDonor[A][LIN] = Float.parseFloat(props.getProperty("Linear Donor a", "0.0"));
		paramsDonor[B][LIN] = Float.parseFloat(props.getProperty("Linear Donor b", "0.0"));
		paramsAccep[A][LIN] = Float.parseFloat(props.getProperty("Linear Accep a", "0.0"));
		paramsAccep[B][LIN] = Float.parseFloat(props.getProperty("Linear Accep b", "0.0"));
		
		paramsDonor[A][EXP] = Float.parseFloat(props.getProperty("Exponential Donor a", "0.0"));
		paramsDonor[B][EXP] = Float.parseFloat(props.getProperty("Exponential Donor b", "0.0"));
		paramsDonor[E][EXP] = Float.parseFloat(props.getProperty("Exponential Donor e", "0.0"));
		paramsAccep[A][EXP] = Float.parseFloat(props.getProperty("Exponential Accep a", "0.0"));
		paramsAccep[B][EXP] = Float.parseFloat(props.getProperty("Exponential Accep b", "0.0"));
		paramsAccep[E][EXP] = Float.parseFloat(props.getProperty("Exponential Accep e", "0.0"));

	 	fact	= Float.parseFloat(props.getProperty("fact", "1.0"));
	 	blur	= Float.parseFloat(props.getProperty("blur", "3.0"));
	 	
	 	int modelDonor = Integer.parseInt(props.getProperty("Model Donor", "2"));
	 	int modelAccep = Integer.parseInt(props.getProperty("Model Accep", "0"));
	 	outputName = props.getProperty("Normalization", outputList[0]);
	 	for (int i=0; i<outputList.length; i++) {
	 		if (outputList[i].equals(outputName))
	 			choiceOutput.setSelectedIndex(i);
	 	}
	 			
		displayBlurred = props.getProperty("Display Blurred Images", "true").equals("true");
		chkDisplayBlur.setSelected(displayBlurred);
		txtBlur.setText("" + blur);
		txtFact.setText("" + fact); 	
		pnBleedThroughDonor.setModelAndParams(modelDonor, paramsDonor);
		pnBleedThroughAccep.setModelAndParams(modelAccep, paramsAccep);
	 	
	}

	/**
	* Set the preferences into the file.
	*/
	private void setPreferences() {

		float[][] paramsDonor = pnBleedThroughDonor.getParams();
		float[][] paramsAccep = pnBleedThroughAccep.getParams();
		int modelDonor = pnBleedThroughDonor.getModel();
		int modelAccep = pnBleedThroughAccep.getModel();
		props.setProperty("Model Donor", ""+modelDonor);
		props.setProperty("Model Accep", ""+modelAccep);
		outputName = outputList[(int)choiceOutput.getSelectedIndex()];

		props.setProperty("Normalization", outputName);
		displayBlurred = chkDisplayBlur.isSelected();
		props.setProperty("Display Blurred Images", ""+displayBlurred);	
		props.setProperty("Constant Donor a", "" 	+ paramsDonor[A][CST]);
		props.setProperty("Constant Accep a", "" 	+ paramsAccep[A][CST]);
		props.setProperty("Linear Donor a", "" 		+ paramsDonor[A][LIN]);
		props.setProperty("Linear Donor b", "" 		+ paramsDonor[B][LIN]);
		props.setProperty("Linear Accep a", "" 		+ paramsAccep[A][LIN]);
		props.setProperty("Linear Accep b", "" 		+ paramsAccep[B][LIN]);
		props.setProperty("Exponential Donor a", "" + paramsDonor[A][EXP]);
		props.setProperty("Exponential Donor b", "" + paramsDonor[B][EXP]);
		props.setProperty("Exponential Donor e", "" + paramsDonor[E][EXP]);
		props.setProperty("Exponential Accep a", "" + paramsAccep[A][EXP]);
		props.setProperty("Exponential Accep b", "" + paramsAccep[B][EXP]);
		props.setProperty("Exponential Accep e", "" + paramsAccep[E][EXP]);
		blur = getFloatValue(txtBlur);
		fact = getFloatValue(txtFact);
		props.setProperty("fact", 					"" + fact);
		props.setProperty("blur", 					"" + blur);
		
		try {
			FileOutputStream out = new FileOutputStream(filename);
			props.store(out, "PixFRET\n");
		}
		catch(Exception e) {
		}
	}

} 

