/**
 * 
 */
package edu.ou.asgbook.rasterization;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * A Catmull-Rom spline, a local spline.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class CatmullRom {

	private static double back(double[] arr){
		return arr[arr.length-1];
	}
	/**
	 * Determines the y coordinates for the given x2 by interpolating
	 * the spline control points (x1,y1). The control points need to be sorted in x.
	 */
	public static double[] interpolate(double[] x1, double[] y1, double[] x2){
		// result: initialize at lower-bound value
		if ( x1.length == 0 ) return new double[x2.length];
		double[] y2 = new double[x2.length];
		for (int i=0; i < y2.length; ++i){
			y2[i] = y1[0];
		}

		// every interval is p1 <= p2 <= p3 where p2 is resampling position
		double p3 = x2[0] - 1; // below first value
		for (int i=0; i < x2.length; ++i){
			// find interval which contains p2
			double p2 = x2[i];
			if ( p2 <= x1[0]     ){ y2[i] = y1[0]; continue; }
			if ( p2 >= back(x1) ){ y2[i] = back(y1); continue; }
			int j = 0;
			while (j < (int)x1.length && p2 > x1[j]){ ++j; }
			--j; //if ( p2 < x1[j] ) --j;

			double p1 = x1[j];
			p3 = x1[j+1];

			// j and j+1 will be in bounds but j-1 and j+2 may not be
			int j1 = j-1; if (j1 < 0) j1 = 0;
			int j2 = j+2; if (j2 > (x1.length-1)) j2 = x1.length-1;

			// spline
			double dx  = 1.0f / (p3 - p1);
			double dx1 = 1.0f / (p3 - x1[j1]);
			double dx2 = 1.0f / (x1[j2] - p1);
			double dy  = (y1[j+1] - y1[j]) * dx;
			double yd1 = (y1[j+1] - y1[j1]) * dx1;
			double yd2 = (y1[j2] - y1[j]) * dx2;
			double a0y =  y1[j];
			double a1y =  yd1;
			double a2y =  dx *  ( 3*dy - 2*yd1 - yd2);
			double a3y =  dx*dx*(-2*dy +   yd1 + yd2);

			// cubic polynomial
			double x = p2 - p1;
			y2[i] = ((a3y*x + a2y)*x + a1y)*x + a0y;
		}
		return y2;
	}

	private static class XtoY implements Comparable<XtoY>{
		int orig_index;
		int scaledx;
		@SuppressWarnings("unused")
		int scaledy;
		@Override
		public int compareTo(XtoY b){
			XtoY a = this;
			return (a.scaledx - b.scaledx);
		}
	}

	public static double[] sort_and_interpolate(double[] x1, double[] y1, double[] x2){

		int N = x1.length;

		// create structure for sorting
		XtoY[] data = new XtoY[N];
		for (int i=0; i < N; ++i){
			data[i] = new XtoY();
			data[i].orig_index = i;
			data[i].scaledx = (int)Math.round(x1[i] * 1000 + 0.5);
			data[i].scaledy = (int)Math.round(y1[i] * 1000 + 0.5);
		}
		Arrays.sort(data);

		// create input data
		double[] x= new double[N];
		double[] y= new double[N];
		int curr = 0;
		for (int i=0; i < N; ++i){
			// if you have two or more y values for same x, then use avg
			int start_i = i;
			while ( (i+1) < N && data[i].scaledx == data[i+1].scaledx ){
				++i;
			}
			x[curr] = x1[data[i].orig_index];
			double sumy = 0;
			for (int k=start_i; k <= i; ++k){
				sumy += y1[data[k].orig_index];
			}
			y[curr] = sumy/(i-start_i+1);
			++curr;
		}

		x = Arrays.copyOf(x, curr);
		y = Arrays.copyOf(y, curr);

		// call interpolate
		return interpolate( x, y, x2 );
	}
	
	public static List<Pixel> getPositionIn(double[] controllat, double[] controllon, LatLonGrid grid){
		List<Pixel> result = new ArrayList<Pixel>();
		// we want to find the intersection at all the lat of the grid
		double[] lat2 = new double[grid.getNumLat()];
		for (int i=0; i < lat2.length; ++i){
			lat2[i] = grid.getLocation(i, 0).getLat(); // lat of row
		}
		double[] lon2 = sort_and_interpolate(controllat, controllon, lat2);
		for (int i=0; i < lon2.length; ++i){
			int col = grid.getCol(new LatLon(lat2[i],lon2[i])); // col to fill in
			result.add(new Pixel(i,col,grid.getValue(i,col)));
		}
		return result;
	}
	
	public static void main(String args[]) throws Exception {
		LatLonGrid grid = new LatLonGrid(100,100,0,new LatLon(100,-90),0.01,0.01);
		double[] controlx = new double[]{99.4,99.3,99.5,99.7};
		double[] controly = new double[]{-89.5,-89.3,-89.6,-89.4};
		
		List<Pixel> pixels = getPositionIn(controlx, controly, grid);
		for (Pixel p : pixels){
			grid.setValue(p.getRow(), p.getCol(), 10);
		}
		
		File out = OutputDirectory.getDefault("raster");
		KmlWriter.write(grid, out, "drawspline", PngWriter.createCoolToWarmColormap());
	}
}