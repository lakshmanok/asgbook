/**
 * 
 */
package edu.ou.asgbook.imgstat;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.SurfaceAlbedo;
import edu.ou.asgbook.histogram.Histogram;
import edu.ou.asgbook.histogram.HistogramBinSelection;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Develops a quantization scheme using vector quantization
 * 
 * @author valliappa.lakshmanan
 *
 */
public class VectorQuantizer {
	public static class Vector {
		double[] values;
		Vector(int n){
			values = new double[n];
		}
		public double computeDist(LatLonGrid[] params, int x, int y) {
			double totdist = 0;
			for (int p=0; p < values.length; ++p){
				int val = params[p].getValue(x,y);
				if (val != params[p].getMissing()){
					double dist = val - values[p];
					totdist += dist*dist;
				}
			}
			return totdist;
		}
		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder("{");
			for (int p=0; p < values.length; ++p){
				sb.append(Math.round(values[p]*100)/100.0);
				sb.append(",");
			}
			sb.append("}");
			return sb.toString();
		}
	}
	
	private Vector[] centroids;


	/**
	 * @param params  where to get the vectors from.  These grids should be normalized.
	 * @param K
	 */
	public VectorQuantizer(LatLonGrid[] params, int K){
		int nrows = params[0].getNumLat();
		int ncols = params[0].getNumLon();
		// 1. initialize centroid with mean
		centroids = new Vector[1];
		centroids[0] = new Vector(params.length); // zero
		for (int p=0; p < params.length; ++p){
			int N = 0;
			for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
				int val = params[p].getValue(i,j);
				if ( val != params[p].getMissing() ){
					centroids[0].values[p] += val;
					++N;
				}
			}
			if (N > 0){
				centroids[0].values[p] /= N;
			}
		}
		System.out.println(this);
		while ( centroids.length < K ){
			// 2. split the centroids
			final double epsilon = 0.1;
			centroids = split(centroids, epsilon);
			// System.out.println(this);
			// 3. update centroid
			centroids = computeCentroids(params);
			// System.out.println(this);
		}
		
		System.out.println(this);
	}

	public static LatLonGrid[] normalize(LatLonGrid[] params){
		LatLonGrid[] norm = new LatLonGrid[params.length];
		for (int i=0; i < params.length; ++i){
			norm[i] = normalize(params[i]);
		}
		return norm;
	}
	
	/**
	 * The output grid ranges from 0 to 100
	 * @param input
	 * @return
	 */
	public static LatLonGrid normalize(LatLonGrid data){
		LatLonGrid result = LatLonGrid.copyOf(data);
		result.setMissing(-1);
		// find range
		int min = data.getMissing();
		int max = data.getMissing();
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int val = data.getValue(i,j);
			if ( val != data.getMissing() ){
				if (min == data.getMissing() || val < min){
					min = val;
				}
				if (max == data.getMissing() || val > max){
					max = val;
				}
			}
		}
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int val = data.getValue(i,j);
			if ( val != data.getMissing() ){
				result.setValue(i, j, (int)Math.round((100.0*(val-min))/max) );
			} else {
				result.setValue(i, j, result.getMissing() );
			}
		}
		return result;
	}
	
	private Vector[] split(Vector[] input, double epsilon){
		int numparams = input[0].values.length;
		Vector[] output = new Vector[input.length*2];
		for (int i=0; i < input.length; ++i){
			Vector v1 = new Vector(numparams);
			Vector v2 = new Vector(numparams);
			for (int p=0; p < numparams; ++p){
				v1.values[p] = input[i].values[p] * (1+epsilon);
				v2.values[p] = input[i].values[p] * (1-epsilon);
			}
			output[2*i] = v1; 
			output[2*i+1] = v2;
		}
		return output;
	}
	
	private Vector[] computeCentroids(LatLonGrid[] params){
		int nrows = params[0].getNumLat();
		int ncols = params[0].getNumLon();
		// init new centroids at zero
		Vector[] result = new Vector[centroids.length];
		for (int i=0; i < centroids.length; ++i){
			result[i] = new Vector(params.length);
		}
		int[][] N = new int[centroids.length][params.length];

		// assign each point to closest centroid and update that center
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			// find closest centroid
			int bin_no = getBinNumber(params, i, j);
			for (int p=0; p < params.length; ++p){
				int val = params[p].getValue(i,j);
				if ( val != params[p].getMissing() ){
					result[bin_no].values[p] += val; // sum
					N[bin_no][p]++;
				}
			}
		}
		for (int i=0; i < centroids.length; ++i){
			for (int p=0; p < params.length; ++p){
				if (N[i][p] > 0){
					result[i].values[p] /= N[i][p]; // now average
				}
			}
		}

		return result;
	}
	
	public int getBinNumber(LatLonGrid[] params, int x, int y){
		// closest centroid wins
		int best = 0;
		double mindist = centroids[0].computeDist(params, x, y);
		for (int p=1; p < centroids.length; ++p){
			double dist = centroids[p].computeDist(params, x, y);
			if (dist < mindist){
				mindist = dist;
				best = p;
			}
		}
		return best;
	}
	
	public Vector getCenterValue(int bin_no){
		return centroids[bin_no];
	}
	
	/** replaces each pixel by the bin number it belongs to. */
	public LatLonGrid band(LatLonGrid[] params){
		LatLonGrid result = LatLonGrid.copyOf(params[0]);
		result.setMissing(0);
		int nrows = result.getNumLat();
		int ncols = result.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int bin_no = getBinNumber(params, i, j);
			result.setValue(i,j, bin_no+1);
		}
		return result;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("Centroids: ");
		for (int p=0; p < centroids.length; ++p){
			sb.append(centroids[p]);
		}
		return sb.toString();
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File outdir = OutputDirectory.getDefault("quantizer");

		// read input
		LatLonGrid conus = SurfaceAlbedo.read(SurfaceAlbedo.CONUS, 100);
		
		// compute a few local and texture measures
		LocalMeasures local = new LocalMeasures(conus, 11, 11);
		LatLonGrid mean = local.getMean();
		// LatLonGrid stdev = local.getStdDeviation();
		Histogram hist = HistogramBinSelection.createBasedOnRange(conus);
		StructuralMeasures texture = new StructuralMeasures(conus, 11, 11, hist.getMin(), hist.getIncr(), hist.getNumBins());
		LatLonGrid uniformity = texture.getUniformity()[0];
		
		LatLonGrid[] params = new LatLonGrid[]{ conus, mean, uniformity };
		params = normalize(params);
		
		for (int k=4; k < 32; k *= 2){ // 4, 8, 16
			VectorQuantizer quant = new VectorQuantizer(params, k);
			LatLonGrid banded = quant.band(params);
			KmlWriter.write(banded, outdir, "vecquant_" + k, PngWriter.createCoolToWarmColormap());
		}
	}
}
