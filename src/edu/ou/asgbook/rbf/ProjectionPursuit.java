/**
 * 
 */
package edu.ou.asgbook.rbf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.core.ScalarStatistic;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Approximates a spatial grid by a RBF when nothing is known beyond the number of Gaussians desired.
 * 
 * @author v.lakshmanan
 *
 */
public class ProjectionPursuit {
	private final int MAX_TOT_ABS_ERROR;
	private final int MAX_NUMBER_RBFS;
	private final File outDir;
	private int outputInterval;
	
	// public static final NextRBF STRATEGY = new SpatialMean();
	public static final NextRBF STRATEGY = new LocalMax();
	
	public ProjectionPursuit(LatLonGrid orig, int max_tot_abs_error, int max_number_rbfs, File outDir){
		this.MAX_TOT_ABS_ERROR = max_tot_abs_error;
		this.MAX_NUMBER_RBFS = max_number_rbfs;
		if ( MAX_NUMBER_RBFS < 10 ){
			outputInterval = 1;
		} else if ( MAX_NUMBER_RBFS < 50){
			outputInterval = 5;
		} else {
			outputInterval = 10;
		}
		this.outDir = outDir;
		fit(orig, STRATEGY);
	}
	
	public Pixel[] getCenters() {
		return centers;
	}

	public double[] getSigmax() {
		return sigmax;
	}

	public double[] getSigmay() {
		return sigmay;
	}

