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
 * <p>Title: Cholesky Decomposition</p>
 * <p>Description: Performs a Cholesky decomposition of a matrix and solve
 * a linear system using this decomposition. Ported to Java from the Numerical
 * Recipes in C. Press, Teukolsky, Vetterling,and  Flannery. 2nd edition.
 * Cambridge University Press, 1992.</p>
 */

public class Cholesky {

    /**
     * Given a positive-definite symmetric matrix A[1..n][1..n], this method
     * constructs its Cholesky decomposition, A = L &sdot; L' . On input, only the
     * upper triangle of a need be given; it is not modified. The Cholesky
     * factor L is returned in the lower triangle of a, except for its diagonal
     * elements which are returned in p[1..n].
     * @param A double[][] Input matrix.
     * @param p double[]
     * @return boolean Returns false if the decomposition is not possible.
     */
    public static boolean decomp(double[][] A, double[] p) {
        int n = A.length;
        int i, j, k;
        double sum;
        for (i = 0; i < n; i++) {
            for (j = 0; j < n; j++) {
                sum = A[i][j];
                for (k = i - 1; k >= 0; k--) {
                    sum -= (A[i][k] * A[j][k]);
                }
                if (i == j) {
                    if (sum <= 0.) {
                        return false; // not positive definite
                    }
                    p[i] = Math.sqrt(sum);
                }
                else {
                    A[j][i] = sum / p[i];
                }
            }
        }
        return true;
    } //decomp

    /**
     * Solves a the linear system Ax=b.
     * @param A double[][] Is the result of decomp(A)
     * @param p double[] The resulting diagonal vector.
     * @param b double[]
     * @param x double[]
     */
    private static void solve(double[][] A, double[] p, double[] b, double[] x) {
        int n = A.length;
        int i, k;
        double sum;
        //Solve L ⋅ y = b, storing y in x.
        for (i = 0; i < n; i++) {
            sum = b[i];
            for (k = i - 1; k >= 0; k--) {
                sum -= (A[i][k] * x[k]);
            }
            x[i] = sum / p[i];
        }

        //Solve L' ⋅ x = y.
        for (i = n - 1; i >= 0; i--) {
            sum = x[i];
            for (k = i + 1; k < n; k++) {
                sum -= (A[k][i] * x[k]);
            }
            x[i] = sum / p[i];
        }
    } //solve

    /**
     * Solves the linear system Ax=b.
     * @param A double[][]
     * @param x double[]
     * @param b double[]
     * @return boolean returns false if the system can  not be solved using
     * Cholesky decomposition.
     */
    public static boolean solve(double[][] A, double[] x, double[] b) {
        double[] p = new double[A.length];
        if (!decomp(A, p)) {
            return false;
        }
        solve(A, p, b, x);
        return true;
    } //solve

} //Cholesky
