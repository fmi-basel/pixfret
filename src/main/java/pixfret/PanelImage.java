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

			
	
