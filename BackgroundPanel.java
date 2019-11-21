package pixfret;

import javax.swing.*;
import javax.swing.text.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;              //for layout managers and more
import java.awt.event.*;        //for action events


public class BackgroundPanel extends JPanel implements ActionListener {

	private GridBagLayout 		layout				= new GridBagLayout();
	private GridBagConstraints 	constraint			= new GridBagConstraints();
	private JTextField			txtBackgroundFret;
	private JTextField			txtBackgroundDonor;
	private JTextField			txtBackgroundAcceptor;

	private JButton	bnGet				= new JButton("Get");
	private JButton	bnReset				= new JButton("Reset");

	private int count = 0;
	
	private float backgroundFret, backgroundDonor, backgroundAcceptor;

	/**
	* Constructor.
	*/
	public BackgroundPanel() {
		super();

		JPanel pn = new JPanel();
		pn.setLayout(layout);
		addComponent(pn, 0, 0, 1, 1, 4, bnReset);
		addComponent(pn, 0, 1, 1, 1, 4, bnGet);
	
		setLayout(layout);
		txtBackgroundFret		= new JTextField("" + backgroundFret, 5);
		txtBackgroundDonor		= new JTextField("" + backgroundDonor, 5);
		txtBackgroundAcceptor	= new JTextField("" + backgroundAcceptor, 5);
		addComponent(this, 1, 0, 1, 1, 4, new JLabel("FRET"));
		addComponent(this, 1, 1, 1, 1, 4, txtBackgroundFret);
		addComponent(this, 1, 2, 1, 1, 4, new JLabel(""));
		addComponent(this, 2, 0, 1, 1, 4, new JLabel("Donor"));
		addComponent(this, 2, 1, 1, 1, 4, txtBackgroundDonor);
		addComponent(this, 3, 0, 1, 1, 4, new JLabel("Acceptor"));
		addComponent(this, 3, 1, 1, 1, 4, txtBackgroundAcceptor);
		addComponent(this, 4, 1, 2, 1, 4, pn);
		
		bnReset.addActionListener(this);
		bnGet.addActionListener(this);
		
		setBorder(BorderFactory.createTitledBorder("Background"));
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
	* Background
	*/
	public float getBackgroundFret() {
		return getFloatValue(txtBackgroundFret);
	}
	public float getBackgroundDonor() {
		return getFloatValue(txtBackgroundDonor);
	}
	public float getBackgroundAcceptor() {
		return getFloatValue(txtBackgroundAcceptor);
	}

	/**
	 * Implements the actionPerformed for the ActionListener.
	 */
	public synchronized  void actionPerformed(ActionEvent e) {
	
		if (e.getSource() == bnReset) {
			count = 0;
			backgroundFret = 0.0f;
			backgroundDonor = 0.0f;
			backgroundAcceptor = 0.0f;
			txtBackgroundFret.setText(IJ.d2s(backgroundFret));
			txtBackgroundDonor.setText(IJ.d2s(backgroundDonor));
			txtBackgroundAcceptor.setText(IJ.d2s(backgroundAcceptor));
			bnGet.setText("Get");
		}
		else if (e.getSource() == bnGet) {
			if (measure()) {
				txtBackgroundFret.setText(IJ.d2s(backgroundFret));
				txtBackgroundDonor.setText(IJ.d2s(backgroundDonor));
				txtBackgroundAcceptor.setText(IJ.d2s(backgroundAcceptor));
				bnGet.setText("Add");
			}
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