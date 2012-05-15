/**
 * 
 */
package edu.ou.asgbook.core;

import java.util.Comparator;

/**
 * A grid point in a spatial grid consists of a location and value.
 * @author Valliappa.Lakshmanan
 *
 */
public class Pixel implements Comparable<Pixel>{
	private final int x, y, value;

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getRow() {
		return x;
	}

	public int getCol() {
		return y;
	}

	
	public int getValue() {
		return value;
	}

	public Pixel(int x, int y, int value) {
		super();
		this.x = x;
		this.y = y;
		this.value = value;
	}
	
	@Override
	public boolean equals(Object o){
		if (o == this){
			return true;
		}
		if (o == null || !o.getClass().equals(this.getClass())){
			return false;
		}
		Pixel other = (Pixel) o;
		return (other.x == x && other.y == y && other.value == value);
	}
	
	public int getDistanceSquared(Pixel other){
		return getDistanceSquared(other.x, other.y);
	}
	
	public int getDistanceSquared(int otherx, int othery){
		int distx = this.getX() - otherx;
		int disty = this.getY() - othery;
		return (distx*distx) + (disty*disty);
	}
	
	public static class CompareLocation implements Comparator<Pixel>{
		@Override
		public int compare(Pixel a, Pixel other) {
			if ( other.x == a.x ){
				return (a.y - other.y);
			} else {
				return (a.x - other.x);
			}
		}
	}
	
	public static class CompareValue implements Comparator<Pixel>{
		@Override
		public int compare(Pixel a, Pixel other) {
			return a.value - other.value;
		}
	}

	/**
	 * Compares both location and value. To compare only based on location or based on
	 * value
	 * @see CompareValue
	 * @see CompareLocation
	 */
	@Override
	public int compareTo(Pixel other) {
		if ( other == null ){
			return 1;
		}
		if ( other.value == value ){
			if ( other.x == x ){
				return (y - other.y);
			} else {
				return (x - other.x);
			}
		} else {
			return value - other.value;
		}
	}
	
	@Override
	public String toString(){
		return new StringBuilder().append("[").append(x).append(",").append(y).append(" ").append(value).append("]").toString();
	}
	
	public static void main(String[] args){
		Pixel a = new Pixel(3,4,3);
		System.out.println("should be +ve: " + new CompareValue().compare(a,new Pixel(-1,-1,2)));
		System.out.println("should be -ve: " + new CompareValue().compare(a,new Pixel(-1,-1,4)));
		System.out.println("should be  0: " + new CompareValue().compare(a,new Pixel(-1,-1,3)));

		System.out.println("should be +ve: " + new CompareLocation().compare(a,new Pixel(2,3,13)));
		System.out.println("should be -ve: " + new CompareLocation().compare(a,new Pixel(5,6,13)));
		System.out.println("should be  0: " + new CompareLocation().compare(a,new Pixel(3,4,13)));
		
		System.out.println("should be +ve: " + a.compareTo(new Pixel(2,3,1)));
		System.out.println("should be -ve: " + a.compareTo(new Pixel(3,4,11)));
		System.out.println("should be  0: " + a.compareTo(new Pixel(3,4,3)));
	
		System.out.println("Dist = " + a.getDistanceSquared(new Pixel(4,5,6)));
	}
}
