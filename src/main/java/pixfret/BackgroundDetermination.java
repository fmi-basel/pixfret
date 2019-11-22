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

public class BackgroundDetermination extends JDialog implements ActionListener, WindowListener {

	private JButton bn;
	private JTextField txtBackgroundFret;
	private JTextField txtBackgroundDonor;
	private JTextField txtBackgroundAcceptor;

	private GridBagLayout 		layout				= new GridBagLayout();
	private GridBagConstraints 	constraint			= new GridBagConstraints();
	
	private JButton	bnCancel	 		= new JButton("Cancel");
	private JButton	bnGet				= new JButton("Get");
	private JButton	bnReset				= new JButton("Reset");
	private JButton	bnAccept			= new JButton("Accept");

	private JTextField txtLocalFret		= new JTextField("0.0", 5);
	private JTextField txtLocalDonor		= new JTextField("0.0", 5);
	private JTextField txtLocalAcceptor	= new JTextField("0.0", 5);
	
	private int count = 0;
	
	private float backgroundFret, backgroundDonor, backgroundAcceptor;
	
	/**
	* Constructor
	*/
	public BackgroundDetermination(JButton bn, 
		JTextField txtBackgroundFret, JTextField txtBackgroundDonor, JTextField txtBackgroundAcceptor,
		float backgroundFret, float backgroundDonor, float backgroundAcceptor) {
		super(new Frame(), "Background Determination");
		bn.setEnabled(false);
		this.bn = bn;
		this.txtBackgroundFret 		= txtBackgroundFret;
		this.txtBackgroundDonor 	= txtBackgroundDonor;
		this.txtBackgroundAcceptor 	= txtBackgroundAcceptor;
		
		this.backgroundFret 		= backgroundFret;
		this.backgroundDonor 		= backgroundDonor;
		this.backgroundAcceptor 	= backgroundAcceptor;

		txtLocalFret.setText(IJ.d2s(backgroundFret, 5));
		txtLocalDonor.setText(IJ.d2s(backgroundDonor, 5));
		txtLocalAcceptor.setText(IJ.d2s(backgroundAcceptor, 5));
		
		// JPanel Background
		JPanel pnBackground = new JPanel();
		pnBackground.setLayout(layout);
		addComponent(pnBackground, 0, 1, 1, 1, 4, new JLabel("FRET"));
		addComponent(pnBackground, 0, 2, 1, 1, 4, txtLocalFret);
		addComponent(pnBackground, 0, 3, 1, 1, 4, new JLabel("Donor"));
		addComponent(pnBackground, 0, 4, 1, 1, 4, txtLocalDonor);
		addComponent(pnBackground, 0, 5, 1, 1, 4, new JLabel("Acceptor"));
		addComponent(pnBackground, 0, 6, 1, 1, 4, txtLocalAcceptor);
		
		// JPanel Buttons
		JPanel pnButtons = new JPanel();
		pnButtons.setLayout(layout);
		addComponent(pnButtons, 0, 0, 1, 1, 8, bnCancel);
		addComponent(pnButtons, 0, 1, 1, 1, 8, bnReset);
		addComponent(pnButtons, 0, 2, 1, 1, 8, bnGet);
		addComponent(pnButtons, 0, 3, 1, 1, 8, bnAccept);

		// JPanel Main
		JPanel pnMain = new JPanel();
		pnMain.setLayout(layout);
		addComponent(pnMain, 0, 0, 1, 1, 4, pnBackground);
		addComponent(pnMain, 1, 0, 1, 1, 4, pnButtons);
		
		// Add Listeners
		bnCancel.addActionListener(this);
		bnGet.addActionListener(this);
		bnReset.addActionListener(this);
		bnAccept.addActionListener(this);
		addWindowListener(this);
		
		// Building the main JPanel
		this.getContentPane().add(pnMain);
		pack();
		GUI.center(this);
		setVisible(true);
		IJ.wait(250); 	// work around for Sun/WinNT bug

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
		else if (e.getSource() == bnReset) {
			count = 0;
			backgroundFret = 0.0f;
			backgroundDonor = 0.0f;
			backgroundAcceptor = 0.0f;
			txtLocalFret.setText(IJ.d2s(backgroundFret));
			txtLocalDonor.setText(IJ.d2s(backgroundDonor));
			txtLocalAcceptor.setText(IJ.d2s(backgroundAcceptor));
		}
		else if (e.getSource() == bnGet) {
			if (measure()) {
				txtLocalFret.setText(IJ.d2s(backgroundFret));
				txtLocalDonor.setText(IJ.d2s(backgroundDonor));
				txtLocalAcceptor.setText(IJ.d2s(backgroundAcceptor));
			}
		}
		else if (e.getSource() == bnAccept) {
			txtBackgroundFret.setText(IJ.d2s(backgroundFret));
			txtBackgroundDonor.setText(IJ.d2s(backgroundDonor));
			txtBackgroundAcceptor.setText(IJ.d2s(backgroundAcceptor));
			bn.setEnabled(true);
			dispose();
		}
	}

