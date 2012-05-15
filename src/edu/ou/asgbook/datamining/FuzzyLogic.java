/**
 * 
 */
package edu.ou.asgbook.datamining;

import java.text.DecimalFormat;

/**
 * 
 * A simple fuzzy logic engine
 * 
 * @author valliappa.lakshmanan
 *
 */
public class FuzzyLogic {
	public static class Fuzzy {
		private final double value; 
		public Fuzzy(double value){
			this.value = clamp(value); // 0 to 1, both inclusive
		}
		public double getValue(){
			return value;
		}
		public Fuzzy and(Fuzzy other){
			return new Fuzzy(Math.min(value,other.value));
		}
		public Fuzzy or(Fuzzy other){
			return new Fuzzy(Math.max(value,other.value));
		}
		public Fuzzy not(){
			return new Fuzzy(1-value);
		}
		@Override
		public String toString(){
			return new DecimalFormat("0.00").format(value);
		}
		private static double clamp(double value){
			if ( value <= 0 ) return 0;
			if ( value >= 1 ) return 1;
			return value;
		}
	}
	
	public interface Rule {
		public Fuzzy apply(double value);
	}
	
	public static class IsHigh implements Rule {
		private final double thresh1, thresh2;

		public IsHigh(double thresh1, double thresh2) {
			this.thresh1 = Math.min(thresh1,thresh2);
			this.thresh2 = Math.max(thresh1,thresh2);
		}

		@Override
		public Fuzzy apply(double value) {
			if ( thresh1 == thresh2 ){ // avoid divide-by-zero
				return new Fuzzy( (value < thresh1)? 0 : 1 );
			}
			double linear = (value - thresh1) / (thresh2 - thresh1);
			return new Fuzzy(linear);
		}
	}
	
	public static class IsLow implements Rule {
		private final double thresh1, thresh2;

		public IsLow(double thresh1, double thresh2) {
			this.thresh1 = Math.min(thresh1,thresh2);
			this.thresh2 = Math.max(thresh1,thresh2);
		}

		@Override
		public Fuzzy apply(double value) {
			if ( thresh1 == thresh2 ){ // avoid divide-by-zero
				return new Fuzzy( (value < thresh1)? 1 : 0 );
			}
			double linear = (thresh2 - value) / (thresh2 - thresh1);
			return new Fuzzy(linear);
		}
	}
	
	public static class IsAbout implements Rule {
		private final double center, halfrange;

		public IsAbout(double center, double range) {
			this.center = center;
			this.halfrange = Math.max(0.00001, range/2); // at least epsilon
		}

		@Override
		public Fuzzy apply(double value) {
			double fval = 0;
			if ( value < center ){
				fval = 1 - ( (center - value) / halfrange );
			} else {
				fval = 1 - ( (value - center) / halfrange );
			}
			return new Fuzzy(fval);
		}
	}

	/**
	 * Simply applies an equal weighting to all of the values.
	 * @author valliappa.lakshmanan
	 *
	 */
	public static class Aggregate {
		public Fuzzy apply(Fuzzy ... fz){
			double sum = 0;
			for (Fuzzy f : fz){
				sum += f.getValue();
			}
			return new Fuzzy(sum/fz.length);
		}
	}
}
