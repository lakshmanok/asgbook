package edu.ou.asgbook.imgstat;

import edu.ou.asgbook.core.LatLonGrid;

/**
 * Computes texture properties from a GLCM.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class GraylevelCooccurenceMatrix {
	private double[][] p;
	
	public static enum Direction {
		EASTWARD(0,1),
		SOUTHWARD(1,0), // downward
		NORTHEAST(-1,1),
		SOUTHEAST(1,-1);	
		public final int xadd;
		public final int yadd;
		private Direction(int xadd, int yadd){
			this.xadd = xadd;
			this.yadd = yadd;
		}
	}

	public GraylevelCooccurenceMatrix(LatLonGrid input, int x, int y, Direction dir, int hx, int hy, int min, int incr, int bins){
		p = new double[bins+1][bins+1]; // last bin is for missing data
		int N = 0;
		for (int m=-hx; m <= hx; ++m){
			for (int n=-hy; n <= hy; ++n){
				int value1 = input.getMissing();
				if ( input.isValid(x+m,y+n) ){
					value1 = input.getValue(x+m,y+n);
				}
				int value2 = input.getMissing();
				if ( input.isValid(x+m+dir.xadd,y+n+dir.yadd) ){
					value2 = input.getValue(x+m+dir.xadd,y+n+dir.yadd);
				}
				int bin1 = findBin(value1,input.getMissing(),min,incr,bins);
				int bin2 = findBin(value2,input.getMissing(),min,incr,bins);
				p[bin1][bin2]++;
				++N;
			}
		}
		for (int i=0; i < p.length; ++i) for (int j=0; j < p.length; ++j){
			p[i][j] /= N;
		}
	}
	int findBin(int val, int missing, int min, int incr, int bins){
		if (val != missing && val >= min) {
			int bin_no = (val - min) / incr;
			// last bin is unbounded
			if (bin_no >= bins)
				bin_no = bins - 1;
			return bin_no;
		}
		return bins; // for missing data
	}
	/*
	\item Uniformity $\Sigma_{mn} p_{mn}^2$
	\item Entropy    $\Sigma_{mn} p_{mn} \log_2{p_{mn}}$ Note that this entropy
	is different from the entropy obtained from the pixel values themselves.
	\item Maximum probability $max_{mn} p_{mn}$
	\item Difference moment, computed only for $m \ne n$: $\Sigma_{mn} p_{mn}^2/|m-n|^k$
	 */
	public double computeUniformity(){
		double result = 0;
		int N = p.length;
		for (int i=0; i < N; ++i) for (int j=0; j < N; ++j){
			result += p[i][j]*p[i][j];
		}
		return result;
	}
	
	public double computeEntropy(){
		double result = 0;
		int N = p.length;
		for (int i=0; i < N; ++i) for (int j=0; j < N; ++j){
			if (p[i][j] > 0){
				result += p[i][j]*Math.log(p[i][j])/Math.log(2);
			}
		}
		return result;
	}
	
	public double computeMaximumProbability(){
		double result = 0;
		int N = p.length;
		for (int i=0; i < N; ++i) for (int j=0; j < N; ++j){
			result = Math.max(result, p[i][j]);
		}
		return result;
	}
	
	public double computeDifferenceMoment(int order){
		double result = 0;
		int N = p.length;
		for (int i=0; i < N; ++i) for (int j=0; j < N; ++j){
			if (i != j){
				result += p[i][j]*p[i][j]/Math.pow(Math.abs(i-j),order);
			}
		}
		return result;
	}
}
