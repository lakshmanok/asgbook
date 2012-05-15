/**
 * 
 */
package edu.ou.asgbook.imgstat;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.imgstat.GraylevelCooccurenceMatrix.Direction;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Statistics computed in the neighborhood of a pixel.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class StructuralMeasures {
	private int hx,hy,min,incr,bins;
	private LatLonGrid input;
	private LatLonGrid[] uniformity = new LatLonGrid[Direction.values().length];
	private LatLonGrid[] maximumProbability = new LatLonGrid[Direction.values().length];
	
	public StructuralMeasures(LatLonGrid input, int Nx, int Ny, int min, int incr, int bins){
		this.hx = Nx/2;
		this.hy = Ny/2;
		this.input = input;
		this.min = min;
		this.incr = incr;
		this.bins = bins;
		for (int i=0; i < uniformity.length; ++i){
			this.uniformity[i] = LatLonGrid.copyOf(input);
			this.maximumProbability[i] = LatLonGrid.copyOf(input);
		}
		this.compute();
	}
	
	private void compute(){
		int nrows = input.getNumLat();
		int ncols = input.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int k = 0;
			for (Direction dir : Direction.values()){
				GraylevelCooccurenceMatrix glcm = new GraylevelCooccurenceMatrix(input,i,j,dir,hx,hy,min,incr,bins);
				uniformity[k].setValue(i,j, (int)Math.round(100*glcm.computeUniformity()));
				maximumProbability[k].setValue(i,j, (int)Math.round(100*glcm.computeMaximumProbability()));
				++k;
			}
		}
	}
	
	public LatLonGrid[] getUniformity(){
		return uniformity;
	}
	
	public LatLonGrid[] getMaximumProbability(){
		return maximumProbability;
	}
	
	public static void main(String[] args) throws Exception {
		// log-scaled population density
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling());
		popdensity = popdensity.crop(900, 2500, 200, 200);
		File out = OutputDirectory.getDefault("glcmstat");

		KmlWriter.write(popdensity, out, "popdensity", PngWriter.createCoolToWarmColormap());
		StructuralMeasures stat = new StructuralMeasures(popdensity, 11, 11, 100, 500, 50);
		for (int i=0; i < Direction.values().length; ++i){
			KmlWriter.write(stat.getUniformity()[i], out, "uniformity_" + Direction.values()[i].toString().toLowerCase(), PngWriter.createCoolToWarmColormap());
		}
		for (int i=0; i < Direction.values().length; ++i){
			KmlWriter.write(stat.getMaximumProbability()[i], out, "maxprob_" + Direction.values()[i].toString().toLowerCase(), PngWriter.createCoolToWarmColormap());
		}
	}
	

}
