/**
 * 
 */
package edu.ou.asgbook.rbf;

import Jama.Matrix;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;

/**
 * Finds best fit of a spatial grid to a sum of Gaussians when the centers
 * and sigmas of the Gaussians are known.
 * 
 * @author v.lakshmanan
 *
 */
public class RadialBasisFunction {
	/** Provide the center locations and this function will fill in the optimal amplitude */
	public static double[] fit(LatLonGrid data, Pixel[] center, double[] sigmax, double[] sigmay){
		// inv( transpose(H) * H) * transpose(H) * data
		int p = data.getNumLat() * data.getNumLon();
		int m = center.length;
		Matrix H = new Matrix(p, m);
		Matrix ycap = new Matrix(p, 1);
		for (int i=0; i < p; ++i){
			int x = i / data.getNumLon();
			int y = i % data.getNumLon();
			for (int j=0; j < m; ++j){
				double xdist = x - center[j].getX();
				double ydist = y - center[j].getY();
				double xnorm = (xdist*xdist) / (sigmax[j] * sigmax[j]);
				double ynorm = (ydist*ydist) / (sigmay[j] * sigmay[j]);
				double wt = Math.exp(-(xnorm + ynorm));
				H.set(i,j, wt);
			}
			ycap.set(i, 0, data.getValue(x, y));
		}
		// H.print(H.getColumnDimension(), H.getRowDimension());
		
		Matrix HT = H.transpose();
		Matrix HTH = HT.times(H);
		Matrix HTHinv = HTH.inverse();
		Matrix HTHinvHT = HTHinv.times(HT);
		
		return HTHinvHT.times(ycap).transpose().getArray()[0];
	}
	
	public static void main(String[] args){
		int nrows = 100;
		int ncols = 100;
		Pixel[] centers = new Pixel[]{ new Pixel(nrows/4,ncols/3,20), new Pixel(nrows/3,ncols/2,10) };
		double[] sigmax = new double[] { nrows/12, ncols/8 };
		double[] sigmay = new double[] { nrows/8, ncols/12 };
		LatLonGrid m = DataSimulator.simulateData(centers, sigmax, sigmay, nrows, ncols);
		
		System.out.println("Created data of size " + m.getNumLat() + "x" + m.getNumLon());
		double[] weights = fit( m, centers, sigmax, sigmay );
		for (int i=0; i < weights.length; ++i){
			System.out.println("Actual: " + centers[i].getValue() + " RBF: " + +weights[i]);
		}
	}
}
