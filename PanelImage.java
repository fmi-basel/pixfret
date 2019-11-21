package pixfret;

import javax.swing.*;
import java.awt.*;

public class PanelImage extends JPanel {

	private Image image;
	private int xdim;
	private int ydim;
	
	public PanelImage(Image image, int xdim, int ydim) {
		super();
		this.image = image;
		this.xdim = xdim;
		this.ydim = ydim;
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(xdim, ydim);
	}

	public Dimension getMaximumSize() {
		return new Dimension(xdim, ydim);
	}
	
	public Dimension getMinimumSize() {
		return new Dimension(xdim, ydim);
	}
	
	public void paint(Graphics g) {
		g.drawImage(image, 0, 0, xdim, ydim, this);
	}
}

			
	