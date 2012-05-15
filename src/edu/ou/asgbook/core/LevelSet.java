/**
 * 
 */
package edu.ou.asgbook.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A representation of a spatial grid as a set of levels.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class LevelSet {
	private Map<Integer,List<Pixel>> data = new TreeMap<Integer, List<Pixel>>();
	public void add(Pixel p){
		List<Pixel> level = data.get(p.getValue());
		if ( level == null ){
			level = new ArrayList<Pixel>();
			data.put(p.getValue(), level);
		}
		level.add(p);
	}
	@SuppressWarnings("unchecked")
	public Map.Entry<Integer,List<Pixel>>[] getLevels(){
		return data.entrySet().toArray(new Map.Entry[0]);
	}
	
	/** Creates a level set out of all non missing values in grid. */
	public static LevelSet newInstance(LatLonGrid input){
		LevelSet levelset = new LevelSet();
		for (int i=0; i < input.getNumLat(); ++i){
			for (int j=0; j < input.getNumLon(); ++j){
				if ( input.getValue(i,j) != input.getMissing() ){
					levelset.add(new Pixel(i,j,input.getValue(i,j)));
				}
			}
		}
		return levelset;
	}
}
