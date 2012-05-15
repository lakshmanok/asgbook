/**
 * 
 */
package edu.ou.asgbook.motion;

import java.text.DecimalFormat;
import java.util.Random;

import Jama.Matrix;

/**
 * For the time smoothing of motion vectors
 * 
 * @author valliappa.lakshmanan
 *
 */
public class KalmanFilter {
	private int MAX_HISTORY = 10;
	private int k;
	private Matrix x_k;
	private Matrix p_k;
	private double R_k;
	private Matrix Q_k;

	private final Matrix H = make_HK(); // H_k
	private final Matrix HT= H.copy().transpose();
	private final Matrix phi = make_phiK();
	private final Matrix phiT = phi.copy().transpose();

	/**
	 * Start off with an initial estimate for the position and velocity
	 */
	public KalmanFilter(double x_0, double dx_0){
		init(x_0, dx_0);
	}

	public void init(double x_0, double dx_0){
		k = 0;
		// x_k
		x_k = new Matrix(2,1);
		x_k.set(0,0, x_0);
		x_k.set(1,0, dx_0);

		// p_k
		p_k = new Matrix( 2, 2 ); // all zero

		// assume unit white noise for errors before we see any observations.
		R_k = 1;
		Q_k = Matrix.identity(2,2);
	}

	public boolean updated(){
		return ( k > 0 );
	}

	public void update(double z_k ){
		++k; // observation number ...
		if ( MAX_HISTORY > 0 && k > MAX_HISTORY ){
		    k = MAX_HISTORY; // k is used in computing Q_k and R_k
		}
		
		// P_k+1 and x_k+1 will be computed on next turn around so that getValue()
		// works correctly ...
		p_k = phi.copy().times(p_k).times(phiT).plus(Q_k);
		x_k = phi.copy().times(x_k);


		// Kalman gain
		double inv = H.copy().times(p_k).times(HT).get(0,0) + R_k;
		final Matrix K_k = p_k.copy().times(HT).times( 1.0 / inv );

		// observation error
		final double v_k = z_k - H.copy().times(x_k).get(0,0);

		// update x_k
		final Matrix update = K_k.copy().times(v_k);
		x_k = x_k.plus( update );

		// estimate R_k, covariance of observation error to use next time 'round
		R_k = ( (k-1) * R_k + v_k * v_k ) / k;

		// estimate Q_k, covariance of model error to use in P_k+1 computation
		if ( k != 1 ){ // when k is 1, x_k=old_x_k and so Q_k would become 0
			final Matrix wkT = update.copy().transpose();
			final Matrix wk_wkT = update.copy().times(wkT);
			Q_k = Q_k.times(k-1).plus(wk_wkT).times(1.0/k);
		}

		// update error covariance for updated estimate
		p_k = Matrix.identity(2,2).minus(K_k.copy().times(H)).times(p_k);

		if ( finite(getValue()) == false || finite(getRateOfChange()) == false ){
			double new_val = getValue();
			if ( finite(new_val) == false ) new_val = 0;
			double new_rate = getRateOfChange();
			if ( finite(new_rate) == false ) new_rate = 0;
			init( new_val, new_rate );
		}
	}

	/** get the smoothed centroid position */
	public double getValue(){
		return x_k.get(0, 0);
	}
	public double getRateOfChange(){
		return x_k.get(1,0);
	}

	private boolean finite(double x){
		return !( Double.isInfinite(x) || Double.isNaN(x) );
	}

	private Matrix make_HK(){
		Matrix hk = new Matrix(1,2); // zero
		hk.set(0,0, 1.0);
		return hk;
	}
	private Matrix make_phiK(){
		Matrix phi = Matrix.identity(2,2);
		phi.set(0,1, 1.0);
		return phi;
	}
	
	private static DecimalFormat decimalformat = new DecimalFormat("0.0");
	private static Random random = new Random();
	private static String df(double d){
		return decimalformat.format(d);
	}
	private static double noise(){
		return random.nextGaussian() * 5;
	}
	public static void main(String[] args) throws Exception {
		double[] truex = new double[20];
		double[] trueu = new double[truex.length];
		double[] obsx = new double[truex.length];
		truex[0] = 5;
		trueu[0] = 3;
		obsx[0]= truex[0] + noise();

		KalmanFilter kalman = new KalmanFilter(obsx[0], trueu[0]); // assume that we have a reasonable guess of u to start ...
		double true_acc = 0.2;
		
		System.out.println("true x & true velocity & observed x & estimate of x & estimate of velocity \\\\");
		for (int i=1; i < truex.length; ++i){
			trueu[i] = trueu[i-1] + true_acc;
			truex[i] = truex[i-1] + trueu[i-1];
			obsx[i] = noise() + truex[i];
			kalman.update(obsx[i]);
			System.out.println( df(truex[i]) + " & " + df(trueu[i]) + " & " + df(obsx[i]) + " & " + df(kalman.getValue()) + " & " + df(kalman.getRateOfChange()) + " \\\\");
		}
		
		
		
	}
	
}
