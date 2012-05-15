/**
 * 
 */
package edu.ou.asgbook.linearity;

/**
 * 
 * Transform pixel values, usually to meet linearity requirements.
 * 
 * @author valliappa.lakshmanan
 *
 */
public abstract class DataTransform {
	public int transformAndRoundoff(double value){
		return (int) Math.round(transform(value));
	}
	public abstract double transform(double value);
	public abstract double inverse(double value);
}
