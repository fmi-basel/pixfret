package pixfret;

import java.awt.*;
import java.awt.event.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.util.Vector;
import imageware.*;
import javax.swing.*;
import javax.swing.event.*;
import java.net.*;
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

public class MainDialog extends JDialog implements ActionListener, WindowListener, Runnable, ChangeListener {

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

	private Thread thread		= null;

	private float params[][][]	= new float[3][2][3];		// First index A, B, E; second index DONOR ACCEP; third index CST LIN EXP 

	private float fact				= 1.0f;
	private float blur				= 0.0f;
	private float backgroundFret 	= 0.0f;
	private float backgroundDonor 	= 0.0f;
	private float backgroundAcceptor= 0.0f;
	
	private String normalizationList[]	= {"FRET/Donor", "FRET/Acceptor", "FRET/(Donor*Acceptor)", "FRET/sqrt(Donor*Acceptor)"};
	private String modelName 		 	= "Exponential Model";
	private String normalizationName 	= normalizationList[0];	private GridBagLayout 		layout				= new GridBagLayout();


	// *********************************************************
	// GUI Components
	// *********************************************************

	private GridBagConstraints 	constraint			= new GridBagConstraints();
	private JChoice				choiceModel 		= new Choice();
	private	JChoice choiceNormalization = new Choice();

	private JButton				bnClose	 			= new JButton("Close");
	private JButton				bnRun				= new JButton("Run");
	private JButton				bnCredits			= new JButton("Credits");
	private JButton				bnBackground		= new JButton("Background ...");
	private JButton				bnBleedthrough[][]	= new JButton[2][3];	// chanel, model

	private String[]			paramName			= {"a", "b", "e"};
	private JTextField			txtModelParams[][][]  = new JTextField[3][2][3];	// second index: DONOR or ACCEP, second index CST, LIN or EXP
	private JTextField			txtBackgroundFret;
	private JTextField			txtBackgroundDonor;
	private JTextField			txtBackgroundAcceptor;
	private JTextField			txtBlur 			= new JTextField("3.0", 7);
	private JTextField			txtFact 			= new JTextField("1.0", 7);

	private JPanel 				pnParameters[]		= new JPanel[3];
	private JPanel 				pnFormula[]			= new JPanel[3];
	private JLabel				lblFormula[][]  	= new JLabel[3][3];	// First index: line number, second index CST, LIN or EXP

