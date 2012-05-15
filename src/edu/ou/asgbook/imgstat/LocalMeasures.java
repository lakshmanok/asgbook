/**
 * 
 */
package edu.ou.asgbook.imgstat;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.ScalarStatistic;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Statistics computed in the neighborhood of a pixel.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class LocalMeasures {
	private int hx,hy;
	private LatLonGrid input, mean, stdev, min, max;
	
	public LatLonGrid getMean() {
		return mean;
	}

	public LatLonGrid getStdDeviation() {
		return stdev;
	}

	public LatLonGrid getMin() {
		return min;
	}

	public LatLonGrid getMax() {
		return max;
	}

	public LocalMeasures(LatLonGrid input, int Nx, int Ny){
		this.hx = Nx/2;
		this.hy = Ny/2;
		this.input = input;
		this.mean = LatLonGrid.copyOf(input);
		this.stdev  = LatLonGrid.copyOf(input);
		this.min  = LatLonGrid.copyOf(input);
		this.max  = LatLonGrid.copyOf(input);
		compute();
	}
	
	private ScalarStatistic computeLocalStatistic(int x, int y){
		ScalarStatistic stat = new ScalarStatistic();
		for (int m=-hx; m <= hx; ++m){
			for (int n=-hy; n <= hy; ++n){
				if ( input.isValid(x+m,y+n) ){
					int inval = input.getValue(x+m,y+n);
					if (inval != input.getMissing()){
						stat.update(inval);
					}
				}
			}
		}
		return stat;
	}
	
	private void compute(){
		int nrows = input.getNumLat();
		int ncols = input.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			ScalarStatistic stat = computeLocalStatistic(i, j);
			if (stat.getNumSamples() > 0){
				mean.setValue(i, j, (int)Math.round(stat.getMean()));
				stdev.setValue(i,j, (int)Math.round(stat.getStdDeviation()));
				min.setValue(i,j, (int)Math.round(stat.getMin()));
				max.setValue(i,j, (int)Math.round(stat.getMax()));
			} else {
				mean.setValue(i, j, input.getMissing());
				stdev.setValue(i, j, input.getMissing());
				min.setValue(i, j, input.getMissing());
				max.setValue(i, j, input.getMissing());
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		// log-scaled population density
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling());
		popdensity = popdensity.crop(900, 2500, 200, 200);
		File out = OutputDirectory.getDefault("localstat");

		KmlWriter.write(popdensity, out, "popdensity", PngWriter.createCoolToWarmColormap());
		for (int neigh = 5; neigh < 12; neigh += 6){ // 5, 11
			LocalMeasures stat = new LocalMeasures(popdensity, neigh, neigh);
			KmlWriter.write(stat.getMean(), out, "mean_" + neigh, PngWriter.createCoolToWarmColormap());
			KmlWriter.write(stat.getStdDeviation(), out, "stdev_" + neigh, PngWriter.createCoolToWarmColormap());
			KmlWriter.write(stat.getMin(), out, "min_" + neigh, PngWriter.createCoolToWarmColormap());
			KmlWriter.write(stat.getMax(), out, "max_" + neigh, PngWriter.createCoolToWarmColormap());
		}
	}
}
