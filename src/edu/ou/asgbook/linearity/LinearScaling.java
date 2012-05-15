/**
 * 
 */
package edu.ou.asgbook.linearity;

/**
 * Scales pixel values as Ax. This is useful since the LatLonGrid stores
 * integers.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class LinearScaling extends DataTransform {
	private double scale;

	/** Multiply input values by this amount. */
	public LinearScaling(double multiplier){
		this.scale = multiplier;
	}
	
	@Override
	public double transform(double value){
		return (scale * value);
	}

	@Override
	public double inverse(double value) {
		return (value / scale);
	}
}
