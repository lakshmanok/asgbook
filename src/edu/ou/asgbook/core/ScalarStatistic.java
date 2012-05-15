/**
 * 
 */
package edu.ou.asgbook.core;

/**
 * A utility class to compute mean, variance of a streaming set of inputs.
 * 
 * @author v.lakshmanan
 *
 */
public class ScalarStatistic {
	private double sumx = 0;
	private double sumx2 = 0;
	private int N = 0;
	private int N_at_lastcompute = 0;
	private double mean = 0;
	private double var = 0;
	private double stddev = 0;
	private double min = 0;
	private double max = 0;
	
	public void update(double x){
		sumx += x;
		sumx2 += x*x;
		if ( N == 0 ){
			min = max = x;
		} else {
			min = Math.min(min, x);
			max = Math.max(max, x);
		}
		++N;
	}
	
	public void update(double x, int relwt){
		sumx += (x*relwt);
		sumx2 += (x*x*relwt);
		if ( N == 0 ){
			min = max = x;
		} else {
			min = Math.min(min, x);
			max = Math.max(max, x);
		}
		N += relwt;
	}
	
	public void update(ScalarStatistic other){
		sumx += other.sumx;
		sumx2 += other.sumx2;
		if ( N == 0 ){
			min = other.min;
			max = other.max;
		} else if (other.N != 0){
			min = Math.min(min, other.min);
			max = Math.max(max, other.max);
		}
		N += other.N;
	}
	
	private void compute(){
		if ( N != N_at_lastcompute){ // N > 0 for this to ever happen
			mean = sumx / N;
			double meanx2 = sumx2 / N;
			// variance is really divided by N-1 to be unbiased, but okay
			var = meanx2 - mean*mean;
			if ( var > 0 ){
				stddev = Math.sqrt(var);
			} else {
				stddev = 0;
			}
			N_at_lastcompute = N;
		}
	}

	public double getMean(){
		compute();
		return mean;
	}
	
	public double getMin(){
		return min;
	}
	
	public double getMax(){
		return max;
	}
	
	public double getVariance(){
		compute();
		return var;
	}
	
	public double getStdDeviation(){
		compute();
		return stddev;
	}
	
	@Override
	public String toString(){
		return "value = " + getMean() + "+/-" + getStdDeviation() + " based on " + N + " samples";
	}

	public int getNumSamples() {
		return N;
	}
}
