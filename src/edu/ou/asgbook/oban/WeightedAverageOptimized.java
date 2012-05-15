/**
 * 
 */
package edu.ou.asgbook.oban;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.PointObservations;
import edu.ou.asgbook.dataset.DailyRainfall;
import edu.ou.asgbook.dataset.MadisTemperature;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * @author Valliappa.Lakshmanan
 *
 */
public class WeightedAverageOptimized extends WeightedAverage {
	private final double[][] wtKernel;
	
	@Override
	public LatLonGrid analyze(PointObservations data){
		LatLonGrid grid = ObjectiveAnalysisUtils.createBoundingGrid(data, latres, lonres);
		double[][] sum = new double[grid.getNumLat()][grid.getNumLon()];
		double[][] sumwt = new double[grid.getNumLat()][grid.getNumLon()];
		int[][] numpts = new int[grid.getNumLat()][grid.getNumLon()];
		PointObservations.ObservationPoint[] points = data.getPoints();
		
		final int half_rows = wtKernel.length / 2;
		final int half_cols = wtKernel.length / 2;
		for (int k=0; k < points.length; ++k){
			final int row = grid.getRow(points[k]);
			final int col = grid.getCol(points[k]);
			if ( points[k].getValue() != data.getMissing() ){
				for (int m=-half_rows; m <= half_rows; ++m){
					for (int n=-half_cols; n <= half_cols; ++n){
						final int i = row + m;
						final int j = col + n;
						final double wt = wtKernel[m+half_rows][n+half_rows];
						if ( wt > 0 && grid.isValid(i, j)){	
							sum[i][j] += points[k].getValue() * wt;
							sumwt[i][j] += wt;
							numpts[i][j] ++;
						}
					}
				}
			}
		}
		
		for (int i=0; i < grid.getNumLat(); ++i){
			for (int j=0; j < grid.getNumLon(); ++j){
				if ( numpts[i][j] >= minPoints ){
					grid.setValue(i, j, (int) Math.round(sum[i][j]/sumwt[i][j]));
				} else {
					grid.setValue(i, j, grid.getMissing());
				}
			}
		}
		return grid;
	}

	private static double[][] computeWeightKernel(WeightFunction wtFunc, double latres, double lonres){
		// find size of kernel
		int half_rows, half_cols;
		for (half_rows = 0; ; ++half_rows){
			double wt = wtFunc.computeWt(latres*half_rows, 0);
			if ( wt < 0 ){
				break;
			}
		}
		for (half_cols = 0; ; ++half_cols){
			double wt = wtFunc.computeWt(0, lonres*half_cols);
			if ( wt < 0 ){
				break;
			}
		}
		// form  kernel and compute weights
		double[][] kernel = new double[2*half_rows+1][2*half_cols+1];
		for (int i=0; i < kernel.length; ++i){
			for (int j=0; j < kernel[0].length; ++j){
				double latdist = latres*(i - half_rows);
				double londist = lonres*(j - half_cols);
				kernel[i][j] = wtFunc.computeWt(latdist, londist);
			}
		}
		System.out.println("Precomputed " + kernel.length + "x" + kernel[0].length + " weights");
		return kernel;
	}
	
	public WeightedAverageOptimized(WeightFunction wtFunc, double latres, double lonres,int minPoints) {
		super(wtFunc, latres, lonres, minPoints);
		this.wtKernel = computeWeightKernel(wtFunc, latres, lonres);
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("obanopt");
		run(DailyRainfall.read(DailyRainfall.TN_Oct2010), out, "Precip24H", 2);
		run(MadisTemperature.read(MadisTemperature.TN_Oct2010), out, "Temperature_pass1", 1);
		run(MadisTemperature.read(MadisTemperature.TN_Oct2010), out, "Temperature_pass2", 2);
		run(MadisTemperature.read(MadisTemperature.TN_Oct2010), out, "Temperature_pass3", 3);
		run(MadisTemperature.read(MadisTemperature.TN_Oct2010), out, "Temperature_pass10", 10);
	}
	
	private static void run(PointObservations data, File out, String name, int numPasses) throws Exception {
		double meansep = ObjectiveAnalysisUtils.computeMeanDistance(data);
		System.out.println("Objectively analyzing " + data.getPoints().length + " pts with a mean separation of " + meansep);
		WeightFunction wtFunc = new CressmanWeighting(3*meansep);
		WeightedAverageOptimized analyzer = new WeightedAverageOptimized(wtFunc, 0.01, 0.01, 1);

		long startTime = System.nanoTime();
		LatLonGrid grid = analyzer.analyze(data, numPasses, 0, data.getMaxValue());
		System.out.println("Took " + (System.nanoTime() - startTime)/(1000*1000.0*1000) + " seconds");
		
		// write output
		KmlWriter.write(grid, out, name, PngWriter.createCoolToWarmColormap());
	}
}
