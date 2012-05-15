/**
 * 
 */
package edu.ou.asgbook.gmm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.LevelSet;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * A parametric approximation of a spatial grid as a sum of Gaussians.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class GaussianMixtureModel {
	private List<GaussianComponent> mixture = new ArrayList<GaussianComponent>();
	
	public GaussianMixtureModel(LatLonGrid input, int numModels){
		final int MIN_DISTSQ = 100; // distance between initial centers in px
		initGMM(LevelSet.newInstance(input), numModels, MIN_DISTSQ);
		final int MAX_ITER = 10;
		final double MIN_IMPROVEMENT = 0.01; // 1 percent
		tuneGMM(input.asPixels(), MAX_ITER, MIN_IMPROVEMENT);
	}
	
	public GaussianComponent[] getMixture(){
		return mixture.toArray(new GaussianComponent[0]);
	}
	
	/**
	 * Estimates the value at each pixel in the grid based on GMM
	 * The values will be 0-1, so provide a scale in order to make them integers
	 * @param data
	 */
	public void setValueInGrid(LatLonGrid data, double scale){
		data.setMissing(0);
		double peakval = 0;
		for (int m=0; m < mixture.size(); ++m){
			peakval = Math.max( peakval, mixture.get(m).getWeight() );
		}
		for (int i=0; i < data.getNumLat(); ++i){
			for (int j=0; j < data.getNumLon(); ++j){
				double raw = 0;
				for (int m=0; m < mixture.size(); ++m){
					raw += mixture.get(m).computeProbabilityDensityAt(i,j);
				}
				data.setValue(i,j, (int) Math.round(raw*scale/peakval));
			}
		}
	}
	
	private void initGMM(LevelSet levelset, int numModels, int MIN_DISTSQ){
		// determine initial centers based on levelset i.e. peaks
		Map.Entry<Integer, List<Pixel>>[] levels = levelset.getLevels();
		List<Pixel> result = new ArrayList<Pixel>();
		int curr = levels.length;
		while (result.size() < numModels && curr > 0){
			--curr; // next
			List<Pixel> level = levels[curr].getValue(); // all pixels at this level
			// prune so that we do not add any points too close to earlier
			for (Pixel cand : level){
				boolean canAdd = true;
				for (Pixel center : result){
					int distx = cand.getX() - center.getX();
					int disty = cand.getY() - center.getY();
					int distsq = distx*distx + disty*disty;
					if (distsq < MIN_DISTSQ){
						canAdd = false;
						break; // do not add
					}
				}
				if (canAdd){
					result.add(cand);
				}
			}
		}
		// use the centers
		mixture.clear();
		numModels = Math.min(result.size(), numModels);
		for (int i=0; i < numModels; ++i){
			GaussianComponent gc = new GaussianComponent(result.get(i).getX(),result.get(i).getY(),MIN_DISTSQ,MIN_DISTSQ,0,1.0/numModels);
			mixture.add(gc);
		}
	}
	
	private double tuneGMM(Pixel[] data, int MAX_ITER, double MIN_IMPROVEMENT)
	{
	   final int n_pts = data.length;
	   final int n_models = mixture.size();
	   if ( n_models == 0 ){
	      throw new IllegalArgumentException("To use this method, the GMM must have been initialized. \n");
	   }

	   // EM algorithm from p. 263 of Principles of Data Mining (Hand et al.)
	   double[][] P_kx = new double[n_models][n_pts];
	   int iter = 0;
	   double last_log_likelihood = Integer.MIN_VALUE;
	   double improvement = 0;
	   do {

	      if ( iter != 0 ){
	         // The M-step
	         mixture.clear();
	         for (int m=0; m < n_models; ++m){
	            GaussianComponent model = new GaussianComponent(data, P_kx[m]);
	            mixture.add( model );
	            System.out.println("M-step#" + iter + " Model#" + mixture.size() + ":" + mixture.get(mixture.size()-1));
	         }
	      }

	      // The E-step: probability that x[i] came from mixture m
	      double[] pt_likelihood = new double[n_pts];
	      for (int i=0; i < n_pts; ++i){
	        for (int m=0; m < mixture.size(); ++m){
	           double raw = mixture.get(m).computeProbabilityDensityAt(data[i]);
	           P_kx[m][i] = raw;
	           pt_likelihood[i] += raw;
	        }
	      }

	      // for next M-step, make sure that P(k | x) adds up to 1 at each x
	      double log_likelihood = 0;
	      for (int i=0; i < n_pts; ++i){
	          for (int m=0; m < mixture.size(); ++m){
	             double raw =  P_kx[m][i];
	             double wt = (pt_likelihood[i] > 0.00001)? (raw / pt_likelihood[i]) : 0;
	             P_kx[m][i] = wt; // for next time
	          }
	          if (pt_likelihood[i] > 0.00001){
	        	  log_likelihood += Math.log( pt_likelihood[i] );
	          }
	      }
	      System.out.println("E-step#" + iter + ": total log-likelihood=" + log_likelihood + " from " + mixture.size() + " models.");

	      // finished?
	      improvement = (log_likelihood - last_log_likelihood) / Math.abs(log_likelihood);
	      last_log_likelihood = log_likelihood;
	      ++iter;
	   } while ( (improvement > MIN_IMPROVEMENT && iter < MAX_ITER) ); 

	   return (last_log_likelihood);
	}

	public static void main(String[] args) throws Exception {
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);

		File out = OutputDirectory.getDefault("gmmpopdensity");
		KmlWriter.write(popdensity, out, "original", PngWriter.createCoolToWarmColormap());
				
		GaussianMixtureModel gmm = new GaussianMixtureModel(popdensity, 10);
		GaussianComponent[] fit = gmm.getMixture();
		List<LatLon> locs = new ArrayList<LatLon>();
		List<String> names = new ArrayList<String>();
		for (int i=0; i < fit.length; ++i){
			LatLon loc = popdensity.getLocation( fit[i].getCx(), fit[i].getCy() );
			String name = ("GMM#" + i + " ampl=" +  fit[i].getWeight() + " sigmax=" + fit[i].getSigmax() + " sigmay=" + fit[i].getSigmay());			
			System.out.println(" loc: " + loc + name);
			locs.add(loc);
			names.add(name);
		}		
		KmlWriter.write(locs, names, out, "gmmcities");
		
		// write out the approximation
		gmm.setValueInGrid(popdensity, 500);
		KmlWriter.write(popdensity, out, "gmmapprox", PngWriter.createCoolToWarmColormap());
	}
	
}
