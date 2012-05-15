/**
 * 
 */
package edu.ou.asgbook.oban;

import java.io.File;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.PointObservations;
import edu.ou.asgbook.dataset.DailyRainfall;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * 
 * Interpolation methods for point observations.  This is here only for
 * illustration; you should use the WeightedAverageOptimized
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class WeightedAverage {
	protected final WeightFunction wtFunc;
	protected final double latres;
	protected final double lonres;
	protected final int minPoints;
	
	public LatLonGrid analyze(PointObservations data){
		LatLonGrid grid = ObjectiveAnalysisUtils.createBoundingGrid(data, latres, lonres);
		PointObservations.ObservationPoint[] points = data.getPoints();
		for (int i=0; i < grid.getNumLat(); ++i){
			for (int j=0; j < grid.getNumLon(); ++j){
				LatLon gridpt = grid.getLocation(i, j);
				double sum = 0;
				double sumwt = 0;
				int n = 0;
				for (int k=0; k < points.length; ++k){
					if ( points[k].getValue() != data.getMissing() ){
						double wt = wtFunc.computeWt( points[k].getLat() - gridpt.getLat(), points[k].getLon() - gridpt.getLon());
						if ( wt > 0 ){
							sum += wt * points[k].getValue();
							sumwt += wt;
							++n;
						}
					}
				}
				if ( n >= minPoints ){
					grid.setValue(i, j, (int) Math.round(sum/sumwt));
				} else {
					grid.setValue(i, j, grid.getMissing());
				}
			}
		}
		return grid;
	}
	
	public LatLonGrid analyze(PointObservations data, int numPasses, int physicalMin, int physicalMax){
		LatLonGrid result = analyze(data); // pass #1
		final PointObservations.ObservationPoint[] points = data.getPoints();
		for (int pass=1; pass < numPasses; ++pass){
			// find error at each point
			PointObservations.ObservationPoint[] errors = new PointObservations.ObservationPoint[points.length];
			for (int k=0; k < points.length; ++k){
				int a = points[k].getValue();
				int b = result.getValue(points[k]);
				int error = 0;
				if ( a != data.getMissing() && b != result.getMissing() ){
					error = a - b;
				}
				errors[k] = new PointObservations.ObservationPoint(points[k].getLat(), points[k].getLon(), error);
			}
			// create a grid of errors and add this to the original grid
			LatLonGrid errGrid = analyze(new PointObservations(errors,data.getMissing()));
			add( result, errGrid , physicalMin, physicalMax);
		}
		return result;
	}
	
	private void add(LatLonGrid result, final LatLonGrid delta, int physicalMin, int physicalMax){
		for (int i=0; i < result.getNumLat(); ++i){
			for (int j=0; j < result.getNumLon(); ++j){
				int a = result.getData()[i][j];
				int b = delta.getData()[i][j];
				if ( a != result.getMissing() && b != delta.getMissing() ){
					int v = a + b;
					if ( v < physicalMin ) v = physicalMin;
					if ( v > physicalMax ) v = physicalMax;
					result.getData()[i][j] = v;
				}
			}
		}
	}

	public WeightedAverage(WeightFunction wtFunc, double latres, double lonres,int minPoints) {
		this.wtFunc = wtFunc;
		this.latres = latres;
		this.lonres = lonres;
		this.minPoints = minPoints;
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
		File out = OutputDirectory.getDefault("oban");
		KmlWriter.write(grid, out, "Precip24H", PngWriter.createCoolToWarmColormap());
	}
}
