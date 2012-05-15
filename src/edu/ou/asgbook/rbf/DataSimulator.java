/**
 * 
 */
package edu.ou.asgbook.rbf;

import java.io.File;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Simulates RBF data to be fit.
 * 
 * @author v.lakshmanan
 *
 */
public class DataSimulator {
	public static LatLonGrid simulateData(Pixel[] centers, double[] sigmax, double[] sigmay, int nrows, int ncols){
		LatLon nwCorner = new LatLon(38, -100);
		double latres = 0.01;
		double lonres = 0.01;
		LatLonGrid result = new LatLonGrid(nrows, ncols, -999, nwCorner, latres, lonres );
		simulateData(result, centers, sigmax, sigmay);
		return result;
	}
	
	public static void simulateData(LatLonGrid result, Pixel[] centers, double[] sigmax, double[] sigmay){
		for (int i=0; i < result.getNumLat(); ++i) for (int j=0; j < result.getNumLon(); ++j){
			double tot = 0;
			for (int k=0; k < centers.length; ++k){
				double xdist = i - centers[k].getX();
				double ydist = j - centers[k].getY();
				double xnorm = (xdist*xdist) / (sigmax[k] * sigmax[k]);
				double ynorm = (ydist*ydist) / (sigmay[k] * sigmay[k]);
				double wt = Math.exp(-(xnorm + ynorm));
				tot += wt * centers[k].getValue();
			}
			if ( tot > 0 ){
				result.setValue(i, j, (int) Math.round(tot));
			} else {
				result.setValue(i, j, 0);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		int nrows = 100;
		int ncols = 100;
		Pixel[] centers = new Pixel[]{ new Pixel(nrows/4,ncols/3,20), new Pixel(nrows/3,ncols/2,10) };
		double[] sigmax = new double[] { nrows/12, ncols/8 };
		double[] sigmay = new double[] { nrows/8, ncols/12 };
		LatLonGrid m = DataSimulator.simulateData(centers, sigmax, sigmay, nrows, ncols);
		
		// write out as image, for viewing
		File out = OutputDirectory.getDefault("rbf");
		KmlWriter.write(m, out, "simulated", PngWriter.createCoolToWarmColormap());
	
		System.out.println("Done");
	}
}
