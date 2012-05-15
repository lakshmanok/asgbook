package edu.ou.asgbook.oban;

/**
 * Used by WeightedAverage
 * @author Valliappa.Lakshmanan
 *
 */
public interface WeightFunction {
	public static final double INVALID_WEIGHT = -100.0;

	/**
	 * Subclasses implement a weighting function. If -ve value is returned, then
	 * the point will be considered too far away and not used in weighting.
	 */
	public abstract double computeWt(double latdist, double londist);
}
