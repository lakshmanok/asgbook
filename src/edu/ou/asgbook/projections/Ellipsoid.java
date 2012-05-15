/**
 * 
 */
package edu.ou.asgbook.projections;

/**
 * An ellipsoidal approximation to the earth
 * 
 * @author v.lakshmanan
 *
 */
public class Ellipsoid {
	/** Equatorial radius (the semi-major axis) in meters */
	public final double eqr;
	/** Square of the eccentricity */
	public final double eccsq;

	public Ellipsoid(double eqr, double eccsq) {
		super();
		this.eqr = eqr;
		this.eccsq = eccsq;
	}
	
	public static Ellipsoid WGS84(){
		return new Ellipsoid(6378137, 0.00669438);
	}
	
	public static Ellipsoid NAD27(){
		return new Ellipsoid(6378206, 0.006768658);
	}
}
