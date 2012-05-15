/**
 * 
 */
package edu.ou.asgbook.linearity;

import java.text.DecimalFormat;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.ScalarStatistic;
import edu.ou.asgbook.dataset.GlobalPopulation;

/**
 * Given a 2D array of points, reports error measures of assuming linearity.
 * By passing in different transformations, it is possible to compare
 * potential ways of transforming the data.
 * @author valliappa.lakshmanan
 *
 */
public class LinearityVerifier {
	public static interface DataSelector {
		boolean shouldSelect(int centerval, int val_a, int val_b);
	}
	
	public static class NotMissing implements DataSelector {
		protected int missing;
		public NotMissing(int missing){
			this.missing = missing;
		}
		public boolean shouldSelect(int centerval, int val_a, int val_b){
			return centerval != missing && val_a != missing && val_b != missing;
		}
	}
	
	public static class InRange extends NotMissing {
		private int thresh0, thresh1;
		public InRange(int thresh0, int thresh1, int missing){
			super(missing);
			this.thresh0 = thresh0;
			this.thresh1 = thresh1;
		}
		public boolean shouldSelect(int centerval, int val_a, int val_b){
			return super.shouldSelect(centerval, val_a, val_b) 
				&& val_a >= thresh0 && val_a < thresh1;
		}
	}	

	/**
	 * Returns the Mean Square Error statistic
	 */
	public static ScalarStatistic verify(int[][] data, DataSelector selector, DataTransform transform, int neighSize){
		// setup
		ScalarStatistic errorstat = new ScalarStatistic();
		int nrows = data.length;
		if ( nrows == 0 ){
			return errorstat;
		}
		int ncols = data[0].length;
		if ( ncols == 0 ){
			return errorstat;
		}
		
		// find the error in every triad interpolating along rows
		for (int col=0; col < ncols; ++col){
			for (int row=neighSize; row < nrows-neighSize; ++row){
				if (selector.shouldSelect(data[row][col], data[row-neighSize][col], data[row+neighSize][col])){
					int actualValue = data[row][col];
					double trans0 = transform.transform(data[row-neighSize][col]);
					double trans1 = transform.transform(data[row+neighSize][col]);
					double trans_interp = (trans0 + trans1)/2;
					double interpValue = transform.inverse(trans_interp);
					double error = (interpValue - actualValue);
					errorstat.update(error*error);
				}
			}
		}
		
		// repeat for columns
		for (int row=0; row < nrows; ++row){
			for (int col=neighSize; col < ncols-neighSize; ++col){
				if (selector.shouldSelect(data[row][col], data[row][col-neighSize], data[row][col+neighSize])){
					int actualValue = data[row][col];
					double trans0 = transform.transform(data[row][col-neighSize]);
					double trans1 = transform.transform(data[row][col+neighSize]);
					double trans_interp = (trans0 + trans1)/2;
					double interpValue = transform.inverse(trans_interp);
					double error = (interpValue - actualValue);
					errorstat.update(error*error);
				}
			}
		}	
		
		return errorstat;
	}
	
	public static void main(String[] args) throws Exception {
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new LinearScaling(1));//.crop(900, 2500, 200, 200);;
		for (int i=0; i < popdensity.getNumLat(); ++i){
			for (int j=0; j < popdensity.getNumLon(); ++j){
				if (popdensity.getValue(i,j) < 1){
					popdensity.setValue(i, j, popdensity.getMissing());
				}
			}
		}
		
		// DataSelector selector = new NotMissing(popdensity.getMissing());
		DecimalFormat df = new DecimalFormat("0.0");
		// int maxval = new NHighest(1).findHighestValued(popdensity)[0].getValue();
		int[] neighSize = new int[]{ 1, 3, 5, 11, 21, 31, 41 };
		int[] thresh1 = new int[]{      1,  1,  50,  500,  5000, 50000 };
		int[] thresh2 = new int[]{ 500000, 50, 500, 5000, 50000, 500000 };
		final String sep = " & ";
		System.out.println("D & data range & N & RMSE (raw) & RMSE (log) \\\\ \\hline");
		for (int D : neighSize){
			for (int i=0; i < thresh1.length; ++i){
				int minval = thresh1[i];
				int maxval = thresh2[i];
				DataSelector selector = new InRange(minval, maxval, popdensity.getMissing());
				// check linearity two ways
				ScalarStatistic logstat = verify(popdensity.getData(), selector, new LogScaling(10), D);		
				ScalarStatistic rawstat = verify(popdensity.getData(), selector, new LinearScaling(100), D);
				System.out.println(D + sep + 
						minval + "-" + maxval + sep +
						rawstat.getNumSamples() + sep + 
						df.format(Math.sqrt(rawstat.getMean())) + sep +
						df.format(Math.sqrt(logstat.getMean())) + " \\\\");			
			}
			System.out.println("\\hline");
		}
	}
}
