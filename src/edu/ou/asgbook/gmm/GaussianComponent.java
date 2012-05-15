/**
 * 
 */
package edu.ou.asgbook.gmm;

import edu.ou.asgbook.core.Pixel;

/**
 * Component of a Gaussian Mixture Model
 * 
 * @author valliappa.lakshmanan
 * 
 */
public class GaussianComponent {
	private double mux, muy;
	private double sxx, syy, sxy;
	private double wt, det, denom;

	/** Initialize with known values */
	public GaussianComponent(double cx, double cy, double varx, double vary,
			double sigmaxy, double inwt) {
		mux = cx;
		muy = cy;
		sxx = varx;
		syy = vary;
		sxy = sigmaxy;
		wt = inwt;
		det = Math.abs(sxx * syy - sxy * sxy);
		denom = 2 * Math.PI * Math.sqrt(det);
	}

	public GaussianComponent(double cx, double cy, double varx, double vary) {
		this(cx, cy, varx, vary, 0, 1);
	}

	public double getWeight() {
		return wt;
	}

	public double getCx() {
		return mux;
	}

	public double getCy() {
		return muy;
	}

	public double getSigmax() {
		return Math.sqrt(sxx);
	}

	public double getSigmay() {
		return Math.sqrt(syy);
	}

	public double getSigmaxy() {
		return sxy;
	}

	private static class Expectation {
		double sumx = 0;
		double sumwt = 0;

		void update(double x, double wt) {
			sumx += x * wt;
			sumwt += wt;
		}

		double result() {
			if (sumwt > 0) {
				return (sumx / sumwt);
			} else
				return sumx;
		}
	}

	/** Finds best fit (the M-step in E-M) */
	public GaussianComponent(Pixel[] pixels, double[] wts) {
		int n_pts = pixels.length;
		if (wts.length != pixels.length){
			throw new IllegalArgumentException("Array lengths have to match");
		}
		// compute pi_k (wt)
		wt = 0;
		for (int i = 0; i < n_pts; ++i) {
			wt += wts[i];
		}
		wt /= n_pts;

		// mean
		Expectation wm_x = new Expectation(), wm_y = new Expectation();
		for (int i = 0; i < n_pts; ++i) {
			wm_x.update(pixels[i].getX(), wts[i]);
			wm_y.update(pixels[i].getY(), wts[i]);
		}
		mux = wm_x.result();
		muy = wm_y.result();

		// covariance matrix
		Expectation wv_x = new Expectation();
		Expectation wv_y = new Expectation();
		Expectation cv_xy = new Expectation();
		for (int i = 0; i < n_pts; ++i) {
			double dx = pixels[i].getX() - mux;
			double dy = pixels[i].getY() - muy;
			wv_x.update(dx * dx, wts[i]);
			wv_y.update(dy * dy, wts[i]);
			cv_xy.update(dx * dy, wts[i]);
		}
		sxx = wv_x.result();
		syy = wv_y.result();
		sxy = cv_xy.result();

		final double EPSILON = 0.01; // at-least 1/10 pixel of variance ...
		if (sxx < EPSILON || syy < EPSILON) {
			det = denom = 0;
			return;
		}

		// normalizing constant
		det = (sxx * syy - sxy * sxy);
		denom = 2 * Math.PI * Math.sqrt(Math.abs(det)); // always positive

	}

	public double computeProbabilityDensityAt(Pixel p) {
		return computeProbabilityDensityAt(p.getX(), p.getY());
	}
	
	/**
	 * Value of Normal function at x,y given these parameters. You typically
	 * want to weight this contribution by getWeight() This goes into the E-step
	 * in E-M.
	 */
	public double computeProbabilityDensityAt(double x, double y) {
		if (denom < 0.00001) {
			return 0;
		} // singular
		double dx = x - mux;
		double dy = y - muy;
		double term = -(syy * dx * dx - 2 * sxy * dx * dy + sxx * dy * dy);
		double num = Math.exp(term / (2 * det));
		double result = num / denom;
		if (result > 1) {
			// usually because of numerical instability
			return 0;
		}
		return result;

	}

	public boolean isValid() {
		return denom > 0;
	}

	@Override
	public String toString() {
		return "center=(" + mux + "," + muy + ") covar=(" + sxx + "," + sxy
				+ "," + syy + ") wt=" + wt;
	}

}
