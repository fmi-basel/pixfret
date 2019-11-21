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
 * BT = m + b * (exp(x*e)-exp(xo*e) 
 *		b is a[0]
 *		e is a[1]
 *		m is a fix parameter
 *		xo is a fix paramter
 */
public class Function1 {

	private double m;
	private double xo;
	
	public Function1(double m, double xo) {
		this.m = m;
		this.xo = xo;
System.out.println(" " + m + " " + xo);
	}
	
    /**
     * Evaluates the model at point x (may be mulidimensional).
     * @param x double[] Point where we evaluate the model function.
     * @param a double[] Model estimators.
     * @return double
     */
    double eval(double[] x, double[] a) {
    	return m + a[0] * (Math.exp(x[0]*a[1]) - Math.exp(xo*a[1]));
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
    			return Math.exp(x[0]*a[1]) - Math.exp(xo*a[1]);
     		default:
   				return a[0] * x[0] * (Math.exp(x[0]*a[1]) - Math.exp(xo*a[1]));
     	}
    }

}
