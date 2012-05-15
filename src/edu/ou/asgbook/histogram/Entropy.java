/**
 * 
 */
package edu.ou.asgbook.histogram;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.dataset.SurfaceAlbedo;

/**
 * Compute entropy from a histogram.
 * 
 * @author v.lakshmanan
 *
 */
public class Entropy {
	public static double computeEntropy(Histogram hist){
		float[] prob = hist.calcProb();
		double entropy = 0;
		for (int i=0; i < prob.length; ++i){
			if ( prob[i] > 0 ){
				double plogp = prob[i] * Math.log(prob[i]);
				entropy -= plogp;
			}
		}
		// to base 2
		return (entropy / Math.log(2.0));
	}
	
	public static void main(String[] args) throws Exception {
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA);
		Histogram hist = new Histogram(0, 100, 100 );
		hist.update(popdensity);
		double e1 = Entropy.computeEntropy(hist);
		System.out.println("Population density entropy = " + e1);
		
		LatLonGrid albedo = SurfaceAlbedo.read(SurfaceAlbedo.CONUS, 100);
		hist = new Histogram(0, 30, 30 );
		hist.update(albedo);
		double e2 = Entropy.computeEntropy(hist);
		System.out.println("surface albedo entropy = " + e2);
	}
}
