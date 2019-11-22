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

/**
 *
 * <p>Title: Function class</p>
 * <p>Description: Class that defines the model function used to fit data
 * using the Levenberg-Marquardt method. The model functions are of the form
 *  f(x[],a[]), where x[] is the point at which we read data, and a[] are the
 * parameters of the model.</p>
 *
 * The specific model for the PixFRET plugin is:
 * BT = b*exp(Donor*e)
 *		b is a[0]
 *		e is a[1]
 */
public class Function2 {

    /**
     * Evaluates the model at point x (may be mulidimensional).
     * @param x double[] Point where we evaluate the model function.
     * @param a double[] Model estimators.
     * @return double
     */
    double eval(double[] x, double[] a) {
    	return a[0] * Math.exp(x[0]*a[1]);
    }

    /**
     * Returns the kth component of the gradient df(x,a)/da_k
     * @param x double[]
     * @param a double[]
     * @param ak int
     * @return double
     */
    double grad(double[] x, double[] a, int ak) {
    	switch(ak) {
    		case 0:
    			return Math.exp(x[0]*a[1]);
     		default:
   				return a[0] * x[0] * Math.exp(x[0]*a[1]);
     	}
    }

}