	/**
	* Measure the background level in the ROI.
	*/
	private boolean measure() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Please open a stack of images");
			return false;
		}
		if (imp.getStackSize() != 3) {
			IJ.showMessage("The input stack size should be equal to 3.");
			return false;
		}
		int type = imp.getType();
		if (type != ImagePlus.GRAY32 && type != ImagePlus.GRAY16 && type != ImagePlus.GRAY8) {
			IJ.showMessage("32-bits or 16-bits or 8-bits image is required.");
			return false;
		}
		Roi roi = imp.getRoi();
		if (roi == null) {
			IJ.showMessage("Please draw a ROI in the stack to estimate the background.");
			return false;
		}

		ImageProcessor maskRoi = roi.getMask();
		Rectangle rect = roi.getBoundingRect();
		PixFretImageAccess fret 		= new PixFretImageAccess(imp.getStack().getProcessor(1));
		PixFretImageAccess donor  		= new PixFretImageAccess(imp.getStack().getProcessor(2));
		PixFretImageAccess acceptor  	= new PixFretImageAccess(imp.getStack().getProcessor(3));
		
		float bFret 		= 0.0f;
		float bDonor 		= 0.0f;
		float bAcceptor 	= 0.0f;
		int bCount = 0;
		
		if (maskRoi != null) {
			PixFretImageAccess mask = new PixFretImageAccess(maskRoi);
			int nx = mask.getWidth();
			int ny = mask.getHeight();
		
			for(int y = 0; y < ny; y++)
			for(int x = 0; x < nx; x++) {
				if (maskRoi.getPixel(x, y) != 0.0) {
					bFret 		+= fret.getPixel(rect.x+x, rect.y+y);
					bDonor 		+= donor.getPixel(rect.x+x, rect.y+y);
					bAcceptor 	+= acceptor.getPixel(rect.x+x, rect.y+y);
					bCount++;
				}
			}
		}
		else {
			for(int y = 0; y < rect.height; y++)
			for(int x = 0; x < rect.width; x++) {
				bFret 		+= fret.getPixel(rect.x+x, rect.y+y);
				bDonor 		+= donor.getPixel(rect.x+x, rect.y+y);
				bAcceptor 	+= acceptor.getPixel(rect.x+x, rect.y+y);
				bCount++;
			}		
		}	
		if (bCount > 0) {
			bFret 		= bFret 	/ bCount;		
			bDonor 		= bDonor 	/ bCount;		
			bAcceptor 	= bAcceptor / bCount;
		}
		
		backgroundFret		= (backgroundFret*count + bFret*bCount)/(count+bCount);
		backgroundDonor 	= (backgroundDonor*count + bDonor*bCount)/(count+bCount);
		backgroundAcceptor 	= (backgroundAcceptor*count + bAcceptor*bCount)/(count+bCount);
		count += bCount;
		return true;
	}

	/**
	* Implements the methods for the WindowListener.
	*/
	public void windowActivated(WindowEvent e) 		{}
	public void windowClosing(WindowEvent e) 		{ bn.setEnabled(true); dispose();}
	public void windowClosed(WindowEvent e) 		{}
	public void windowDeactivated(WindowEvent e) 	{}
	public void windowDeiconified(WindowEvent e)	{}
	public void windowIconified(WindowEvent e)		{}
	public void windowOpened(WindowEvent e)			{}

}