	private BackgroundDetermination		dlgBackground 		= null;
	private BleedThroughDetermination[]	dlgBleedThrough 	= new BleedThroughDetermination[2];

	
	/**
	* Constructor.
	*/
	public MainDialog() {
		super(new JFrame(), "PixFret");
		
		layout = new GridBagLayout();
		constraint = new GridBagConstraints();

		// Panel Background
		JPanel pnBackground = new JPanel();
		pnBackground.setLayout(layout);
		txtBackgroundFret		= new JTextField("" + backgroundFret, 5);
		txtBackgroundDonor		= new JTextField("" + backgroundDonor, 5);
		txtBackgroundAcceptor	= new JTextField("" + backgroundAcceptor, 5);
		addComponent(pnBackground, 0, 0, 1, 1, 4, bnBackground);
		addComponent(pnBackground, 0, 1, 1, 1, 4, new Label("FRET"));
		addComponent(pnBackground, 0, 2, 1, 1, 4, txtBackgroundFret);
		addComponent(pnBackground, 0, 3, 1, 1, 4, new Label("Donor"));
		addComponent(pnBackground, 0, 4, 1, 1, 4, txtBackgroundDonor);
		addComponent(pnBackground, 0, 5, 1, 1, 4, new Label("Acceptor"));
		addComponent(pnBackground, 0, 6, 1, 1, 4, txtBackgroundAcceptor);

		// Panel Other Parameters
		JPanel pnParams = new JPanel();
		pnParams.setLayout(layout);
		addComponent(pnParams, 0, 0, 1, 1, 4, new JLabel("Gaussian blur"));
		addComponent(pnParams, 0, 1, 1, 1, 4, txtBlur);
		addComponent(pnParams, 0, 2, 1, 1, 4, new JLabel("(0.0 = No blur)"));
		addComponent(pnParams, 1, 0, 1, 1, 4, new JLabel("Threshold"));
		addComponent(pnParams, 1, 1, 1, 1, 4, txtFact);
		addComponent(pnParams, 1, 2, 1, 1, 4, new JLabel("Correction Factor"));
		addComponent(pnParams, 2, 0, 1, 1, 4, pnNormalization);		

		// Panel Buttons
		JPanel pnButtons = new JPanel();
		pnButtons.setLayout(layout);
		addComponent(pnButtons, 0, 0, 1, 1, 8, bnClose);
		addComponent(pnButtons, 0, 1, 1, 1, 8, bnCredits);
		addComponent(pnButtons, 0, 2, 1, 1, 8, bnRun);

		// Panel1
        JPanel panel1 = new JPanel();
        panel1.setLayout(layout);
		addComponent(panel1, 4, 0, 1, 1, 4, pnBackground);
		addComponent(panel1, 5, 0, 1, 1, 4, pnParams);
		addComponent(panel1, 6, 0, 1, 1, 4, pnButtons);

		
	 	
	 	// Tabbed Pane
	 	tabbedPane.addTab("Process", panel1);
        tabbedPane.addTab("Cal. Donor", panel2);
        tabbedPane.addTab("Cal. Acceptor", panel3);
 
 		// Panel Buttons
  		JPanel pnButtons = new JPanel();
		pnButtons.setLayout(layout);
		txtCopyright.append("Biomedical Imaging Group, 2005.\n");
		txtCopyright.append("Swiss Federal Institute of Technology Lausanne (EPFL)");
		txtCopyright.setFont(font);
		txtCopyright.setBorder(BorderFactory.createEtchedBorder());
		txtCopyright.setBackground(pnButtons.getBackground());
		txtCopyright.setForeground(new Color(0, 32, 128));
	 	addComponent(pnButtons, 0, 0, 4, 1, tabbedPane);
		addComponent(pnButtons, 1, 0, 1, 1, bnClose);
		addComponent(pnButtons, 1, 2, 1, 1, bnAbout);
		addComponent(pnButtons, 1, 3, 1, 1, txtCopyright);
	 	
		// Add Listeners
		bnAbout.addActionListener(this);
		bnClose.addActionListener(this);
		bnStart.addActionListener(this);
		bnTestOpen.addActionListener(this);
		bnTestClose.addActionListener(this);
		bnTestSmooth.addActionListener(this);
		bnTestDiffusion.addActionListener(this);
		bnHelp.addActionListener(this);	 	
		bnSaveResult.addActionListener(this);	 	
		bnSaveLifeV.addActionListener(this);
		bnAddSeed.addActionListener(this);
		bnRemoveSeed.addActionListener(this);
		bnSetSeed.addActionListener(this);
		
		addWindowListener(this);
			
		sliderControlPoint.addChangeListener(this);
		sliderObjectSize.addChangeListener(this);
		tabbedPane.addChangeListener(this);
		
		this.setResizable(false);
		// Building the main panel
		this.getContentPane().add(pnButtons);
		
		getPreferences();
		
		pack();
		GUI.center(this);
		setVisible(true);
		IJ.wait(250); 	// work around for Sun/WinNT bug
		info.canvas.displaySpline((tabbedPane.getSelectedIndex() == 4));
		
		preprocess = new Preprocess(tableLog, chrono);
	}

	/**
	* Add a component in a panel in the northwest of the cell.
	*/
	private void addComponent(JPanel pn, int row, final int col, int width, final int height, JComponent comp) {
	    constraint.gridx = col;
	    constraint.gridy = row;
	    constraint.gridwidth = width;
	    constraint.gridheight = height;
	    constraint.anchor = GridBagConstraints.NORTHWEST;
	    constraint.insets = new Insets(2, 2, 2, 2);
		constraint.weightx = IJ.isMacintosh()?90:100;
		constraint.fill = constraint.HORIZONTAL;
	    layout.setConstraints(comp, constraint);
	    pn.add(comp);
	}
	
