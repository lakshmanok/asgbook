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
 * An interpolation method that uses exp(-1/r^2)
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class GaussWeighting implements WeightFunction {
	private final double sigmasq;
	private final double epsilonDistSq;
		
	/**
	 * @param sigma    Set extent of gaussian in degrees
	 */
	public GaussWeighting(double sigma) {
		this.sigmasq = sigma * sigma;
		this.epsilonDistSq = (3*3)*(2*sigmasq); // at distance of 3*sigma in both directions
	}

	@Override
	public double computeWt(double latdist, double londist){
		double r2 = latdist * latdist + londist * londist;
		if ( r2 < epsilonDistSq ){
			return Math.exp(-r2 / (2*sigmasq));
		} else {
			return INVALID_WEIGHT;
		}
	}
	
	public static void main(String[] args) throws Exception {
		PointObservations data = DailyRainfall.read(DailyRainfall.TN_Oct2010);
		
		double sigma = ObjectiveAnalysisUtils.computeMeanDistance(data);
		System.out.println("Objectively analyzing " + data.getPoints().length + " pts with a mean separation of " + sigma);
		WeightFunction wtFunc = new GaussWeighting(sigma);
		WeightedAverage analyzer = new WeightedAverage(wtFunc, 0.01, 0.01, 1);
		LatLonGrid grid = analyzer.analyze(data);
		
		// write output
		File out = OutputDirectory.getDefault("gaussoban");
		KmlWriter.write(grid, out, "Precip24H", PngWriter.createCoolToWarmColormap());
	}
}
