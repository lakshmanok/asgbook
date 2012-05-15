/**
 * 
 */
package edu.ou.asgbook.motion;

import java.io.File;
import java.util.Date;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.dataset.SeviriInfraredTemperature;
import edu.ou.asgbook.filters.SobelEdgeFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.transforms.FFT2D;
import edu.ou.asgbook.transforms.FFT.Complex;

/**
 * Estimate motion based on FFT.
 * 
 * @author v.lakshmanan
 *
 */
public class PhaseCorrelation {
	final int MAXU, MAXV;
	
	public PhaseCorrelation(int maxu, int maxv){
		this.MAXU = maxu;
		this.MAXV = maxv;
	}
	
	public Pair<Integer,Integer> compute(LatLonGrid data0, LatLonGrid data1){
		int motNS = 0, motEW = 0;
		// a
		Complex[][] in1 = FFT2D.fft(FFT2D.zeropad(data0));
		
		// zero-out an area of thickness MAXU/MAXV around the boundary to avoid boundary issues
		LatLonGrid centerb = LatLonGrid.copyOf(data1);
		int minx = MAXU;
		int miny = MAXV;
		int maxx = centerb.getNumLat() - minx;
		int maxy = centerb.getNumLon() - miny;
		for (int i=0; i < data1.getNumLat(); ++i){
			for (int j=0; j < data1.getNumLon(); ++j){
				if (i < minx || j < miny || i > maxx || j > maxy){
					centerb.setValue(i, j, 0);
				}
			}
		}
		Complex[][] in2 = FFT2D.fft(FFT2D.zeropad(centerb));
		
		// find phase shift at this point
		for (int i=0; i < in1.length; ++i) for (int j=0; j < in1[0].length; ++j){
			in1[i][j] = in1[i][j].multiply(in2[i][j].conjugate());
			in1[i][j] = in1[i][j].multiply( 1.0 / in1[i][j].norm() );
		}
		// take ifft
		Complex[][] result = FFT2D.ifft(in1);
		
		// find location at which the cross-power specturm is maximum
		double bestValue = Integer.MIN_VALUE;
		int startx = 0; // result.length/2 - MAXU;
		int starty = 0; // result[0].length/2 - MAXV;
		int endx = result.length; // /2 + MAXU;
		int endy = result[0].length; // /2 + MAXV;
		for (int i=startx; i < endx; ++i) for (int j=starty; j < endy; ++j){
			if ( result[i][j].normsq() > bestValue ){
				bestValue = result[i][j].real;
				motNS = -i;
				motEW = -j;
			}
		}
		
		// we don't want a 345-degree phase shift; we want it to be 15-degrees
		if ( Math.abs(motNS) > result.length/2 ){
			if (motNS < 0) motNS += result.length;
			else motNS -= result.length;
		}
		if ( Math.abs(motEW) > result[0].length/2 ){
			if (motEW < 0) motEW += result[0].length;
			else motEW -= result[0].length;
		}
		
		return new Pair<Integer,Integer>(motNS, motEW);
	}

	public static Pixel computeCentroid(LatLonGrid a){
		double sumx = 0;
		double sumy = 0;
		double sumwt = 0;
		int N = 0;
		for (int i=0; i < a.getNumLat(); ++i) for (int j=0; j < a.getNumLon(); ++j){
			double wt = a.getValue(i,j);
			sumx += i * wt;
			sumy += j * wt;
			sumwt += wt;
			++N;
		}
		return new Pixel((int)Math.round(sumx/sumwt), (int)Math.round(sumy/sumwt), (int)Math.round(sumwt/N));
	}
	
	public static void test(File out) throws Exception {
		// because the alignment doesn't really check lat-lon extents,
		// cropping from offset corners will look like translation ...
		LatLonGrid conus = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA);
		LatLonGrid[] grids = new LatLonGrid[2];
		grids[0] = conus.crop(900, 2500, 256, 256);
		int motx = 5; int moty = 9;
		grids[1] = conus.crop(900-motx, 2500-moty, 256, 256);
		
		// do alg
		Pair<Integer,Integer> motion = new PhaseCorrelation(30,30).compute(grids[0], grids[1]);
		System.out.println("Motion N/S ="  + motion.first + " true N/S=" + motx);
		System.out.println("Motion E/W ="  + motion.second + " true E/W=" + moty);
		
		System.out.println("Centroid of first = " + computeCentroid(grids[0]));
		System.out.println("Centroid of second = " + computeCentroid(grids[1]));
		
		KmlWriter.write(grids[0], out, "popdensityA", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(grids[1], out, "popdensityB", PngWriter.createCoolToWarmColormap());
		
		// based on edges alone
		SobelEdgeFilter edgeFilter = new SobelEdgeFilter();
		LatLonGrid edge1 = edgeFilter.edgeFilter(grids[0]);
		LatLonGrid edge2 = edgeFilter.edgeFilter(grids[1]);
		motion = new PhaseCorrelation(30,30).compute(edge1, edge2);
		System.out.println("Motion N/S ="  + motion.first + " true N/S=" + motx);
		System.out.println("Motion E/W ="  + motion.second + " true E/W=" + moty);
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("phasecorr");
		test(out);
		
		// read
		File f = new File("data/seviri");
		Pair<LatLonGrid,Date>[] grids = SeviriInfraredTemperature.readAll(f);
		
		// do alg
		Pair<Integer,Integer> uv = new PhaseCorrelation(5, 5).compute(grids[0].first, grids[1].first);
		System.out.println("u=" + uv.first + " v=" + uv.second);
	}
}
