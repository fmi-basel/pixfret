package pixfret;

public class Function3 {

    /**
     * Evaluates the model at point x (may be mulidimensional).
     * @param x double[] Point where we evaluate the model function.
     * @param a double[] Model estimators.
     * @return double
     */
    double eval(double[] x, double[] a) {
    	return a[0] + a[1] * (Math.exp(x[0]*a[2]));
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
    			return 1.0;
    		case 1:
    			return Math.exp(x[0]*a[2]);
     		default:
   				return a[1] * x[0] * Math.exp(x[0]*a[2]);
     	}
    }
}
