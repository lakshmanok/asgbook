package edu.ou.asgbook.filters;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.linearity.DataTransform;

/**
 * Find edges in a grid.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class SobelEdgeFilter implements SpatialFilter {
	private final ConvolutionFilter gx;
	private final ConvolutionFilter gy;
	
	public SobelEdgeFilter(){
		double[][] xc = new double[3][3];
		double[][] yc = new double[3][3];
		xc[0][0] = xc[0][2] = yc[0][0] = yc[2][0] = -1;
		xc[0][1] = yc[1][0] = -2;
		xc[2][0] = xc[2][2] = yc[0][2] = yc[2][2] = 1;
		xc[2][1] = yc[1][2] = 2;
		gx = new ConvolutionFilter(xc);
		gy = new ConvolutionFilter(yc);
	}
	
	/**
	 * treat values > thresh as equal to thresh
	 */
	public LatLonGrid saturate(final LatLonGrid input, int thresh){
		LatLonGrid result = LatLonGrid.copyOf(input);
		for (int i=0; i < input.getNumLat(); ++i){
			for (int j=0; j < input.getNumLon(); ++j){
				if ( result.getValue(i, j) > thresh ){
					result.setValue(i,j, thresh);
				}
			}
		}
		return result;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return edgeFilter(input);
	}
	
	public LatLonGrid edgeFilter(final LatLonGrid input){
		return edgeFilter(input, null);
	}
	
	public LatLonGrid edgeFilter(final LatLonGrid input, File out){
		LatLonGrid g1 = gx.convolve(input);
		KmlWriter.debugWrite(g1, out, "gx");
		LatLonGrid g2 = gy.convolve(input);
		KmlWriter.debugWrite(g2, out, "gy");
		for (int i=0; i < g1.getNumLat(); ++i){
			for (int j=0; j < g1.getNumLon(); ++j){
				if (g1.getValue(i,j) != g1.getMissing() && g2.getValue(i,j) != g2.getMissing()){
					int gradient = Math.abs( g1.getValue(i, j) ) + Math.abs( g2.getValue(i,j) );
					g1.setValue(i, j, gradient);
				} else {
					g1.setValue(i,j, g1.getMissing());
				}
			}
		}
		return g1;
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("sobel");
		
		// read input
		DataTransform t = new GlobalPopulation.LogScaling();
		// DataTransform t = new GlobalPopulation.LinearScaling();
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, t).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
				
		SobelEdgeFilter filter = new SobelEdgeFilter();
		LatLonGrid edges = filter.edgeFilter(popdensity, out);
		KmlWriter.write(edges, out, "edge", PngWriter.createCoolToWarmColormap());
		
		ConvolutionFilter sm = new ConvolutionFilter(ConvolutionFilter.gauss(11, 11));
		popdensity = sm.smooth(popdensity);
		edges = filter.edgeFilter(popdensity, null);
		KmlWriter.write(edges, out, "smoothedge", PngWriter.createCoolToWarmColormap());
		
		LatLonGrid saturated = filter.saturate(edges, 5000);
		KmlWriter.write(saturated, out, "saturated", PngWriter.createCoolToWarmColormap());
	}
}
