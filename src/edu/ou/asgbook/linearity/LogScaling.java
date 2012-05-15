/**
 * 
 */
package edu.ou.asgbook.linearity;

/**
 * Transforms pixel values as log(x)
 * 
 * @author valliappa.lakshmanan
 *
 */
public class LogScaling extends DataTransform {
	private double scale;

	/** Multiply log(input) values by this amount i.e. it is multiplier*log(value) */
	public LogScaling(double multiplier){
		this.scale = multiplier;
	}
	
	@Override
	public double transform(double value){
		if ( value > 1 ){
			return (scale*Math.log10(value));
		} else {
			return 0;
		}
	}

	@Override
	public double inverse(double value) {
		if ( value == 0 ){
			return 1;
		} else {
			return Math.pow(10, value/scale);
		}
	}
}
