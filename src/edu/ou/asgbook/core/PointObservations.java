package edu.ou.asgbook.core;

/**
 * A set of observation points.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class PointObservations {
	private ObservationPoint[] points;
	private final int missing;
	
	public PointObservations(ObservationPoint[] points, int missing) {
		this.points = points;
		this.missing = missing;
	}
	
	public int getMaxValue() {
		int result = missing;
		for (int i=0; i < points.length; ++i){
			if ( result == missing || points[i].getValue() > result ){
				result = points[i].getValue();
			}
		}
		return result;
	}
	
	public ObservationPoint[] getPoints() {
		return points;
	}

	public int getMissing() {
		return missing;
	}

	/**
	 * An observation point is a value at a given location.
	 * 
	 * @author valliappa.lakshmanan
	 *
	 */
	public static class ObservationPoint extends LatLon {

		private final int value;

		public ObservationPoint(double lat, double lon, int value) {
			super(lat, lon);
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		@Override
		public String toString() {
			return new StringBuilder().append("[").append(getLat()).append(",")
					.append(getLon()).append(",").append(getValue())
					.append("]").toString();
		}
	}
}
