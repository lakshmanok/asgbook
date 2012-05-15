/**
 * 
 */
package edu.ou.asgbook.core;

/**
 * An utility class so that methods can return two objects.
 * 
 * @author v.lakshmanan
 *
 */
public final class Pair<X, Y> {
	public final X first;
	public final Y second;
	public Pair(X a, Y b){
		first = a;
		second = b;
	}
}
