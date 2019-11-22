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
 * <p>Title: Levenberg-Marquardt</p>
 * <p>Description: Perfoms data fitting to a non linear model using the
 * Levenberg-Marquadrt method. Ported to Java from the Numerical
 * Recipes in C. Press, Teukolsky, Vetterling,and  Flannery. 2nd edition.
 * Cambridge University Press, 1992.</p>
 */

public class LevenbergMarquardt {

    private static final int ITMAX = 100;

    /**
     * Calls method mrqMin(double x[], double y[], double sig[], double a[],
                                boolean ia[], ModelFunction f,
                                double alamda, int itmax)
     with itmax=100.
     * @param x double[]
     * @param y double[]
     * @param sig double[]
     * @param a double[]
     * @param ia boolean[]
     * @param f Function
     * @param alamda double
     * @return double
     */
    public static double mrqMin(double x[], double y[], double sig[], double a[],
                                boolean ia[], Function f,
                                double alamda) {
        return mrqMin(x, y, sig, a, ia, f, alamda, ITMAX);
    }

    /**
     * Levenberg-Marquardt method, attempting to reduce the value chi2 of a fit
     * between a set of data points x[1..ndata], y[1..ndata] with individual
     * standard deviations sig[1..ndata], and a nonlinear function dependent
     * on ma coefficients a[1..ma]. The input array ia[1..ma] indicates by
     * true, entries those components of a that should be fitted for, and
     * by false, entries those components that should be held fixed at their
     * input values. The program returns  current best-fit values for the
     * parameters a[1..ma], and chi2 = chisq. Supply a Function object, f,
     * that evaluates the fitting function y, and its derivatives dyda[1..ma]
     * with respect to the fitting parameters a at x. On the first call provide
     * an initial guess for the parameters a, and set alamda to some small
     * value, e.g. alambda=0.001. If a step succeeds chisq becomes smaller
     * and alamda decreases by a factor of 10. If a step fails alamda grows
     * by a factor of 10.
     * @param x double[]
     * @param y double[]
     * @param sig double[]
     * @param a double[]
     * @param ia boolean[]
     * @param f Function
     * @param alamda double
     * @param itmax int
     * @return double
     *
     * Daniel Sage: PixFRET modification:
     * return the number of iteration instead the chi.
     */
    public static int mrqMin(double x[], double y[], double sig[], double a[],
                                boolean ia[], Function f,
                                double alamda, int itmax) {

        int rep = 0;
        boolean done = false;
        double eps = 0;
        int mfit = 0;
        int j, k, l;
        int ma = a.length;
        double ochisq = 0, chisq;
		int iter = 0;
		
        double[][] covar = new double[ma][ma];
        double[][] alpha = new double[ma][ma];
        double[] beta = new double[ma];
        double[] atry = new double[ma];
        double[] da = new double[ma];

        double[] oneda;

        //initialization
        for (mfit = 0, j = 0; j < ma; j++) {
            if (ia[j]) {
                mfit++;
            }
        }
        oneda = new double[mfit];
        chisq = mrqcof(x, y, sig, a, ia, alpha, beta, f);
        ochisq = chisq;
        for (j = 0; j < ma; j++) {
            atry[j] = a[j];
        }

        do {
            //Alter linearized fitting matrix, by augmenting diagonal elements.
            for (j = 0; j < mfit; j++) {
                for (k = 0; k < mfit; k++) {
                    covar[j][k] = alpha[j][k];
                }
                covar[j][j] = alpha[j][j] * (1.0 + (alamda));
                oneda[j] = beta[j];
            }

            Cholesky.solve(covar, oneda, oneda); //Matrix solution.

            for (j = 0; j < mfit; j++) {
                da[j] = oneda[j];
            }

            for (j = 0, l = 0; l < ma; l++) {
                if (ia[l]) {
                    atry[l] = a[l] + da[j++];
                }
            }
            chisq = mrqcof(x, y, sig, atry, ia, covar, da, f);
            eps = Math.abs(chisq - ochisq);
//System.out.println("Iter:" + iter + " a[0]:" + a[0] + " a[1]:"+ a[1] + " " + eps); 

            if (chisq < ochisq) {
                //Success, accept the new solution.
                alamda *= 0.1;
                ochisq = chisq;
                for (j = 0; j < mfit; j++) {
                    for (k = 0; k < mfit; k++) {
                        alpha[j][k] = covar[j][k];
                    }
                    beta[j] = da[j];
                }
                for (l = 0; l < ma; l++) {
                    a[l] = atry[l];
                }
            }
            else {
                //Failure, increase alamda and return.
                alamda *= 10.0;
                chisq = ochisq;
            }
            iter++;
            if (eps > 0.001) {
                rep = 0;
            }
            else {
                rep++;
                if (rep == 4) {
                    done = true;
                }
            }

        }
        while (iter < itmax && !done);

        //return chisq;
        return iter;
    }
    
    /**
     * Used by mrqmin to evaluate the linearized fitting matrix alpha,
     * and vector beta as in "NR in C"(15.5.8), and calculate chi2.
     *
     * @param x double[]
     * @param y double[]
     * @param sig double[]
     * @param a double[]
     * @param ia boolean[]
     * @param alpha double[][]
     * @param beta double[]
     * @param f LMfunc
     * @return double
     */
    public static double mrqcof(double x[], double y[], double sig[],
                                double a[], boolean ia[],
                                double alpha[][], double beta[],
                                Function f) {

        int ndata = x.length;
        int ma = a.length;
        double chisq;
        int i, j, k, l, m, mfit = 0;
        double ymod, wt, sig2i, dy;
        double[] dyda = new double[ma];

        for (j = 0; j < ma; j++) {
            if (ia[j]) {
                mfit++;
            }
        }

        //Initialize(symmetric) alpha, beta.
        for (j = 0; j < mfit; j++) {
            for (k = 0; k <= j; k++) {
                alpha[j][k] = 0;
            }
            beta[j] = 0;
        }

        chisq = 0;

        //Summation loop over all data.
        for (i = 0; i < ndata; i++) {
            double[] xi = new double[1];
            xi[0] = x[i];
            ymod = f.eval(xi, a);
            for (k = 0; k < a.length; k++) {
                dyda[k] = f.grad(xi, a, k);
            }

            sig2i = 1.0 / (sig[i] * sig[i]);
            dy = y[i] - ymod;
            for (j = 0, l = 0; l < ma; l++) {
                if (ia[l]) {
                    wt = dyda[l] * sig2i;
                    for (k = 0, m = 0; m <= l; m++) {
                        if (ia[m]) {
                            alpha[j][k++] += wt * dyda[m];
                        }
                    }
                    beta[j] += dy * wt;
                    j++;
                }
            }
            chisq += dy * dy * sig2i; //And find chi2.
        }

        //Fill in the symmetric side of alpha
        for (j = 1; j < mfit; j++) {
            for (k = 0; k < j; k++) {
                alpha[k][j] = alpha[j][k];
            }
        }
        return chisq;
    } //mrqcof

} //LM
