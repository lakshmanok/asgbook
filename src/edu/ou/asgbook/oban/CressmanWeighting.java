/**
 * 
 */
package edu.ou.asgbook.oban;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.PointObservations;
import edu.ou.asgbook.dataset.DailyRainfall;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * An interpolation method that uses 1/r^2
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class CressmanWeighting implements WeightFunction {
	private final double R2;
		
	/**
	 * @param radiusOfInfluence    Set extent of influence in degrees
	 */
	public CressmanWeighting(double radiusOfInfluence) {
		this.R2 = radiusOfInfluence * radiusOfInfluence;
	}

	@Override
	public double computeWt(double latdist, double londist){
		double r2 = latdist * latdist + londist * londist;
		if ( r2 > R2 ){
			return INVALID_WEIGHT;
		}
		double factor = r2/R2;
		return (1 - factor)/(1 + factor);
	}
	
	public static void main(String[] args) throws Exception {
		PointObservations data = DailyRainfall.read(DailyRainfall.TN_Oct2010);
		
		double meansep = ObjectiveAnalysisUtils.computeMeanDistance(data);
		System.out.println("Objectively analyzing " + data.getPoints().length + " pts with a mean separation of " + meansep);
		WeightFunction wtFunc = new CressmanWeighting(3*meansep);
		WeightedAverage analyzer = new WeightedAverage(wtFunc, 0.01, 0.01, 1);

		long startTime = System.nanoTime();
		final int numPasses = 2;
		LatLonGrid grid = analyzer.analyze(data, numPasses, 0, data.getMaxValue());
		System.out.println("Took " + (System.nanoTime() - startTime)/(1000*1000.0*1000) + " seconds");
		
		// write output
		File out = OutputDirectory.getDefault("cressman");
		KmlWriter.write(grid, out, "Precip24H", PngWriter.createCoolToWarmColormap());
	}
}