	private Pixel[] centers = new Pixel[0];
	private double[] sigmax = new double[0];
	private double[] sigmay = new double[0];
	private int toterr;
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < centers.length; ++i){
			sb.append("RBF#" + i + " center: " + centers[i] + " sigmax=" + sigmax[i] + " sigmay=" + sigmay[i] + "\n");
		}
		return sb.toString();
	}
	
	public interface NextRBF {
		public double[] getNewCenterAndSigmas(LatLonGrid error);
	}
	
	public static class SpatialMean implements NextRBF {
		public double[] getNewCenterAndSigmas(LatLonGrid error){
			// Find spatial mean and variance
			ScalarStatistic xstat = new ScalarStatistic();
			ScalarStatistic ystat = new ScalarStatistic();
			for (int i=0; i < error.getNumLat(); ++i) for (int j=0; j < error.getNumLon(); ++j){
				int wt = error.getValue(i,j);
				if ( wt > 0 ){
					xstat.update(i, wt);
					ystat.update(j, wt);
				}
			}
			double[] result = new double[4];
			result[0] = xstat.getMean();
			result[1] = ystat.getMean();
			result[2] = xstat.getStdDeviation();
			result[3] = ystat.getStdDeviation();
			return result;
		}
	}

	public static class LocalMax implements NextRBF {
		public double[] getNewCenterAndSigmas(LatLonGrid error){
			// Find the location of the maximum error
			int maxerr = 0;
			int x = 0;
			int y = 0;
			for (int i=0; i < error.getNumLat(); ++i) for (int j=0; j < error.getNumLon(); ++j){
				int wt = error.getValue(i,j);
				if ( wt > maxerr ){
					maxerr = wt;
					x = i;
					y = j;
				}
			}
			double[] result = new double[4];
			result[0] = x;
			result[1] = y;
			// Walk from max-error to point with half the error ("bandwidth")
			int xdist = 0;
			int ydist = 0;
			int thresh = maxerr / 2;
			for (xdist = 0; xdist < error.getNumLat(); ++xdist){
				if (error.isValid(x+xdist,y) && error.getValue(x+xdist,y) < thresh){
					break;
				}
				if (error.isValid(x-xdist,y) && error.getValue(x-xdist,y) < thresh){
					break;
				}
			}
			for (ydist = 0; ydist < error.getNumLon(); ++ydist){
				if (error.isValid(x,y+ydist) && error.getValue(x,y+ydist) < thresh){
					break;
				}
				if (error.isValid(x,y-ydist) && error.getValue(x,y-ydist) < thresh){
					break;
				}
			}
			result[2] = xdist;
			result[3] = ydist;
			return result;
		}
	}
	
	private void fit(LatLonGrid orig, NextRBF nextRBF){
		// simulate data using current centers and sigmas
		LatLonGrid curr = LatLonGrid.copyOf(orig);
		DataSimulator.simulateData(curr, centers, sigmax, sigmay);
		if (outDir != null && (centers.length % outputInterval) == 0){
			try {
				KmlWriter.write(curr, outDir, "pursuit_" + centers.length, PngWriter.createCoolToWarmColormap());
			} catch (Exception e) {
				System.err.println(e);
			}
		}
		
		// compute the error. This is what we will use to find new center
		LatLonGrid m = LatLonGrid.copyOf(orig);
		toterr = 0;
		for (int i=0; i < m.getNumLat(); ++i) for (int j=0; j < m.getNumLon(); ++j){
			int err = 0;
			if (orig.getValue(i, j) != orig.getMissing()){
				err = Math.abs( orig.getValue(i, j) - curr.getValue(i,j) );
			}
			m.setValue(i, j, err);
			toterr += err;
		}
		System.out.println("Total absolute error after " + centers.length + " RBFs is: " + toterr);
		if ( toterr < MAX_TOT_ABS_ERROR ){
			return;
		}
		
		double[] newRBF = nextRBF.getNewCenterAndSigmas(m);
		System.out.println("New Center: " + newRBF[0] + "," + newRBF[1] + " sigma=" + newRBF[2] + "," + newRBF[3]);

		// add the new center and sigma
		Pixel[] tc = new Pixel[centers.length + 1];
		double[] ts1 = new double[sigmax.length + 1];
		double[] ts2 = new double[sigmay.length + 1];
		for (int i=0; i < centers.length; ++i){
			tc[i] = centers[i];
			ts1[i] = sigmax[i];
			ts2[i] = sigmay[i];
		}
		tc[centers.length] = new Pixel((int)Math.round(newRBF[0]), (int)Math.round(newRBF[1]), 0);
		ts1[centers.length] = newRBF[2];
		ts2[centers.length] = newRBF[3];
		centers = tc;
		sigmax = ts1;
		sigmay = ts2;
		
		// fit (find amplitudes)
		double[] wt = RadialBasisFunction.fit(orig, centers, sigmax, sigmay);
		for (int i=0; i < wt.length; ++i){
			centers[i] = new Pixel(centers[i].getX(), centers[i].getY(), (int)Math.round(wt[i]));
		}
		
		if (centers.length == MAX_NUMBER_RBFS){
			if (outDir != null){
				try {
					LatLonGrid c = LatLonGrid.copyOf(orig);
					DataSimulator.simulateData(c, centers, sigmax, sigmay);
					KmlWriter.write(c, outDir, "pursuit_" + centers.length, PngWriter.createCoolToWarmColormap());
				} catch (Exception e) {
					System.err.println(e);
				}
			}
			return;
		}
		fit(orig, nextRBF); // next iteration
	}
	
	public static void runOnSimulatedInput() throws Exception {
		int nrows = 100;
		int ncols = 100;
		Pixel[] centers = new Pixel[]{ new Pixel(nrows/4,ncols/3,20), new Pixel(nrows/3,ncols/2,10) };
		double[] sigmax = new double[] { nrows/12, ncols/8 };
		double[] sigmay = new double[] { nrows/8, ncols/12 };
		LatLonGrid m = DataSimulator.simulateData(centers, sigmax, sigmay, nrows, ncols);
		System.out.println("Created data of size " + m.getNumLat() + "x" + m.getNumLon());
		for (int i=0; i < centers.length; ++i){
			System.out.println("true RBF#" + i + " center: " + centers[i] + " sigmax=" + sigmax[i] + " sigmay=" + sigmay[i]);
		}
		
		File out = OutputDirectory.getDefault("rbf");
		
		ProjectionPursuit fit = new ProjectionPursuit(m, 100, 4, out);
		System.out.println(fit);
	}
	
	public static void runOnPopDensity(boolean crop) throws Exception {
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling());
		if (crop){
			popdensity = popdensity.crop(900, 2500, 200, 200);
		} else {
			LatLon nwCorner = new LatLon(60, -130);
			LatLon seCorner = new LatLon(12, -52);
			popdensity = popdensity.crop(popdensity.getRow(nwCorner),
					popdensity.getCol(nwCorner),
					popdensity.getRow(seCorner) - popdensity.getRow(nwCorner),
					popdensity.getCol(seCorner) - popdensity.getCol(nwCorner));
		}
		
		File out = OutputDirectory.getDefault("rbfpopdensity");
		
		ProjectionPursuit fit = new ProjectionPursuit(popdensity, 1000, 9, out);
		List<LatLon> locs = new ArrayList<LatLon>();
		List<String> names = new ArrayList<String>();
		for (int i=0; i < fit.getCenters().length; ++i){
			LatLon loc = popdensity.getLocation( fit.getCenters()[i].getX(), fit.getCenters()[i].getY() );
			String name = ("RBF#" + i + " ampl=" +  fit.getCenters()[i].getValue() + " sigmax=" + fit.getSigmax()[i] + " sigmay=" + fit.getSigmax()[i]);			
			System.out.println(" loc: " + loc + name);
			if (fit.getCenters()[i].getValue() > 0){
				locs.add(loc);
				names.add(name);
			}
		}		
		KmlWriter.write(locs, names, out, "rbfcities");
	}
	
	public static void main(String[] args) throws Exception {
		// runOnSimulatedInput();
		runOnPopDensity(false); // run with -Xmx1024m otherwise, you'll get out-of-memory error
	}
}