	/**
	*
	*/
	public synchronized  void stateChanged(ChangeEvent e) {
		if (e.getSource() == tabbedPane) {
			info.canvas.displaySpline((tabbedPane.getSelectedIndex() == 4));
		}
		else {
			for(int s=0; s<3; s++)
				if (e.getSource() == sliderLambda[s])
					lblLambda[s].setText(title[s] + ":" + computeWeight(sliderLambda[s]));
		}
		if (e.getSource() == sliderControlPoint) {
			lblControlPoint.setText("" + sliderControlPoint.getValue());
			if (curves != null) {
				curves.buildBSpline(sliderControlPoint.getValue(), sliderObjectSize.getValue(), info);
				info.canvas.setCurves(curves);
			}
		}
		if (e.getSource() == sliderObjectSize) {
			lblObjectSize.setText("" + sliderObjectSize.getValue());
			if (curves != null) {
				curves.buildBSpline(sliderControlPoint.getValue(), sliderObjectSize.getValue(), info);
				info.canvas.setCurves(curves);
			}
		}
		notify();
	}

	/**
	* Implements the actionPerformed for the ActionListener.
	*/
	public synchronized  void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnClose) {
			setPreferences();
			dispose();
		}
		else if (e.getSource() == bnAbout) {
			showAbout();
		}
		else if (e.getSource() == bnHelp) {
			showHelp();
		}
		else if (e.getSource() == bnAddSeed) {
 			Roi roi = info.imp.getRoi();
			if (roi == null) {
				IJ.showMessage("No ROI selected.");
				return;
			}
			Rectangle rect = roi.getBoundingRect();
			int xi = rect.x + rect.width / 2;
			int yi = rect.y + rect.height / 2;
			int zi = info.imp.getCurrentSlice();
			if (nbSeedPoint < NBMAXSEED) {
				tableSeed.setValueAt("" + xi, nbSeedPoint, 0);
				tableSeed.setValueAt("" + yi, nbSeedPoint, 1);
				tableSeed.setValueAt("" + zi, nbSeedPoint, 2);
				nbSeedPoint++;
			}
			info.canvas.setSeedPoints(getSeedPointFromTable());
		}
		else if (e.getSource() == bnRemoveSeed) {
			int row = tableSeed.getSelectedRow();
			if (row >= 0) {
				Vector seedPoint = getSeedPointFromTable();
				nbSeedPoint = 0;
				for (int i=0; i<seedPoint.size(); i++) {
					Point3D pt = (Point3D)seedPoint.elementAt(i);
					if (i!=row) {
						tableSeed.setValueAt("" + pt.x, nbSeedPoint, 0);
						tableSeed.setValueAt("" + pt.y, nbSeedPoint, 1);
						tableSeed.setValueAt("" + pt.z, nbSeedPoint, 2);
						nbSeedPoint++;
					}
				}
				tableSeed.setValueAt("", nbSeedPoint, 0);
				tableSeed.setValueAt("", nbSeedPoint, 1);
				tableSeed.setValueAt("", nbSeedPoint, 2);
			}
			else 
				IJ.showMessage("No seed points selected.");
			info.canvas.setSeedPoints(getSeedPointFromTable());
		}
		else if (e.getSource() == bnSetSeed) {
			info.canvas.setSeedPoints(getSeedPointFromTable());
		}
		else if (e.getSource() == bnStart) {
			ImagePlus imp = info.imp;
			if (imp != null) {
				if (bnStart.getLabel().equals("Stop")) {
					if (rg != null) {
						rg.stop();
						bnStart.setLabel("Start");
					}
				}
				else {
					if (threadProcess == null) {
						threadProcess = new Thread(this);
						threadProcess.setPriority(Thread.MIN_PRIORITY);
						threadProcess.start();
					}
				}
			}
			else {
				IJ.error("The input image is not valid. Close the plugin.");
			}
		}
		else if (e.getSource() == bnTestClose) {
			ImagePlus imp = info.imp;
			if (imp != null) {
				ImagePlus slice = new ImagePlus("", imp.getProcessor());
				ImageWare input = Builder.create(slice);
				preprocess.close(input, sliderCloseXY.getValue(), sliderCloseXY.getValue(), sliderCloseZ.getValue());
				info.showCalibrated(input, "Test Close");
			}
			else {
				IJ.error("The input image is not valid. Close the plugin.");
			}
		}
		else if (e.getSource() == bnTestOpen) {
			ImagePlus imp = info.imp;
			if (imp != null) {
				ImagePlus slice = new ImagePlus("", imp.getProcessor());
				ImageWare input = Builder.create(slice);
				preprocess.open(input, sliderOpenXY.getValue(), sliderOpenXY.getValue(), sliderOpenZ.getValue());
				info.showCalibrated(input, "Test Open");
			}
			else {
				IJ.error("The input image is not valid. Close the plugin.");
			}
		}
		else if (e.getSource() == bnTestSmooth) {
			ImagePlus imp = info.imp;
			if (imp != null) {
				ImagePlus slice = new ImagePlus("", imp.getProcessor());
				ImageWare input = Builder.create(slice);
				double sigmaXY = getDoubleValue(txtSmoothXY, 0, Double.MAX_VALUE);
				double sigmaZ  = getDoubleValue(txtSmoothZ, 0, Double.MAX_VALUE);
				preprocess.smooth(input, sigmaXY, sigmaXY, sigmaZ);
				info.showCalibrated(input, "Test Smoothing");
			}
			else {
				IJ.error("The input image is not valid. Close the plugin.");
			}
		}
		else if (e.getSource() == bnTestDiffusion) {
			ImagePlus imp = info.imp;
			if (imp != null) {
				ImagePlus slice = new ImagePlus("", imp.getProcessor());
				ImageWare input = Builder.create(slice, ImageWare.FLOAT);
				int iterations = FMath.round(getDoubleValue(txtDiffusion, 0, Double.MAX_VALUE));
				preprocess.diffusion(input, iterations);
				info.showCalibrated(input, "Test Diffusion (iterations:" + iterations + ")");
			}
			else {
				IJ.error("The input image is not valid. Close the plugin.");
			}
		}
		else if (e.getSource() == bnSaveResult) {
			tableResult.saveText("Result_" + info.imp.getTitle() );
		}
		else if (e.getSource() == bnSaveLifeV) {
			tableLifeV.saveTextLifeV("LifeV_" + info.imp.getTitle(), info);
		}
		notify();
	}

	/**
	*/
	public Vector getSeedPointFromTable() {
	
		Vector seedPoints = new Vector();

		for (int i=0; i<nbSeedPoint; i++) {
			Point3D pt = new Point3D(0, 0, 0, 0);
			try {
				pt.x = (int)(new Double((String)tableSeed.getValueAt(i, 0))).doubleValue();
			}
			catch(Exception e) {
				pt.x = 0.0;
			}
			try {
				pt.y = (int)(new Double((String)tableSeed.getValueAt(i, 1))).doubleValue();
			}
			catch(Exception e) {
				pt.y = 0.0;
			}
			try {
				pt.z = (int)(new Double((String)tableSeed.getValueAt(i, 2))).doubleValue();
			}
			catch(Exception e) {
				pt.z = 1;
			}
			if (pt.x < 0)
				pt.x = 0;
			if (pt.x >= info.nx)
				pt.x = info.nx-1;
			if (pt.y < 0)
				pt.y = 0;
			if (pt.y >= info.ny)
				pt.y = info.ny-1;
			if (pt.z < 1)
				pt.z = 1;
			if (pt.z >= info.nz)
				pt.z = info.nz;
			seedPoints.addElement(pt);
		}
		return seedPoints;
	}

	/**
	*/
	public Vector getSeedPointFromTableShift() {
	
		Vector seedPoints = new Vector();

		for (int i=0; i<nbSeedPoint; i++) {
			Point3D pt = new Point3D(0, 0, 0, 0);
			try {
				pt.x = (int)(new Double((String)tableSeed.getValueAt(i, 0))).doubleValue();
			}
			catch(Exception e) {
				pt.x = 0.0;
			}
			try {
				pt.y = (int)(new Double((String)tableSeed.getValueAt(i, 1))).doubleValue();
			}
			catch(Exception e) {
				pt.y = 0.0;
			}
			try {
				pt.z = (int)(new Double((String)tableSeed.getValueAt(i, 2))).doubleValue()-1;
			}
			catch(Exception e) {
				pt.z = 0;
			}
			if (pt.x < 0)
				pt.x = 0;
			if (pt.x >= info.nx)
				pt.x = info.nx-1;
			if (pt.y < 0)
				pt.y = 0;
			if (pt.y >= info.ny)
				pt.y = info.ny-1;
			if (pt.z < 0)
				pt.z = 0;
			if (pt.z >= info.nz)
				pt.z = info.nz-1;
			seedPoints.addElement(pt);
		}
		return seedPoints;
	}

	/**
	*/
	public void setSeedPointIntoTable(Vector seedPoint) {
		for (int i=0; i<nbSeedPoint; i++) {
			Point3D pt = (Point3D)seedPoint.elementAt(i);
			tableSeed.setValueAt( "" + pt.x, i, 0);
			tableSeed.setValueAt( "" + pt.y, i, 1);
			tableSeed.setValueAt( "" + pt.z, i, 2);
		}
	}
	/**
	 * Implements the run for the Runnable.
	 */
	public void run() {
		if (info.imp == null) {
			IJ.error("Open a stack of images.");
			threadProcess = null;
			bnStart.setLabel("Start");
			return;
		}
		
		int z1 = FMath.round(getDoubleValue(txtZFrom, 1, info.nz));
		int z2 = FMath.round(getDoubleValue(txtZTo,   1, info.nz));
			
		if (z2<z1) {
			IJ.error("Incompatible Z range.");
			threadProcess = null;
			bnStart.setLabel("Start");
			return;
		}
		if (z1 < 1)
			z1 = 1;
		if (z2 >= info.nz)
			z2 = info.nz;
		bnStart.setEnabled(false);
		Cursor cursor = getCursor();
		progression.setColor(Color.orange);
		setCursor( new Cursor(Cursor.WAIT_CURSOR));
		
		ImageWare input = Builder.wrap(info.imp);
		
		volume = Builder.create(input.getWidth(), input.getHeight(), z2-z1+1, input.getType());

		z1 = z1 - 1;
		
		input.getXYZ(0, 0, z1, volume);
		
		if (chkOpen.isSelected()) {
			preprocess.open(volume, sliderOpenXY.getValue(), sliderOpenXY.getValue(), sliderOpenZ.getValue());
		}
		if (chkClose.isSelected()) {
			preprocess.close(volume, sliderCloseXY.getValue(), sliderCloseXY.getValue(), sliderCloseZ.getValue());
		}
		if (chkSmooth.isSelected()) {
			double sigmaXY = getDoubleValue(txtSmoothXY, 0, Double.MAX_VALUE);
			double sigmaZ  = getDoubleValue(txtSmoothZ, 0, Double.MAX_VALUE);
			preprocess.smooth(volume, sigmaXY, sigmaXY, sigmaZ);
		}
		if (chkDiffusion.isSelected()) {
			int iter = FMath.round(getDoubleValue(txtDiffusion, 0, Double.MAX_VALUE));
			preprocess.diffusion(volume, iter);
		}
		bnStart.setEnabled(true);
		bnStart.setLabel("Stop");
		// seedPoint.z -= z1;
		rg = new RegionGrowing(volume, info.canvas, chkSegVolume.isSelected(), chkColorVolume.isSelected(), info, progression, tableLog, chrono, tableResult, tableLifeV);
		
		double lambda0 = computeWeightValue(sliderLambda[0]);
		double lambda1 = computeWeightValue(sliderLambda[1]);
		double lambda2 = computeWeightValue(sliderLambda[2]);
		Vector seedPoint = getSeedPointFromTableShift();
		info.canvas.setZ(z1, volume.getSizeZ());

		if (seedPoint.size() > 0) {
			curves = rg.process(seedPoint, lambda0, lambda1, lambda2, z1, info.imp.getCurrentSlice());
			if (curves != null) {
			//info.canvas.setPerimeters(perimeters, sliderControlPoint.getValue(), z1, z2, tableLifeV);
				curves.buildBSpline(sliderControlPoint.getValue(), sliderObjectSize.getValue(), info);
				info.canvas.setCurves(curves);
			}
		}
		else {
			IJ.showMessage("No seed points selected");
		}
		setCursor(cursor);
		progression.setColor(Color.green);
				
		threadProcess = null;
		bnStart.setLabel("Start");

	}

	/**
	* Implements the methods for the WindowListener.
	*/
	public void windowActivated(WindowEvent e) 		{}
	public void windowClosed(WindowEvent e) 		{}
	public void windowDeactivated(WindowEvent e) 	{}
	public void windowDeiconified(WindowEvent e)	{}
	public void windowIconified(WindowEvent e)		{}
	public void windowOpened(WindowEvent e)			{}			
	public void windowClosing(WindowEvent e) 		{ 
		dispose(); 
	}
	
	/**
	* Build a panel for the About box
	*/ 
	private void showAbout() {
		JFrame frame = new JFrame("About");
	
		JEditorPane pane = new JEditorPane();
		pane.setEditable(false);

		String path = "file:"+Menus.getPlugInsPath()+"RegionGrowing3D"+System.getProperty("file.separator")+"about.html";
		try {
			URL helpURL = new URL(path);
			pane.setPage(helpURL);
			pane.setContentType("text/html; charset=ISO-8859-1");
			JScrollPane helpScrollPane=new JScrollPane(pane);
			helpScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			helpScrollPane.setPreferredSize(new Dimension(600,300));
			frame.getContentPane().add(helpScrollPane,BorderLayout.CENTER);
			frame.pack();
			GUI.center(frame);
			frame.show();
		} catch (Exception e) {
			IJ.error("No about file available");
		}
	}
	
	/**
	* Build a panel for the About box
	*/ 
	private void showHelp() {
		JFrame frame = new JFrame("Help");
	
		JEditorPane pane = new JEditorPane();
		pane.setEditable(false);

		String path = "file:"+Menus.getPlugInsPath()+"RegionGrowing3D"+System.getProperty("file.separator")+"help.html";
		try {
			URL helpURL = new URL(path);
			pane.setPage(helpURL);
			pane.setContentType("text/html; charset=ISO-8859-1");
			JScrollPane helpScrollPane=new JScrollPane(pane);
			helpScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			helpScrollPane.setPreferredSize(new Dimension(600,600));
			frame.getContentPane().add(helpScrollPane,BorderLayout.CENTER);
			frame.pack();
			GUI.center(frame);
			frame.show();
		} catch (Exception e) {
			IJ.error("No about file available");
		}
	}

	/**
	* Get a double value from a TextField between minimal and maximal values.
	*/
	private double getDoubleValue(JTextField text, double mini, double maxi) {
		double d;
		try {
			d = (new Double(text.getText())).doubleValue();
			if (d < mini)
				d = mini;
			if (d > maxi)
				d = maxi;
			text.setText("" + d);
		}
		
		catch (Exception e) {
			throw new  NumberFormatException(); 
		}
		return d;
	}
	


} // end of main class

